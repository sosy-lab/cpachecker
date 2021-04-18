// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.ArrayAbstraction;

@Options(prefix = "arrayAbstraction")
public final class ArrayAbstractionAlgorithm implements Algorithm {

  @Option(
      secure = true,
      name = "delegateAnalysis",
      description = "Configuration of the delegate analysis running on the translated program")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path delegateAnalysisConfigurationFile;

  private final Configuration configuration;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;
  private final CFA originalCfa;

  public ArrayAbstractionAlgorithm(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {

    configuration = pConfiguration;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    specification = pSpecification;
    originalCfa = pCfa;

    pConfiguration.inject(this);
  }

  private Configuration createDelegateAnalysisConfiguration() {

    try {

      ConfigurationBuilder delegateConfigBuilder = Configuration.builder();
      delegateConfigBuilder.copyFrom(configuration);
      delegateConfigBuilder.clearOption("analysis.useArrayAbstraction");
      delegateConfigBuilder.clearOption("arrayAbstraction.delegateAnalysis");
      delegateConfigBuilder.loadFromFile(delegateAnalysisConfigurationFile);

      Configuration delegateConfig = delegateConfigBuilder.build();
      NestingAlgorithm.checkConfigs(
          configuration, delegateConfig, delegateAnalysisConfigurationFile, logger);
      return delegateConfig;

    } catch (IOException | InvalidConfigurationException ex) {

      logger.logfUserException(
          Level.SEVERE,
          ex,
          "Cannot read configuration file for delegate analysis: %s",
          delegateAnalysisConfigurationFile);

      return null;
    }
  }

  private ReachedSet createInitialReachedSet(
      CoreComponentsFactory pCoreComponents, ConfigurableProgramAnalysis pCpa, CFA pCfa)
      throws InterruptedException {

    ReachedSet reached = pCoreComponents.createReachedSet();
    CFANode mainFunction = pCfa.getMainFunction();

    AbstractState initialState =
        pCpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        pCpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());
    reached.add(initialState, initialPrecision);

    return reached;
  }

  private AlgorithmStatus runDelegateAnalysis(
      CoreComponentsFactory pCoreComponents, CFA pCfa, ForwardingReachedSet pForwardingReachedSet)
      throws InvalidConfigurationException, InterruptedException,
          CPAEnabledAnalysisPropertyViolationException, CPAException {

    ConfigurableProgramAnalysis cpa = pCoreComponents.createCPA(pCfa, specification);
    Algorithm algorithm = pCoreComponents.createAlgorithm(cpa, pCfa, specification);
    ReachedSet reached = createInitialReachedSet(pCoreComponents, cpa, pCfa);

    AlgorithmStatus status = algorithm.run(reached);

    while (reached.hasWaitingState()) {
      status = algorithm.run(reached);
    }

    pForwardingReachedSet.setDelegate(reached);

    return status;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    ForwardingReachedSet forwardingReachedSet = (ForwardingReachedSet) pReachedSet;
    AggregatedReachedSets aggregatedReached =
        new AggregatedReachedSets(ImmutableSet.of(pReachedSet));
    Configuration delegateAnalysisConfiguration = createDelegateAnalysisConfiguration();

    CFA translatedCfa = ArrayAbstraction.transformCfa(configuration, logger, originalCfa);

    try {

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(
              delegateAnalysisConfiguration, logger, shutdownNotifier, aggregatedReached);

      AlgorithmStatus status =
          runDelegateAnalysis(coreComponents, translatedCfa, forwardingReachedSet);

      if (forwardingReachedSet.hasViolatedProperties()) {
        status = runDelegateAnalysis(coreComponents, originalCfa, forwardingReachedSet);
      }

      return status;
    } catch (InvalidConfigurationException ex) {
      logger.logUserException(Level.SEVERE, ex, "Cannot run delegate analysis");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
  }
}
