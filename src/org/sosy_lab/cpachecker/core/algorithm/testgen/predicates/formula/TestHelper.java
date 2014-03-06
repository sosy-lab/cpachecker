/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.testgen.predicates.formula;

import java.io.IOException;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;


public class TestHelper {

  public StartupConfig createPredicateConfig()
  {
    try {
      return createConfigSet("config/predicateAnalysis.properties");
    } catch (InvalidConfigurationException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public StartupConfig createConfigSet(String configFileName) throws InvalidConfigurationException, IOException
  {

    Configuration lConfig = Configuration.builder()
        .loadFromFile(configFileName).build();
    LogManager logManager = new BasicLogManager(lConfig);

    ShutdownNotifier lShutdownNotifier = ShutdownNotifier.create();
    return new StartupConfig(lConfig, logManager, lShutdownNotifier);
  }

  public class StartupConfig {

    public StartupConfig(Configuration pLConfig, LogManager pLogManager, ShutdownNotifier pLShutdownNotifier) {
      this.config = pLConfig;
      this.notifier = pLShutdownNotifier;
      this.log = pLogManager;
    }

    private Configuration config;
    private ShutdownNotifier notifier;
    private LogManager log;

    public Configuration getConfig() {
      return config;
    }

    public ShutdownNotifier getNotifier() {
      return notifier;
    }

    public LogManager getLog() {
      return log;
    }
  }

}
