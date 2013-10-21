package gov.usgs.cida.n52.wps.config;

import static gov.usgs.cida.gdp.constants.AppConstant.*;
import java.io.IOException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.xmlbeans.XmlException;
import org.n52.wps.commons.WPSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web application lifecycle listener.
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class ForceConfigInitializeListener implements ServletContextListener {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(ForceConfigInitializeListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String configLocation = WPS_CONFIG_LOCATION.getValue();
        try {
            WPSConfig.forceInitialization(configLocation);
        }
        catch (XmlException ex) {
            LOGGER.error("Could not initialize configuration", ex);
        }
        catch (IOException ex) {
            LOGGER.error("Input/Output exception initializing configuration", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // do nothing
    }
}
