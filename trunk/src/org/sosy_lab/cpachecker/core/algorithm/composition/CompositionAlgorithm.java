// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import javax.management.JMException;
import javax.xml.transform.TransformerException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.annotations.SuppressForbidden;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.ProgressReportingAlgorithm;
import org.sosy_lab.cpachecker.core.defaults.precision.ConfigurablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.FullConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.RefinableConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundPrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTimeLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

@Options(prefix = "compositionAlgorithm")
public class CompositionAlgorithm implements Algorithm, StatisticsProvider {

  private class CompositionAlgorithmStatistics implements Statistics {
    private final Timer totalTimer;
    private final Collection<Statistics> currentSubStat;
    private int noOfRuns = 0;

    public CompositionAlgorithmStatistics() {
      totalTimer = new Timer();
      currentSubStat = new ArrayList<>();
    }

    @Override
    public @Nullable String getName() {
      return "Composition Algorithm";
    }

    private void printIntermediateStatistics(
        PrintStream pOut, Result pResult, ReachedSet pReached) {

      String text = "Statistics for " + noOfRuns + ". execution of composition algorithm";
      pOut.println(text);
      pOut.println("=".repeat(text.length()));

      printSubStatistics(pOut, pResult, pReached);
      pOut.println();
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {

      pOut.println("Number of algorithms provided:    " + configFiles.size());
      pOut.println("Number of composite analysis runs:        " + noOfRuns);
      pOut.println("Total time: " + totalTimer);

      printSubStatistics(pOut, pResult, pReached);
    }

    private void printSubStatistics(
        PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      pOut.println();
      pOut.println("Statistics for the component analysis in run " + noOfRuns + " of " + getName());

      for (Statistics s : currentSubStat) {
        StatisticsUtils.printStatistics(s, pOut, logger, pResult, pReached);
      }
    }

    @Override
    public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
      for (Statistics s : currentSubStat) {
        StatisticsUtils.writeOutputFiles(s, logger, pResult, pReached);
      }
    }

    public Collection<Statistics> getSubStatistics() {
      return currentSubStat;
    }

    public void resetSubStatistics() {
      currentSubStat.clear();
    }
  }

  @Option(
      secure = true,
      required = true,
      description =
          "list of files with configurations to use, which are optionally suffixed according to one"
              + " of the followig schemes:either ::MODE or ::MODE_LIMIT, where MODE and LIMIT are"
              + " place holders.MODE may take one of the following values continue (i.e., continue"
              + " analysis with same CPA and reached set), reuse-precision (i.e., reuse the"
              + " aggregation of the precisions from the previous analysis run), noreuse (i.e.,"
              + " start from scratch).LIMIT is a positive integer number specifying the time limit"
              + " of the analysis in each round.If no (correct) limit is given a default limit is"
              + " used.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<AnnotatedValue<Path>> configFiles;

  public enum INTERMEDIATESTATSOPT {
    EXECUTE,
    NONE,
    PRINT
  }

  @Option(
      secure = true,
      description =
          "print the statistics of each component of the composition algorithm"
              + " directly after the component's computation is finished")
  private INTERMEDIATESTATSOPT intermediateStatistics = INTERMEDIATESTATSOPT.NONE;

  @Option(
      secure = true,
      description =
          "let each analysis part of the composition algorithm write output files"
              + " and not only the last one that is executed")
  private boolean writeIntermediateOutputFiles = true;

  @Option(
      secure = true,
      name = "initCondition",
      description =
          "Whether or not to create an initial condition, that excludes no paths, "
              + "before first analysis is run."
              + "Required when first analysis uses condition from conditional model checking")
  private boolean generateInitialFalseCondition = false;

  @Option(
      secure = true,
      name = "propertyChecked",
      description = "Enable when composition algorithm is used to check a specification")
  private boolean isPropertyChecked = true;

  @Option(
      secure = true,
      name = "condition.file",
      description = "where to store initial condition, when generated")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path initialCondition = Path.of("AssumptionAutomaton.txt");

  private AlgorithmCompositionStrategy selectionStrategy; // TODO initialize, set up

  private final CFA cfa;
  private final Configuration globalConfig;
  private final LogManager logger;
  private final ShutdownRequestListener logShutdownListener;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;
  private final CompositionAlgorithmStatistics stats;

  public CompositionAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    if (configFiles.isEmpty()) {
      throw new InvalidConfigurationException(
          "Need at least one configuration for composition algorithm!");
    }
    cfa = pCfa;
    globalConfig = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    specification = checkNotNull(pSpecification);
    stats = new CompositionAlgorithmStatistics();

    selectionStrategy =
        AlgorithmCompositionStrategyBuilder.buildStrategy(
            pConfig, logger, shutdownNotifier, cfa, specification);

    logShutdownListener =
        reason ->
            logger.logf(
                Level.WARNING,
                "Shutdown of analysis run %d requested (%s).",
                stats.noOfRuns,
                reason);
    if (generateInitialFalseCondition) {
      generateInitialFalseCondition();
    }
  }

