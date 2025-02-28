package myavocat.legit.service;

import myavocat.legit.dto.RoleDTO;
import myavocat.legit.exception.ResourceAlreadyExistsException;
import myavocat.legit.exception.ResourceNotFoundException;
import myavocat.legit.model.Role;
import myavocat.legit.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return convertToDTO(role);
    }

    public RoleDTO getRoleByName(String name) {
        Role role = roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + name));
        return convertToDTO(role);
    }

    @Transactional
    public RoleDTO createRole(RoleDTO roleDTO) {
        if (roleRepository.existsByName(roleDTO.getName())) {
            throw new ResourceAlreadyExistsException("Role already exists with name: " + roleDTO.getName());
        }

        Role role = new Role();
        role.setName(roleDTO.getName());
        role.setDescription(roleDTO.getDescription());

        Role savedRole = roleRepository.save(role);
        return convertToDTO(savedRole);
    }

    @Transactional
    public RoleDTO updateRole(Long id, RoleDTO roleDTO) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        // Vérifier si le nouveau nom existe déjà pour un autre rôle
        if (!role.getName().equals(roleDTO.getName()) && roleRepository.existsByName(roleDTO.getName())) {
            throw new ResourceAlreadyExistsException("Role already exists with name: " + roleDTO.getName());
        }

        role.setName(roleDTO.getName());
        role.setDescription(roleDTO.getDescription());

        Role updatedRole = roleRepository.save(role);
        return convertToDTO(updatedRole);
    }


    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        // Vérifier si des utilisateurs utilisent ce rôle
        if (!role.getUsers().isEmpty()) {
            throw new IllegalStateException("Cannot delete role with id " + id + " as it is assigned to " +
                    role.getUsers().size() + " users. Remove the role from these users first.");
        }

        roleRepository.deleteById(id);
    }

    // Méthode d'initialisation pour créer les rôles par défaut
    @Transactional
    public void initDefaultRoles() {
        createRoleIfNotExists("ADMIN", "Administrateur avec accès complet");
        createRoleIfNotExists("AVOCAT", "Peut gérer les dossiers, clients, paiements");
        createRoleIfNotExists("SECRETAIRE", "Gère les RDV, documents, courriers");
        createRoleIfNotExists("ALTERNANT", "Accès limité, peut assister les avocats");
        createRoleIfNotExists("CUSTOM", "Rôle personnalisé créé par l'admin");
    }

    private void createRoleIfNotExists(String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role(name, description);
            roleRepository.save(role);
        }
    }

    // Méthodes de conversion entre entité et DTO
    private RoleDTO convertToDTO(Role role) {
        return new RoleDTO(
                role.getId(),
                role.getName(),
                role.getDescription()
        );
    }

    private Role convertToEntity(RoleDTO roleDTO) {
        Role role = new Role();
        role.setId(roleDTO.getId());
        role.setName(roleDTO.getName());
        role.setDescription(roleDTO.getDescription());
        return role;
    }
}