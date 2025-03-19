package myavocat.legit.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import myavocat.legit.model.Facture;
import myavocat.legit.repository.FactureRepository;
import myavocat.legit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FactureExportService {

    private final FactureRepository factureRepository;
    private final UserRepository userRepository;

    @Autowired
    public FactureExportService(FactureRepository factureRepository, UserRepository userRepository) {
        this.factureRepository = factureRepository;
        this.userRepository = userRepository;
    }

    public byte[] generateFacturePdf(UUID factureId) {
        try {
            Facture facture = factureRepository.findById(factureId)
                    .orElseThrow(() -> new RuntimeException("Facture non trouvée"));

            // Créer un document PDF en mémoire
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Style du document
            document.setMargins(50, 50, 50, 50);

            // Définir les polices
            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // En-tête avec logo
            Table header = new Table(2).useAllAvailableWidth();
            Cell logoCell = new Cell();
            // Ajouter votre logo (à adapter selon vos ressources)
            // Image logo = new Image(ImageDataFactory.create("path/to/logo.png"));
            // logoCell.add(logo.setWidth(100));
            logoCell.setBorder(Border.NO_BORDER);

            Cell infoCell = new Cell();
            infoCell.add(new Paragraph("Votre Cabinet d'Avocats").setFont(titleFont).setFontSize(20));
            infoCell.add(new Paragraph("Adresse: 123 rue du Palais, 75000 Paris").setFont(regularFont));
            infoCell.add(new Paragraph("Tél: 01 23 45 67 89").setFont(regularFont));
            infoCell.add(new Paragraph("Email: contact@cabinet.fr").setFont(regularFont));
            infoCell.setBorder(Border.NO_BORDER);

            header.addCell(logoCell);
            header.addCell(infoCell);
            document.add(header);

            // Titre Facture
            Paragraph title = new Paragraph("FACTURE")
                    .setFont(titleFont)
                    .setFontSize(24)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30)
                    .setMarginBottom(30);
            document.add(title);

            // Informations de la facture
            Table infoTable = new Table(2).useAllAvailableWidth();

            // Colonne 1: Infos facture
            Cell factureInfos = new Cell();
            factureInfos.add(new Paragraph("N° Facture: " + facture.getNumeroFacture()).setFont(regularFont));
            factureInfos.add(new Paragraph("Date d'émission: " +
                    facture.getDateEmission().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setFont(regularFont));
            factureInfos.add(new Paragraph("Référence dossier: " + facture.getDossier().getReference()).setFont(regularFont));
            factureInfos.setBorder(Border.NO_BORDER);

            // Colonne 2: Infos client
            Cell clientInfos = new Cell();
            clientInfos.add(new Paragraph("Client:").setFont(titleFont).setFontSize(14));
            clientInfos.add(new Paragraph(facture.getClient().getNom() + " " + facture.getClient().getPrenom()).setFont(regularFont));
            // Ajouter d'autres infos client si disponibles
            clientInfos.setBorder(Border.NO_BORDER);

            infoTable.addCell(factureInfos);
            infoTable.addCell(clientInfos);
            document.add(infoTable);

            // Contenu de la facture
            document.add(new Paragraph("Détails de la prestation").setFont(titleFont).setFontSize(14).setMarginTop(20));

            Table detailsTable = new Table(new float[]{3, 2}).useAllAvailableWidth();

            // En-têtes du tableau
            detailsTable.addHeaderCell(new Cell().add(new Paragraph("Description").setFont(titleFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            detailsTable.addHeaderCell(new Cell().add(new Paragraph("Montant").setFont(titleFont)).setBackgroundColor(ColorConstants.LIGHT_GRAY));

            // Contenu du tableau
            detailsTable.addCell(new Cell().add(new Paragraph(facture.getIntitule()).setFont(regularFont)));
            detailsTable.addCell(new Cell().add(new Paragraph(facture.getMontantHt() + " €").setFont(regularFont)));

            document.add(detailsTable);

            // Totaux
            Table totalsTable = new Table(new float[]{3, 2}).useAllAvailableWidth();
            totalsTable.setMarginTop(10);

            // TVA
            if (facture.getTvaApplicable()) {
                BigDecimal tva = facture.getMontantHt().multiply(new BigDecimal("0.2"));

                totalsTable.addCell(new Cell().add(new Paragraph("TVA (20%)").setFont(regularFont)).setBorder(Border.NO_BORDER));
                totalsTable.addCell(new Cell().add(new Paragraph(tva + " €").setFont(regularFont)).setBorder(Border.NO_BORDER));
            }

            // Total TTC
            totalsTable.addCell(new Cell().add(new Paragraph("Total TTC").setFont(titleFont)).setBorder(Border.NO_BORDER));
            totalsTable.addCell(new Cell().add(new Paragraph(facture.getMontantTtc() + " €").setFont(titleFont)).setBorder(Border.NO_BORDER));

            document.add(totalsTable);

            // Informations de paiement
            document.add(new Paragraph("Informations de paiement").setFont(titleFont).setFontSize(14).setMarginTop(30));

            Table paymentTable = new Table(1).useAllAvailableWidth();
            paymentTable.addCell(new Cell().add(new Paragraph("Mode de paiement: " +
                    (facture.getModePaiement() != null ? facture.getModePaiement().toString() : "Non spécifié")).setFont(regularFont)).setBorder(Border.NO_BORDER));
            paymentTable.addCell(new Cell().add(new Paragraph("Statut: " + facture.getStatutPaiement().toString()).setFont(regularFont)).setBorder(Border.NO_BORDER));

            // Ajouter des détails bancaires si nécessaire
//            paymentTable.addCell(new Cell().add(new Paragraph("IBAN: FR76 1234 5678 9012 3456 7890 123").setFont(regularFont)).setBorder(Border.NO_BORDER));
//            paymentTable.addCell(new Cell().add(new Paragraph("BIC: ABCDEFGH").setFont(regularFont)).setBorder(Border.NO_BORDER));

            document.add(paymentTable);

            // Notes et conditions
            document.add(new Paragraph("Conditions de paiement").setFont(titleFont).setFontSize(14).setMarginTop(30));
            document.add(new Paragraph("Paiement à réception de facture. En cas de retard de paiement, des pénalités seront appliquées au taux légal en vigueur.").setFont(regularFont));

            // Pied de page
            document.add(new Paragraph("Merci pour votre confiance !").setFont(regularFont).setTextAlignment(TextAlignment.CENTER).setMarginTop(30));

            // Fermer le document
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF: " + e.getMessage(), e);
        }
    }
}