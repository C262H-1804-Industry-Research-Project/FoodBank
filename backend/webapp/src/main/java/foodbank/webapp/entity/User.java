package foodbank.webapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class User {

    @Id
    private Integer userId;
    private String email;
    private String password;
    private String phone;
    private String status;
}
