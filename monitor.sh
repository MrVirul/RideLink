#!/bin/bash

################################################################################
#                    RideLink Services Monitor Dashboard                       #
#                         Real-time Service Status                            #
################################################################################

# Color Codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
LIGHT_GRAY='\033[0;37m'
NC='\033[0m'

# Unicode Symbols
CHECK_MARK='✓'
CROSS_MARK='✗'
CIRCLE='●'
CLOCK='⏱'
ARROW='→'
WARNING='⚠'
INFO='ℹ'
DATABASE='🗄'

# Directory of this script (resolves symlinks, works from any CWD)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/logs"

# Services Configuration (index-aligned arrays for bash 3.x compatibility)
SERVICE_NAMES=("Eureka Server" "API Gateway" "Account Service" "Driver Service" "Ride Service" "Fare Service")
SERVICE_PORTS=(8761 8080 8081 8082 8083 8084)
SERVICE_DIRS=("service-registry" "api-gateway" "account-service" "driver-service" "ride-service" "fare-service")

################################################################################
# Utility Functions
################################################################################

check_service_status() {
    local port=$1

    if curl -s -f http://localhost:${port}/actuator/health > /dev/null 2>&1; then
        return 0
    fi

    # No actuator deployed (or secured): any HTTP response means the app is serving
    local code=$(curl -s -o /dev/null -w "%{http_code}" -m 3 http://localhost:${port}/actuator/health 2>/dev/null)
    if [ -n "$code" ] && [ "$code" != "000" ]; then
        return 0
    fi

    # Fallback: port accepting connections
    if nc -z localhost $port 2>/dev/null; then
        return 0
    fi

    return 1
}

get_cpu_usage() {
    local process=$1
    ps aux | grep "$process" | grep -v grep | awk '{print $3}' | head -1
}

get_memory_usage() {
    local process=$1
    ps aux | grep "$process" | grep -v grep | awk '{print $6}' | head -1
}

get_process_id() {
    local port=$1
    lsof -ti :$port 2>/dev/null
}

get_uptime() {
    local pid=$1
    if [ -z "$pid" ]; then
        echo "N/A"
        return
    fi

    # Get process start time
    local start_time=$(ps -o lstart= -p $pid 2>/dev/null)
    if [ -z "$start_time" ]; then
        echo "N/A"
        return
    fi

    # Calculate uptime (macOS/Linux compatible)
    local start_epoch=""
    if command -v date > /dev/null 2>&1 && [[ "$(uname)" == "Darwin" ]]; then
        start_epoch=$(date -j -f "%a %b %e %H:%M:%S %Y" "$start_time" +%s 2>/dev/null)
    else
        start_epoch=$(date -d "$start_time" +%s 2>/dev/null)
    fi

    if [ -z "$start_epoch" ]; then
        echo "N/A"
        return
    fi

    local current_epoch=$(date +%s)
    local uptime_seconds=$((current_epoch - start_epoch))

    # Convert to readable format
    local hours=$((uptime_seconds / 3600))
    local minutes=$(((uptime_seconds % 3600) / 60))
    echo "${hours}h ${minutes}m"
}

clear_screen() {
    clear
}

print_header() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}${WHITE}             RideLink Microservices - Real-time Status Monitor             ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}${LIGHT_GRAY}                        Last updated: $(date '+%Y-%m-%d %H:%M:%S')                      ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_service_row() {
    local service=$1
    local port=$2
    local status=$3
    local pid=$4
    local cpu=$5
    local memory=$6
    local uptime=$7

    # Service name (left-aligned, 25 chars)
    printf "  %-25s" "${service}"

    # Status indicator
    if [ "$status" = "UP" ]; then
        printf "${GREEN}${CHECK_MARK} UP${NC}    "
    else
        printf "${RED}${CROSS_MARK} DOWN${NC}  "
    fi

    # Port
    printf "| Port: ${CYAN}%-6s${NC} "  "$port"

    # PID
    if [ -n "$pid" ]; then
        printf "| PID: ${CYAN}%-8s${NC} "  "$pid"
    else
        printf "| PID: ${LIGHT_GRAY}%-8s${NC} "  "N/A"
    fi

    # CPU
    printf "| CPU: ${YELLOW}%-6s${NC} "  "${cpu}%"

    # Memory
    printf "| Mem: ${YELLOW}%-8s${NC} "  "${memory}KB"

    # Uptime
    printf "| Up: ${CYAN}%-12s${NC}\n"  "$uptime"
}

print_divider() {
    echo -e "${MAGENTA}══════════════════════════════════════════════════════════════════════════════════${NC}"
}

