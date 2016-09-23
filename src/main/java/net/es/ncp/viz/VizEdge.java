package net.es.ncp.viz;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VizEdge {
    String to;
    String from;
    Integer value;
    String color;
}
