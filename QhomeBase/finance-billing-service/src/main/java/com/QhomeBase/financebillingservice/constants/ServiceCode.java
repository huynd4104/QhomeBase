package com.QhomeBase.financebillingservice.constants;

public final class ServiceCode {
    
    private ServiceCode() {}
    
    public static final String ELECTRIC = "ELECTRIC";
    public static final String WATER = "WATER";
    public static final String PARKING_PRORATA = "PARKING_PRORATA";
    public static final String PARKING_CAR = "PARKING_CAR";
    public static final String PARKING_MOTORBIKE = "PARKING_MOTORBIKE";
    
    public static boolean isValid(String serviceCode) {
        if (serviceCode == null) {
            return false;
        }
        return ELECTRIC.equals(serviceCode) ||
               WATER.equals(serviceCode) ||
               PARKING_PRORATA.equals(serviceCode) ||
               PARKING_CAR.equals(serviceCode) ||
               PARKING_MOTORBIKE.equals(serviceCode);
    }
    
    public static String normalize(String serviceCode) {
        if (serviceCode == null) {
            return null;
        }
        String upper = serviceCode.toUpperCase().trim();
        if ("ELEC".equals(upper) || "ELECTRICITY".equals(upper)) {
            return ELECTRIC;
        }
        if ("WTR".equals(upper) || "WATER_SERVICE".equals(upper)) {
            return WATER;
        }
        return upper;
    }
}

