// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;

public class ConfigurationLoader {
  private Configuration configuration = null;

  public ConfigurationLoader(
      final Class<? extends Task> defaultContext, 
      final String defaultConfigurationName,
      @Nullable final Path customConfigFile,
      final LogManager pLogManager) throws InvalidConfigurationException {
    if (customConfigFile != null) {
      try {
        configuration = Configuration.builder().loadFromFile(customConfigFile).build();
      } catch (IOException ignored) {
        pLogManager.log(
            Level.SEVERE,
            "Failed to load file ",
            customConfigFile,
            ". Using default configuration.");
      }
    }

    if (configuration == null) {
      configuration =
          Configuration.builder()
              .loadFromResource(defaultContext, defaultConfigurationName)
              .build();
    }
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}
