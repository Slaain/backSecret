package myavocat.legit.controller;

import jakarta.validation.Valid;
import myavocat.legit.dto.RoleDTO;
import myavocat.legit.model.User;
import myavocat.legit.response.ApiResponse;
import java.util.UUID;
import myavocat.legit.exception.ResourceNotFoundException;
import myavocat.legit.service.RoleService;
import myavocat.legit.service.UserService;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleService roleService;

    @PostMapping
    public ApiResponse createRole(@Valid @RequestBody RoleDTO roleDTO) {
        try{
            RoleDTO createdRole = roleService.createRole(roleDTO);

            return new ApiResponse(true, "Role created successfully", null);
        }catch(Exception e){
            return new ApiResponse(false, "Error while creating role", null);
        }
    }

    @GetMapping
    public ApiResponse getAllRoles(){
        try{
            List<RoleDTO> roles = roleService.getAllRoles();
            return new ApiResponse(true, "Users retrieved successfully",roles);
        }catch(Exception e){
            return new ApiResponse(false, "Error while retrieving roles", null);
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return new ApiResponse(true, "Role deleted successfully", null);
        } catch (ResourceNotFoundException e) {
            // Cas où le rôle n'existe pas
            return new ApiResponse(false, e.getMessage(), null);
        } catch (IllegalStateException e) {
            // Cas où le rôle est assigné à des utilisateurs
            return new ApiResponse(false, e.getMessage(), null);
        } catch (Exception e) {
            // Log l'exception pour le débogage
            logger.error("Unexpected error when deleting role with id " + id, e);
            return new ApiResponse(false, "An unexpected error occurred: " + e.getMessage(), null);
        }
    }

    @PutMapping("/{id}")
    public ApiResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO roleDTO) {
        try {
            RoleDTO updatedRole = roleService.updateRole(id, roleDTO);
            return new ApiResponse(true, "Role updated successfully", updatedRole);
        } catch (Exception e) {
            return new ApiResponse(false, "Error while updating role", null);
        }
    }
}