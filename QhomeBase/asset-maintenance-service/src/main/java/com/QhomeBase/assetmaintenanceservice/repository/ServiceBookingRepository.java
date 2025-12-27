package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.ServiceBooking;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingStatus;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePaymentStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, UUID> {

    List<ServiceBooking> findAllByUserIdOrderByCreatedAtDesc(@NotNull UUID userId);

    List<ServiceBooking> findAllByServiceIdOrderByCreatedAtDesc(@NotNull UUID serviceId);

    List<ServiceBooking> findAllByStatusOrderByCreatedAtDesc(@NotNull ServiceBookingStatus status);

    List<ServiceBooking> findAllByServiceIdAndStatusOrderByCreatedAtDesc(@NotNull UUID serviceId,
                                                                         @NotNull ServiceBookingStatus status);

    Optional<ServiceBooking> findByIdAndUserId(@NotNull UUID id, @NotNull UUID userId);

    Optional<ServiceBooking> findByVnpayTransactionRef(String vnpayTransactionRef);

    List<ServiceBooking> findAllByUserIdAndPaymentStatusInOrderByCreatedAtDesc(@NotNull UUID userId,
                                                                               Collection<ServicePaymentStatus> statuses);

    List<ServiceBooking> findAllByUserIdAndPaymentStatusInAndStatusNotInOrderByCreatedAtDesc(@NotNull UUID userId,
                                                                                               Collection<ServicePaymentStatus> paymentStatuses,
                                                                                               Collection<ServiceBookingStatus> excludedBookingStatuses);

    boolean existsByUserIdAndPaymentStatusIn(@NotNull UUID userId,
                                             Collection<ServicePaymentStatus> statuses);

    boolean existsByUserIdAndPaymentStatusInAndStatusNotIn(@NotNull UUID userId,
                                                             Collection<ServicePaymentStatus> paymentStatuses,
                                                             Collection<ServiceBookingStatus> excludedBookingStatuses);

    List<ServiceBooking> findAllByBookingDateBetweenOrderByCreatedAtDesc(@NotNull LocalDate start,
                                                                         @NotNull LocalDate end);

    List<ServiceBooking> findAllByServiceIdAndBookingDateBetweenOrderByCreatedAtDesc(@NotNull UUID serviceId,
                                                                                     @NotNull LocalDate start,
                                                                                     @NotNull LocalDate end);
}







