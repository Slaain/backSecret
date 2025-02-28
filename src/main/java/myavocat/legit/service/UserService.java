package myavocat.legit.service;

import myavocat.legit.dto.UserDTO;
import myavocat.legit.model.User;
import myavocat.legit.model.Role;
import myavocat.legit.repository.UserRepository;
import myavocat.legit.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Transactional
    public User createUser(UserDTO userDTO) {
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        User user = new User();
        user.setNom(userDTO.getNom());
        user.setPrenom(userDTO.getPrenom());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword())); // Encoder le mot de passe

        // Vérifier que le rôle existe et le récupérer
        Role role = roleRepository.findByName(userDTO.getRoleName())
                .orElseThrow(() -> new RuntimeException("⚠ Rôle non trouvé en base : " + userDTO.getRoleName()));

        System.out.println("✅ Rôle récupéré : " + role.getName() + " (ID: " + role.getId() + ")"); // 🔥 Vérification

        // Assigner le rôle à l'utilisateur
        user.setRole(role);

        return userRepository.save(user);
    }

    // Récupérer tous les utilisateurs
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Mettre à jour un utilisateur
    @Transactional
    public User updateUser(UUID id, UserDTO userDTO) {
        return userRepository.findById(id).map(user -> {
            user.setNom(userDTO.getNom());
            user.setPrenom(userDTO.getPrenom());
            user.setEmail(userDTO.getEmail());

            // Mettre à jour le mot de passe seulement si un nouveau est fourni
            if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            }

            // Mise à jour du rôle si fourni
            if (userDTO.getRoleName() != null) {
                Role role = roleRepository.findByName(userDTO.getRoleName())
                        .orElseThrow(() -> new RuntimeException("⚠ Rôle non trouvé en base : " + userDTO.getRoleName()));
                user.setRole(role);
            }

            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    // ✅ Supprimer un utilisateur
    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
