package act.db.ebean;

import act.event.ActEvent;
import com.avaje.ebean.config.ServerConfig;

/**
 * The event get triggered when {@link ServerConfig} is loaded and
 * before the {@link com.avaje.ebean.EbeanServer} is created.
 *
 * Application can use this event to do further configuration on
 * {@link ServerConfig}
 */
public class EbeanConfigLoaded extends ActEvent<ServerConfig> {
    public EbeanConfigLoaded(ServerConfig source) {
        super(source);
    }
}
