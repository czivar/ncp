package net.es.ncp.prop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.es.ncp.topo.EdgeMetric;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "input")

public class InputConfig {
    private File topology;
    private Boolean randomTopology;
    private EdgeMetric metric;

    private File traffic;
    private Boolean randomTraffic;

    private File positions;

}
