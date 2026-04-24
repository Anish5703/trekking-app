package com.example.trekking_app.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = """
        Role assigned to a user account. Determines what the user can access.
        
        ADMIN    -> can manage users, routes, destinations, trackpoints , poi and all content
        CUSTOMER -> standard user, can browse treks and multiple routes
        """,
        enumAsRef = true
)
public enum Role {
    ADMIN,CUSTOMER
}
