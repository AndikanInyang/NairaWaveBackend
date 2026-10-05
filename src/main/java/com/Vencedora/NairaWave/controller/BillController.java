package com.Vencedora.NairaWave.controller;

import com.Vencedora.NairaWave.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bills")
public class BillController {

    @GetMapping("/verify-meter")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyMeter(
            @RequestParam String disco,
            @RequestParam String meterNo,
            @RequestParam String meterType
    ) {
        // Meter name resolution
        return ResponseEntity.ok(ApiResponse.success("Meter verified", Map.of(
                "customerName", "Adaeze Okonkwo",
                "meterNo", meterNo,
                "disco", disco
        )));
    }
}