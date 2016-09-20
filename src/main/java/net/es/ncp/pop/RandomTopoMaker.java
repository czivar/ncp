package net.es.ncp.pop;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.ncp.prop.RandomizingConfig;
import net.es.ncp.topo.Edge;
import net.es.ncp.topo.Topology;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Random;

@Service
@Slf4j
@Data
public class RandomTopoMaker {
    @Autowired
    private RandomizingConfig config;


    public Topology generate() {
        Topology topology = Topology.builder().devices(new ArrayList<>()).edges(new ArrayList<>()).build();

        for (int i = 0; i < config.getNumNodes(); i++) {
            String name = RandomStringUtils.randomAlphabetic(8);
            topology.getDevices().add(name);
        }
        Random r = new Random();

        for (int i = 0; i < config.getNumNodes(); i++) {
            Integer numLinks = r.nextInt(config.getMaxNodeLinks()) + 1;

            for (int link = 0; link < numLinks; link++) {
                boolean self = true;
                Integer neighbor = i;
                while (self) {
                    neighbor = r.nextInt(config.getNumNodes());
                    if (i != neighbor) {
                        self = false;
                    }
                }

                Integer metric = r.nextInt(config.getMaxMetric()) + 1;
                Integer mbps = r.nextInt(config.getMaxLinkMbps().intValue()) + 1;

                String a = topology.getDevices().get(i);
                String z = topology.getDevices().get(neighbor);
                String name = a + " <-> " + z;
                Edge e = Edge.builder().a(a).z(z).name(name).metric(metric.longValue()).mbps(mbps.longValue()).build();

                topology.getEdges().add(e);


            }
        }


        return topology;
    }

}
