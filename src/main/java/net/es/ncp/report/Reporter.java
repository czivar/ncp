package net.es.ncp.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mines.jtk.awt.ColorMap;
import net.es.ncp.prop.ReportingConfig;
import net.es.ncp.prop.VizConfig;
import net.es.ncp.topo.Edge;
import net.es.ncp.viz.VizEdge;
import net.es.ncp.viz.VizGraph;
import net.es.ncp.viz.VizNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Component
public class Reporter {
    @Autowired
    private ReportingConfig config;

    public void saveReports(List<UtilizationReport> reports, PathReport pathReport) {


        ObjectMapper mapper = new ObjectMapper();
        for (UtilizationReport report : reports) {
            Date date = report.getDate();
            try {
                String filename = config.getEdgesFilename().replace("%date", date.toString());
                filename = config.getOutputDir()+"/"+filename;
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), report);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            String filename = config.getPathsFilename();
            filename = config.getOutputDir()+"/"+filename;
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), pathReport);
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

}
