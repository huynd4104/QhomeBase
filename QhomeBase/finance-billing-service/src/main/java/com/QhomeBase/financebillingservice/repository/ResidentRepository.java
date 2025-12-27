package com.QhomeBase.financebillingservice.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ResidentRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public Optional<UUID> findResidentIdByUserId(UUID userId) {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT id FROM data.residents WHERE user_id = :userId LIMIT 1"
            );
            query.setParameter("userId", userId);
            
            Object result = query.getSingleResult();
            if (result == null) {
                return Optional.empty();
            }
            
            if (result instanceof UUID) {
                return Optional.of((UUID) result);
            } else if (result instanceof String) {
                return Optional.of(UUID.fromString((String) result));
            } else {
                return Optional.empty();
            }
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<ResidentContact> findContactByResidentId(UUID residentId) {
        try {
            Query query = entityManager.createNativeQuery(
                    "SELECT r.full_name, COALESCE(u.email, r.email) AS email " +
                    "FROM data.residents r " +
                    "LEFT JOIN iam.users u ON u.id = r.user_id " +
                    "WHERE r.id = :residentId " +
                    "LIMIT 1"
            );
            query.setParameter("residentId", residentId);

            Object result = query.getSingleResult();
            if (result == null) {
                return Optional.empty();
            }

            Object[] row;
            if (result instanceof Object[]) {
                row = (Object[]) result;
            } else {
                row = new Object[]{result, null};
            }

            String fullName = row[0] != null ? row[0].toString() : null;
            String email = row.length > 1 && row[1] != null ? row[1].toString() : null;

            return Optional.of(new ResidentContact(fullName, email));
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    public record ResidentContact(String fullName, String email) { }
}

