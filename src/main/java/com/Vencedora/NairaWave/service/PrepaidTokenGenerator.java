/**
 This particular code is only being used because
 this a demo case and should be substituted for
 an actual provider token in real life production
 **/

package com.Vencedora.NairaWave.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PrepaidTokenGenerator {

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            digits.append(random.nextInt(10));
        }
        String d = digits.toString();
        return d.substring(0, 4) + "-" + d.substring(4, 8) + "-"
                + d.substring(8, 12) + "-" + d.substring(12, 16) + "-" + d.substring(16, 20);
    }
}