package act.db.ebean;

import act.event.ActEvent;
import com.avaje.ebean.config.ServerConfig;

/**
 * The event triggered right before Ebean server is created
 */
public class PreEbeanCreation extends ActEvent<ServerConfig> {
    public PreEbeanCreation(ServerConfig source) {
        super(source);
    }
}
