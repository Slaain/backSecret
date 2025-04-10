package myavocat.legit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "template_document")
@Getter
@Setter
@NoArgsConstructor

public class TemplateDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; //Contenu HTML avec les {{varibles}}

    @ManyToOne
    @JoinColumn(name="created_by")
    private User createdBy;

    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
