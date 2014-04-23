package org.n52.wps.server.handler;

import java.util.concurrent.FutureTask;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author abramhall
 */
public class PostgresWPSJobListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresWPSJobListener.class);
    private static final String RUNNING_JOBS_QUERY = "select request_id from results where response_type <> 'ProcessSucceeded';";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
//        LOGGER.info("*****************************************************WPSJobListener - context Initialized");
//        Statement st = null;
//        ResultSet rs = null;
//        try {
//            final PostgresDatabase db = PostgresDatabase.getInstance();
//            st = db.getConnection().createStatement();
//            rs = st.executeQuery(RUNNING_JOBS_QUERY);
//            while (rs.next()) {
//                String id = rs.getString(1);
//                LOGGER.info("(\"*****************************************************should probably mark " + id + " as error");
//            }
//        } catch (SQLException ex) {
//            LOGGER.error("Failed to pull unfinished jobs", ex);
//        } finally {
//            if (null != rs) {
//                try {
//                    rs.close();
//                } catch (SQLException ex) {
//                    LOGGER.warn("failed to close result set", ex);
//                }
//            }
//            if (null != st) {
//                try {
//                    st.close();
//                } catch (SQLException ex) {
//                    LOGGER.warn("failed to close statement", ex);
//                }
//            }
//        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("(\"*****************************************************WPSJobListener - context destroyed, cancelling request tasks");
        for (Runnable runnable : RequestHandler.pool.getQueue()) {
            ((FutureTask) runnable).cancel(true);
            LOGGER.info("cancelling task");
        }
    }

}
