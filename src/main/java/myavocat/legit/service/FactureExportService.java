package myavocat.legit.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import myavocat.legit.model.Facture;
import myavocat.legit.repository.FactureRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
public class FactureExportService {

    private final FactureRepository factureRepository;

    public FactureExportService(FactureRepository factureRepository) {
        this.factureRepository = factureRepository;
    }

    public byte[] generateFacturePdf(UUID factureId) {
        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'ID: " + factureId));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // En-tête
            document.add(new Paragraph("Facture #" + facture.getNumeroFacture())
                    .setBold()
                    .setFontSize(16));

            document.add(new Paragraph("Client: " + facture.getClient().getNom() + " " + facture.getClient().getPrenom()));
            document.add(new Paragraph("Date d'émission: " + facture.getDateEmission()));
            document.add(new Paragraph("Intitulé: " + facture.getIntitule()));
            document.add(new Paragraph("Montant HT: " + facture.getMontantHt() + " €"));
            document.add(new Paragraph("TVA: " + (facture.getTvaApplicable() ? "Oui (+20%)" : "Non")));
            document.add(new Paragraph("Montant TTC: " + facture.getMontantTtc() + " €"));
            document.add(new Paragraph("Statut: " + facture.getStatutPaiement()));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }
}
