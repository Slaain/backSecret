package myavocat.legit.controller;

import myavocat.legit.dto.ClientDTO;
import myavocat.legit.response.ApiResponse;
import myavocat.legit.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @PostMapping
    public ApiResponse createClient(@RequestBody ClientDTO clientDTO) {
        try {
            ClientDTO createdClient = clientService.createClient(clientDTO);
            return new ApiResponse(true, "Client créé avec succès", createdClient);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la création du client: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}")
    public ApiResponse getAllClients(@PathVariable UUID userId) {
        try {
            List<ClientDTO> clients = clientService.getAllClients(userId);
            return new ApiResponse(true, "Clients récupérés", clients);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la récupération des clients: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/{clientId}")
    public ApiResponse getClientById(@PathVariable UUID userId, @PathVariable UUID clientId) {
        try {
            ClientDTO client = clientService.getClientById(clientId, userId);
            return new ApiResponse(true, "Client trouvé", client);
        } catch (Exception e) {
            return new ApiResponse(false, "Accès refusé ou client non trouvé: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/{clientId}")
    public ApiResponse deleteClient(@PathVariable UUID userId, @PathVariable UUID clientId) {
        try {
            clientService.deleteClient(clientId, userId);
            return new ApiResponse(true, "Client supprimé");
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la suppression: " + e.getMessage());
        }
    }
}
