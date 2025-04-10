package myavocat.legit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signatures")
@Getter
@Setter
@NoArgsConstructor
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(optional = false)
    @JoinColumn(name = "signed_by")
    private User signedBy;

    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @PrePersist
    public void onCreate() {
        this.signedAt = LocalDateTime.now();
    }
}
