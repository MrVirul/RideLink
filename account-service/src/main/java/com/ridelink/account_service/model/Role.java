package com.ridelink.account_service.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Role", description = "The kind of account, which decides role-based access")
public enum Role {
    @Schema(description = "Drives rides. Requires a JWT carrying `ROLE_DRIVER` on /api/v1/driver/**")
    DRIVER,

    @Schema(description = "Requests rides. Requires a JWT carrying `ROLE_PASSENGER` on /api/v1/passenger/**")
    PASSENGER
}
