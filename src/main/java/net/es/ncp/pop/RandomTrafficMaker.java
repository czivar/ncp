package net.es.ncp.pop;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.ncp.in.Entry;
import net.es.ncp.in.Traffic;
import net.es.ncp.prop.RandomizingConfig;
import net.es.ncp.topo.Topology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@Data
public class RandomTrafficMaker {
    @Autowired
    private RandomizingConfig config;


    public Traffic generate(List<String> nodes) {

        Traffic traffic = Traffic.builder()
                .entries(new HashMap<>())
                .build();

        Random r = new Random();
        Long range = config.getMaxTrafficMbps();
        Date date = new Date();
        traffic.getEntries().put(date, new ArrayList<>());


        nodes.forEach(n1 -> {
            nodes.stream().filter(n2 -> !n2.equals(n1)).forEach(n2 -> {
                long mbps = (long)(r.nextDouble() * range);

                Entry e = Entry.builder().a(n1).z(n2).mbps(mbps).build();
                traffic.getEntries().get(date).add(e);

            });
        });


        return traffic;

    }


}
