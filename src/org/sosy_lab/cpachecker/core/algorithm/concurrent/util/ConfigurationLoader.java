// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.util;

import static org.sosy_lab.cpachecker.core.AnalysisDirection.BACKWARD;
import static org.sosy_lab.cpachecker.core.AnalysisDirection.FORWARD;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisFull;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward.ForwardAnalysis;

@Options(prefix = "concurrent.task.config")
public class ConfigurationLoader {
  @Option(secure = true, description =
      "Check provided configurations of ForwardAnalysis and BackwardAnalysis regarding a set of "
          + "properties required for a successful analysis.")
  private boolean verify = true;

  private Configuration configuration = null;

  public ConfigurationLoader(
      final Configuration analysisConfiguration,
      final Class<? extends Task> pTask,
      final String defaultConfigurationName,
      @Nullable final Path customConfigFile,
      final LogManager pLogManager) throws InvalidConfigurationException {
    analysisConfiguration.inject(this);

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
              .loadFromResource(pTask, defaultConfigurationName)
              .build();
    }

    checkConfigurationValidity(pTask, configuration);
  }

  private void checkConfigurationValidity(
      final Class<? extends Task> task,
      final Configuration pConfiguration)
      throws InvalidConfigurationException {
    if (!verify) {
      return;
    }

    RequiredOptions options = new RequiredOptions();
    pConfiguration.inject(options);

    if (!options.cpa.endsWith("ARGCPA")) {
      throw new InvalidConfigurationException(
          "Concurrent analysis requires cpa.arg.ARGCPA as top-level CPA for both ForwardAnalysis and BackwardAnalysis!");
    }

    if (!options.cpas.contains("cpa.predicate.PredicateCPA")) {
      throw new InvalidConfigurationException(
          "Concurrent analysis requires cpa.predicate.PredicateCPA as component CPA for both ForwardAnalysis and BackwardAnalysis!");
    }

    if (task == ForwardAnalysis.class) {
      if (!options.cpas.contains("cpa.location.LocationCPA")) {
        throw new InvalidConfigurationException(
            "ForwardAnalysis configuration requires cpa.location.LocationCPA as component CPA!");
      }
      if (options.direction != FORWARD) {
        throw new InvalidConfigurationException(
            "ForwardAnalysis configuration requires cpa.predicate.direction=FORWARD!");
      }
    } else if (task == BackwardAnalysisFull.class) {
      if (!options.cpas.contains("cpa.location.LocationCPABackwards")) {
        throw new InvalidConfigurationException(
            "BackwardAnalysis configuration requires cpa.location.LocationCPABackwards as component CPA!");
      }
      if (options.direction != BACKWARD) {
        throw new InvalidConfigurationException(
            "BackwardAnalysis configuration requires cpa.predicate.direction=BACKWARD!");
      }
    }

    if (options.handlePointerAliasing) {
      throw new InvalidConfigurationException(
          "Concurrent analysis requires cpa.predicate.handlePointerAliasing=false for both ForwardAnalysis and BackwardAnalysis!");
    }

    if (options.alwaysAtFunctions) {
      throw new InvalidConfigurationException(
          "Concurrent analysis requires cpa.predicate.blk.alwaysAtFunctions=false for both ForwardAnalysis and BackwardAnalysis!");
    }

    if (options.alwaysAtTarget) {
      throw new InvalidConfigurationException(
          "Concurrent analysis requires cpa.predicate.blk.alwaysAtTarget=false for both ForwardAnalysis and BackwardAnalysis!");
    }

    if (options.alwaysAtLoops) {
      throw new InvalidConfigurationException(
          "Concurrent analysis requires cpa.predicate.blk.alwaysAtLoops=false for both ForwardAnalysis and BackwardAnalysis!");
    }
  }

  public Configuration getConfiguration() throws InvalidConfigurationException {
    return configuration;
  }

  @Options
  private static class RequiredOptions {
    @Option(name = "cpa", description = "duplicate for verification")
    public String cpa;

    @Option(name = "CompositeCPA.cpas", description = "duplicate for verification")
    public List<String> cpas;

    @Option(name = "cpa.predicate.direction", description = "duplicate for verification")
    public AnalysisDirection direction = FORWARD;

    @Option(name = "cpa.predicate.handlePointerAliasing", description = "duplicate for verification")
    public boolean handlePointerAliasing = true;

    @Option(name = "cpa.predicate.blk.alwaysAtFunctions", description = "duplicate for verification")
    public boolean alwaysAtFunctions = true;

    @Option(name = "cpa.predicate.blk.alwaysAtLoops", description = "duplicate for verification")
    public boolean alwaysAtLoops = true;

    @Option(name = "cpa.predicate.blk.alwaysAtTarget", description = "duplicate for verification")
    public boolean alwaysAtTarget = true;
  }
}
