package ru.mal.reminder.dto.keycloak;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakCredentialsRepresentation {
    private String type;
    private String value;
    private Boolean temporary;

}
