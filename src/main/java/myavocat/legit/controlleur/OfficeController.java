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
import myavocat.legit.dto.OfficeWithUsersDTO;
import java.util.stream.Collectors;

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
    public ApiResponse getOfficeWithUsers(@PathVariable UUID officeId) {
        try {
            // Récupérer les informations du cabinet
            Office office = officeService.getOfficeById(officeId);

            // Récupérer les utilisateurs du cabinet
            List<User> users = userService.getUsersByOffice(officeId);

            // Transformer les utilisateurs en UserInOfficeDTO
            List<OfficeWithUsersDTO.UserInOfficeDTO> userDTOs = users.stream()
                    .map(user -> new OfficeWithUsersDTO.UserInOfficeDTO(
                            user.getId(),
                            user.getNom(),
                            user.getPrenom(),
                            user.getEmail(),
                            user.getRole().getName(), // Supposant que User a un Role qui a un getName()
                            user.getCreatedAt()
                    ))
                    .collect(Collectors.toList());

            // Créer le DTO complet
            OfficeWithUsersDTO officeWithUsersDTO = new OfficeWithUsersDTO(
                    office.getId(),
                    office.getName(),
                    office.getAddress(),
                    office.getPhone(),
                    office.getEmail(),
                    office.getSiret(),
                    office.isActif(),
                    office.getCreatedAt(),
                    userDTOs
            );

            return new ApiResponse(true, "Office with users retrieved successfully", officeWithUsersDTO);
        } catch (Exception e) {
            return new ApiResponse(false, "Failed to retrieve office with users: " + e.getMessage());
        }
    }
}
