package com.parkhfx.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One injectable Clock so anything time-dependent (hold expiry, pricing windows)
 * can be tested with a fixed clock instead of the system time.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
