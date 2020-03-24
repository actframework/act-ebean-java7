package act.db.ebean;

/*-
 * #%L
 * ACT Ebean
 * %%
 * Copyright (C) 2015 - 2020 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.avaje.ebean.config.ServerConfig;

/**
 * Application to implement this interface to do further
 * configuration to {@link ServerConfig Ebean ServerConfig}.
 */
public interface EbeanConfigurator {
    /**
     * Configure the Ebean {@link ServerConfig}.
     * @param ebeanConfig the Ebean config instance
     */
    void configure(ServerConfig ebeanConfig);
}
