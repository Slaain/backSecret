package myavocat.legit.service;

import myavocat.legit.dto.PaiementDTO;
import myavocat.legit.dto.CreatePaiementDTO;
import myavocat.legit.model.Paiement;
import myavocat.legit.model.Facture;
import myavocat.legit.model.User;
import myavocat.legit.model.ModePaiement;
import myavocat.legit.repository.PaiementRepository;
import myavocat.legit.repository.FactureRepository;
import myavocat.legit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaiementService {

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FactureService factureService;

    // 🔥 CRÉATION ET MODIFICATION DE PAIEMENTS

    /**
     * Créer un nouveau paiement et mettre à jour automatiquement le statut de la facture
     */
    @Transactional
    public PaiementDTO creerPaiement(UUID userId, CreatePaiementDTO createPaiementDTO) {
        // Vérifier l'utilisateur
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Vérifier la facture et l'accès
        Facture facture = factureRepository.findById(createPaiementDTO.getFactureId())
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));

        if (!facture.getDossier().getOffice().getId().equals(user.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : cette facture n'appartient pas à votre cabinet");
        }

        // Vérifier que le montant ne dépasse pas le restant dû
        BigDecimal montantRestant = facture.getMontantRestantDu();
        if (createPaiementDTO.getMontant().compareTo(montantRestant) > 0) {
            throw new RuntimeException("Le montant du paiement (" + createPaiementDTO.getMontant() +
                    "€) dépasse le montant restant dû (" + montantRestant + "€)");
        }

        // Créer le paiement
        Paiement paiement = new Paiement();
        paiement.setFacture(facture);
        paiement.setMontant(createPaiementDTO.getMontant());
        paiement.setDatePaiement(createPaiementDTO.getDatePaiement());
        paiement.setModePaiement(createPaiementDTO.getModePaiement());
        paiement.setReference(createPaiementDTO.getReference());
        paiement.setNotes(createPaiementDTO.getNotes());

        // Sauvegarder le paiement
        Paiement savedPaiement = paiementRepository.save(paiement);

        // 🔥 AUTOMATIQUE : Recalculer le statut de la facture
        factureService.recalculerStatutPaiement(facture.getId());

        return convertToDTO(savedPaiement);
    }

    /**
     * Modifier un paiement existant
     */
    @Transactional
    public PaiementDTO modifierPaiement(UUID userId, UUID paiementId, CreatePaiementDTO updateData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

        // Vérifier l'accès
        if (!paiement.getFacture().getDossier().getOffice().getId().equals(user.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : ce paiement n'appartient pas à votre cabinet");
        }

        // Mettre à jour les champs
        paiement.setMontant(updateData.getMontant());
        paiement.setDatePaiement(updateData.getDatePaiement());
        paiement.setModePaiement(updateData.getModePaiement());
        paiement.setReference(updateData.getReference());
        paiement.setNotes(updateData.getNotes());

        Paiement updatedPaiement = paiementRepository.save(paiement);

        // 🔥 AUTOMATIQUE : Recalculer le statut de la facture
        factureService.recalculerStatutPaiement(paiement.getFacture().getId());

        return convertToDTO(updatedPaiement);
    }

    /**
     * Supprimer un paiement
     */
    @Transactional
    public void supprimerPaiement(UUID userId, UUID paiementId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

        // Vérifier l'accès
        if (!paiement.getFacture().getDossier().getOffice().getId().equals(user.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : ce paiement n'appartient pas à votre cabinet");
        }

        UUID factureId = paiement.getFacture().getId();

        // Supprimer le paiement
        paiementRepository.delete(paiement);

        // 🔥 AUTOMATIQUE : Recalculer le statut de la facture
        factureService.recalculerStatutPaiement(factureId);
    }

    // 🔥 CONSULTATION DES PAIEMENTS

    /**
     * Récupérer tous les paiements d'une facture
     */
    public List<PaiementDTO> getPaiementsByFacture(UUID userId, UUID factureId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));

        // Vérifier l'accès
        if (!facture.getDossier().getOffice().getId().equals(user.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : cette facture n'appartient pas à votre cabinet");
        }

        List<Paiement> paiements = paiementRepository.findByFactureIdOrderByDatePaiementDesc(factureId);
        return paiements.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Récupérer un paiement par ID
     */
    public PaiementDTO getPaiementById(UUID userId, UUID paiementId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

        // Vérifier l'accès
        if (!paiement.getFacture().getDossier().getOffice().getId().equals(user.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : ce paiement n'appartient pas à votre cabinet");
        }

        return convertToDTO(paiement);
    }

    /**
     * Récupérer tous les paiements du cabinet
     */
    public List<PaiementDTO> getAllPaiements(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        List<Paiement> paiements = paiementRepository.findAllByOfficeId(user.getOffice().getId());
        return paiements.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Récupérer les paiements par période
     */
    public List<PaiementDTO> getPaiementsByPeriode(UUID userId, LocalDate dateDebut, LocalDate dateFin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        List<Paiement> paiements = paiementRepository.findByOfficeIdAndDateBetween(
                user.getOffice().getId(), dateDebut, dateFin);
        return paiements.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 🔥 STATISTIQUES ET KPI

    /**
     * Statistiques des paiements pour un cabinet
     */
    public Map<String, Object> getStatistiquesPaiements(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        UUID officeId = user.getOffice().getId();

        // Total encaissé
        BigDecimal totalEncaisse = paiementRepository.sumMontantByOfficeId(officeId);

        // Paiements du mois
        LocalDate debutMois = LocalDate.now().withDayOfMonth(1);
        LocalDate finMois = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        BigDecimal totalMois = paiementRepository.sumMontantByOfficeIdAndDateBetween(officeId, debutMois, finMois);

        // Paiements de la semaine
        LocalDate debutSemaine = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        LocalDate finSemaine = debutSemaine.plusDays(6);
        BigDecimal totalSemaine = paiementRepository.sumMontantByOfficeIdAndDateBetween(officeId, debutSemaine, finSemaine);

        // Répartition par mode de paiement
        List<Object[]> repartitionModes = paiementRepository.countPaiementsByModePaiementAndOfficeId(officeId);

        return Map.of(
                "totalEncaisse", totalEncaisse != null ? totalEncaisse : BigDecimal.ZERO,
                "totalMois", totalMois != null ? totalMois : BigDecimal.ZERO,
                "totalSemaine", totalSemaine != null ? totalSemaine : BigDecimal.ZERO,
                "repartitionModesPaiement", repartitionModes
        );
    }

    /**
     * Récapitulatif des paiements d'une facture
     */
    public Map<String, Object> getRecapitulatifPaiements(UUID userId, UUID factureId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));

        // Vérifier l'accès
        if (!facture.getDossier().getOffice().getId().equals(user.getOffice().getId())) {
            throw new RuntimeException("Accès refusé");
        }

        List<Paiement> paiements = paiementRepository.findByFactureIdOrderByDatePaiementDesc(factureId);
        BigDecimal totalPaye = paiementRepository.sumMontantByFactureId(factureId);

        return Map.of(
                "nombrePaiements", paiements.size(),
                "totalPaye", totalPaye != null ? totalPaye : BigDecimal.ZERO,
                "montantReclame", facture.getMontantReclame(),
                "montantRestant", facture.getMontantRestantDu(),
                "paiements", paiements.stream().map(this::convertToDTO).collect(Collectors.toList()),
                "dernierPaiement", paiements.isEmpty() ? null : convertToDTO(paiements.get(0))
        );
    }

    // 🔥 MÉTHODES UTILITAIRES

    /**
     * Conversion Paiement → PaiementDTO
     */
    private PaiementDTO convertToDTO(Paiement paiement) {
        Facture facture = paiement.getFacture();
        return new PaiementDTO(
                paiement.getId(),
                facture.getId(),
                paiement.getMontant(),
                paiement.getDatePaiement(),
                paiement.getModePaiement(),
                paiement.getReference(),
                paiement.getNotes(),
                facture.getNumeroFacture(),
                facture.getIntitule(),
                facture.getClient().getNom(),
                facture.getClient().getPrenom(),
                paiement.getCreatedAt(),
                paiement.getUpdatedAt()
        );
    }

    /**
     * Valider qu'un paiement peut être créé
     */
    private void validerPaiement(CreatePaiementDTO paiementDTO, Facture facture) {
        if (paiementDTO.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant doit être positif");
        }

        if (paiementDTO.getDatePaiement().isAfter(LocalDate.now())) {
            throw new RuntimeException("La date de paiement ne peut pas être dans le futur");
        }

        BigDecimal montantRestant = facture.getMontantRestantDu();
        if (paiementDTO.getMontant().compareTo(montantRestant) > 0) {
            throw new RuntimeException("Le montant dépasse le restant dû (" + montantRestant + "€)");
        }
    }
}
