package com.ridelink.account_service.cotroller;

import com.ridelink.account_service.model.Role;
import com.ridelink.account_service.model.User;
import com.ridelink.account_service.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Account registration and JWT issuing")
public class AuthController {

    @Autowired
    private AuthService authService;

    public record LoginRequest(
            @Schema(description = "Registered email address", example = "geeth@example.com")
            String email,

            @Schema(description = "Plain-text password", example = "password123", format = "password")
            String password) {}

    public record SignupRequest(
            @Schema(description = "Full name of the account holder", example = "Geeth Perera")
            String name,

            @Schema(description = "Unique email address, doubles as the login username", example = "geeth@example.com")
            String email,

            @Schema(description = "Plain-text password, stored BCrypt-hashed", example = "password123", format = "password")
            String password,

            @Schema(description = "Account role. Defaults to PASSENGER when omitted", example = "DRIVER")
            Role role) {}

    @Schema(name = "UserResponse", description = "Public view of a registered account")
    public record UserResponse(
            @Schema(description = "Account id, referenced as `userId` by the other services", example = "1")
            Integer id,

            @Schema(description = "Full name of the account holder", example = "Geeth Perera")
            String name,

            @Schema(description = "Unique email address, doubles as the login username", example = "geeth@example.com")
            String email,

            @Schema(description = "Account role", example = "DRIVER")
            Role role) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
        }
    }

    @Operation(
            summary = "Register an account",
            description = """
                    Creates a `DRIVER` or `PASSENGER` account. The email must be unique.

                    No request field is validated yet, so a malformed body is stored as sent
                    and a duplicate email surfaces as a 500 from the database constraint.

                    The response is a `UserResponse`, not the `User` entity: the BCrypt hash
                    and the Spring Security `UserDetails` fields are not exposed.
                    """
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "name": "Geeth Perera",
                                      "email": "geeth@example.com",
                                      "role": "DRIVER"
                                    }"""))),
            @ApiResponse(responseCode = "500", description = "Email already registered - the unique constraint fails and nothing handles it",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-09-26T07:27:07.208Z",
                                      "status": 500,
                                      "error": "Internal Server Error",
                                      "path": "/api/v1/auth/signup"
                                    }""")))
    })
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody SignupRequest request){
        User registeredUser = authService.registerUser(request);
        return ResponseEntity.ok(UserResponse.from(registeredUser));
    }

    @Operation(
            summary = "Log in and receive a JWT",
            description = """
                    Authenticates the credentials and returns a signed JWT.

                    The response body is the **raw token string** (`text/plain`) - not a JSON
                    object, so there is no `token` field to unwrap. The token carries the
                    account role as a claim and expires after 24 hours.

                    Send it on subsequent requests as `Authorization: Bearer <token>`, or
                    paste it into the **Authorize** dialog in Swagger UI.

                    Bad credentials answer `403` rather than `401`, because no
                    `AuthenticationEntryPoint` is registered on the security chain.
                    """
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated - body is the raw JWT string",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRFJJVkVSIiwic3ViIjoiZ2VldGhAZXhhbXBsZS5jb20ifQ.4gJ0k3s1n-1cT8xJ2kQ9vZ0hW5rXbN7mLqYdA3eF6gH8jK1lO0pQ"))),
            @ApiResponse(responseCode = "403", description = "Unknown email or wrong password",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-09-26T07:26:23.458Z",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "path": "/api/v1/auth/login"
                                    }""")))
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        String token = authService.authenticateAndGetToken(request.email(), request.password());
        return ResponseEntity.ok(token);
    }
}
