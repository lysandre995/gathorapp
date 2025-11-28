package com.alfano.gathorapp.voucher;

import com.alfano.gathorapp.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Voucher entity.
 */
@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {

    /**
     * Find all vouchers for a specific user.
     */
    List<Voucher> findByUser(User user);

    /**
     * Find all vouchers for a specific user ID.
     */
    List<Voucher> findByUserId(UUID userId);

    /**
     * Find voucher by QR code.
     */
    Optional<Voucher> findByQrCode(String qrCode);

    /**
     * Find all active vouchers for a user.
     */
    @Query("SELECT v FROM Voucher v WHERE v.user.id = :userId AND v.status = 'ACTIVE' AND v.expiresAt > :now")
    List<Voucher> findActiveVouchersByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Find all redeemed vouchers for a user.
     */
    List<Voucher> findByUserIdAndStatus(UUID userId, VoucherStatus status);

    /**
     * Find all expired active vouchers (for cleanup).
     */
    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' AND v.expiresAt < :now")
    List<Voucher> findExpiredVouchers(@Param("now") LocalDateTime now);

    /**
     * Count active vouchers for a user.
     */
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.user.id = :userId AND v.status = 'ACTIVE' AND v.expiresAt > :now")
    long countActiveVouchersByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Count all active vouchers (for admin statistics).
     */
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.status = 'ACTIVE' AND v.expiresAt > :now")
    long countAllActiveVouchers(@Param("now") LocalDateTime now);
}
