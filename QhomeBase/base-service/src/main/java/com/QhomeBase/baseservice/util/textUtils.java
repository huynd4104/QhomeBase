package com.QhomeBase.baseservice.util;

import java.text.Normalizer;

public class textUtils {
    public static String  stripAccents (String input) {
        if (input == null) return null;
        input = input.replace("Đ", "D").replace("đ", "d");
        input = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return input;
    };
    public static String getCode (String input) {
        if (input == null) return "";
        StringBuilder result = new StringBuilder();
        String toUpperCase = input.toUpperCase().trim();
        if (toUpperCase.isEmpty()) {return "";}
        String[] all = toUpperCase.split("\\s+");
        if (all.length ==1) {
            return all[0].substring(0,1).toUpperCase();
        }
        for (String s : all) {
            result.append(s.charAt(0));
        }

        return result.toString();
    }
}
