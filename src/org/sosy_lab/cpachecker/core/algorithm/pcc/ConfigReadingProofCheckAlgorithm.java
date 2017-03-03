/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pcc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.util.ValidationConfigurationBuilder;

@Options(prefix = "pcc")
public class ConfigReadingProofCheckAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
      secure = true,
      name = "proof",
      description = "file in which proof representation needed for proof checking is stored")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  protected Path proofFile = Paths.get("arg.obj");

  private final Configuration valConfig;

  private final ProofCheckAlgorithm checkingAlgorithm;

  public ConfigReadingProofCheckAlgorithm(final Configuration pConfig,
      final LogManager pLogger, final ShutdownNotifier pShutdownNotifier, final CFA pCfa,
      final Specification pSpecification) throws InvalidConfigurationException {
    pConfig.inject(this);

    valConfig = readValidationConfiguration();

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.copyFrom(pConfig);
    configBuilder.copyOptionFrom(valConfig, "pcc.strategy");

    checkingAlgorithm = new ProofCheckAlgorithm(
        instantiateCPA(pLogger, pShutdownNotifier, pCfa, pSpecification),
        configBuilder.build(), pLogger, pShutdownNotifier, pCfa, pSpecification);
  }

  private Configuration readValidationConfiguration() throws InvalidConfigurationException {
    try {
      return ValidationConfigurationBuilder.readConfigFromProof(proofFile);
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Failed to read and build validation configuration from proof", e);
    }
  }

  private ConfigurableProgramAnalysis instantiateCPA(final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa, final Specification pSpecification)
      throws InvalidConfigurationException {
    try {
      CoreComponentsFactory coreFact =
          new CoreComponentsFactory(valConfig, pLogger, pShutdownNotifier,
              new AggregatedReachedSets());

      return coreFact.createCPA(pCfa, pSpecification);
    } catch (CPAException e) {
      throw new InvalidConfigurationException(
          "Failed to read and build validation configuration from proof", e);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    checkingAlgorithm.collectStatistics(pStatsCollection);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    return checkingAlgorithm.run(pReachedSet);
  }

}
