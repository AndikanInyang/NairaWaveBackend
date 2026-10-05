package com.Vencedora.NairaWave.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankResolveResponse {
    private String accountName;
    private String bankCode;
    private String accountNumber;
    private String bankName;
}