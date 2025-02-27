package myavocat.legit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthErrorDTO {
    private String message;
    private String errorCode;
    private int status;
}