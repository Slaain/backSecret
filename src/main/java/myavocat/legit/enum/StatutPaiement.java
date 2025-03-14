package myavocat.legit.model;

public enum StatutPaiement {
    ATTENTE_REGLEMENT, // Facture non payée
    REGLEE, // Facture payée
    PARTIELLEMENT_REGLEE, // Facture partiellement payée
    ANNULEE // Facture annulée
}