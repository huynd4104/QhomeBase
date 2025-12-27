package com.QhomeBase.datadocsservice.service;

import com.QhomeBase.datadocsservice.client.BaseServiceClient;
import com.QhomeBase.datadocsservice.client.NotificationClient;
import com.QhomeBase.datadocsservice.model.Contract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractScheduler {

    private final ContractService contractService;
    private final NotificationClient notificationClient;
    private final BaseServiceClient baseServiceClient;
    
    // @EventListener(ApplicationReadyEvent.class)
    // public void onApplicationReady() {
    //     contractService.markExpiredContracts();
    //     contractService.triggerRenewalReminders();
    //     sendRenewalReminders();
    //     markRenewalDeclined();
    //     //trigger for third sent
    //     triggerReminder3ForTesting();
        
    //     log.info("Initial contract status checks completed");
    // }
    

    private void triggerReminder3ForTesting() {
        try {
            // Force triggering reminder 3 for testing
            LocalDate today = LocalDate.now();
            
            List<Contract> contracts = contractService.findContractsNeedingRenewalReminder();
            int thirdReminderCount = 0;
            
            for (Contract contract : contracts) {
                if (contract.getEndDate() == null || !"RENTAL".equals(contract.getContractType()) 
                        || !"ACTIVE".equals(contract.getStatus())) {
                    continue;
                }
                
                LocalDate endDate = contract.getEndDate();
                long daysUntilEndDate = ChronoUnit.DAYS.between(today, endDate);
                
                // Check if contract is eligible for reminder 3 (has sent reminder 1, in endDate month, not expired)
                if (contract.getRenewalReminderSentAt() != null
                        && daysUntilEndDate >= 9 && daysUntilEndDate <= 11) {
                    
                    try {
                        // Force send reminder 3 for testing (bypass normal check)
                        contractService.sendRenewalReminder(contract.getId());
                        sendReminderNotificationToAllResidents(contract, 3, true);
                        thirdReminderCount++;
                        log.info("✅ [TEST] Force sent THIRD (FINAL) renewal reminder for contract {} (expires on {})", 
                                contract.getContractNumber(), endDate);
                    } catch (Exception e) {
                        log.error("Error force sending reminder 3 for contract {}", contract.getId(), e);
                    }
                }
            }
            
            if (thirdReminderCount > 0) {
                log.info("🔧 [ContractScheduler] Force triggered {} reminder 3(s) for testing", thirdReminderCount);
            } else {
                log.debug("🔧 [ContractScheduler] No contracts eligible for force reminder 3");
            }
        } catch (Exception e) {
            log.error("Error in force trigger reminder 3 for testing", e);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void activateInactiveContractsDaily() {
        try {
            log.info("Starting scheduled task: Activate inactive contracts");
            int activatedCount = contractService.activateInactiveContracts();
            log.info("Scheduled task completed: Activated {} contract(s)", activatedCount);
        } catch (Exception e) {
            log.error("Error in scheduled task to activate inactive contracts", e);
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void markExpiredContractsDaily() {
        try {
            log.info("Starting scheduled task: Mark expired contracts");
            int expiredCount = contractService.markExpiredContracts();
            log.info("Scheduled task completed: Marked {} contract(s) as expired", expiredCount);
        } catch (Exception e) {
            log.error("Error in scheduled task to mark expired contracts", e);
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendRenewalReminders() {
        try {
            log.info("Starting scheduled task: Send renewal reminders");
            LocalDate today = LocalDate.now();
            
            // Get all active RENTAL contracts that need reminders
            List<Contract> allContracts = contractService.findContractsNeedingRenewalReminder();
            log.info("Found {} contract(s) that may need renewal reminders", allContracts.size());
            
            int firstReminderCount = 0;
            int secondReminderCount = 0;
            int thirdReminderCount = 0;
            
            for (Contract contract : allContracts) {
                if (contract.getEndDate() == null || !"RENTAL".equals(contract.getContractType()) 
                        || !"ACTIVE".equals(contract.getStatus())) {
                    continue;
                }
                
                LocalDate endDate = contract.getEndDate();
                
                // Calculate days until end date
                long daysUntilEndDate = ChronoUnit.DAYS.between(today, endDate);
                
                log.info("Checking contract {}: endDate={}, today={}, daysUntilEndDate={}, renewalStatus={}, reminderSentAt={}, todayDay={}, endDateMonth={}, endDateYear={}", 
                        contract.getContractNumber(), endDate, today, daysUntilEndDate,
                        contract.getRenewalStatus(), contract.getRenewalReminderSentAt(),
                        today.getDayOfMonth(), endDate.getMonth(), endDate.getYear());
                
                try {
                    // Lần 1: 30 ngày trước khi hết hạn hợp đồng
                    // Gửi khi còn 29-31 ngày (buffer để đảm bảo không bỏ sót do scheduler chạy 1 lần/ngày)
                    if (daysUntilEndDate >= 29 && daysUntilEndDate <= 31 
                            && contract.getRenewalReminderSentAt() == null) {
                        contractService.sendRenewalReminder(contract.getId());
                        sendReminderNotificationToAllResidents(contract, 1, false);
                        firstReminderCount++;
                        log.info("✅ Sent FIRST renewal reminder for contract {} (expires on {}, {} days until end date)", 
                                contract.getContractNumber(), endDate, daysUntilEndDate);
                    }
                    // Lần 2: 20 ngày trước khi hết hạn hợp đồng
                    // Chỉ gửi nếu:
                    // - Đã gửi lần 1 (renewalReminderSentAt != null)
                    // - Còn 19-21 ngày trước khi hết hạn (buffer)
                    // - Lần 1 đã được gửi trước đó (ít nhất 1 ngày trước)
                    else if (contract.getRenewalReminderSentAt() != null
                            && daysUntilEndDate >= 19 && daysUntilEndDate <= 21) {
                        LocalDate firstReminderDate = contract.getRenewalReminderSentAt().toLocalDate();
                        // Đảm bảo lần 1 đã được gửi trước đó (ít nhất 1 ngày)
                        if (firstReminderDate.isBefore(today)) {
                            contractService.sendRenewalReminder(contract.getId());
                            sendReminderNotificationToAllResidents(contract, 2, false);
                            secondReminderCount++;
                            log.info("✅ Sent SECOND renewal reminder for contract {} (expires on {}, {} days until end date)", 
                                    contract.getContractNumber(), endDate, daysUntilEndDate);
                        } else {
                            log.debug("⏭️ Skipping reminder 2 for contract {}: firstReminderDate={}, today={}", 
                                    contract.getContractNumber(), firstReminderDate, today);
                }
            }
                    // Lần 3: 10 ngày trước khi hết hạn hợp đồng - BẮT BUỘC
                    // Chỉ gửi nếu:
                    // - Đã gửi lần 1 (renewalReminderSentAt != null)
                    // - Còn 9-11 ngày trước khi hết hạn (buffer)
                    // - Lần 1 đã được gửi trước đó (ít nhất 1 ngày trước)
                    else if (contract.getRenewalReminderSentAt() != null
                            && daysUntilEndDate >= 9 && daysUntilEndDate <= 11) {
                        LocalDate firstReminderDate = contract.getRenewalReminderSentAt().toLocalDate();
                        // Đảm bảo lần 1 đã được gửi trước đó (ít nhất 1 ngày)
                        if (firstReminderDate.isBefore(today)) {
                            contractService.sendRenewalReminder(contract.getId());
                            // Set thirdReminderSentAt to track when third reminder was sent
                            contractService.setThirdReminderSentAt(contract.getId());
                            sendReminderNotificationToAllResidents(contract, 3, true);
                            thirdReminderCount++;
                            log.info("✅ Sent THIRD (FINAL) renewal reminder for contract {} (expires on {}, {} days until end date - BẮT BUỘC HỦY HOẶC GIA HẠN)", 
                                    contract.getContractNumber(), endDate, daysUntilEndDate);
                        } else {
                            log.debug("⏭️ Skipping reminder 3 for contract {}: firstReminderDate={}, today={}", 
                                    contract.getContractNumber(), firstReminderDate, today);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error sending renewal reminder for contract {}", contract.getId(), e);
                }
            }
            
            log.info("Scheduled task completed: Sent {} first reminder(s), {} second reminder(s), {} third reminder(s)", 
                    firstReminderCount, secondReminderCount, thirdReminderCount);
        } catch (Exception e) {
            log.error("Error in scheduled task to send renewal reminders", e);
        }
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void markRenewalDeclined() {
        try {
            log.info("Starting scheduled task: Mark renewal declined");
            LocalDate today = LocalDate.now();
            
            // Get all active RENTAL contracts with REMINDED status
            List<Contract> allContracts = contractService.findContractsNeedingRenewalReminder();
            log.info("Found {} contract(s) that may need to be marked as declined", allContracts.size());
            
            int declinedCount = 0;
            for (Contract contract : allContracts) {
                try {
                    if (!"REMINDED".equals(contract.getRenewalStatus()) 
                            || contract.getRenewalReminderSentAt() == null
                            || contract.getEndDate() == null) {
                        continue;
                    }
                    
                    LocalDate endDate = contract.getEndDate();
                    long daysUntilEndDate = ChronoUnit.DAYS.between(today, endDate);
                    long daysSinceFirstReminder = ChronoUnit.DAYS.between(
                            contract.getRenewalReminderSentAt().toLocalDate(),
                            today
                    );
                    
                    // Calculate reminder count to check if reminder 3 has been sent
                    int reminderCount = contractService.calculateReminderCount(contract);
                    
                    log.info("Checking contract {}: daysUntilEndDate={}, daysSinceFirstReminder={}, reminderCount={}", 
                            contract.getContractNumber(), daysUntilEndDate, daysSinceFirstReminder, reminderCount);
                    
                    // Đánh dấu DECLINED nếu:
                    // 1. Đã gửi reminder lần 3 (reminderCount >= 3) VÀ
                    // 2. (Đã hết hạn HOẶC đã qua 3 ngày sau reminder 3) VÀ
                    // 3. Chưa được đánh dấu DECLINED
                    boolean shouldDecline = false;
                    String reason = "";
                    
                    if (reminderCount >= 3) {
                        // Đã gửi reminder lần 3
                        if (daysUntilEndDate < 0) {
                            // Đã hết hạn
                            shouldDecline = true;
                            reason = String.format("Contract expired (endDate: %s, today: %s)", endDate, today);
                        } else if (daysUntilEndDate <= 5 && daysSinceFirstReminder >= 20) {
                            // Còn <= 5 ngày và đã qua 20 ngày từ lần nhắc đầu (đã gửi reminder 3)
                            shouldDecline = true;
                            reason = String.format("Less than 5 days remaining (daysUntilEndDate: %d, daysSinceFirstReminder: %d)", 
                                    daysUntilEndDate, daysSinceFirstReminder);
                        }
                    } else if (daysUntilEndDate < 0 && daysSinceFirstReminder >= 20) {
                        // Đã hết hạn và đã qua 20 ngày từ lần nhắc đầu (fallback)
                        shouldDecline = true;
                        reason = String.format("Contract expired and reminder sent >= 20 days ago (endDate: %s, daysSinceFirstReminder: %d)", 
                                endDate, daysSinceFirstReminder);
                    }
                    
                    if (shouldDecline) {
                        contractService.markRenewalDeclined(contract.getId());
                        declinedCount++;
                        log.info("✅ Marked contract {} as renewal declined. Reason: {}", 
                                contract.getContractNumber(), reason);
                    } else {
                        log.debug("⏭️ Contract {} skipped: reminderCount={}, daysUntilEndDate={}, daysSinceFirstReminder={}", 
                                contract.getContractNumber(), reminderCount, daysUntilEndDate, daysSinceFirstReminder);
                    }
                } catch (Exception e) {
                    log.error("Error marking contract {} as renewal declined", contract.getId(), e);
                }
            }
            
            log.info("Scheduled task completed: Marked {} contract(s) as renewal declined", declinedCount);
        } catch (Exception e) {
            log.error("Error in scheduled task to mark renewal declined", e);
        }
    }

    /**
     * Auto-cancel contracts after 24 hours from third reminder if user hasn't taken action
     * Runs every hour to check contracts that need to be auto-cancelled
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoCancelContractsAfterThirdReminder() {
        try {
            log.info("Starting scheduled task: Auto-cancel contracts after 24 hours from third reminder");
            int cancelledCount = contractService.autoCancelContractsAfterThirdReminder();
            log.info("Scheduled task completed: Auto-cancelled {} contract(s)", cancelledCount);
        } catch (Exception e) {
            log.error("Error in scheduled task to auto-cancel contracts after third reminder", e);
        }
    }

    /**
     * Send notification for contract renewal reminder to all residents in the unit
     */
    private void sendReminderNotificationToAllResidents(Contract contract, int reminderNumber, boolean isFinalReminder) {
        try {
            Optional<UUID> buildingIdOpt = baseServiceClient.getBuildingIdByUnitId(contract.getUnitId());
                UUID buildingId = buildingIdOpt.orElse(null);
                
            // Get all residents in the unit (including household members)
            List<UUID> residentIds = baseServiceClient.getAllResidentIdsByUnitId(contract.getUnitId());
            
            if (residentIds.isEmpty()) {
                log.warn("[ContractScheduler] Could not find any residents for unitId: {}", contract.getUnitId());
                return;
            }
            
            // Send notification to each resident
            for (UUID residentId : residentIds) {
                try {
                notificationClient.sendContractRenewalReminderNotification(
                        residentId,
                        buildingId,
                        contract.getId(),
                        contract.getContractNumber(),
                        reminderNumber,
                        isFinalReminder
                );
                    log.debug("✅ Sent notification for contract {} reminder #{} to resident {}", 
                        contract.getContractNumber(), reminderNumber, residentId);
                } catch (Exception e) {
                    log.error("❌ Error sending notification to resident {} for contract {} reminder #{}", 
                            residentId, contract.getContractNumber(), reminderNumber, e);
                }
            }
            
            log.info("✅ Sent reminder #{} notifications for contract {} to {} resident(s)", 
                    reminderNumber, contract.getContractNumber(), residentIds.size());
        } catch (Exception e) {
            log.error("❌ Error sending notifications for contract {} reminder #{}", 
                    contract.getContractNumber(), reminderNumber, e);
        }
    }
}

