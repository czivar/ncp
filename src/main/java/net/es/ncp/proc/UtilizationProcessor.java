package net.es.ncp.proc;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import lombok.extern.slf4j.Slf4j;
import net.es.ncp.report.PathReport;
import net.es.ncp.report.Reporter;
import net.es.ncp.report.UtilizationReport;
import net.es.ncp.topo.Edge;
import net.es.ncp.topo.Topology;
import net.es.ncp.in.Traffic;
import net.es.ncp.pop.Input;
import net.es.ncp.viz.VizExporter;
import org.apache.commons.collections15.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@Slf4j
public class UtilizationProcessor {
    @Autowired
    private Input input;

    @Autowired
    private VizExporter exporter;

    @Autowired
    private Reporter reporter;

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


        PathReport pathReport = PathReport.builder().paths(new HashMap<>()).build();
        Map<Date, Map<Edge, Long>> utilizationsByDate = new HashMap<>();

        traffic.getEntries().keySet().forEach(date -> {

            Map<Edge, Long> utilization = new HashMap<>();
            utilizationsByDate.put(date, utilization);

            traffic.getEntries().get(date).forEach(entry -> {
                List<Edge> path;

                String az = entry.getA() + "-" + entry.getZ();
                if (cache.containsKey(az)) {
                    path = cache.get(az);

                } else {
                    path = alg.getPath(entry.getA(), entry.getZ());
                    cache.put(az, path);
                }

                if (path.size() > 0) {
                    for (Edge edge : path) {
                        if (utilization.containsKey(edge)) {
                            utilization.put(edge, utilization.get(edge) + entry.getMbps());
                        } else {
                            utilization.put(edge, entry.getMbps());
                        }
                    }
                    pathReport.getPaths().put(az, path);
                }
            });
        });

        List<UtilizationReport> reports = new ArrayList<>();

        utilizationsByDate.keySet().forEach(date -> {
            Map<Edge, Long> utilization = utilizationsByDate.get(date);
            exporter.export(date, utilization);

            UtilizationReport report = UtilizationReport.builder()
                    .date(date)
                    .edges(new HashMap<>())
                    .build();
            reports.add(report);


            for (Edge edge : utilization.keySet()) {
                String az = edge.getA() +" - "+ edge.getZ();
                Long bw = utilization.get(edge);
                report.getEdges().put(az, bw);
            }

        });

        reporter.saveReports(reports, pathReport);

    }


}
