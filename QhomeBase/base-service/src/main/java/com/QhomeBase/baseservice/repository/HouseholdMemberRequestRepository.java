package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.HouseholdMemberRequest;
import com.QhomeBase.baseservice.model.HouseholdMemberRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdMemberRequestRepository extends JpaRepository<HouseholdMemberRequest, UUID> {

    List<HouseholdMemberRequest> findByRequestedByOrderByCreatedAtDesc(UUID requestedBy);

    List<HouseholdMemberRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);

    Optional<HouseholdMemberRequest> findFirstByHouseholdIdAndResidentIdAndStatusIn(
            UUID householdId,
            UUID residentId,
            List<RequestStatus> statuses
    );

    Optional<HouseholdMemberRequest> findFirstByHouseholdIdAndResidentNationalIdAndStatusIn(
            UUID householdId,
            String residentNationalId,
            List<RequestStatus> statuses
    );

    Optional<HouseholdMemberRequest> findFirstByHouseholdIdAndResidentFullNameIgnoreCaseAndResidentPhoneAndStatusIn(
            UUID householdId,
            String residentFullName,
            String residentPhone,
            List<RequestStatus> statuses
    );

    long countByHouseholdIdAndStatus(UUID householdId, RequestStatus status);
}
