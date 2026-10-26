package foodbank.webapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLink {
    @Id
    @GeneratedValue
    private Integer linkId;

    @Column(nullable = false, unique = true)
    private Integer donorUserId;

    @Column(nullable = false, unique = true)
    private Integer recipientUserId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
