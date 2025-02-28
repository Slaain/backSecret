package myavocat.legit.service;

import myavocat.legit.dto.UserDTO;
import myavocat.legit.model.Role;
import myavocat.legit.model.User;
import myavocat.legit.model.Office;
import myavocat.legit.repository.RoleRepository;
import myavocat.legit.repository.UserRepository;
import myavocat.legit.repository.OfficeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

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
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        // 🔍 Log pour voir si `roleName` est bien reçu
        System.out.println("🔍 Recherche du rôle : " + userDTO.getRoleName());

        Role role = roleRepository.findByName(userDTO.getRoleName())
                .orElseThrow(() -> new RuntimeException("❌ Rôle introuvable : " + userDTO.getRoleName()));

        System.out.println("✅ Rôle trouvé : " + role.getName());

        user.setRole(role);

        return userRepository.save(user);
    }


    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUser(UUID id, UserDTO userDTO) {
        return userRepository.findById(id).map(user -> {
            user.setNom(userDTO.getNom());
            user.setPrenom(userDTO.getPrenom());
            user.setEmail(userDTO.getEmail());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User assignUserToOffice(UUID userId, UUID officeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new RuntimeException("Office not found with id: " + officeId));

        user.setOffice(office);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByOffice(UUID officeId) {
        return userRepository.findByOfficeId(officeId);
    }

    @Transactional
    public User removeUserFromOffice(UUID userId, UUID officeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new RuntimeException("Office not found with id: " + officeId));

        if (user.getOffice() == null || !user.getOffice().getId().equals(officeId)) {
            throw new RuntimeException("User is not assigned to this office");
        }

        user.setOffice(null);
        return userRepository.save(user);
    }
}
