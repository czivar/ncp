package net.es.ncp.topo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Edge {
    private String a;
    private String z;
    private String name;
    private Long metric;
    private Long mbps;
}
