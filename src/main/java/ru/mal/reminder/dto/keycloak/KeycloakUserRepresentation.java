package ru.mal.reminder.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KeycloakUserRepresentation {
    private String id;
    private String username;
    private String email;
    private Boolean enabled;
    private Boolean emailVerified;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    public KeycloakUserRepresentation(String username, String email, Boolean enabled, Boolean emailVerified) {
        this.username = username;
        this.email = email;
        this.enabled = enabled;
        this.emailVerified = emailVerified;
    }
}
