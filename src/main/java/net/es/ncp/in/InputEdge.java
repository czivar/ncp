package net.es.ncp.in;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InputEdge {
    private String name;
    private String a;
    private String z;
    private String a_addr;
    private String z_addr;
    private Long isis_cost;
    private Long latency;
    private Long mbps;
}
