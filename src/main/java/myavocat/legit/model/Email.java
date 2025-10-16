package myavocat.legit.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "emails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String sender; // expéditeur

    @Column(nullable = true)
    private String subject; // objet du mail

    @Column(columnDefinition = "TEXT")
    private String snippet; // aperçu du contenu

    @Column(nullable = true)
    private String attachmentFilename; // nom de la pièce jointe

    @Column(nullable = false)
    private LocalDateTime receivedAt; // date de réception

    // Relation vers le compte mail (idris1390@gmail.com par ex)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id")
    private EmailAccount emailAccount;
}
