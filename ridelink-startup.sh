#!/bin/bash

################################################################################
#                     RideLink Microservices Startup Script                    #
#                   Real-time Interactive Input & Refresh System               #
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
NC='\033[0m' # No Color

# Unicode Symbols
CHECK_MARK='✓'
CROSS_MARK='✗'
CIRCLE='●'
CLOCK='⏱'
DATABASE='🗄'
GEAR='⚙'
WARNING='⚠'
INFO='ℹ'
ARROW='→'

# Configuration
EUREKA_PORT=8761
GATEWAY_PORT=8080
ACCOUNT_PORT=8081
DRIVER_PORT=8082
RIDE_PORT=8083
FARE_PORT=8084

# Timeout for service startup
STARTUP_TIMEOUT=60
HEALTH_CHECK_INTERVAL=2
HEALTH_CHECK_RETRIES=$((STARTUP_TIMEOUT / HEALTH_CHECK_INTERVAL))

# Global state
DB_RESULT=""
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/logs"
mkdir -p "$LOG_DIR"

# Control flags
SHOULD_EXIT=0

################################################################################
# Utility Functions
################################################################################

print_header() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}${WHITE}       RideLink Microservices Ecosystem - Startup Script      ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_section() {
    echo -e "${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}${INFO} $1${NC}"
    echo -e "${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

print_service() {
    local service=$1
    local port=$2
    local status=$3

    case $status in
        "starting")
            echo -e "${YELLOW}${CLOCK} Starting${NC}    ${ARROW} ${WHITE}${service}${NC} (Port: ${CYAN}${port}${NC})"
            ;;
        "running")
            echo -e "${GREEN}${CHECK_MARK} Running${NC}    ${ARROW} ${WHITE}${service}${NC} (Port: ${CYAN}${port}${NC})"
            ;;
        "failed")
            echo -e "${RED}${CROSS_MARK} Failed${NC}     ${ARROW} ${WHITE}${service}${NC} (Port: ${CYAN}${port}${NC})"
            ;;
    esac
}

print_status() {
    local service=$1
    local status=$2
    local detail=$3

    if [ "$status" = "success" ]; then
        echo -e "${GREEN}  ${CHECK_MARK}${NC} ${WHITE}${service}${NC}: ${GREEN}${detail}${NC}"
    elif [ "$status" = "warning" ]; then
        echo -e "${YELLOW}  ${WARNING}${NC} ${WHITE}${service}${NC}: ${YELLOW}${detail}${NC}"
    else
        echo -e "${RED}  ${CROSS_MARK}${NC} ${WHITE}${service}${NC}: ${RED}${detail}${NC}"
    fi
}

is_port_in_use() {
    nc -z localhost $1 2>/dev/null
    return $?
}

