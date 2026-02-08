package com.QhomeBase.iamservice.service.token;

public interface TokenBlacklistService {

    boolean isBlacklisted(String jti);

    void blacklist(String jti, long ttlSeconds);
}