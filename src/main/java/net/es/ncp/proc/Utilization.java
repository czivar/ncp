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
import java.util.*;

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
        topo.getDevices().forEach(graph::addVertex);

        topo.getEdges().forEach(e -> {
            graph.addEdge(e, e.getA(), e.getZ(), EdgeType.DIRECTED);
        });

        Map<String, List<Edge>> cache = new HashMap<>();

        DijkstraShortestPath<String, Edge> alg = new DijkstraShortestPath<>(graph, wtTransformer);

        List<String> reports = new ArrayList<>();
        Map<Date, Map<Edge, Long>> utilizations = new HashMap<>();

        traffic.getEntries().keySet().forEach(date -> {

            Map<Edge, Long> utilization = new HashMap<>();
            utilizations.put(date, utilization);

            traffic.getEntries().get(date).forEach(entry -> {
                List<Edge> path;

                String az = entry.getA()+"-"+entry.getZ();
                if (cache.containsKey(az)) {
                    long startTime = System.nanoTime();
                    path = cache.get(az);
                    long endTime = System.nanoTime();
                    // log.info("Cache lookup took " + (endTime - startTime) + " usec");

                } else {
                    long startTime = System.nanoTime();
                    path = alg.getPath(entry.getA(), entry.getZ());
                    long endTime = System.nanoTime();
                    // log.info("Normal pathfinding took " + (endTime - startTime) + " usec");
                    cache.put(az, path);
                }

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
        });

        utilizations.keySet().forEach(date -> {
            reports.add("report for: "+date);
            Map<Edge, Long> utilization = utilizations.get(date);
            utilization.keySet().forEach(edge -> {
                Long mbps = utilization.get(edge);
                reports.add(edge.getA() + " -- " + edge.getZ() + " (" + edge.getName() + ") : " + mbps);
            });
        });

        String report = String.join("\n", reports);
        log.info("\n" + report);


    }


}
