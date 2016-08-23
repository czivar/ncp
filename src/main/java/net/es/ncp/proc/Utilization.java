package net.es.ncp.proc;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.ncp.in.Edge;
import net.es.ncp.in.Topology;
import net.es.ncp.in.Traffic;
import net.es.ncp.pop.Input;
import org.apache.commons.collections15.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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

        topo.getEdges().stream().forEach(e ->  {
            graph.addEdge(e, e.getA(), e.getZ(), EdgeType.DIRECTED);

            Edge rev = Edge.builder().a(e.getZ()).z(e.getA()).gbps(e.getGbps()).metric(e.getMetric()).build();
            graph.addEdge(rev, rev.getA(), rev.getZ(), EdgeType.DIRECTED);
        });

        DijkstraShortestPath<String, Edge> alg = new DijkstraShortestPath<>(graph, wtTransformer);


        Map<Edge, Double> utilization = new HashMap<>();

        traffic.getEntries().stream().forEach(entry -> {
            List<Edge> path = alg.getPath(entry.getA(), entry.getZ());

            if (path.size() > 0) {
                String pathString = path.get(0).getA();
                for (Edge edge : path) {
                    pathString += " - " + edge.getZ();

                    if (utilization.containsKey(edge)) {
                        utilization.put(edge, utilization.get(edge) + entry.getGbps());
                    } else {
                        utilization.put(edge, entry.getGbps());
                    }
                }
                log.info(entry.getA() + " path to "+ entry.getZ()+": "+pathString);

            }



        });
        utilization.keySet().stream().forEach(edge -> {
            Double gbps = utilization.get(edge);
            log.info(edge.getA() + " -- " + edge.getZ() + " : "+gbps);

        });

    }


}
