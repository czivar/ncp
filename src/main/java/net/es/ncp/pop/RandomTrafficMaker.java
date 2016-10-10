package net.es.ncp.pop;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.ncp.in.Entry;
import net.es.ncp.in.DateTraffic;
import net.es.ncp.prop.RandomizingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@Data
public class RandomTrafficMaker {
    @Autowired
    private RandomizingConfig config;


    public Map<String, List<DateTraffic>>  generate(List<String> nodes) {
        Map<String, List<DateTraffic>> traffic = new HashMap<>();

        Random r = new Random();
        Long range = config.getMaxTrafficMbps();

        List<Date> dates = new ArrayList<>();

        Random random = new Random();

        Date now = new Date();
        dates.add(now);
        for (int i = 1; i < config.getTimeslices(); i++) {
            Long delta = random.nextLong();
            Long ms = now.getTime() + delta;
            Date randomDate = new Date(ms);
            dates.add(randomDate);
        }

        List<DateTraffic> dtls = new ArrayList<>();
        dates.forEach(date -> {
            DateTraffic dt = DateTraffic.builder().date(date).comment("some comment").entries(new ArrayList<>()).build();


            nodes.forEach(n1 -> {
                nodes.stream().filter(n2 -> !n2.equals(n1)).forEach(n2 -> {
                    long mbps = (long) (r.nextDouble() * range);

                    Entry e = Entry.builder().a(n1).z(n2).mbps(mbps).build();
                    dt.getEntries().add(e);

                });
            });
            dtls.add(dt);
        });

        traffic.put("random", dtls);


        return traffic;

    }


}
