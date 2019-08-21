/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.pcc.strategy.util.cmc;

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
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

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

  public ConfigurableProgramAnalysis buildPartialCPA(int iterationNumber, ReachedSetFactory pFactory)
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
      throw new InvalidConfigurationException("Cannot read configuration for current partial ARG checking.");
    }
    Configuration singleConfig = singleConfigBuilder.build();

    // create CPA to check current partial ARG
    logger.log(Level.FINEST, "Create CPA instance");

    return new CPABuilder(singleConfig, logger, shutdown, pFactory)
        .buildCPAs(cfa, specification, new AggregatedReachedSets());
 }


}