  private void generateInitialFalseCondition() {
    String condition =
        "OBSERVER AUTOMATON AssumptionAutomaton\n\n"
            + "INITIAL STATE __FALSE;\n\n"
            + "STATE __FALSE :\n    TRUE -> GOTO __FALSE;\n\n"
            + "END AUTOMATON\n";

    try (Writer w = IO.openOutputFile(initialCondition, Charset.defaultCharset())) {
      w.write(condition);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write initial condition to file");
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
    checkArgument(
        pReached instanceof ForwardingReachedSet,
        "CompositionAlgorithm needs ForwardingReachedSet");
    checkArgument(
        pReached.size() <= 1,
        "CompositionAlgorithm does not support being called several times with the same reached"
            + " set");
    checkArgument(!pReached.isEmpty(), "CompositionAlgorithm needs non-empty reached set");

    stats.totalTimer.start();
    try {

      ForwardingReachedSet fReached = (ForwardingReachedSet) pReached;

      Iterable<CFANode> initialNodes = AbstractStates.extractLocations(pReached.getFirstState());
      CFANode mainFunction = Iterables.getOnlyElement(initialNodes);

      selectionStrategy.initializeAlgorithmContexts(configFiles);

      AlgorithmStatus status;
      if (isPropertyChecked) {
        status = AlgorithmStatus.UNSOUND_AND_PRECISE;
      } else {
        status = AlgorithmStatus.NO_PROPERTY_CHECKED;
      }

      AlgorithmContext previousContext = null;
      AlgorithmContext currentContext = null;
      Configuration currentConfig;
      Pair<Algorithm, ShutdownManager> currentRun;
      boolean analysisFinishedWithResult;

      while (!shutdownNotifier.shouldShutdown() && selectionStrategy.hasNextAlgorithm()) {

        analysisFinishedWithResult = false;
        currentRun = null;

        previousContext = currentContext;
        // retrieve context from last execution of current algorithm
        currentContext = selectionStrategy.getNextAlgorithm();
        currentContext.resetProgress();

        Preconditions.checkState(currentContext != null, "Retrieved invalid algorithm context.");

        currentContext.startTimer();
        stats.noOfRuns++;
        try {
          currentConfig =
              currentContext.getAndCreateConfigIfNecessary(globalConfig, logger, shutdownNotifier);

          // if configuration is still null, skip it in this iteration
          if (currentConfig == null) {
            logger.log(
                Level.WARNING, "Skip current analysis because no configuration is available.");
            continue;
          }

          currentRun = createNextAlgorithm(currentContext, mainFunction, previousContext);
          if (currentRun == null) {
            logger.log(
                Level.WARNING, "Skip current analysis because analysis could not be set up.");
            continue;
          }

          if (fReached instanceof HistoryForwardingReachedSet) {
            ((HistoryForwardingReachedSet) fReached).saveCPA(currentContext.getCPA());
          }
          fReached.setDelegate(currentContext.getReachedSet());

          shutdownNotifier.shutdownIfNecessary();
          logger.logf(
              Level.INFO,
              "Starting component analysis %s in run %d ...",
              currentContext.configToString(),
              stats.noOfRuns);
          status = currentRun.getFirst().run(currentContext.getReachedSet());

          if (status.wasPropertyChecked() != isPropertyChecked) {
            logger.logf(
                Level.WARNING,
                "Component algorithm and composition algorithm do not agree on property checking"
                    + " (%b, %b).",
                status.wasPropertyChecked(),
                isPropertyChecked);
          }

          if (from(currentContext.getReachedSet()).anyMatch(AbstractStates::isTargetState)
              && status.isPrecise()) {
            analysisFinishedWithResult = true;
            return status;
          }
          if (status.wasPropertyChecked() && !status.isSound()) {
            logger.logf(
                Level.FINE,
                "Analysis %s in run %d terminated, but result is unsound.",
                currentContext.configToString(),
                stats.noOfRuns);

          } else if (currentContext.getReachedSet().hasWaitingState()) {
            logger.logf(
                Level.FINE,
                "Analysis %s in run %d terminated but did not finish: There are still states to be"
                    + " processed.",
                currentContext.configToString(),
                stats.noOfRuns);

          } else if (!(from(currentContext.getReachedSet()).anyMatch(AbstractStates::isTargetState)
              && !status.isPrecise())) {
            // sound analysis and completely finished, terminate
            analysisFinishedWithResult = true;
            return status;
          }

          shutdownNotifier.shutdownIfNecessary();

        } catch (CPAException e) {
          if (e instanceof CounterexampleAnalysisFailed || e instanceof RefinementFailedException) {
            status = status.withPrecise(false);
          }

          logger.logUserException(
              Level.WARNING,
              e,
              "Analysis "
                  + currentContext.configToString()
                  + "in run "
                  + stats.noOfRuns
                  + " not completed.");

        } catch (InterruptedException e) {
          logger.logUserException(
              Level.FINE,
              e,
              "Analysis "
                  + currentContext.configToString()
                  + "in run "
                  + stats.noOfRuns
                  + " stopped.");

          shutdownNotifier.shutdownIfNecessary();

        } finally {
          if (currentRun != null) {
            tidyUpShutdownManager(currentRun.getSecond());

            if (!analysisFinishedWithResult
                && !shutdownNotifier.shouldShutdown()
                && selectionStrategy.hasNextAlgorithm()) {

              printIntermediateStatistics(pReached, currentContext);

              if (!currentContext.reuseCPA()) {
                CPAs.closeCpaIfPossible(currentContext.getCPA(), logger);
              }

              if (currentRun.getFirst() instanceof ProgressReportingAlgorithm) {
                currentContext.setProgress(
                    ((ProgressReportingAlgorithm) currentRun.getFirst()).getProgress());
              }

              CPAs.closeIfPossible(currentRun.getFirst(), logger);
            }
          }

          currentContext.stopTimer();
        }
      }

      selectionStrategy.finalCleanUp(currentContext);

      logger.log(Level.INFO, "Shutdown of composition algorithm, analysis not finished yet.");
      return status;

    } catch (RuntimeException e2) {
      if (e2.getCause() instanceof TransformerException || e2 instanceof IllegalStateException) {
        logger.logUserException(
            Level.FINE, e2, "Problem with one one the analysis, try to save result");
        if (isPropertyChecked) {
          return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
        } else {
          return AlgorithmStatus.NO_PROPERTY_CHECKED;
        }
      } else {
        throw e2;
      }
    } finally {
      stats.totalTimer.stop();
    }
  }

  @SuppressFBWarnings(
      value = "DM_DEFAULT_ENCODING",
      justification = "Encoding is irrelevant for null output stream")
  @SuppressForbidden("System.out is correct for statistics")
  private void printIntermediateStatistics(ReachedSet pReached, AlgorithmContext currentContext) {
    switch (intermediateStatistics) {
      case PRINT:
        stats.printIntermediateStatistics(
            System.out, Result.UNKNOWN, currentContext.getReachedSet());
        break;
      case EXECUTE:
        @SuppressWarnings("checkstyle:IllegalInstantiation") // ok for statistics
        final PrintStream dummyStream = new PrintStream(ByteStreams.nullOutputStream());
        stats.printIntermediateStatistics(
            dummyStream, Result.UNKNOWN, currentContext.getReachedSet());
        break;
      default: // do nothing
    }

    if (writeIntermediateOutputFiles) {
      stats.writeOutputFiles(Result.UNKNOWN, pReached);
    }

    stats.resetSubStatistics();
  }

  private void tidyUpShutdownManager(ShutdownManager pShutdownManager) {
    pShutdownManager.getNotifier().unregister(logShutdownListener);
    pShutdownManager.requestShutdown("Analysis terminated.");
  }

  private @Nullable Pair<Algorithm, ShutdownManager> createNextAlgorithm(
      final AlgorithmContext pCurrentContext,
      final CFANode pMainFunction,
      final AlgorithmContext pPreviousContext) {

    ShutdownManager localShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    List<ResourceLimit> limits = new ArrayList<>();
    try {
      limits.add(ProcessCpuTimeLimit.fromNowOn(TimeSpan.ofSeconds(pCurrentContext.getTimeLimit())));
    } catch (JMException e) {
      logger.log(
          Level.SEVERE,
          "Your Java VM does not support measuring the cpu time. Ignore time limit.",
          e);
    }

    ResourceLimitChecker singleLimits = new ResourceLimitChecker(localShutdownManager, limits);
    singleLimits.start();
    localShutdownManager.getNotifier().register(logShutdownListener);

    ConfigurableProgramAnalysis cpa = null;
    try {
      AggregatedReachedSets aggregateReached = AggregatedReachedSets.empty();
      CoreComponentsFactory localCoreComponents =
          new CoreComponentsFactory(
              pCurrentContext.getConfig(),
              logger,
              localShutdownManager.getNotifier(),
              aggregateReached);

      boolean newReachedSet = false;

      if (pCurrentContext.reuseCPA()) {
        cpa = pCurrentContext.getCPA();
        if (cpa == null) {
          // create cpa only once when not initialized, use global limits (i.e. shutdownNotifier)
          CoreComponentsFactory globalCoreComponents =
              new CoreComponentsFactory(
                  pCurrentContext.getConfig(), logger, shutdownNotifier, aggregateReached);
          cpa = globalCoreComponents.createCPA(cfa, specification);
          pCurrentContext.setCPA(cpa);
          if (!pCurrentContext.reusePrecision()) {
            // create reached set only once, continue analysis
            newReachedSet = true;
          }
        }

      } else {
        // do not reuse cpa, and, thus reached set
        try {
          cpa = localCoreComponents.createCPA(cfa, specification);
          pCurrentContext.setCPA(cpa);
          newReachedSet = true;
        } catch (InvalidConfigurationException e) {
          pCurrentContext.setCPA(null);
          tidyUpShutdownManager(localShutdownManager);
          return null;
        }
      }

      if (pCurrentContext.reusePrecision()) {
        // start with new reached set each time, but precision from previous analysis if possible
        List<ReachedSet> previousResults = new ArrayList<>(2);
        FormulaManagerView fmgr = null;

        if (pCurrentContext.reuseOwnPrecision()) {
          previousResults.add(pCurrentContext.getReachedSet());
        }

        if (pCurrentContext.reusePredecessorPrecision() && pPreviousContext != null) {
          previousResults.add(pPreviousContext.getReachedSet());
          PredicateCPA predCPA = CPAs.retrieveCPA(pPreviousContext.getCPA(), PredicateCPA.class);
          if (predCPA != null) {
            fmgr = predCPA.getSolver().getFormulaManager();
          }
        }

        pCurrentContext.setReachedSet(
            createInitialReachedSet(
                cpa,
                pMainFunction,
                localCoreComponents,
                previousResults,
                fmgr,
                pCurrentContext.getConfig()));
      } else {
        if (newReachedSet) {
          pCurrentContext.setReachedSet(
              createInitialReachedSet(
                  cpa,
                  pMainFunction,
                  localCoreComponents,
                  null,
                  null,
                  pCurrentContext.getConfig()));
        }
      }

      // always create algorithm with new "local" shutdown manager
      Algorithm algorithm = localCoreComponents.createAlgorithm(cpa, cfa, specification);

      if (algorithm instanceof CompositionAlgorithm) {
        // To avoid accidental infinitely-recursive nesting.
        logger.log(Level.SEVERE, "Component analyses mus not be composition analyses themselves.");
        tidyUpShutdownManager(localShutdownManager);
        if (pCurrentContext.reuseCPA() && cpa != null) {
          CPAs.closeCpaIfPossible(cpa, logger);
        }
        return null;
      }

      if (algorithm instanceof StatisticsProvider) {
        ((StatisticsProvider) algorithm).collectStatistics(stats.getSubStatistics());
      }

      if (pCurrentContext.getCPA() instanceof StatisticsProvider) {
        ((StatisticsProvider) pCurrentContext.getCPA()).collectStatistics(stats.getSubStatistics());
      }

      return Pair.of(algorithm, localShutdownManager);

    } catch (CPAException | InterruptedException | InvalidConfigurationException e) {
      tidyUpShutdownManager(localShutdownManager);
      if (pCurrentContext.reuseCPA() && cpa != null) {
        CPAs.closeCpaIfPossible(cpa, logger);
      }
      logger.logUserException(
          Level.WARNING,
          e,
          "Problem during creation of analysis " + pCurrentContext.configToString());
    }
    return null;
  }

  private ReachedSet createInitialReachedSet(
      final ConfigurableProgramAnalysis pCpa,
      final CFANode pMainFunction,
      final CoreComponentsFactory pFactory,
      final @Nullable List<ReachedSet> previousReachedSets,
      final @Nullable FormulaManagerView pFMgr,
      final Configuration pConfig)
      throws InterruptedException {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState =
        pCpa.getInitialState(pMainFunction, StateSpacePartition.getDefaultPartition());

    Precision initialPrecision =
        pCpa.getInitialPrecision(pMainFunction, StateSpacePartition.getDefaultPartition());
    if (previousReachedSets != null && !previousReachedSets.isEmpty()) {
      initialPrecision =
          aggregatePrecisionsForReuse(previousReachedSets, initialPrecision, pFMgr, pConfig);
    }

    ReachedSet reached = pFactory.createReachedSet(pCpa);
    reached.add(initialState, initialPrecision);
    return reached;
  }

  private Precision aggregatePrecisionsForReuse(
      final List<ReachedSet> pPreviousReachedSets,
      final Precision pInitialPrecision,
      final @Nullable FormulaManagerView pFMgr,
      final Configuration pConfig) {
    Preconditions.checkArgument(!pPreviousReachedSets.isEmpty());
    Precision resultPrec = pInitialPrecision;

    VariableTrackingPrecision varPrec =
        Precisions.extractPrecisionByType(resultPrec, VariableTrackingPrecision.class);
    if (varPrec != null) {
      try {
        if (varPrec instanceof ConfigurablePrecision) {
          varPrec = VariableTrackingPrecision.createRefineablePrecision(pConfig, varPrec);
        }
        VariableTrackingPrecision varPrecInter;

        boolean changed = false;

        for (ReachedSet previousReached : pPreviousReachedSets) {
          if (previousReached != null) {
            for (Precision prec : previousReached.getPrecisions()) {
              varPrecInter =
                  Precisions.extractPrecisionByType(prec, VariableTrackingPrecision.class);
              if (varPrecInter != null && !(varPrecInter instanceof ConfigurablePrecision)) {
                varPrec = varPrec.join(varPrecInter);
                changed = true;
              }

              PredicatePrecision predPrec =
                  Precisions.extractPrecisionByType(resultPrec, PredicatePrecision.class);
              if (predPrec != null && pFMgr != null) {
                varPrec =
                    varPrec.withIncrement(convertPredPrecToVariableTrackingPrec(predPrec, pFMgr));
                changed = true;
              }
            }
          }
        }
        if (changed) {
          resultPrec =
              Precisions.replaceByType(
                  resultPrec, varPrec, Predicates.instanceOf(VariableTrackingPrecision.class));
        }
      } catch (InvalidConfigurationException e) {
        logger.logException(Level.INFO, e, "Reuse of precision failed. Continue without reuse");
      }
    }

    ConstraintsPrecision constrPrec =
        Precisions.extractPrecisionByType(resultPrec, ConstraintsPrecision.class);
    if (constrPrec != null) {
      try {
        if (!(constrPrec instanceof RefinableConstraintsPrecision)) {

          constrPrec = new RefinableConstraintsPrecision(pConfig);
        }
        ConstraintsPrecision constrPrecInter;
        boolean changed = false;

        for (ReachedSet previousReached : pPreviousReachedSets) {
          if (previousReached != null) {
            for (Precision prec : previousReached.getPrecisions()) {
              constrPrecInter = Precisions.extractPrecisionByType(prec, ConstraintsPrecision.class);
              if (constrPrecInter != null
                  && !(constrPrecInter instanceof FullConstraintsPrecision)) {
                constrPrec = constrPrec.join(constrPrecInter);
                changed = true;
              }
            }
          }
        }
        if (changed) {
          resultPrec =
              Precisions.replaceByType(
                  resultPrec, constrPrec, Predicates.instanceOf(ConstraintsPrecision.class));
        }
      } catch (InvalidConfigurationException e) {
        logger.logException(Level.INFO, e, "Reuse of precision failed. Continue without reuse");
      }
    }

    LoopBoundPrecision loopPrec =
        Precisions.extractPrecisionByType(resultPrec, LoopBoundPrecision.class);
    if (loopPrec != null && pPreviousReachedSets.get(0) != null) {
      resultPrec =
          Precisions.replaceByType(
              resultPrec, loopPrec, Predicates.instanceOf(LoopBoundPrecision.class));
    }

    PredicatePrecision predPrec =
        Precisions.extractPrecisionByType(resultPrec, PredicatePrecision.class);

    if (predPrec != null && pPreviousReachedSets.get(0) != null) {
      Iterable<Precision> allPrecisions =
          from(ImmutableList.of(resultPrec)).append(pPreviousReachedSets.get(0).getPrecisions());

      resultPrec =
          Precisions.replaceByType(
              resultPrec,
              PredicatePrecision.unionOf(allPrecisions),
              Predicates.instanceOf(PredicatePrecision.class));
    }

    return resultPrec;
  }

  private Multimap<CFANode, MemoryLocation> convertPredPrecToVariableTrackingPrec(
      final PredicatePrecision pPredPrec, final FormulaManagerView pFMgr) {
    Collection<AbstractionPredicate> predicates = new HashSet<>();
    predicates.addAll(pPredPrec.getGlobalPredicates());
    predicates.addAll(pPredPrec.getFunctionPredicates().values());
    predicates.addAll(pPredPrec.getLocalPredicates().values());

    SetMultimap<CFANode, MemoryLocation> trackedVariables = HashMultimap.create();
    CFANode dummyNode = CFANode.newDummyCFANode();

    for (AbstractionPredicate pred : predicates) {
      for (String var : pFMgr.extractVariables(pred.getSymbolicAtom()).keySet()) {
        trackedVariables.put(dummyNode, MemoryLocation.parseExtendedQualifiedName(var));
      }
    }

    return trackedVariables;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    if (selectionStrategy instanceof StatisticsProvider) {
      ((StatisticsProvider) selectionStrategy).collectStatistics(pStatsCollection);
    }
    if (selectionStrategy instanceof Statistics) {
      pStatsCollection.add((Statistics) selectionStrategy);
    }
  }
}
