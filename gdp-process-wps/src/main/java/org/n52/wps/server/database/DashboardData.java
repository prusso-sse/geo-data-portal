package org.n52.wps.server.database;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author abramhall
 */
public class DashboardData {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;
    private static final long MILLIS_PER_HOUR = 1000 * 60 * 60;
    private static final long MILLIS_PER_MIN = 1000 * 60;

    private String identifier = null;
    private String status = null;
    private String creationTime = null;
    private String elapsedTime = null;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(creationTime);
        this.creationTime = DATE_FORMAT.format(cal.getTime());
    }

    public String getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = convertMilliTimeToHumanReadable(elapsedTime);
    }

    /**
     * @param time in milliseconds
     * @return human readable string of time elapsed in terms of hours, minutes, and seconds (fractional seconds truncated)
     */
    private String convertMilliTimeToHumanReadable(long time) {
        StringBuilder returnString = new StringBuilder();
        long days = time / MILLIS_PER_DAY;
        if (days > 0) {
            time = time - days * MILLIS_PER_DAY;
            returnString.append(days).append("d ");
        }
        long hours = time / MILLIS_PER_HOUR;
        if (hours > 0) {
            time = time - hours * MILLIS_PER_HOUR;
            returnString.append(hours).append("h ");
        }
        long minutes = time / MILLIS_PER_MIN;
        if (minutes > 0) {
            time = time - minutes * MILLIS_PER_MIN;
            returnString.append(minutes).append("m ");
        }
        long seconds = time / 1000;
        returnString.append(seconds).append("s");
        return returnString.toString();
    }

}
