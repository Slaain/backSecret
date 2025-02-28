package myavocat.legit.controller;

import jakarta.validation.Valid;
import myavocat.legit.dto.OfficeDTO;
import myavocat.legit.model.Office;
import myavocat.legit.model.User;
import myavocat.legit.service.UserService;
import myavocat.legit.response.ApiResponse;
import myavocat.legit.service.OfficeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/offices")
public class OfficeController {

    @Autowired
    private OfficeService officeService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ApiResponse createOffice(@Valid @RequestBody OfficeDTO officeDTO) {
        try {
            Office createdOffice = officeService.createOffice(officeDTO);
            return new ApiResponse(true, "Office created successfully", createdOffice);
        } catch (Exception e) {
            return new ApiResponse(false, "Office creation failed: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse getOfficeById(@PathVariable UUID id) {
        try {
            Office office = officeService.getOfficeById(id);
            return new ApiResponse(true, "Office found", office);
        } catch (Exception e) {
            return new ApiResponse(false, "Office not found: " + e.getMessage());
        }
    }

    @GetMapping("/name/{name}")
    public ApiResponse getOfficeByName(@PathVariable String name) {
        try {
            Office office = officeService.findByName(name);
            return new ApiResponse(true, "Office found", office);
        } catch (Exception e) {
            return new ApiResponse(false, "Office not found: " + e.getMessage());
        }
    }

    @GetMapping("/exists/{name}")
    public ApiResponse checkOfficeExists(@PathVariable String name) {
        boolean exists = officeService.existsByName(name);
        return new ApiResponse(true, exists ? "Office exists" : "Office does not exist", exists);
    }

    @GetMapping
    public ApiResponse getAllOffices() {
        try {
            List<Office> offices = officeService.getAllOffices();
            return new ApiResponse(true, "Offices retrieved successfully", offices);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to retrieve offices: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse updateOffice(@PathVariable UUID id, @Valid @RequestBody OfficeDTO officeDTO) {
        try {
            Office updatedOffice = officeService.updateOffice(id, officeDTO);
            return new ApiResponse(true, "Office updated successfully", updatedOffice);
        } catch (Exception e) {
            return new ApiResponse(false, "Office update failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse deleteOffice(@PathVariable UUID id) {
        try {
            officeService.deleteOffice(id);
            return new ApiResponse(true, "Office deleted successfully");
        } catch (Exception e) {
            return new ApiResponse(false, "Office deletion failed: " + e.getMessage());
        }
    }

    @GetMapping("/{officeId}/users")
    public ApiResponse getOfficeUsers(@PathVariable UUID officeId) {
        try {
            List<User> users = userService.getUsersByOffice(officeId);
            // Masquer les mots de passe dans la réponse
            users.forEach(user -> user.setPassword(null));
            return new ApiResponse(true, "Office users retrieved successfully", users);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to retrieve office users: " + e.getMessage());
        }
    }
}
