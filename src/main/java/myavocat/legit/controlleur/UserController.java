package myavocat.legit.controller;

import jakarta.validation.Valid;
import myavocat.legit.dto.UserDTO;
import myavocat.legit.model.User;
import myavocat.legit.response.ApiResponse;
import java.util.UUID;
import myavocat.legit.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse createUser(@Valid @RequestBody UserDTO userDTO) {
        try {
            // Créer le nouvel utilisateur
            User createdUser = userService.createUser(userDTO);

            // Ne pas renvoyer le mot de passe dans la réponse
            if (createdUser != null) {
                createdUser.setPassword(null);
            }

            return new ApiResponse(true, "User created successfully", createdUser);
        } catch (Exception e) {
            return new ApiResponse(false, "User creation failed: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse getUserById(@PathVariable UUID id) {
        try {
            User user = userService.getUserById(id);
            user.setPassword(null);
            return new ApiResponse(true, "User found", user);
        } catch (Exception e) {
            return new ApiResponse(false, "User not found: " + e.getMessage());
        }
    }

    @GetMapping("/email/{email}")
    public ApiResponse getUserByEmail(@PathVariable String email) {
        try {
            User user = userService.findByEmail(email);
            user.setPassword(null);
            return new ApiResponse(true, "User found", user);
        } catch (Exception e) {
            return new ApiResponse(false, "User not found: " + e.getMessage());
        }
    }

    @GetMapping("/exists/{email}")
    public ApiResponse checkUserExists(@PathVariable String email) {
        boolean exists = userService.existsByEmail(email);
        return new ApiResponse(true, exists ? "User exists" : "User does not exist", exists);
    }

}
