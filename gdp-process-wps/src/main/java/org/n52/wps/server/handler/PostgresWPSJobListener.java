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

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        for (Runnable runnable : RequestHandler.pool.getQueue()) {
            ((FutureTask) runnable).cancel(true);
            LOGGER.info("cancelling task");
        }
    }
}
