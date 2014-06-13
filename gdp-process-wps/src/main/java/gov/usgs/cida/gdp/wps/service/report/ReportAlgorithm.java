package gov.usgs.cida.gdp.wps.service.report;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author abramhall
*/
public class ReportAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(ReportAlgorithm.class);
    private int count = 0;
    private final String identifier;
    Map<String, ReportDataSet> dataSets = new HashMap<>();

    public ReportAlgorithm(String identifier) {
        this.identifier = identifier;
    }
    
    public ReportAlgorithm incrementCount() {
        count++;
        return this;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Collection<ReportDataSet> getDataSets() {
        return Collections.unmodifiableCollection(dataSets.values());
    }
    
    public final void addDataSet(ReportDataSet dataSet) {
        ReportDataSet rds = dataSets.get(dataSet.getDataSetURI());
        if (null == rds) {
            rds = dataSet;
        }
        dataSets.put(rds.getDataSetURI(), rds.incrementCount());
    }
    
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"identifier\":").append("\"").append(identifier).append("\"");
        json.append(",");
        json.append("\"count\":").append("\"").append(count).append("\"");
        json.append(",");
        json.append("\"dataSets\":[");
        String prefix = "";
        for (ReportDataSet dataSet : getDataSets()) {
            json.append(prefix);
            prefix = ",";
            json.append(dataSet.toJSON());
        }
        json.append("]}");
        return json.toString();
    }
}