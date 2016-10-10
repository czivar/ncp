package net.es.ncp.viz;

import edu.mines.jtk.awt.ColorMap;

import lombok.extern.slf4j.Slf4j;
import net.es.ncp.pop.Input;
import net.es.ncp.report.UtilizationReport;
import net.es.ncp.topo.Edge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VizExporter {
    @Autowired
    private Input input;

    public VizGraph vizGraph(UtilizationReport report) {
        log.info("exporting report " + report.getDate());

        VizGraph g = VizGraph.builder().edges(new ArrayList<>()).nodes(new ArrayList<>()).build();

        List<String> seenNodes = new ArrayList<>();
        Map<Edge, Long> utilization = report.getEdges();
        Map<String, Long> nodeIngresses = report.getNodeIngresses();


        List<Edge> sorted = utilization.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());


        List<Edge> top40 = sorted.subList(sorted.size() - 40, sorted.size());

        Double minBw = utilization.get(top40.get(0)).doubleValue();
        Double maxBw = utilization.get(top40.get(top40.size() - 1)).doubleValue();

        // log.info("min / max bw " + minBw + " " + maxBw);
        java.awt.image.IndexColorModel icm = ColorMap.JET;
        ColorMap ecm = new ColorMap(minBw, maxBw, icm);


        utilization.keySet().forEach(edge -> {
            Long value = utilization.get(edge);
            String a = edge.getA();
            String z = edge.getZ();
            Long aIngress = nodeIngresses.get(a);
            Long zIngress = nodeIngresses.get(z);

            this.makeNode(a, seenNodes, aIngress, g);
            this.makeNode(z, seenNodes, zIngress, g);

            String rgb = this.toWeb(ecm.getColor(value.doubleValue()));
            String title = shorten(value.doubleValue());
            String label = title;
            /*
            if (top40.contains(edge)) {
            }
            */

            VizEdge ve = VizEdge.builder()
                    .from(a).to(z).title(title).label(label).value(value.intValue())
                    .arrows("to").arrowStrikethrough(false).color(rgb)
                    .build();
            g.getEdges().add(ve);

        });
        return g;

    }

    private void makeNode(String node, List<String> seenNodes, Long nodeIngress, VizGraph g) {
        if (seenNodes.contains(node)) {
            return;
        }
        if (nodeIngress == null) {
            nodeIngress = 0L;
        }

        seenNodes.add(node);
        Double ingress = nodeIngress.doubleValue();
        // log.info("node " + node + " ingress: " + nodeIngress);
        String title = shorten(ingress);

        VizNode n = VizNode.builder().id(node).label(node).title(title).value(ingress.intValue()).build();
        if (input.getPositions().keySet().contains(node)) {
            n.setFixed(new HashMap<>());
            n.getFixed().put("x", true);
            n.getFixed().put("y", true);
            n.setX(input.getPositions().get(node).getX());
            n.setY(input.getPositions().get(node).getY());
        }
        g.getNodes().add(n);
    }

    private String toWeb(Color c) {
        String rgb = Integer.toHexString(c.getRGB());
        rgb = "#" + rgb.substring(2, rgb.length());
        return rgb;
    }

    private String shorten(Double mbps) {

        BigDecimal bd = new BigDecimal(mbps);
        bd = bd.round(new MathContext(3));
        double rounded = bd.doubleValue();

        if (mbps < 1000.0) {
            return rounded + "M";
        } else if (mbps < 1000.0 * 1000) {
            return rounded / 1000 + "G";
        } else if (mbps < 1000.0 * 1000000) {
            return rounded / 1000000 + "T";
        } else if (mbps < 1000.0 * 1000000000) {
            return rounded / 1000000000 + "P";
        } else {
            return ">1000P";
        }


    }

}
