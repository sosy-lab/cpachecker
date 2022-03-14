// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.residualprogram;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "conditional.verifier")
public class ConditionalVerifierAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
      secure = true,
      description =
          "configuration for the verification of the residual program which is constructed from"
              + " another verifier's condition")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private @Nullable Path verifierConfig;

  @Option(secure = true, description = "configuration of the residual program generator")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private @Nullable Path generatorConfig;

  private final LogManager logger;
  private final ShutdownNotifier shutdown;
  private final CFA cfa;
  private final Specification spec;
  private final Configuration globalConfig;
  private final ConditionalVerifierStats stats = new ConditionalVerifierStats();

  public ConditionalVerifierAlgorithm(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Specification pSpecification,
      final CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;
    shutdown = pShutdownNotifier;
    spec = pSpecification;
    cfa = pCfa;

    globalConfig = pConfig;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    Preconditions.checkArgument(pReachedSet instanceof ForwardingReachedSet);

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    try {
      Path residProg = Files.createTempFile("residualProg", ".c");

      CFANode entryFunction = AbstractStates.extractLocation(pReachedSet.getFirstState());
      if (!generateResidualProgram(entryFunction, residProg.toString())) {
        return status.withSound(false);
      }

      shutdown.shutdownIfNecessary();

      status =
          status.update(
              verifyResidualProgram(
                  entryFunction.getFunctionName(),
                  residProg.toString(),
                  (ForwardingReachedSet) pReachedSet));

    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "Failed to create temporary file for residual program");
      return status.withSound(false);
    }
    return status;
  }

  private boolean generateResidualProgram(final CFANode pEntryNode, final String residualProg)
      throws InterruptedException, CPAException {
    stats.residGen.start();
    try {
      logger.log(Level.INFO, "Start constructing residual program");

      logger.log(Level.FINE, "Build configuration for residual program generation");
      ConfigurationBuilder configBuild = Configuration.builder();
      try {
        configBuild
            .copyFrom(globalConfig)
            .clearOption("analysis.asConditionalVerifier")
            .clearOption("conditional.verifier.verifierConfig")
            .clearOption("conditional.verifier.generatorConfig")
            .loadFromFile(generatorConfig)
            .copyOptionFromIfPresent(
                globalConfig, "AssumptionGuidingAutomaton.cpa.automaton.inputFile")
            .copyOptionFromIfPresent(globalConfig, "AssumptionAutomaton.cpa.automaton.inputFile")
            .setOption("residualprogram.file", residualProg);
        Configuration config = configBuild.build();
        shutdown.shutdownIfNecessary();

        CoreComponentsFactory coreComponents =
            new CoreComponentsFactory(config, logger, shutdown, AggregatedReachedSets.empty());

        logger.log(Level.FINE, "Build configurable program analysis");
        ConfigurableProgramAnalysis cpa;
        cpa = coreComponents.createCPA(cfa, spec);
        shutdown.shutdownIfNecessary();

        logger.log(Level.FINE, "Instantiate residual program construction algorithm");
        Algorithm algorithm =
            new ResidualProgramConstructionAlgorithm(
                cfa,
                config,
                logger,
                shutdown,
                spec,
                cpa,
                CPAAlgorithm.create(cpa, logger, config, shutdown));
        shutdown.shutdownIfNecessary();

        logger.log(Level.FINE, "Create reached set");
        AbstractState initialState =
            cpa.getInitialState(pEntryNode, StateSpacePartition.getDefaultPartition());
        Precision initialPrecision =
            cpa.getInitialPrecision(pEntryNode, StateSpacePartition.getDefaultPartition());
        ReachedSet reachedSet = coreComponents.createReachedSet(cpa);
        reachedSet.add(initialState, initialPrecision);
        shutdown.shutdownIfNecessary();

        logger.log(Level.FINE, "Run algorithm for residual program construction");
        AlgorithmStatus status = algorithm.run(reachedSet);
        collectStatistics(algorithm);
        Preconditions.checkState(!status.wasPropertyChecked());

        if (reachedSet.hasWaitingState()) {
          logger.log(Level.SEVERE, "Residual program construction failed.");
          return false;
        }

      } catch (IOException | InvalidConfigurationException e) {
        logger.logException(Level.SEVERE, e, "Residual program construction failed");
        return false;
      }

      logger.log(Level.INFO, "Finished construction of residual program");

      return true;
    } finally {
      stats.residGen.stop();
    }
  }

  private AlgorithmStatus verifyResidualProgram(
      final String pEntryFunctionName,
      final String pResidProgPath,
      final ForwardingReachedSet reached)
      throws InterruptedException, CPAException {
    stats.residVerif.start();
    try {
      logger.log(Level.INFO, "Start verification of residual program");

      logger.log(Level.FINE, "Build configuration for verification");
      ConfigurationBuilder configBuild = Configuration.builder();
      try {
        configBuild
            .copyFrom(globalConfig)
            .clearOption("analysis.asConditionalVerifier")
            .clearOption("conditional.verifier.verifierConfig")
            .clearOption("conditional.verifier.generatorConfig")
            .loadFromFile(verifierConfig)
            .setOption("analysis.entryFunction", pEntryFunctionName);
        Configuration config = configBuild.build();
        shutdown.shutdownIfNecessary();

        logger.log(Level.FINE, "Parse constructed residual program");
        stats.residParse.start();
        CFA cfaResidProg =
            new CFACreator(config, logger, shutdown)
                .parseFileAndCreateCFA(Collections.singletonList(pResidProgPath));

        stats.residParse.stop();
        stats.numResidLoc = cfaResidProg.getAllNodes().size();
        shutdown.shutdownIfNecessary();

        CoreComponentsFactory coreComponents =
            new CoreComponentsFactory(config, logger, shutdown, AggregatedReachedSets.empty());

        logger.log(Level.FINE, "Build configurable program analysis");
        ConfigurableProgramAnalysis cpa;
        cpa = coreComponents.createCPA(cfaResidProg, spec);
        collectStatistics(cpa);
        shutdown.shutdownIfNecessary();

        logger.log(Level.FINE, "Get verification algorithm");
        Algorithm algorithm = coreComponents.createAlgorithm(cpa, cfaResidProg, spec);
        shutdown.shutdownIfNecessary();

        logger.log(Level.FINE, "Create reached set");
        AbstractState initialState =
            cpa.getInitialState(
                cfaResidProg.getMainFunction(), StateSpacePartition.getDefaultPartition());
        Precision initialPrecision =
            cpa.getInitialPrecision(
                cfaResidProg.getMainFunction(), StateSpacePartition.getDefaultPartition());
        ReachedSet reachedSet = coreComponents.createReachedSet(cpa);
        reachedSet.add(initialState, initialPrecision);
        reached.setDelegate(reachedSet);
        shutdown.shutdownIfNecessary();

        logger.log(Level.FINE, "Run verification algorithm");
        AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_IMPRECISE;
        try {
          stats.residAnalysis.start();
          status = algorithm.run(reachedSet);
        } finally {
          stats.residAnalysis.stop();
        }
        collectStatistics(algorithm);

        logger.log(Level.INFO, "Finished verification of residual program");

        return status;

      } catch (IOException | InvalidConfigurationException | ParserException e) {
        logger.logException(Level.SEVERE, e, "Verification of residual program failed");
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
    } finally {
      stats.residParse.stopIfRunning();
      stats.residVerif.stop();
    }
  }

  private void collectStatistics(final Object pStatisticsProviderCandidate) {
    if (pStatisticsProviderCandidate instanceof StatisticsProvider) {
      ((StatisticsProvider) pStatisticsProviderCandidate).collectStatistics(stats.substats);
    }
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  private class ConditionalVerifierStats implements Statistics {

    private final Collection<Statistics> substats = new ArrayList<>();
    private final Timer residGen = new Timer();
    private final Timer residVerif = new Timer();
    private final Timer residParse = new Timer();
    private final Timer residAnalysis = new Timer();
    private int numResidLoc;

    @Override
    public void printStatistics(
        final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {
      StatisticsWriter statWriter = StatisticsWriter.writingStatisticsTo(pOut);

      statWriter.put("Time for residual program construction", residGen);
      statWriter.put("Time for residual program verification", residVerif);
      statWriter.put("Time for residual program parsing", residParse);
      statWriter.put("Time for residual program analysis", residAnalysis);
      statWriter.put("Size of original program", cfa.getAllNodes().size());
      statWriter.put("Size of residual program", numResidLoc);
      statWriter.spacer();

      for (Statistics substat : substats) {
        substat.printStatistics(pOut, pResult, pReached);
        substat.writeOutputFiles(pResult, pReached);
      }
    }

    @Override
    public @Nullable String getName() {
      return "Conditional Verifier Statistics";
    }
  }
}
