// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.pcc;

import java.io.IOException;
import java.nio.file.Path;
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
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.util.ValidationConfigurationBuilder;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

@Options(prefix = "pcc")
public class ConfigReadingProofCheckAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
      secure = true,
      name = "proof",
      description = "file in which proof representation needed for proof checking is stored")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  protected Path proofFile = Path.of("arg.obj");

  private final Configuration valConfig;
  private final ProofCheckAlgorithm checkingAlgorithm;
  private final CoreComponentsFactory coreFact;
  private final ConfigurableProgramAnalysis valCPA;
  private final CFA cfa;

  public ConfigReadingProofCheckAlgorithm(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa,
      final Specification pSpecification)
      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this);

    cfa = pCfa;

    valConfig = readValidationConfiguration();

    coreFact =
        new CoreComponentsFactory(
            valConfig, pLogger, pShutdownNotifier, AggregatedReachedSets.empty());

    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.copyFrom(pConfig);
    configBuilder.copyOptionFrom(valConfig, "pcc.strategy");

    valCPA = instantiateCPA(pCfa, pSpecification);
    GlobalInfo.getInstance().setUpInfoFromCPA(valCPA);

    checkingAlgorithm =
        new ProofCheckAlgorithm(
            valCPA, configBuilder.build(), pLogger, pShutdownNotifier, pCfa, pSpecification);
  }

  private Configuration readValidationConfiguration() throws InvalidConfigurationException {
    try {
      return ValidationConfigurationBuilder.readConfigFromProof(proofFile);
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Failed to read and build validation configuration from proof", e);
    }
  }

  private ConfigurableProgramAnalysis instantiateCPA(
      final CFA pCfa, final Specification pSpecification) throws InvalidConfigurationException {
    try {
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
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    ReachedSet internalReached = coreFact.createReachedSet(valCPA);
    internalReached.add(
        valCPA.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()),
        valCPA.getInitialPrecision(
            cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()));

    AlgorithmStatus status = checkingAlgorithm.run(internalReached);

    pReachedSet.popFromWaitlist();

    if (!status.isSound()) {
      pReachedSet.add(
          new DummyErrorState(pReachedSet.getFirstState()), SingletonPrecision.getInstance());
    }

    return status;
  }
}
