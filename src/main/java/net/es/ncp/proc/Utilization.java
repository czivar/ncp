package net.es.ncp.proc;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import lombok.extern.slf4j.Slf4j;
import net.es.ncp.topo.Edge;
import net.es.ncp.topo.Topology;
import net.es.ncp.in.Traffic;
import net.es.ncp.pop.Input;
import org.apache.commons.collections15.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class Utilization {
    @Autowired
    private Input input;

    @PostConstruct
    public void process() {
        Topology topo = input.getTopology();
        Traffic traffic = input.getTraffic();

        Graph<String, Edge> graph = new DirectedSparseMultigraph<>();

        Transformer<Edge, Double> wtTransformer = edge -> edge.getMetric().doubleValue();
        topo.getDevices().stream().forEach(graph::addVertex);

        topo.getEdges().stream().forEach(e -> {
            graph.addEdge(e, e.getA(), e.getZ(), EdgeType.DIRECTED);
        });

        DijkstraShortestPath<String, Edge> alg = new DijkstraShortestPath<>(graph, wtTransformer);

        List<String> reports = new ArrayList<>();
        Map<Edge, Long> utilization = new HashMap<>();

        traffic.getEntries().stream().forEach(entry -> {
            List<Edge> path = alg.getPath(entry.getA(), entry.getZ());

            if (path.size() > 0) {
                String pathString = path.get(0).getA();
                for (Edge edge : path) {
                    pathString += " - " + edge.getZ();

                    if (utilization.containsKey(edge)) {
                        utilization.put(edge, utilization.get(edge) + entry.getMbps());
                    } else {
                        utilization.put(edge, entry.getMbps());
                    }
                }
                reports.add(entry.getA() + " <===> " + entry.getZ() + ": " + pathString);

            }
        });

        utilization.keySet().stream().forEach(edge -> {
            Long mbps = utilization.get(edge);
            reports.add(edge.getA() + " -- " + edge.getZ() + " ("+edge.getName()+") : " + mbps);
        });
        String report = String.join("\n", reports);
        log.info("\n"+report);



    }


}
