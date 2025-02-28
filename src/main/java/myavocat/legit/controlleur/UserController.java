package myavocat.legit.controller;

import jakarta.validation.Valid;
import myavocat.legit.dto.UserDTO;
import myavocat.legit.model.User;
import myavocat.legit.response.ApiResponse;
import java.util.UUID;
import myavocat.legit.service.UserService;
import java.util.List;
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


    @GetMapping
    public ApiResponse getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            users.forEach(user -> user.setPassword(null)); // Masquer les mots de passe
            return new ApiResponse(true, "Users retrieved successfully", users);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to retrieve users: " + e.getMessage());
        }
    }


    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ApiResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UserDTO userDTO) {
        try {
            User updatedUser = userService.updateUser(id, userDTO);
            if (updatedUser != null) {
                updatedUser.setPassword(null);
            }
            return new ApiResponse(true, "User updated successfully", updatedUser);
        } catch (Exception e) {
            return new ApiResponse(false, "User update failed: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return new ApiResponse(true, "User deleted successfully");
        } catch (Exception e) {
            return new ApiResponse(false, "User deletion failed: " + e.getMessage());
        }
    }


    @PostMapping("/{userId}/office/{officeId}")
    public ApiResponse assignUserToOffice(@PathVariable UUID userId, @PathVariable UUID officeId) {
        try {
            User updatedUser = userService.assignUserToOffice(userId, officeId);
            return new ApiResponse(true, "User assigned to office successfully", updatedUser);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to assign user to office: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/office/{officeId}")
    public ApiResponse removeUserFromOffice(@PathVariable UUID userId, @PathVariable UUID officeId) {
        try {
            User updatedUser = userService.removeUserFromOffice(userId, officeId);
            return new ApiResponse(true, "User removed from office successfully", updatedUser);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to remove user from office: " + e.getMessage());
        }
    }
}