check_service_health() {
    local port=$1
    local service_name=$2
    local attempt=1

    while [ $attempt -le $HEALTH_CHECK_RETRIES ]; do
        if curl -s -f http://localhost:${port}/actuator/health > /dev/null 2>&1; then
            return 0
        fi

        local code=$(curl -s -o /dev/null -w "%{http_code}" -m 3 http://localhost:${port}/actuator/health 2>/dev/null)
        if [ -n "$code" ] && [ "$code" != "000" ]; then
            return 0
        fi

        if is_port_in_use $port; then
            return 0
        fi

        if [ $attempt -lt $HEALTH_CHECK_RETRIES ]; then
            echo -ne "${YELLOW}.${NC}"
            sleep $HEALTH_CHECK_INTERVAL
        fi

        ((attempt++))
    done

    return 1
}

check_database_connection() {
    local service_name=$1
    local log_file="$LOG_DIR/$(echo "$service_name" | tr '[:upper:]' '[:lower:]').log"

    echo -ne "  ${CYAN}${DATABASE} Connecting to database${NC} "
    local i
    for i in 1 2 3; do
        echo -ne "${YELLOW}.${NC}"
        sleep 1
    done
    echo ""

    DB_RESULT="unknown"

    if [ ! -f "$log_file" ]; then
        return 1
    fi

    local recent=$(tail -100 "$log_file" 2>/dev/null)

    if echo "$recent" | grep -iqE 'HikariPool.*Start completed'; then
        DB_RESULT="connected"
        return 0
    fi

    if echo "$recent" | grep -iqE 'Unable to acquire JDBC Connection|Connection refused|Communications link failure|PSQLException|FATAL:|Failed to configure a DataSource|password authentication failed|Cannot load driver class'; then
        DB_RESULT="failed"
    fi

    return 1
}

get_service_logs() {
    local lower_service=$(echo "$1" | tr '[:upper:]' '[:lower:]')
    if [ -f "$LOG_DIR/${lower_service}.log" ]; then
        tail -20 "$LOG_DIR/${lower_service}.log"
    fi
}

################################################################################
# Service Handlers
################################################################################

handle_kill_all() {
    echo ""
    echo -e "${YELLOW}${WARNING} Shutting down all services...${NC}"

    # Remove traps so it doesn't fire multiple times
    trap - EXIT INT TERM

    pkill -f "spring-boot:run" 2>/dev/null

    for port in $EUREKA_PORT $GATEWAY_PORT $ACCOUNT_PORT $DRIVER_PORT $RIDE_PORT $FARE_PORT; do
        local pid
        for pid in $(lsof -ti :$port 2>/dev/null); do
            kill -9 $pid 2>/dev/null
        done
    done

    sleep 1
    echo -e "${GREEN}${CHECK_MARK} All services killed.${NC}"
    echo ""
}

handle_force_quit() {
    handle_kill_all
    SHOULD_EXIT=1
    echo -e "${CYAN}Goodbye!${NC}"
    exit 0
}

draw_footer() {
    local line1="${MAGENTA}─────────────────────────────────────────────────────────────────────────────────${NC}"
    local line2="${CYAN}${INFO} All services running in background${NC}"
    local line3="${LIGHT_GRAY}(Press Ctrl+C to stop all services)${NC}"

    echo -e "\n$line1"
    echo -e "$line2"
    echo -e "$line3"
}

start_service() {
    local service_name=$1
    local service_dir=$2
    local port=$3
    local has_db=$4
    local log_file="$LOG_DIR/$(echo "$service_name" | tr '[:upper:]' '[:lower:]').log"

    print_service "$service_name" "$port" "starting"

    if is_port_in_use $port; then
        print_status "$service_name" "warning" "Port $port already in use. Skipping..."
        echo ""
        return 1
    fi

    cd "${SCRIPT_DIR}/${service_dir}" 2>/dev/null || {
        print_status "$service_name" "error" "Directory $service_dir not found"
        cd - > /dev/null
        echo ""
        return 1
    }

    # CRITICAL FIX: Detach stdin (< /dev/null) and use nohup to prevent terminal lockups
    nohup mvn spring-boot:run < /dev/null > "$log_file" 2>&1 &
    local pid=$!
    cd - > /dev/null

    echo -ne "  ${CYAN}Waiting for startup${NC} "
    sleep 3

    if check_service_health $port "$service_name"; then
        print_service "$service_name" "$port" "running"

        if [ "$has_db" = "1" ]; then
            if check_database_connection "$service_name"; then
                print_status "$service_name" "success" "Database connected ${CHECK_MARK}"
            elif [ "$DB_RESULT" = "failed" ]; then
                print_status "$service_name" "error" "Database connection failed ${CROSS_MARK}"
            else
                print_status "$service_name" "warning" "Database status unknown (still initializing)"
            fi
        fi

        echo -e "  ${GREEN}${CHECK_MARK} Process ID: ${CYAN}${pid}${NC}"
    else
        print_service "$service_name" "$port" "failed"
        print_status "$service_name" "error" "Failed to start. Check logs:"
        echo ""
        get_service_logs "$service_name"
    fi

    echo ""
}

show_summary() {
    echo ""
    print_section "Service Summary"

    echo -e "${WHITE}Service Configuration:${NC}"
    echo -e "  ${ARROW} Eureka Server       : ${CYAN}http://localhost:${EUREKA_PORT}${NC} (Service Registry)"
    echo -e "  ${ARROW} API Gateway        : ${CYAN}http://localhost:${GATEWAY_PORT}${NC} (Entry Point)"
    echo -e "  ${ARROW} Account Service    : ${CYAN}http://localhost:${ACCOUNT_PORT}${NC}"
    echo -e "  ${ARROW} Driver Service     : ${CYAN}http://localhost:${DRIVER_PORT}${NC}"
    echo -e "  ${ARROW} Ride Service       : ${CYAN}http://localhost:${RIDE_PORT}${NC}"
    echo -e "  ${ARROW} Fare Service       : ${CYAN}http://localhost:${FARE_PORT}${NC}"

    echo ""
    echo -e "${WHITE}Log Files Location:${NC}"
    echo -e "  ${ARROW} ${CYAN}${LOG_DIR}/${NC} (All service logs stored here)"

    echo ""
}

check_prerequisites() {
    print_section "Checking Prerequisites"

    local missing=0

    if command -v mvn &> /dev/null; then
        echo -e "${GREEN}${CHECK_MARK} Maven${NC}           ${GREEN}installed${NC}"
    else
        echo -e "${RED}${CROSS_MARK} Maven${NC}           ${RED}not found${NC}"
        missing=$((missing + 1))
    fi

    if command -v java &> /dev/null; then
        local java_version=$(java -version 2>&1 | grep -oE 'version "[0-9]+' | grep -oE '[0-9]+' | head -1)
        echo -e "${GREEN}${CHECK_MARK} Java${NC}            ${GREEN}installed (v${java_version})${NC}"
    else
        echo -e "${RED}${CROSS_MARK} Java${NC}            ${RED}not found${NC}"
        missing=$((missing + 1))
    fi

    echo ""

    if [ $missing -gt 0 ]; then
        echo -e "${RED}${WARNING} Some required tools are missing. Please install them and try again.${NC}"
        return 1
    fi

    return 0
}

################################################################################
# Main Script
################################################################################

main() {
    if ! check_prerequisites; then
        exit 1
    fi

    print_header

    # Catch Ctrl+C and exit gracefully
    trap 'handle_force_quit' EXIT INT TERM

    print_section "Starting RideLink Microservices"

    echo -e "${MAGENTA}${GEAR} Phase 1: Service Registry${NC}"
    echo ""
    start_service "Eureka Server" "service-registry" "$EUREKA_PORT" 0
    sleep 5

    echo -e "${MAGENTA}${GEAR} Phase 2: API Gateway${NC}"
    echo ""
    start_service "API Gateway" "api-gateway" "$GATEWAY_PORT" 0
    sleep 2

    echo -e "${MAGENTA}${GEAR} Phase 3: Microservices${NC}"
    echo ""
    start_service "Account Service" "account-service" "$ACCOUNT_PORT" 1 &
    PID_ACC=$!
    sleep 1
    start_service "Driver Service" "driver-service" "$DRIVER_PORT" 1 &
    PID_DRV=$!
    sleep 1
    start_service "Ride Service" "ride-service" "$RIDE_PORT" 1 &
    PID_RID=$!
    sleep 1
    start_service "Fare Service" "fare-service" "$FARE_PORT" 1 &
    PID_FAR=$!

    # Wait for the service checkers to finish
    wait $PID_ACC $PID_DRV $PID_RID $PID_FAR 2>/dev/null

    show_summary
    echo -e "${GREEN}${CHECK_MARK} ${WHITE}All services have been started!${NC}"
    draw_footer

    # Keep the script alive so Ctrl+C triggers tidy shutdown of all services
    while true; do
        sleep 1
    done
}

# Run main function
main "$@"
