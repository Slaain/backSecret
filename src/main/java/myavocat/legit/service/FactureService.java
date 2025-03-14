package myavocat.legit.service;

import myavocat.legit.dto.FactureDTO;
import myavocat.legit.model.Client;
import myavocat.legit.model.Dossier;
import myavocat.legit.model.Facture;
import myavocat.legit.model.StatutPaiement;
import myavocat.legit.repository.ClientRepository;
import myavocat.legit.repository.DossierRepository;
import myavocat.legit.repository.FactureRepository;
import myavocat.legit.repository.UserRepository;
import myavocat.legit.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FactureService {

    private final FactureRepository factureRepository;
    private final ClientRepository clientRepository;
    private final DossierRepository dossierRepository;
    private final UserRepository userRepository;

    public FactureService(FactureRepository factureRepository, ClientRepository clientRepository, DossierRepository dossierRepository, UserRepository userRepository) {
        this.factureRepository = factureRepository;
        this.clientRepository = clientRepository;
        this.dossierRepository = dossierRepository;
        this.userRepository = userRepository;
    }

    private String genererNumeroFacture(Client client) {
        String initials = (client.getNom().substring(0, 1) + client.getPrenom().substring(0, 1)).toUpperCase();
        int currentYear = LocalDateTime.now().getYear();

        // Recherche la dernière facture de l'année en cours
        String pattern = initials + "-" + currentYear + "-%";
        String lastFacture = factureRepository.findLastFactureByClient(pattern);

        int lastNum = 0;
        if (lastFacture != null) {
            try {
                lastNum = Integer.parseInt(lastFacture.substring(lastFacture.lastIndexOf('-') + 1));
            } catch (NumberFormatException ignored) {}
        }

        return initials + "-" + currentYear + "-" + String.format("%03d", lastNum + 1);
    }


    @Transactional
    public FactureDTO creerFacture(UUID userId, UUID clientId, UUID dossierId, String intitule, BigDecimal montantHt, boolean tvaApplicable) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable avec l'ID: " + clientId));

        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable avec l'ID: " + dossierId));

        if (!dossier.getClient().getId().equals(client.getId())) {
            throw new RuntimeException("Le dossier sélectionné n'est pas associé à ce client.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID: " + userId));

        if (!dossier.getOffice().getId().equals(user.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : l'utilisateur ne peut pas créer une facture pour ce dossier.");
        }

        String numeroFacture = genererNumeroFacture(client);
        BigDecimal montantTtc = tvaApplicable ? montantHt.multiply(BigDecimal.valueOf(1.2)) : montantHt;

        Facture facture = new Facture();
        facture.setClient(client);
        facture.setDossier(dossier);
        facture.setNumeroFacture(numeroFacture);
        facture.setIntitule(intitule);
        facture.setMontantHt(montantHt);
        facture.setMontantTtc(montantTtc);
        facture.setTvaApplicable(tvaApplicable);
        facture.setStatutPaiement(StatutPaiement.ATTENTE_REGLEMENT);
        facture.setDateEmission(LocalDateTime.now());

        return convertToDTO(factureRepository.save(facture));
    }

    public List<FactureDTO> getAllFactures(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        UUID officeId = user.getOffice().getId();

        return factureRepository.findAllByOffice(officeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public FactureDTO getFactureById(UUID userId, UUID id) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        UUID officeId = user.getOffice().getId();

        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));

        if (!facture.getDossier().getOffice().getId().equals(officeId)) {
            throw new RuntimeException("Accès refusé : cette facture n'appartient pas à votre cabinet.");
        }

        return convertToDTO(facture);
    }

    @Transactional
    public FactureDTO updateStatutFacture(UUID userId, UUID id, StatutPaiement statut) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));

        facture.setStatutPaiement(statut);
        return convertToDTO(factureRepository.save(facture));
    }

    private FactureDTO convertToDTO(Facture facture) {
        return new FactureDTO(
                facture.getId(),
                facture.getNumeroFacture(),
                facture.getIntitule(),
                facture.getDateEmission(),
                facture.getMontantHt(),
                facture.getMontantTtc(),
                facture.getStatutPaiement(),
                facture.getModePaiement(),
                facture.getDossier().getReference(),
                facture.getDossier().getNomDossier(),
                facture.getDossier().getStatut(),
                facture.getClient().getNom(),
                facture.getClient().getPrenom()
        );
    }


    public Map<String, BigDecimal> getStatistiquesFactures(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        UUID officeId = user.getOffice().getId();

        List<Facture> factures = factureRepository.findAllByOffice(officeId);

        BigDecimal totalEmis = factures.stream().map(Facture::getMontantTtc).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRegle = factures.stream()
                .filter(f -> f.getStatutPaiement() == StatutPaiement.REGLEE)
                .map(Facture::getMontantTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEnAttente = totalEmis.subtract(totalRegle);

        return Map.of(
                "Total Facturé", totalEmis,
                "Total Réglé", totalRegle,
                "Total en Attente", totalEnAttente
        );
    }
    public void relancerFacturesImpayees(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        UUID officeId = user.getOffice().getId();

        List<Facture> facturesEnRetard = factureRepository.findFacturesEnRetard(officeId);

        for (Facture facture : facturesEnRetard) {
            System.out.println("⚠ Relance envoyée au client " + facture.getClient().getNom() + " pour la facture " + facture.getNumeroFacture());
            // Ici, on pourrait envoyer un email de relance au client
        }
    }


    /**
     * Récupérer toutes les factures associées à un dossier
     * @param userId ID de l'utilisateur faisant la demande
     * @param dossierId ID du dossier
     * @return Liste des factures du dossier
     */
    public List<FactureDTO> getFacturesByDossier(UUID userId, UUID dossierId) {
        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Vérifier l'appartenance au même cabinet
        UUID officeId = user.getOffice().getId();

        // Récupérer les factures associées à ce dossier
        List<Facture> factures = factureRepository.findByDossierId(dossierId);

        // Filtrer pour ne garder que les factures auxquelles l'utilisateur a accès
        List<Facture> accessibleFactures = factures.stream()
                .filter(facture -> facture.getDossier().getOffice().getId().equals(officeId))
                .collect(Collectors.toList());

        // Convertir en DTOs
        return convertToFactureDTOList(accessibleFactures);
    }

    // Méthode utilitaire pour convertir une liste de Factures en une liste de FactureDTOs
    private List<FactureDTO> convertToFactureDTOList(List<Facture> factures) {
        return factures.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
