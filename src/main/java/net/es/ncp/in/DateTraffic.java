package net.es.ncp.in;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DateTraffic {
    private Date date;
    private String comment;

    private List<Entry> entries;
}
