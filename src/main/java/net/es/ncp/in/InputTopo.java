package net.es.ncp.in;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.ncp.topo.Edge;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InputTopo {
    private List<InputEdge> edges;
    private List<String> nodes;
}
