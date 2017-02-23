package net.es.ncp.cont;

import lombok.extern.slf4j.Slf4j;
import net.es.ncp.in.Entry;
import net.es.ncp.proc.UtilizationProcessor;
import net.es.ncp.report.UtilizationReport;
import net.es.ncp.topo.Edge;
import net.es.ncp.viz.VizExporter;
import net.es.ncp.viz.VizGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import javax.servlet.http.HttpServletResponse;
import java.awt.print.Book;
import java.io.IOException;
import java.util.*;

@Slf4j
@Controller
@EnableAutoConfiguration
public class MainController {
    @Autowired
    private UtilizationProcessor processor;

    @Autowired
    private VizExporter vizExporter;

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public void handleResourceNotFoundException(NoSuchElementException ex) {
        // LOG.warn("user requested a strResource which didn't exist", ex);
    }

    @RequestMapping("/")
    public String main_page(Model model) {
        List<Map<String, String>> tuples = new ArrayList<>();

        Map<String, List<UtilizationReport>> reports = processor.getReports();


        reports.keySet().forEach(c -> {
            reports.get(c).forEach(report -> {
                Date date  = report.getDate();
                String comment = report.getComment();

                Map<String, String> tuple = new HashMap();
                tuple.put("href", "classifier/" + c + "/timestamp/" + date.getTime());
                tuple.put("text", c + " - " + date.toString());
                tuple.put("comment", comment);
                tuples.add(tuple);
            });

        });

        model.addAttribute("tuples", tuples);
        return "list";
    }


    @RequestMapping("/utilization/classifier/{classifier}/timestamp/{timestamp}")
    public String utilization(@PathVariable String classifier, @PathVariable Long timestamp, Model model) {

        String util_url = "/report/"+classifier+"/"+timestamp.toString();
        Date date = new Date(timestamp);

        model.addAttribute("util_url", util_url);

        List<Entry> entries = new ArrayList<>();
        Optional<UtilizationReport> opt = processor.getReport(classifier, date);
        if (opt.isPresent()) {

            Map<Edge, Long> edges = opt.get().getEdges();
            for (Edge edge : edges.keySet()) {
                Long mbps = edges.get(edge);
                Entry entry = Entry.builder().a(edge.getA()).z(edge.getZ()).mbps(mbps).build();
                entries.add(entry);
            }
        }

        model.addAttribute("entries", entries);
        return "utilization";
    }

    @RequestMapping("/csv/classifier/{classifier}/timestamp/{timestamp}")
    public void downloadCSV(@PathVariable String classifier, @PathVariable Long timestamp, HttpServletResponse response) throws IOException {

        Date date = new Date(timestamp);

        String csvFileName = classifier+ "_"+date.toString()+".csv";

        response.setContentType("text/csv");

        // creates mock data
        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"",
                csvFileName);
        response.setHeader(headerKey, headerValue);



        // uses the Super CSV API to generate CSV data from the model data
        ICsvMapWriter csvWriter = new CsvMapWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);

        String[] header = { "a", "z", "mbps" };

        csvWriter.writeHeader(header);

        List<Entry> entries = new ArrayList<>();
        Optional<UtilizationReport> opt = processor.getReport(classifier, date);
        if (opt.isPresent()) {
            Map<Edge, Long> edges = opt.get().getEdges();
            for (Edge e : edges.keySet()) {
                Map<String, Object> entry = new HashMap<String, Object>();
                entry.put("a", e.getA());
                entry.put("z", e.getZ());
                entry.put("mbps", edges.get(e));
                csvWriter.write(entry, header);

            }
        }


        csvWriter.close();
    }

    @RequestMapping(value = "/classifiers", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, List<Date>> classifiers() {
        Map<String, List<Date>> result = new HashMap<>();
        processor.getClassifiers().forEach(c -> {
            result.put(c, new ArrayList<>());
            processor.getDates(c).forEach(date -> {
                result.get(c).add(date);
            });

        });
        return result;
    }


    @RequestMapping(value = "/classifier/{classifier}/dates", method = RequestMethod.GET)
    @ResponseBody
    public List<Date> dates_for(@PathVariable String classifier) {
        List<Date> result = new ArrayList<>();
        processor.getDates(classifier).forEach(date -> {
            result.add(date);
        });
        return result;
    }

    @RequestMapping(value = "/report/{classifier}/{timestamp}", method = RequestMethod.GET)
    @ResponseBody
    public VizGraph viz_for(@PathVariable String classifier, @PathVariable Long timestamp) {
        Date date = new Date(timestamp);
        Optional<UtilizationReport> opt = processor.getReport(classifier, date);
        if (opt.isPresent()) {
            VizGraph g = vizExporter.vizGraph(opt.get());

            return g;
        } else {
            throw new NoSuchElementException("graph not found");
        }
    }

}
