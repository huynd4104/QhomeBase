package com.QhomeBase.datadocsservice.service;

import com.QhomeBase.datadocsservice.dto.ContractDto;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public final class PdfFieldMapper {

    private PdfFieldMapper() {}

    public record BuyerInfo(
            String name,
            String idNo,
            String idDate,
            String idPlace,
            String residence,
            String address,
            String phone,
            String fax,
            String bankAcc,
            String bankName,
            String taxCode
    ) {}

    public static Map<String, String> mapFromContract(ContractDto c, BuyerInfo buyer) {
        Map<String, String> m = new HashMap<>();
        DateTimeFormatter d = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (c.getStartDate() != null) {
            m.put("day", String.valueOf(c.getStartDate().getDayOfMonth()));
            m.put("month", String.valueOf(c.getStartDate().getMonthValue()));
            m.put("year", String.valueOf(c.getStartDate().getYear()));
            m.put("startDate", d.format(c.getStartDate()));
        }
        if (c.getEndDate() != null) {
            m.put("endDate", d.format(c.getEndDate()));
        }
        if (c.getPurchaseDate() != null) {
            m.put("purchaseDate", d.format(c.getPurchaseDate()));
        }

        m.put("contractNo", nz(c.getContractNumber()));
        m.put("contractType", nz(c.getContractType()));
        m.put("notes", nz(c.getNotes()));

        // seller left blank per request
        m.put("sellerName", "");
        m.put("sellerRegNo", "");
        m.put("sellerRep", "");
        m.put("sellerRepTitle", "");
        m.put("sellerAddress", "");
        m.put("sellerPhone", "");
        m.put("sellerFax", "");
        m.put("sellerBankAcc", "");
        m.put("sellerBankName", "");
        m.put("sellerTaxCode", "");

        if (buyer != null) {
            m.put("buyerName", nz(buyer.name()));
            m.put("buyerIdNo", nz(buyer.idNo()));
            m.put("buyerIdDate", nz(buyer.idDate()));
            m.put("buyerIdPlace", nz(buyer.idPlace()));
            m.put("buyerResidence", nz(buyer.residence()));
            m.put("buyerAddress", nz(buyer.address()));
            m.put("buyerPhone", nz(buyer.phone()));
            m.put("buyerFax", nz(buyer.fax()));
            m.put("buyerBankAcc", nz(buyer.bankAcc()));
            m.put("buyerBankName", nz(buyer.bankName()));
            m.put("buyerTaxCode", nz(buyer.taxCode()));
        } else {
            m.put("buyerName", "");
            m.put("buyerIdNo", "");
            m.put("buyerIdDate", "");
            m.put("buyerIdPlace", "");
            m.put("buyerResidence", "");
            m.put("buyerAddress", "");
            m.put("buyerPhone", "");
            m.put("buyerFax", "");
            m.put("buyerBankAcc", "");
            m.put("buyerBankName", "");
            m.put("buyerTaxCode", "");
        }

        if (c.getMonthlyRent() != null) {
            m.put("price", c.getMonthlyRent().toString());
        } else if (c.getPurchasePrice() != null) {
            m.put("price", c.getPurchasePrice().toString());
        } else {
            m.put("price", "");
        }
        return m;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}


