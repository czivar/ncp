package net.es.ncp.proc;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import lombok.extern.slf4j.Slf4j;
import net.es.ncp.in.InputTraffic;
import net.es.ncp.in.Traffic;
import net.es.ncp.report.PathReport;
import net.es.ncp.report.UtilizationReport;
import net.es.ncp.topo.Edge;
import net.es.ncp.topo.Topology;
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

    private PathReport pathReport;

    public PathReport getPathReport() {
        return pathReport;
    }

    private Map<String, Map<Date, UtilizationReport>> utilizationReports;

    public Optional<UtilizationReport> getReport(String classifier, Date date) {
        if (utilizationReports.keySet().contains(classifier)) {
            Map<Date, UtilizationReport> ofClassifier = utilizationReports.get(classifier);
            if (ofClassifier.keySet().contains(date)) {
                return Optional.of(ofClassifier.get(date));
            }
        }
        return Optional.empty();
    }

    public Set<String> getClassifiers() {
        return utilizationReports.keySet();
    }

    public Set<Date> getDates(String classifier) {
        if (utilizationReports.keySet().contains(classifier)) {
            return utilizationReports.get(classifier).keySet();
        } else {
            return new HashSet<Date>();
        }
    }


    @PostConstruct
    public void process() {
        Topology topo = input.getTopology();
        InputTraffic inputTraffic = input.getTraffic();

        Graph<String, Edge> graph = new DirectedSparseMultigraph<>();

        Transformer<Edge, Double> wtTransformer = edge -> edge.getMetric().doubleValue();
        topo.getDevices().forEach(graph::addVertex);

        topo.getEdges().forEach(e -> {
            graph.addEdge(e, e.getA(), e.getZ(), EdgeType.DIRECTED);
        });

        Map<String, List<Edge>> cache = new HashMap<>();

        DijkstraShortestPath<String, Edge> alg = new DijkstraShortestPath<>(graph, wtTransformer);

        PathReport pathReport = PathReport.builder().paths(new HashMap<>()).build();

        utilizationReports = new HashMap<>();


        inputTraffic.getClassified().forEach(classifiedTraffic -> {
            Traffic traffic = classifiedTraffic.getTraffic();
            String classifier = classifiedTraffic.getClassifier();

            utilizationReports.put(classifier, new HashMap<>());

            Map<Date, Map<Edge, Long>> utilizationsByDate = new HashMap<>();
            // for each date in classifier
            traffic.getEntries().keySet().forEach(date -> {
                log.info("processing cls: " + classifier + " date:" + date);
                Map<Edge, Long> edgeUtilForDate = new HashMap<>();

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
                            if (edgeUtilForDate.containsKey(edge)) {
                                edgeUtilForDate.put(edge, edgeUtilForDate.get(edge) + entry.getMbps());
                            } else {
                                edgeUtilForDate.put(edge, entry.getMbps());
                            }
                        }
                        pathReport.getPaths().put(az, path);
                    }
                    utilizationsByDate.put(date, edgeUtilForDate);

                });
                UtilizationReport report = UtilizationReport.builder()
                        .date(date)
                        .edges(edgeUtilForDate)
                        .build();
                utilizationReports.get(classifier).put(date, report);

                // end for each date in classifier
            });

        });


    }


}
