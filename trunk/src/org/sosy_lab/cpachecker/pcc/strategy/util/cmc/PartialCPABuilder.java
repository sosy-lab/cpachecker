// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.util.cmc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "pcc.cmc")
public class PartialCPABuilder {

  @Option(secure = true, description = "List of files with configurations to use. ")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> configFiles;

  private final LogManager logger;
  private final Configuration globalConfig;
  private final CFA cfa;
  private final ShutdownNotifier shutdown;
  private final Specification specification;

  public PartialCPABuilder(
      final Configuration config,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa,
      final Specification pSpecification)
      throws InvalidConfigurationException {
    config.inject(this);
    globalConfig = config;
    logger = pLogger;
    cfa = pCfa;
    shutdown = pShutdownNotifier;
    specification = pSpecification;
  }

  public ConfigurableProgramAnalysis buildPartialCPA(
      int iterationNumber, ReachedSetFactory pFactory)
      throws InvalidConfigurationException, CPAException {
    // create configuration for current partial ARG checking
    logger.log(Level.FINEST, "Build CPA configuration");
    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    try {
      if (configFiles == null) {
        throw new InvalidConfigurationException(
            "Require that option pcc.arg.cmc.configFiles is set for proof checking");
      }
      singleConfigBuilder.loadFromFile(configFiles.get(iterationNumber));
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Cannot read configuration for current partial ARG checking.");
    }
    Configuration singleConfig = singleConfigBuilder.build();

    // create CPA to check current partial ARG
    logger.log(Level.FINEST, "Create CPA instance");

    return new CPABuilder(singleConfig, logger, shutdown, pFactory)
        .buildCPAs(cfa, specification, AggregatedReachedSets.empty());
  }
}
