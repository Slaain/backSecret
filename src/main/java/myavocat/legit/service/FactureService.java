package myavocat.legit.service;

import myavocat.legit.dto.FactureDTO;
import myavocat.legit.model.*;
import myavocat.legit.model.StatutPaiement;
import myavocat.legit.repository.ClientRepository;
import myavocat.legit.repository.DossierRepository;
import myavocat.legit.repository.FactureRepository;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import myavocat.legit.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
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
            } catch (NumberFormatException ignored) {
            }
        }

        return initials + "-" + currentYear + "-" + String.format("%03d", lastNum + 1);
    }

    @Transactional
    public FactureDTO creerFacture(UUID userId, UUID clientId, UUID dossierId, String intitule,
                                   BigDecimal montantHt, boolean tvaApplicable, myavocat.legit.model.ModePaiement modePaiement) {
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
        facture.setModePaiement(modePaiement);

        // 🔥 NOUVEAU : Initialiser le montant réclamé
        facture.setMontantReclame(montantTtc);

        return convertToDTO(factureRepository.save(facture));
    }

    // 🔥 NOUVELLE MÉTHODE : Mettre à jour le montant réclamé
    @Transactional
    public FactureDTO updateMontantReclame(UUID userId, UUID factureId, BigDecimal nouveauMontantReclame) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));

        // Vérifier l'accès
        if (!facture.getDossier().getOffice().getId().equals(user.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : cette facture n'appartient pas à votre cabinet.");
        }

        // Mettre à jour le montant réclamé
        facture.setMontantReclame(nouveauMontantReclame);

        // Recalculer automatiquement le statut
        facture.updateStatutPaiement();

        return convertToDTO(factureRepository.save(facture));
    }

    // 🔥 NOUVELLE MÉTHODE : Recalculer le statut après ajout/suppression de paiements
    @Transactional
    public FactureDTO recalculerStatutPaiement(UUID factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));

        // La méthode updateStatutPaiement() est dans le modèle Facture
        facture.updateStatutPaiement();

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

    // 🔥 MÉTHODE CONVERTTODT MISE À JOUR avec nouveaux champs
    private FactureDTO convertToDTO(Facture facture) {
        return new FactureDTO(
                facture.getId(),
                facture.getNumeroFacture(),
                facture.getIntitule(),
                facture.getDateEmission(),
                facture.getMontantHt(),
                facture.getMontantTtc(),
                facture.getMontantReclame(), // 🔥 NOUVEAU
                facture.getMontantRegleTtc(), // 🔥 NOUVEAU (calculé)
                facture.getMontantRestantDu(), // 🔥 NOUVEAU (calculé)
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

        // 🔥 MISE À JOUR : Utiliser les nouveaux calculs
        BigDecimal totalEmis = factures.stream()
                .map(Facture::getMontantReclame)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRegle = factures.stream()
                .map(Facture::getMontantRegleTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEnAttente = factures.stream()
                .map(Facture::getMontantRestantDu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

    public List<FactureDTO> getFacturesByDossier(UUID userId, UUID dossierId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        UUID officeId = user.getOffice().getId();

        List<Facture> factures = factureRepository.findByDossierId(dossierId);

        List<Facture> accessibleFactures = factures.stream()
                .filter(facture -> facture.getDossier().getOffice().getId().equals(officeId))
                .collect(Collectors.toList());

        return convertToFactureDTOList(accessibleFactures);
    }

    private List<FactureDTO> convertToFactureDTOList(List<Facture> factures) {
        return factures.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getKpiFactures(UUID userId) {
        UUID officeId = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"))
                .getOffice().getId();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1).withHour(0).withMinute(0).withSecond(0);

        List<Facture> allFactures = factureRepository.findAllByOffice(officeId);
        List<Facture> facturesDuMois = allFactures.stream().filter(f -> f.getDateEmission().isAfter(startOfMonth)).collect(Collectors.toList());
        List<Facture> facturesDeLaSemaine = allFactures.stream().filter(f -> f.getDateEmission().isAfter(startOfWeek)).collect(Collectors.toList());
        List<Facture> facturesEnAttente = allFactures.stream().filter(f -> f.getStatutPaiement() == StatutPaiement.ATTENTE_REGLEMENT).collect(Collectors.toList());

        // 🔥 MISE À JOUR : Utiliser montantReclame et montantRegleTtc
        BigDecimal totalFacturesMois = facturesDuMois.stream()
                .map(Facture::getMontantReclame)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFacturesSemaine = facturesDeLaSemaine.stream()
                .map(Facture::getMontantReclame)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaiementsEnAttente = facturesEnAttente.stream()
                .map(Facture::getMontantRestantDu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long nombreFacturesMois = facturesDuMois.size();
        long nombreFacturesPayeesMois = facturesDuMois.stream().filter(Facture::isPayee).count();
        long nombreFacturesEnAttenteMois = facturesDuMois.stream().filter(f -> f.getMontantRestantDu().compareTo(BigDecimal.ZERO) > 0).count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalFacturesMois", totalFacturesMois);
        result.put("totalFacturesSemaine", totalFacturesSemaine);
        result.put("totalPaiementsEnAttente", totalPaiementsEnAttente);
        result.put("nombreFacturesMois", nombreFacturesMois);
        result.put("nombreFacturesPayeesMois", nombreFacturesPayeesMois);
        result.put("nombreFacturesEnAttenteMois", nombreFacturesEnAttenteMois);

        return result;
    }

    public Map<String, Object> getKpiFacturesMensuelles(UUID userId) {
        UUID officeId = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"))
                .getOffice().getId();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0);

        List<Facture> facturesDuMois = factureRepository.findAllByOffice(officeId).stream()
                .filter(f -> f.getDateEmission().isAfter(startOfMonth))
                .collect(Collectors.toList());

        // 🔥 MISE À JOUR : Utiliser les nouveaux calculs
        BigDecimal totalFacturesMois = facturesDuMois.stream()
                .map(Facture::getMontantReclame)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFacturesRegleesMois = facturesDuMois.stream()
                .map(Facture::getMontantRegleTtc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFacturesImpayeesMois = facturesDuMois.stream()
                .map(Facture::getMontantRestantDu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "totalFacturesMois", totalFacturesMois,
                "totalFacturesRegleesMois", totalFacturesRegleesMois,
                "totalFacturesImpayeesMois", totalFacturesImpayeesMois
        );
    }
}
