package com.stentio.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class OrcamentoPublicoConfig {
    @Bean
    public Clock orcamentoClock() {
        return Clock.systemUTC();
    }
}
