package net.es.ncp.pop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.ncp.in.*;
import net.es.ncp.topo.Edge;
import net.es.ncp.topo.EdgeMetric;
import net.es.ncp.topo.Topology;
import net.es.ncp.prop.InputConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Data
public class Input {
    @Autowired
    private InputConfig inputConfig;

    private Topology topology;
    private Map<String, List<DateTraffic>> traffic;

    private Map<String, Position> positions;


    @Autowired
    private RandomTrafficMaker randomTrafficMaker;

    @Autowired
    private RandomTopoMaker randomTopoMaker;

    @PostConstruct
    public void startup() throws IOException {
        log.info("Startup. Will attempt import from files set in input.[topology|traffic].filename properties.");
        ObjectMapper mapper = new ObjectMapper();

        if (inputConfig.getRandomTopology()) {
            topology = randomTopoMaker.generate();

        } else {
            File topoFile = inputConfig.getTopology();
            InputTopo inTopo = mapper.readValue(topoFile, InputTopo.class);

            topology = Topology.builder()
                    .devices(inTopo.getNodes())
                    .edges(new ArrayList<>())
                    .build();

            for (InputEdge ie : inTopo.getEdges()) {
                Edge e = Edge.builder().mbps(ie.getMbps()).a(ie.getA()).z(ie.getZ()).name(ie.getA_addr()).build();
                if (inputConfig.getMetric().equals(EdgeMetric.ISIS_COST)) {
                    e.setMetric(ie.getIsis_cost());
                } else if (inputConfig.getMetric().equals(EdgeMetric.LATENCY)) {
                    e.setMetric(ie.getLatency());
                }
                topology.getEdges().add(e);

            }
        }

        if (inputConfig.getRandomTraffic()) {
            traffic = randomTrafficMaker.generate(topology.getDevices());
        } else {
            File trafficFile = inputConfig.getTraffic();
            TypeReference tr = new TypeReference<Map<String, List<DateTraffic>>>(){};
            traffic = mapper.readValue(trafficFile, tr);
        }


        File positionsFile = inputConfig.getPositions();
        positions = mapper.readValue(positionsFile, new TypeReference< Map<String, Position>>() {});
        log.info("positions imported for devices: " + positions.size());


    }

}
