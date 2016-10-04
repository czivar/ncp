package net.es.ncp.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.ncp.topo.Edge;

import java.util.Date;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UtilizationReport {
    private Date date;
    private Map<Edge, Long> edges;
    private Map<String, Long> nodeIngresses;
}
