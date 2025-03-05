package myavocat.legit.service;

import myavocat.legit.dto.ClientDTO;
import myavocat.legit.model.Client;
import myavocat.legit.model.Dossier;
import myavocat.legit.model.Office;
import myavocat.legit.model.User;
import myavocat.legit.repository.ClientRepository;
import myavocat.legit.repository.DossierRepository;
import myavocat.legit.repository.OfficeRepository;
import myavocat.legit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public ClientDTO createClient(ClientDTO clientDTO) {
        // Vérifier que les champs obligatoires ne sont pas vides
        if (clientDTO.getQualite() == null || clientDTO.getType() == null) {
            throw new RuntimeException("Les champs 'qualite' et 'type' sont obligatoires.");
        }

        Office office = officeRepository.findById(clientDTO.getOfficeId())
                .orElseThrow(() -> new RuntimeException("Cabinet introuvable"));

        Client client = new Client();
        client.setNom(clientDTO.getNom());
        client.setPrenom(clientDTO.getPrenom());
        client.setEmail(clientDTO.getEmail());
        client.setTelephone(clientDTO.getTelephone());
        client.setType(clientDTO.getType());
        client.setQualite(clientDTO.getQualite());
        client.setCommune(clientDTO.getCommune());
        client.setOffice(office); // ✅ Attribution du cabinet

        Client savedClient = clientRepository.save(client);
        return convertToDTO(savedClient);
    }

    @Transactional(readOnly = true)
    public List<ClientDTO> getAllClients(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        UUID officeId = user.getOffice().getId();

        // Récupérer les clients ayant au moins un dossier dans le cabinet de l'utilisateur
        List<Dossier> dossiers = dossierRepository.findAll().stream()
                .filter(dossier -> dossier.getOffice().getId().equals(officeId))
                .collect(Collectors.toList());

        System.out.println("Nombre de dossiers trouvés pour l'office " + officeId + ": " + dossiers.size());

        List<Client> clients = dossiers.stream()
                .map(Dossier::getClient)
                .filter(client -> client != null)  // Cette ligne filtre les clients null
                .distinct()
                .collect(Collectors.toList());

        System.out.println("Nombre de clients uniques et non-null: " + clients.size());

        return clients.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientDTO getClientById(UUID clientId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // Vérifier que ce client est lié à un dossier du même cabinet que l'utilisateur
        boolean clientAccessible = dossierRepository.findByClientId(clientId).stream()
                .anyMatch(dossier -> dossier.getOffice().getId().equals(user.getOffice().getId()));

        if (!clientAccessible) {
            throw new RuntimeException("Accès refusé : ce client appartient à un autre cabinet.");
        }

        return convertToDTO(client);
    }

    @Transactional
    public void deleteClient(UUID clientId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // Vérifier que ce client n'a plus aucun dossier actif dans le cabinet de l'utilisateur
        boolean isLinkedToDossier = dossierRepository.findByClientId(clientId).stream()
                .anyMatch(dossier -> dossier.getOffice().getId().equals(user.getOffice().getId()));

        if (isLinkedToDossier) {
            throw new RuntimeException("Impossible de supprimer ce client : il est encore lié à un dossier actif.");
        }

        clientRepository.deleteById(clientId);
    }

    private ClientDTO convertToDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setNom(client.getNom());
        dto.setPrenom(client.getPrenom());
        dto.setEmail(client.getEmail());
        dto.setTelephone(client.getTelephone());
        dto.setType(client.getType());
        dto.setQualite(client.getQualite());
        dto.setCommune(client.getCommune());
        dto.setOfficeId(client.getOffice().getId()); // ✅ Retourne l'ID du cabinet
        return dto;
    }
}