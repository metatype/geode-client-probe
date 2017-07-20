/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import oshi.SystemInfo;

public class GeodeClientProbe implements Runnable {
  private static final Logger logger = LogManager.getLogger(GeodeClientProbe.class);
  
  @Parameter(names={"--locator", "-l"}, description="locator hostname or IP address", required=true)
  String locator;

  @Parameter(names={"--port", "-p"}, description="locator port", required=true)
  int port;

  @Parameter(names={"--region", "-r"}, description="region name", required=true)
  String regionName;

  @Parameter(names={"--log-level"}, description="log level")
  String logLevel = "info";

  @Parameter(names={"--dump-host"}, description="dump host information")
  boolean dumpHost;

  @Parameter(names = "--help", help = true)
  private boolean help;
  
  public static void main(String[] args) throws Exception {
    GeodeClientProbe probe = new GeodeClientProbe();
    JCommander cmd = JCommander.newBuilder()
        .addObject(probe)
        .build();
    cmd.parse(args);
    
    if (probe.help) {
      cmd.usage();
      System.exit(0);
    }
    
    probe.run();
  }

  @Override
  public void run() {
    if (dumpHost) {
      SystemInfoTest.main(null);
    }
    
    logger.info("Connecting to {}:{}", locator, port);
    ClientCache cache = new ClientCacheFactory()
        .addPoolLocator(locator, port)
        .set("log-level", logLevel)
        .create();
    
    logger.info("Creating client proxy region {}", regionName);
    Region region = cache
      .createClientRegionFactory(ClientRegionShortcut.PROXY)
      .create(regionName);
   
    int keys = region.sizeOnServer();
    logger.info("Found {} keys in region {}", keys, regionName);

    logger.info("\nRegion {} contains the following keys:", regionName);
      for (Object key : region.keySetOnServer()) {
        Object val = region.get(key);
        logger.info("\t{} = {}", key, val);
      }

    logger.info("Closing connection to cache");
    cache.close();
  }
}