show_service_details() {
    local service=$1
    local port=$2

    echo ""
    echo -e "${CYAN}${INFO} Detailed Health Check for ${WHITE}${service}${NC}"
    print_divider

    # Get health endpoint response
    local health_response=$(curl -s http://localhost:${port}/actuator/health 2>&1)

    if [ -z "$health_response" ]; then
        echo -e "${RED}${CROSS_MARK} Unable to reach health endpoint${NC}"
        return
    fi

    echo -e "${WHITE}Health Status:${NC}"
    echo "$health_response" | python3 -m json.tool 2>/dev/null || echo "$health_response"

    # Try to get database info (if available)
    echo ""
    echo -e "${WHITE}Database Status:${NC}"
    local db_response=$(curl -s http://localhost:${port}/actuator/health/db 2>&1)

    if [ -n "$db_response" ] && [ "$db_response" != "404" ]; then
        echo "$db_response" | python3 -m json.tool 2>/dev/null || echo -e "${YELLOW}${WARNING} No database info available${NC}"
    else
        echo -e "${YELLOW}${WARNING} Database monitoring not available for this service${NC}"
    fi

    echo ""
}

display_dashboard() {
    clear_screen
    print_header

    echo -e "${WHITE}┌─ Service Status ─────────────────────────────────────────────────────────────────┐${NC}"
    echo ""

    local total_services=${#SERVICE_NAMES[@]}
    local running_services=0

    # Iterate through services
    local i
    for i in "${!SERVICE_NAMES[@]}"; do
        local service="${SERVICE_NAMES[$i]}"
        local port="${SERVICE_PORTS[$i]}"

        # Check if service is running
        if check_service_status "$port"; then
            local status="UP"
            running_services=$((running_services + 1))
        else
            local status="DOWN"
        fi

        # Get additional info
        local pid=$(get_process_id $port)
        local cpu=$(get_cpu_usage "${SERVICE_DIRS[$i]}" 2>/dev/null || echo "0.0")
        local memory=$(get_memory_usage "${SERVICE_DIRS[$i]}" 2>/dev/null || echo "0")
        local uptime=$(get_uptime "$pid")

        # Print service row
        print_service_row "$service" "$port" "$status" "$pid" "$cpu" "$memory" "$uptime"
    done

    echo ""
    print_divider

    # Summary
    local status_color=$GREEN
    local status_symbol=$CHECK_MARK
    if [ $running_services -lt $total_services ]; then
        status_color=$YELLOW
        status_symbol=$WARNING
    fi

    echo -e "${status_color}${status_symbol} Summary: ${WHITE}${running_services}/${total_services}${NC} services running"
    echo -e "${LIGHT_GRAY}└──────────────────────────────────────────────────────────────────────────────────┘${NC}"
    echo ""

    # Quick info
    echo -e "${CYAN}${INFO} Commands:${NC}"
    echo -e "  ${ARROW} Press ${CYAN}R${NC} to refresh | ${ARROW} Press ${CYAN}D${NC} for details | ${ARROW} Press ${CYAN}L${NC} for logs | ${ARROW} Press ${CYAN}Q${NC} to quit"
    echo ""
}

show_logs_menu() {
    clear_screen
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}${WHITE}                    View Service Logs                         ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    local i=0
    local service
    for service in "${SERVICE_NAMES[@]}"; do
        i=$((i + 1))
        echo -e "  ${i}. ${CYAN}${service}${NC}"
    done

    local exit_option=$((i + 1))
    echo -e "  ${exit_option}. ${CYAN}Exit${NC}"
    echo ""
    read -p "Select service (1-${exit_option}): " choice

    if [ $choice -eq $exit_option ]; then
        return
    fi

    if [ $choice -ge 1 ] && [ $choice -le $i ]; then
        local service="${SERVICE_NAMES[$((choice - 1))]}"
        local log_file="$LOG_DIR/$(echo "$service" | tr '[:upper:]' '[:lower:]').log"

        clear_screen
        echo -e "${CYAN}${INFO} Last 50 lines from ${WHITE}${service}${NC} log:${NC}"
        print_divider

        if [ -f "$log_file" ]; then
            tail -50 "$log_file" | while IFS= read -r line; do
                if [[ $line == *"ERROR"* ]] || [[ $line == *"WARN"* ]]; then
                    echo -e "${RED}${line}${NC}"
                elif [[ $line == *"INFO"* ]]; then
                    echo -e "${GREEN}${line}${NC}"
                else
                    echo "$line"
                fi
            done
        else
            echo -e "${YELLOW}${WARNING} Log file not found: ${log_file}${NC}"
        fi

        echo ""
        read -p "Press Enter to continue..."
    fi
}

show_details_menu() {
    clear_screen
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}${WHITE}                   Service Details                           ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    local i=0
    local service
    for service in "${SERVICE_NAMES[@]}"; do
        i=$((i + 1))
        echo -e "  ${i}. ${CYAN}${service}${NC}"
    done

    echo -e "  $((i + 1)). ${CYAN}Exit${NC}"
    echo ""
    read -p "Select service (1-$((i + 1))): " choice

    if [ $choice -eq $((i + 1)) ]; then
        return
    fi

    if [ $choice -ge 1 ] && [ $choice -le $i ]; then
        local service="${SERVICE_NAMES[$((choice - 1))]}"
        local port="${SERVICE_PORTS[$((choice - 1))]}"

        clear_screen
        show_service_details "$service" "$port"
        read -p "Press Enter to continue..."
    fi
}

################################################################################
# Main Interactive Loop
################################################################################

main() {
    while true; do
        display_dashboard

        read -t 5 -n 1 choice
        if [ -z "${choice:-}" ]; then
            continue
        fi

        case $choice in
            r|R)
                continue
                ;;
            d|D)
                show_details_menu
                ;;
            l|L)
                show_logs_menu
                ;;
            q|Q)
                echo -e "${CYAN}Exiting monitor...${NC}"
                exit 0
                ;;
            *)
                # Refresh automatically after 5 seconds if no input
                continue
                ;;
        esac
    done
}

# Run main function
main