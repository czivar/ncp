package net.es.ncp.viz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mines.jtk.awt.ColorMap;
import net.es.ncp.prop.VizConfig;
import net.es.ncp.topo.Edge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.DocumentDefaultsDefinition;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.List;

@Component
public class VizExporter {
    @Autowired
    private VizConfig config;

    public void export(Date date, Map<Edge, Long> utilization) {
        VizGraph g = VizGraph.builder().edges(new ArrayList<>()).nodes(new ArrayList<>()).build();

        List<String> seenNodes = new ArrayList<>();
        Double minBw = Double.MAX_VALUE;
        Double maxBw = 0.0;

        Map<String, Double> nodeIngresses = new HashMap<>();
        for (Edge edge : utilization.keySet()) {
            Double bw = utilization.get(edge).doubleValue();

            String a = edge.getA();

            Double nodeIngress = bw;
            if (nodeIngresses.keySet().contains(a)) {
                nodeIngress += nodeIngresses.get(a);
            }
            nodeIngresses.put(a, nodeIngress);

            if (bw > maxBw) {
                maxBw = bw;
            }
            if (bw < minBw) {
                minBw = bw;
            }
        }


        java.awt.image.IndexColorModel icm = ColorMap.JET;
        ColorMap ecm = new ColorMap(minBw, maxBw, icm);

        utilization.keySet().forEach(edge -> {
            Long value = utilization.get(edge);
            String a = edge.getA();
            String z = edge.getZ();
            this.makeNode(a, seenNodes, nodeIngresses, g);
            this.makeNode(z, seenNodes, nodeIngresses, g);

            String rgb = this.toWeb(ecm.getColor(value.doubleValue()));
            String title = shorten(value.doubleValue());

            VizEdge ve = VizEdge.builder()
                    .from(a).to(z).title(title).label(title).value(value.intValue())
                    .arrows("to").arrowStrikethrough(false).color(rgb)
                    .build();
            g.getEdges().add(ve);

        });

        ObjectMapper mapper = new ObjectMapper();

        String filename = config.getOutputFilename().replace("%date", date.toString());
        filename = config.getOutputDir() + "/" + filename;

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), g);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeNode(String node, List<String> seenNodes, Map<String, Double> nodeIngresses, VizGraph g) {
        if (seenNodes.contains(node)) {
            return;
        }
        seenNodes.add(node);
        Double ingress = 0.0;
        if (nodeIngresses.keySet().contains(node)) {
            ingress = nodeIngresses.get(node);
        }
        String title = shorten(ingress);

        VizNode n = VizNode.builder().id(node).label(node).title(title).value(ingress.intValue()).build();
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
