package myavocat.legit.service;

import myavocat.legit.model.Client;
import myavocat.legit.model.Dossier;
import myavocat.legit.model.Facture;
import myavocat.legit.model.StatutPaiement;
import myavocat.legit.repository.ClientRepository;
import myavocat.legit.repository.DossierRepository;
import myavocat.legit.repository.FactureRepository;
import myavocat.legit.repository.UserRepository;
import myavocat.legit.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Arrays.stream;

@Service
public class FactureService {

    @Autowired
    private UserRepository userRepository;

    private final FactureRepository factureRepository;

    private final ClientRepository clientRepository;

    private final DossierRepository dossierRepository;

    private String genererNumeroFacture(Client client) {
        // Récupérer les initiales du client
        String initials = (client.getNom().substring(0, 1) + client.getPrenom().substring(0, 1)).toUpperCase();

        // Récupérer la dernière facture pour ce client
        String lastFacture = factureRepository.findLastFactureByClient(initials + "%");

        int lastNum = 0;
        if (lastFacture != null) {
            try {
                lastNum = Integer.parseInt(lastFacture.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                lastNum = 0; // Sécurité si jamais il y a une erreur dans le format de numéro
            }
        }

        return initials + String.format("%03d", lastNum + 1);
    }


    public FactureService(FactureRepository factureRepository, ClientRepository clientRepository, DossierRepository dossierRepository) {
        this.factureRepository = factureRepository;
        this.clientRepository = clientRepository;
        this.dossierRepository = dossierRepository;
    }

    @Transactional
    public Facture creerFacture(UUID userId, UUID clientId, UUID dossierId, String intitule, BigDecimal montantHt, boolean tvaApplicable) {
        // Vérifier que le client existe
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client introuvable avec l'ID: " + clientId));

        // Vérifier que le dossier existe et appartient bien au client
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable avec l'ID: " + dossierId));

        if (!dossier.getClient().getId().equals(client.getId())) {
            throw new RuntimeException("Le dossier sélectionné n'est pas associé à ce client.");
        }

        // Récupérer l'utilisateur par son ID pour vérifier son office
        // Vous devez injecter UserRepository dans ce service
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID: " + userId));

        // Vérifier que l'utilisateur appartient au même office que le dossier
        if (!dossier.getOffice().getId().equals(user.getOffice().getId())) {
            throw new RuntimeException("Accès refusé : l'utilisateur ne peut pas créer une facture pour ce dossier.");
        }
        // Générer le numéro de facture
        String numeroFacture = genererNumeroFacture(client);

        // Calculer le montant TTC
        BigDecimal montantTtc = tvaApplicable ? montantHt.multiply(BigDecimal.valueOf(1.2)) : montantHt;

        // Créer la facture
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

        return factureRepository.save(facture);
    }

    /**
     * Récupérer toutes les factures accessibles par l'utilisateur (dans le même office)
     */
    public List<Facture> getAllFactures(UUID userId) {
        return factureRepository.findAll().stream()
                .filter(facture -> facture.getDossier().getOffice().getId().equals(userId))
                .toList();
    }

    /**
     * Récupérer une facture par ID
     * Seul un utilisateur du même office peut accéder aux factures.
     */
    public Facture getFactureById(UUID userId, UUID id) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec ID: " + id));

        if (!facture.getDossier().getOffice().getId().equals(userId)) {
            throw new RuntimeException("Accès refusé : cette facture n'appartient pas à votre cabinet.");
        }

        return facture;
    }

    /**
     * Mettre à jour le statut d'une facture (Réglée / En attente)
     * Seul un utilisateur du même office peut modifier une facture.
     */
    @Transactional
    public Facture updateStatutFacture(UUID userId, UUID id, StatutPaiement statut) {
        Facture facture = getFactureById(userId, id);
        facture.setStatutPaiement(statut);
        return factureRepository.save(facture);
    }

    /**
     * Calculer les statistiques des factures (Total édité, payé, en attente)
     * Seul un utilisateur du même office peut voir les statistiques.
     */
    public Map<String, BigDecimal> getStatistiquesFactures(UUID userId) {
        List<Facture> factures = getAllFactures(userId);

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

    /**
     * Relancer les clients pour les factures impayées
     * Seul un utilisateur du même office peut relancer les factures.
     */
    public void relancerFacturesImpayees(UUID userId) {
        List<Facture> facturesEnRetard = factureRepository.findFacturesEnRetard(userId)
                .stream()
                .filter(f -> f.getDossier().getOffice().getId().equals(userId))
                .toList();

        for (Facture facture : facturesEnRetard) {
            System.out.println("⚠ Relance envoyée au client " + facture.getClient().getNom() + " pour la facture " + facture.getNumeroFacture());
        }
    }



}
