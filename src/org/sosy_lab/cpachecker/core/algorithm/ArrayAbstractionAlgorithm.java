// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder;
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
import org.sosy_lab.cpachecker.util.arrayabstraction.ArrayAbstractionNondetRead;
import org.sosy_lab.cpachecker.util.arrayabstraction.ArrayAbstractionNondetSingleCell;
import org.sosy_lab.cpachecker.util.arrayabstraction.ArrayAbstractionSmashing;

@Options(prefix = "arrayAbstraction")
public final class ArrayAbstractionAlgorithm implements Algorithm {

  @Option(
      secure = true,
      name = "delegateAnalysis",
      description = "Configuration of the delegate analysis running on the translated program")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path delegateAnalysisConfigurationFile;

  @Option(secure = true, name = "method", description = "The array abstraction method")
  private ArrayAbstractionMethod method = ArrayAbstractionMethod.NONDET_SINGLE_CELL;

  @Option(
      secure = true,
      name = "checkCounterexamples",
      description =
          "Use a second delegate analysis run to check counterexamples on the original program that"
              + " contains (non-abstracted) arrays.")
  private boolean checkCounterexamples = false;

  @Option(
      secure = true,
      name = "cfa.export",
      description = "Whether to export the CFA with abstracted arrays as DOT file.")
  private boolean exportTranslatedCfa = true;

  @Option(
      secure = true,
      name = "cfa.file",
      description = "DOT file path for CFA with abstracted arrays.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportTranslatedCfaFile = Path.of("cfa-abstracted-arrays.dot");

  private enum ArrayAbstractionMethod {
    NONDET_READ,
    SMASHING,
    NONDET_SINGLE_CELL;
  }

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

    ReachedSet reached = pCoreComponents.createReachedSet(pCpa);
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

  private void exportTranslatedCfa(CFA pTranslatedCfa) {

    if (exportTranslatedCfa && exportTranslatedCfaFile != null && pTranslatedCfa != null) {
      try (Writer w = IO.openOutputFile(exportTranslatedCfaFile, Charset.defaultCharset())) {
        DOTBuilder.generateDOT(w, pTranslatedCfa);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write CFA to dot file");
      }
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    ForwardingReachedSet forwardingReachedSet = (ForwardingReachedSet) pReachedSet;
    AggregatedReachedSets aggregatedReached = AggregatedReachedSets.singleton(pReachedSet);
    Configuration delegateAnalysisConfiguration = createDelegateAnalysisConfiguration();

    CFA translatedCfa;
    switch (method) {
      case NONDET_READ:
        translatedCfa = ArrayAbstractionNondetRead.transformCfa(configuration, logger, originalCfa);
        break;
      case SMASHING:
        translatedCfa = ArrayAbstractionSmashing.transformCfa(configuration, logger, originalCfa);
        break;
      case NONDET_SINGLE_CELL:
        translatedCfa =
            ArrayAbstractionNondetSingleCell.transformCfa(configuration, logger, originalCfa);
        break;
      default:
        translatedCfa = null;
    }

    exportTranslatedCfa(translatedCfa);

    try {

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(
              delegateAnalysisConfiguration, logger, shutdownNotifier, aggregatedReached);

      AlgorithmStatus status = AlgorithmStatus.NO_PROPERTY_CHECKED;

      if (translatedCfa != null) {
        status = runDelegateAnalysis(coreComponents, translatedCfa, forwardingReachedSet);
      }

      if (checkCounterexamples && forwardingReachedSet.wasTargetReached()) {
        status = runDelegateAnalysis(coreComponents, originalCfa, forwardingReachedSet);
      }

      return status;
    } catch (InvalidConfigurationException ex) {
      logger.logUserException(Level.SEVERE, ex, "Cannot run delegate analysis");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
  }
}
