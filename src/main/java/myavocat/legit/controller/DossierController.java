package myavocat.legit.controller;

import myavocat.legit.dto.DossierDTO;
import myavocat.legit.model.Dossier;
import myavocat.legit.response.ApiResponse;
import myavocat.legit.service.DossierService;
import myavocat.legit.service.AdversaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

    @Autowired
    private DossierService dossierService;

    @Autowired
    private AdversaireService adversaireService;


    /**
     * Créer un nouveau dossier dans un cabinet donné
     */
    @PostMapping("/create/{userId}/{officeId}")
    public ApiResponse createDossier(@PathVariable UUID userId,
                                     @PathVariable UUID officeId,
                                     @RequestBody Dossier dossier) {
        try {
            // Récupérer clientId et adversaireId du corps de la requête (s'ils existent)
            UUID clientId = dossier.getClientId();
            UUID adversaireId = dossier.getAdversaireId();

            // Supprimer ces champs pour éviter des erreurs de mappage avant d'envoyer à la DB
            dossier.setClientId(null);
            dossier.setAdversaireId(null);

            // Créer le dossier
            Dossier createdDossier = dossierService.createDossier(dossier, userId, officeId, null, null);

            // ✅ Assigner le client au dossier s'il existe
            if (clientId != null) {
                createdDossier = dossierService.assignClientToDossier(userId, createdDossier.getId(), clientId);
            }

            // ✅ Assigner l'adversaire au dossier s'il existe
            if (adversaireId != null) {
                createdDossier = dossierService.assignAdversaireToDossier(userId, createdDossier.getId(), adversaireId);
            }

            return new ApiResponse(true, "Dossier créé avec succès et assignations effectuées", createdDossier);
        } catch (RuntimeException e) {
            return new ApiResponse(false, "Erreur lors de la création du dossier: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur inconnue: " + e.getMessage(), null);
        }
    }


    /**
     * Récupérer tous les dossiers accessibles par un utilisateur
     */

    /**
     * Récupérer tous les dossiers accessibles par un utilisateur
     */
    @GetMapping("/{userId}")
    public ApiResponse getAllDossiers(@PathVariable UUID userId) {
        try {
            List<Dossier> dossiers = dossierService.getAllDossiers(userId);

            // Trier les dossiers par date de création (du plus récent au plus ancien)
            dossiers.sort((dossier1, dossier2) -> {
                // Si une des dates est null, la placer à la fin
                if (dossier1.getCreatedAt() == null) return 1;
                if (dossier2.getCreatedAt() == null) return -1;
                // Sinon, trier par ordre décroissant (plus récent d'abord)
                return dossier2.getCreatedAt().compareTo(dossier1.getCreatedAt());
            });

            // Convertir les dossiers en DTO
            List<DossierDTO> dossierDTOs = dossiers.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return new ApiResponse(true, "Dossiers récupérés", dossierDTOs);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la récupération des dossiers: " + e.getMessage(), null);
        }
    }

    /**
     * Récupérer un dossier spécifique, si l'utilisateur y a accès
     */
    @GetMapping("/{userId}/{dossierId}")
    public ApiResponse getDossierById(@PathVariable UUID userId, @PathVariable UUID dossierId) {
        try {
            Dossier dossier = dossierService.getDossierById(dossierId, userId);
            DossierDTO dossierDTO = convertToDTO(dossier);
            return new ApiResponse(true, "Dossier trouvé", dossierDTO);
        } catch (RuntimeException e) {
            return new ApiResponse(false, e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur inattendue: " + e.getMessage(), null);
        }
    }
    // Méthode privée pour convertir un Dossier en DossierDTO
    private DossierDTO convertToDTO(Dossier dossier) {
        DossierDTO dto = new DossierDTO();

        // Informations de base
        dto.setId(dossier.getId());
        dto.setReference(dossier.getReference());
        dto.setNomDossier(dossier.getNomDossier());
        dto.setTypeAffaire(dossier.getTypeAffaire());
        dto.setStatut(dossier.getStatut());
        dto.setQualiteProcedurale(dossier.getQualiteProcedurale());
        dto.setContentieux(dossier.getContentieux());
        dto.setCreatedAt(dossier.getCreatedAt());

        // IDs des entités associées
        dto.setOfficeId(dossier.getOffice().getId());

        // Conversion du client si présent
        if (dossier.getClient() != null) {
            DossierDTO.PersonneSimpleDTO clientDTO = new DossierDTO.PersonneSimpleDTO();
            clientDTO.setId(dossier.getClient().getId());
            clientDTO.setNom(dossier.getClient().getNom());
            clientDTO.setPrenom(dossier.getClient().getPrenom());
            clientDTO.setEmail(dossier.getClient().getEmail());

            dto.setClient(clientDTO);
            dto.setClientId(dossier.getClient().getId());
        }

        // Conversion de l'adversaire si présent
        if (dossier.getAdversaire() != null) {
            DossierDTO.PersonneSimpleDTO adversaireDTO = new DossierDTO.PersonneSimpleDTO();
            adversaireDTO.setId(dossier.getAdversaire().getId());
            adversaireDTO.setNom(dossier.getAdversaire().getNom());
            adversaireDTO.setPrenom(dossier.getAdversaire().getPrenom());
            adversaireDTO.setEmail(dossier.getAdversaire().getEmail());

            dto.setAdversaire(adversaireDTO);
            dto.setAdversaireId(dossier.getAdversaire().getId());
        }

        // Conversion de l'avocat si présent
        if (dossier.getAvocat() != null) {
            DossierDTO.PersonneSimpleDTO avocatDTO = new DossierDTO.PersonneSimpleDTO();
            avocatDTO.setId(dossier.getAvocat().getId());
            avocatDTO.setNom(dossier.getAvocat().getNom());
            avocatDTO.setPrenom(dossier.getAvocat().getPrenom());
            avocatDTO.setEmail(dossier.getAvocat().getEmail());

            dto.setAvocat(avocatDTO);
            dto.setAvocatId(dossier.getAvocat().getId());
        }

        return dto;
    }
    /**
     * Récupérer tous les dossiers d'un client spécifique, si l'utilisateur y a accès
     */
    @GetMapping("/{userId}/client/{clientId}")
    public ApiResponse getDossiersByClient(@PathVariable UUID userId, @PathVariable UUID clientId) {
        try {
            // Utilisez la méthode existante pour récupérer tous les dossiers de l'utilisateur
            List<Dossier> allDossiers = dossierService.getAllDossiers(userId);

            // Filtrez pour ne garder que les dossiers du client spécifié
            List<Dossier> clientDossiers = allDossiers.stream()
                    .filter(dossier -> dossier.getClient() != null
                            && dossier.getClient().getId().equals(clientId))
                    .collect(Collectors.toList());

            // Transformation en réponse JSON (comme dans la méthode getAllDossiers)
            List<Map<String, Object>> dossiersResponse = clientDossiers.stream().map(dossier -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", dossier.getId());
                map.put("reference", dossier.getReference());
                map.put("nomDossier", dossier.getNomDossier());
                map.put("typeAffaire", dossier.getTypeAffaire());
                map.put("statut", dossier.getStatut());
                map.put("qualiteProcedurale", dossier.getQualiteProcedurale());
                map.put("createdAt", dossier.getCreatedAt());

                // Inclure l'ID de l'adversaire en plus de l'objet complet
                map.put("adversaire", dossier.getAdversaire());
                map.put("adversaireId", dossier.getAdversaire() != null ? dossier.getAdversaire().getId() : null);

                // Inclure l'ID du client
                map.put("client", dossier.getClient());
                map.put("clientId", dossier.getClient() != null ? dossier.getClient().getId() : null);

                return map;
            }).collect(Collectors.toList());

            return new ApiResponse(true, "Dossiers du client récupérés avec succès", dossiersResponse);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur lors de la récupération des dossiers: " + e.getMessage(), null);
        }
    }

    /**
     * Supprimer un dossier (seulement si l'utilisateur y est autorisé)
     */
    @DeleteMapping("/{userId}/{dossierId}")
    public ApiResponse deleteDossier(@PathVariable UUID userId, @PathVariable UUID dossierId) {
        try {
            // Dissocier les adversaires avant la suppression du dossier
            adversaireService.dissociateAdversairesFromDossier(dossierId);

            // Maintenant, supprimer le dossier
            dossierService.deleteDossier(dossierId, userId);

            return new ApiResponse(true, "Dossier dissocié et supprimé avec succès");
        } catch (RuntimeException e) {
            return new ApiResponse(false, "Erreur lors de la suppression du dossier: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur inconnue: " + e.getMessage(), null);
        }
    }


    @PatchMapping("/{userId}/{dossierId}/assign-client/{clientId}")
    public ApiResponse assignClientToDossier(
            @PathVariable UUID userId,
            @PathVariable UUID dossierId,
            @PathVariable UUID clientId) {
        try {
            Dossier updatedDossier = dossierService.assignClientToDossier(userId, dossierId, clientId);
            return new ApiResponse(true, "Client assigné au dossier avec succès", updatedDossier);
        } catch (RuntimeException e) {
            return new ApiResponse(false, e.getMessage(), null);
        }
    }

    @PatchMapping("/{userId}/{dossierId}/assign-adversaire/{adversaireId}")
    public ApiResponse assignAdversaireToDossier(
            @PathVariable UUID userId,
            @PathVariable UUID dossierId,
            @PathVariable UUID adversaireId) {
        try {
            Dossier updatedDossier = dossierService.assignAdversaireToDossier(userId, dossierId, adversaireId);
            return new ApiResponse(true, "Adversaire assigné au dossier avec succès", updatedDossier);
        } catch (RuntimeException e) {
            return new ApiResponse(false, e.getMessage(), null);
        }
    }

    /**
     * Mettre à jour le statut d'un dossier
     */
    @PutMapping("/{dossierId}/statut")
    public ApiResponse updateDossierStatut(
            @PathVariable UUID dossierId,
            @RequestBody Map<String, String> payload) {
        try {
            String statut = payload.get("statut");
            if (statut == null) {
                return new ApiResponse(false, "Le statut est requis", null);
            }

            Dossier updatedDossier = dossierService.updateDossierStatut(dossierId, statut);
            return new ApiResponse(true, "Statut du dossier mis à jour avec succès", updatedDossier);
        } catch (RuntimeException e) {
            return new ApiResponse(false, "Erreur lors de la mise à jour du statut: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(false, "Erreur inconnue: " + e.getMessage(), null);
        }
    }

    @PatchMapping("/{userId}/{dossierId}/assign-avocat")
    public ApiResponse assignAvocatToDossier(
            @PathVariable UUID userId,
            @PathVariable UUID dossierId) {
        try {
            Dossier updatedDossier = dossierService.assignAvocatToDossier(dossierId, userId);
            return new ApiResponse(true, "Avocat principal assigné au dossier avec succès", updatedDossier);
        } catch (RuntimeException e) {
            return new ApiResponse(false, e.getMessage(), null);
        }
    }

    @GetMapping("/{userId}/kpi")
    public ResponseEntity<ApiResponse> getKpiDossiers(@PathVariable UUID userId) {
        try {
            Map<String, Object> kpiData = dossierService.getKpiDossiers(userId);
            return ResponseEntity.ok(new ApiResponse(true, "KPI des dossiers récupérés", kpiData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Erreur lors de la récupération des KPI : " + e.getMessage(), null));
        }
    }

}