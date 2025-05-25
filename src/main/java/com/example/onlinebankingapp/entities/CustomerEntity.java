package com.example.onlinebankingapp.entities;

import com.example.onlinebankingapp.enums.TransactionStatus;
import com.example.onlinebankingapp.utils.DateTimeUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name ="customers")
public class CustomerEntity extends AbstractUser implements OAuth2User, UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="pin_number", length = 6)
    private String pinNumber;

    @Column(name = "is_active")
    private boolean active;

    @Column(name="img_front", length = 200, nullable = false)
    private String imgFront;

    @Column(name="img_back", length = 200, nullable = false)
    private String imgBack;

    @Column(name="img_face", length = 200, nullable = false)
    private String imgFace;

    @Column(name="client_session", length = 300, nullable = false)
    private String clientSession;

    @Column(name = "avatar_hashed_name", length = 300)
    private String avatarHashedName;

    // Implementing OAuth2User and UserDetails interfaces
    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @PrePersist
    protected void onCreate() {
        avatarHashedName = "customer_avatar_NOTFOUND.jpg";
    }
}
