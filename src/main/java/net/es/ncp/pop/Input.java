package net.es.ncp.pop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.ncp.in.Topology;
import net.es.ncp.in.Traffic;
import net.es.ncp.prop.InputConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@Service
@Slf4j
@Data
public class Input {
    @Autowired
    private InputConfig inputConfig;

    private Topology topology;
    private Traffic traffic;

    @PostConstruct
    public void startup() throws IOException {
        log.info("Startup. Will attempt import from files set in input.[topology|traffic].filename properties.");
        File topoFile = inputConfig.getTopology();
        File trafficFile = inputConfig.getTraffic();

        ObjectMapper mapper = new ObjectMapper();

        topology = mapper.readValue(topoFile, Topology.class);
        traffic = mapper.readValue(trafficFile, Traffic.class);

        log.info(topology.toString());
        log.info(traffic.toString());



    }

}
