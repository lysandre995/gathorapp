package com.alfano.gathorapp.voucher;

import com.alfano.gathorapp.outing.Outing;
import com.alfano.gathorapp.reward.Reward;
import com.alfano.gathorapp.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a voucher issued to a Premium user who earned a reward.
 *
 * A voucher is created when a Premium user brings enough participants to an
 * event-linked outing. The voucher contains a QR code that can be redeemed
 * at the business location.
 */
@Entity
@Table(name = "vouchers", indexes = {
        @Index(name = "idx_voucher_user", columnList = "user_id"),
        @Index(name = "idx_voucher_reward", columnList = "reward_id"),
        @Index(name = "idx_voucher_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Premium user who earned this voucher.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /**
     * Reward this voucher was created from.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Reward reward;

    /**
     * Outing through which the user earned this voucher.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outing_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Outing outing;

    /**
     * Unique QR code for redemption.
     * Format: VOUCHER-{UUID}
     */
    @Column(nullable = false, unique = true, length = 100)
    private String qrCode;

    /**
     * Status of the voucher.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    /**
     * When the voucher was issued.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    /**
     * When the voucher was redeemed (null if not redeemed).
     */
    private LocalDateTime redeemedAt;

    /**
     * Expiration date of the voucher.
     * Typically 30-90 days after issuance.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (qrCode == null) {
            qrCode = "VOUCHER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (expiresAt == null) {
            // Default: expires 60 days after issuance
            expiresAt = issuedAt.plusDays(60);
        }
    }

    /**
     * Redeem this voucher.
     */
    public void redeem() {
        if (status != VoucherStatus.ACTIVE) {
            throw new IllegalStateException("Cannot redeem: voucher is " + status);
        }
        if (LocalDateTime.now().isAfter(expiresAt)) {
            throw new IllegalStateException("Cannot redeem: voucher expired on " + expiresAt);
        }
        this.status = VoucherStatus.REDEEMED;
        this.redeemedAt = LocalDateTime.now();
    }

    /**
     * Check if voucher is expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if voucher can be redeemed.
     */
    public boolean canBeRedeemed() {
        return status == VoucherStatus.ACTIVE && !isExpired();
    }
}
