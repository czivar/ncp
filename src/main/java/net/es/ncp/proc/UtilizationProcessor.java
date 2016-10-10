package net.es.ncp.proc;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import lombok.extern.slf4j.Slf4j;
import net.es.ncp.in.DateTraffic;
import net.es.ncp.in.Entry;
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

    private Map<String, List<UtilizationReport>> utilizationReports;

    public Map<String, List<UtilizationReport>> getReports() {
        return utilizationReports;
    }

    public Optional<UtilizationReport> getReport(String classifier, Date date) {
        if (utilizationReports.keySet().contains(classifier)) {
            List<UtilizationReport> ofClassifier = utilizationReports.get(classifier);
            for (UtilizationReport report : ofClassifier) {
                if (report.getDate().equals(date)) {
                    return Optional.of(report);
                }
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    public Set<String> getClassifiers() {
        return utilizationReports.keySet();
    }

    public Set<Date> getDates(String classifier) {
        Set<Date> dates = new HashSet<>();
        if (utilizationReports.keySet().contains(classifier)) {
            List<UtilizationReport> ofClassifier = utilizationReports.get(classifier);
            for (UtilizationReport report : ofClassifier) {
                dates.add(report.getDate());
            }
        }
        return dates;
    }



    @PostConstruct
    public void process() {
        Topology topo = input.getTopology();
        Map<String, List<DateTraffic>> inputTraffic = input.getTraffic();

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


        inputTraffic.keySet().forEach(classifier-> {
            List<DateTraffic> dateTraffics = inputTraffic.get(classifier);


            utilizationReports.put(classifier, new ArrayList<>());

            // for each date in classifier
            dateTraffics.forEach(dateTraffic -> {
                Date date = dateTraffic.getDate();
                String comment = dateTraffic.getComment();

                List<Entry> entries = dateTraffic.getEntries();
                log.info("processing cls: " + classifier + " date:" + date);
                Map<Edge, Long> edgeUtilForDate = new HashMap<>();
                Map<String, Long> nodeIngresses = new HashMap<>();

                entries.forEach(entry -> {

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

                    Long ingress_bw = entry.getMbps();
                    if (nodeIngresses.keySet().contains(entry.getA())) {
                        ingress_bw = ingress_bw + nodeIngresses.get(entry.getA()) + entry.getMbps();
                    }
                    nodeIngresses.put(entry.getA(), ingress_bw);


                });
                UtilizationReport report = UtilizationReport.builder()
                        .date(date)
                        .comment(comment)
                        .edges(edgeUtilForDate)
                        .nodeIngresses(nodeIngresses)
                        .build();
                utilizationReports.get(classifier).add(report);

                // end for each date in classifier
            });

        });


    }


}
