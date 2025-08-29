package myavocat.legit;

import myavocat.legit.model.*;
import myavocat.legit.repository.*;
import myavocat.legit.service.DocumentService;
import myavocat.legit.service.EmailWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class EmailWebhookIntegrationTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private EmailAccountRepository emailAccountRepository;

    @Autowired
    private EmailWebhookLogRepository webhookLogRepository;

    @Autowired
    private EmailWebhookService emailWebhookService;

    @Autowired
    private RoleRepository roleRepository; // ✅ ajouté pour pouvoir save le rôle

    @MockBean
    private DocumentService documentService;

    private Client client;
    private User avocat;
    private Dossier dossier;
    private EmailAccount emailAccount;

    @BeforeEach
    void setup() {
        // Créer un rôle AVOCAT
        Role roleAvocat = new Role();
        roleAvocat.setName("AVOCAT");
        roleAvocat.setDescription("Rôle avocat");
        roleRepository.save(roleAvocat);

        // Créer un avocat
        avocat = new User();
        avocat.setEmail("avocat@test.com");
        avocat.setNom("Dupont");
        avocat.setPrenom("Jean");
        avocat.setPassword("hashed-password"); // obligatoire car @Column(nullable=false)
        avocat.setRole(roleAvocat);
        userRepository.save(avocat);

        // Créer un client
        client = new Client();
        client.setEmail("client@test.com");
        client.setNom("Martin");
        client.setPrenom("Alice");
        client.setOffice(avocat.getOffice());
        clientRepository.save(client);

        // Créer un dossier actif
        dossier = new Dossier();
        dossier.setNomDossier("Procédure divorce");
        dossier.setClient(client);
        dossier.setAvocat(avocat);
        dossier.setStatut("En cours");
        dossierRepository.save(dossier);

        // Créer un compte email
        emailAccount = new EmailAccount();
        emailAccount.setEmailAddress("inbox@test.com");
        emailAccount.setUser(avocat);
        emailAccount.setProvider(EmailAccount.EmailProvider.GMAIL);
        emailAccount.setActive(true);
        emailAccountRepository.save(emailAccount);
    }


    @Test
    void testProcessEmailWebhook_shouldStoreAttachmentsAndMarkSuccess() {
        // Simuler une pièce jointe
        EmailWebhookService.AttachmentData attachment =
                new EmailWebhookService.AttachmentData(
                        "document.pdf",
                        "application/pdf",
                        "Hello world".getBytes(StandardCharsets.UTF_8)
                );

        // Appeler le service
        EmailWebhookLog log = emailWebhookService.processEmailWebhook(
                EmailWebhookLog.WebhookType.GMAIL_PUBSUB,
                emailAccount.getId().toString(),
                client.getEmail(), // expéditeur = client
                "Sujet test",
                UUID.randomUUID().toString(), // emailMessageId unique
                "thread-123",
                List.of(attachment),
                "{json payload}"
        );

        // Vérifier que le log est bien créé
        assertThat(log.getId()).isNotNull();
        assertThat(log.getStatus()).isEqualTo(EmailWebhookLog.ProcessingStatus.SUCCESS);

        // Vérifier rattachement au client et dossier
        assertThat(log.getClient()).isNotNull();
        assertThat(log.getClient().getEmail()).isEqualTo("client@test.com");
        assertThat(log.getDossier()).isNotNull();
        assertThat(log.getDossier().getNomDossier()).isEqualTo("Procédure divorce");

        // Vérifier que la pièce jointe est bien considérée comme traitée
        assertThat(log.getAttachmentsProcessed()).isEqualTo(1);
    }
}
