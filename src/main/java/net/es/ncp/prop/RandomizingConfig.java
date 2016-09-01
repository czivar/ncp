package net.es.ncp.prop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "randomTopo")

public class RandomizingConfig {

    private Integer numNodes;
    private Integer maxMetric;

    private Long minLinkMbps;
    private Long maxLinkMbps;
    private Integer maxNodeLinks;

    private Long maxTrafficMbps;


}
