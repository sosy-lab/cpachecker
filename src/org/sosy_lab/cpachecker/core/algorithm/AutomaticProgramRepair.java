// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair.CFAMutator;
import org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair.Mutation;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "programRepair")
public class AutomaticProgramRepair implements Algorithm, StatisticsProvider, Statistics {

  private final FaultLocalizationWithTraceFormula algorithm;
  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final Specification specification;
  private final ShutdownNotifier shutdownNotifier;
  private final ShutdownManager shutdownManager;

  private final StatTimer totalTime = new StatTimer("Total time for bug repair");
  private boolean fixFound = false;
  private CFAEdgeType fixedEdgeType;


  @Option(secure = true, required = true, description = "Config file of the internal analysis.")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private AnnotatedValue<Path> internalAnalysisConfigFile;

  public AutomaticProgramRepair(
      final Algorithm pStoreAlgorithm,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final Specification pSpecification,
      final ShutdownNotifier pShutdownNotifier,
      final ShutdownManager pShutdownManager)
      throws InvalidConfigurationException {

    if (!(pStoreAlgorithm instanceof FaultLocalizationWithTraceFormula)) {
      throw new InvalidConfigurationException(
          "Option FaultLocalizationWithTraceFormula required, found: "
              + pStoreAlgorithm.getClass());
    }

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "Automatic program repair is only supported for C code.");
    }

    algorithm = (FaultLocalizationWithTraceFormula) pStoreAlgorithm;
    config = pConfig;
    logger = pLogger;
    cfa = pCfa;

    specification = pSpecification;
    shutdownNotifier = pShutdownNotifier;
    shutdownManager = pShutdownManager;

    pConfig.inject(this);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    totalTime.start();

    AlgorithmStatus status = algorithm.runParentAlgorithm(reachedSet);

    try {
      logger.log(Level.INFO, "Starting program repair...");

      for (FaultLocalizationInfo faultLocalizationInfo : localizeFaults(reachedSet)) {
        runAlgorithm(faultLocalizationInfo);
      }

      logger.log(Level.INFO, "Stopping program repair...");
    } catch (InvalidConfigurationException e) {
      logger.logUserException(Level.SEVERE, e, "Invalid configuration");
    } catch (SolverException e) {
      logger.logUserException(Level.SEVERE, e, "Solver Failure");
    } catch (AutomaticProgramRepairException e) {
      Throwables.throwIfInstanceOf(e.getCause(), InterruptedException.class);
      Throwables.throwIfInstanceOf(e.getCause(), CPAException.class);
      throw new AssertionError(e);
    } finally {
      totalTime.stop();
    }

    return status;
  }

  private void runAlgorithm(FaultLocalizationInfo faultLocalizationInfo) {
    for (Fault fault : faultLocalizationInfo.getRankedList()) {
      for (FaultContribution faultContribution : fault) {
        CFAEdge edge = faultContribution.correspondingEdge();
        CFAMutator cfaMutator = new CFAMutator(cfa, edge, config, logger);

        cfaMutator
            .calcPossibleMutations()
            .parallel()
            .filter(
                (Mutation mutation) -> {
                  try {
                    return wasRepairSuccessful(mutation.getCFA());
                  } catch (InvalidConfigurationException e) {
                    logger.logUserException(Level.SEVERE, e, "Invalid configuration");
                    return false;
                  } catch (IOException e) {
                    logger.logUserException(Level.SEVERE, e, "IO failed");
                    return false;
                  } catch (InterruptedException | CPAException e) {
                    // The exception can't be propagated here, because we're overwriting a method in
                    // which the method signature cannot be changed. Thus, we have to throw an
                    // unchecked exceptions here.
                    throw new AutomaticProgramRepairException(e.getMessage(), e);
                  }
                })
            .findAny()
            .ifPresent(
                (Mutation mutation) -> {
                  logger.log(Level.INFO, "Successfully patched fault");
                  logger.log(
                      Level.INFO,
                      "Replaced "
                          + mutation.getSuspiciousEdge()
                          + " ("
                          + mutation.getSuspiciousEdge().getEdgeType()
                          + ") with "
                          + mutation.getNewEdge()
                          + " on line "
                          + edge.getLineNumber());

                  fixFound = true;
                  fixedEdgeType = mutation.getSuspiciousEdge().getEdgeType();
                });

        if (fixFound) {
          return;
        }
      }
    }

    logger.log(Level.INFO, "No fix found. \n" + faultLocalizationInfo);
  }

  private boolean wasRepairSuccessful(CFA mutatedCFA)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    Configuration internalAnalysisConfig = buildSubConfig(internalAnalysisConfigFile.value());

    CPAchecker cpaChecker = new CPAchecker(internalAnalysisConfig, logger, shutdownManager);

    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            internalAnalysisConfig, logger, shutdownNotifier, AggregatedReachedSets.empty());

    ConfigurableProgramAnalysis cpa = coreComponents.createCPA(mutatedCFA, specification);
    GlobalInfo.getInstance().setUpInfoFromCPA(cpa);
    Algorithm algo =
            coreComponents.createAlgorithm(cpa, mutatedCFA, specification);
    ReachedSet reached =
        createInitialReachedSet(cpa, mutatedCFA.getMainFunction(), coreComponents, logger);

    AlgorithmStatus status = algo.run(reached);

    return cpaChecker.isResultTrue(reached, status);
  }

  // TODO temp solution: copied from NestingAlgorithm
  private Configuration buildSubConfig(Path singleConfigFileName)
      throws IOException, InvalidConfigurationException {

    ConfigurationBuilder singleConfigBuilder = Configuration.builder();

    // TODO next line overrides existing options with options loaded from file.
    // Perhaps we want to keep some global options like 'specification'?
    singleConfigBuilder.loadFromFile(singleConfigFileName);

    Configuration singleConfig = singleConfigBuilder.build();
    // checkConfigs(globalConfig, singleConfig, singleConfigFileName, logger);
    return singleConfig;
  }

  // TODO temp solution: copied from NestingAlgorithm
  private ReachedSet createInitialReachedSet(
      ConfigurableProgramAnalysis cpa,
      CFANode mainFunction,
      CoreComponentsFactory pFactory,
      LogManager singleLogger)
      throws InterruptedException {
    singleLogger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState =
        cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());

    ReachedSet reached = pFactory.createReachedSet(cpa);
    reached.add(initialState, initialPrecision);
    return reached;
  }

  private List<FaultLocalizationInfo> localizeFaults(ReachedSet reachedSet)
      throws InterruptedException, InvalidConfigurationException, SolverException, CPAException {
    algorithm.checkOptions();
    List<FaultLocalizationInfo> faultLocalizationInfos = new ArrayList<>();

    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));

    // run algorithm for every error
    logger.log(Level.INFO, "Starting fault localization...");
    for (CounterexampleInfo info : counterExamples) {
      Optional<FaultLocalizationInfo> optionalFaultLocalizationInfo =
          algorithm.calcFaultLocalizationInfo(info);

      if (optionalFaultLocalizationInfo.isPresent()) {
        faultLocalizationInfos.add(optionalFaultLocalizationInfo.orElseThrow());
      }
    }
    logger.log(Level.INFO, "Stopping fault localization...");
    return faultLocalizationInfos;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
    statsCollection.add(algorithm);
    algorithm.collectStatistics(statsCollection);
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out).put(totalTime).put("Fix found", fixFound).put("Edge type fixed", fixedEdgeType);
  }

  @Override
  public @Nullable String getName() {
    return getClass().getSimpleName();
  }

  private static class AutomaticProgramRepairException extends RuntimeException {

    private static final long serialVersionUID = 928410299865065936L;

    AutomaticProgramRepairException(String pMsg, Throwable pCause) {
      super(checkNotNull(pMsg), checkNotNull(pCause));
    }
  }
}
