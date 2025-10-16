package myavocat.legit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private String sender;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String snippet;

    @Column(name = "attachment_filename")
    private String attachmentFilename;

    // 🚫 On ne veut pas exposer cet objet dans le JSON
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id")
    @JsonIgnore
    private EmailAccount emailAccount;

    @Column(nullable = false)
    private LocalDateTime receivedAt;
}
