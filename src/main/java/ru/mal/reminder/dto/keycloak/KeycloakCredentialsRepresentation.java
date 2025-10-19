package ru.mal.reminder.dto.keycloak;

import lombok.Data;

@Data
public class KeycloakCredentialsRepresentation {
    private String type;
    private String value;
    private Boolean temporary;

    public KeycloakCredentialsRepresentation(String type, String value, Boolean temporary) {
        this.type = type;
        this.value = value;
        this.temporary = temporary;
    }
}
