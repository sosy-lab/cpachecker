/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import javax.management.JMException;
import javax.xml.transform.TransformerException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
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
import org.sosy_lab.cpachecker.core.Specification;
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
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTimeLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

@Options(prefix = "interleavedAlgorithm")
public class InterleavedAlgorithm implements Algorithm, StatisticsProvider {

  private class InterleavedAlgorithmStatistics implements Statistics {
    private int noOfAlgorithms;
    private final Timer totalTimer;
    private final Collection<Statistics> currentSubStat;
    private final List<Timer> timersPerAlgorithm;
    private int noOfCurrentAlgorithm;
    private int noOfRounds = 1;

    public InterleavedAlgorithmStatistics() {
      noOfAlgorithms = configFiles.size();
      totalTimer = new Timer();
      currentSubStat = new ArrayList<>();
      timersPerAlgorithm = new ArrayList<>(noOfAlgorithms);
      for (int i = 0; i < noOfAlgorithms; i++) {
        timersPerAlgorithm.add(new Timer());
      }
    }

    @Override
    public @Nullable String getName() {
      return "Interleaved Algorithm";
    }

    private void printIntermediateStatistics(
        PrintStream pOut, Result pResult, ReachedSet pReached) {

      String text =
          "Statistics for " + noOfRounds + ". execution of algorithm " + noOfCurrentAlgorithm;
      pOut.println(text);
      pOut.println(Strings.repeat("=", text.length()));

      printSubStatistics(pOut, pResult, pReached);
      pOut.println();
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {

      pOut.println("Number of algorithms provided:    " + noOfAlgorithms);
      pOut.println("Maximal number of repetitions:        " + noOfRounds);
      pOut.println("Total time: " + totalTimer);
      pOut.println("Times per algorithm: ");
      for (int i = 0; i < noOfAlgorithms; i++) {
        pOut.println("Algorithm " + (i + 1) + ": " + timersPerAlgorithm.get(i));
      }

      printSubStatistics(pOut, pResult, pReached);
    }

    private void printSubStatistics(
        PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      pOut.println();
      pOut.println(
          "Statistics for the analysis part in round "
              + noOfRounds
              + " of "
              + getName()
              + " using algorithm "
              + noOfCurrentAlgorithm
              + " of "
              + noOfAlgorithms);

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

  private static class AlgorithmContext {
    private enum REPETITIONMODE {
      CONTINUE,
      NOREUSE,
      REUSEOWNPRECISION,
      REUSEPREDPRECISION,
      REUSEOWNANDPREDPRECISION,
      REUSECPA_OWNPRECISION,
      REUSECPA_PREDPRECISION,
      REUSECPA_OWNANDPREDPRECISION;
    }

    private final Path configFile;
    private int timeLimit;
    private final REPETITIONMODE mode;
    private final Timer timer;

    private Algorithm algorithm;
    private @Nullable ConfigurableProgramAnalysis cpa;
    private Configuration config;
    private ShutdownManager localShutdownManager;
    private ReachedSet reached;
    private double progress = -1.0;

    private AlgorithmContext(
        final AnnotatedValue<Path> pConfigFile, final Timer pTimer) {
      configFile = pConfigFile.value();
      timer = pTimer;
      timeLimit = extractLimitFromAnnotation(pConfigFile.annotation());
      mode = extractModeFromAnnotation(pConfigFile.annotation());
    }

    private int extractLimitFromAnnotation(final Optional<String> annotation) {
      if (annotation.isPresent()) {
        String str = annotation.get();
        if(str.contains("_")) {
          try {
            int limit = Integer.parseInt(str.substring(str.indexOf("_") + 1, str.length()));
            if (limit > 0) {
              return limit;
            }
          } catch(NumberFormatException e) {

          }
        }
      }
      return DEFAULT_TIME_LIMIT;
    }

    private REPETITIONMODE extractModeFromAnnotation(final Optional<String> annotation) {
      String val = "";
      if (annotation.isPresent()) {
        val = annotation.get();
        if (val.contains("_")) {
          val = val.substring(0, val.indexOf("_"));
        }
        val = val.toLowerCase(Locale.ROOT);
      }

      switch (val) {
        case "continue":
          return REPETITIONMODE.CONTINUE;
        case "reuse-own-precision":
          return REPETITIONMODE.REUSEOWNPRECISION;
        case "reuse-pred-precision":
          return REPETITIONMODE.REUSEPREDPRECISION;
        case "reuse-precisions":
          return REPETITIONMODE.REUSEOWNANDPREDPRECISION;
        case "reuse-cpa-own-precision":
          return REPETITIONMODE.REUSECPA_OWNPRECISION;
        case "reuse-cpa-pred-precision":
          return REPETITIONMODE.REUSECPA_PREDPRECISION;
        case "reuse-cpa-precisions":
          return REPETITIONMODE.REUSECPA_OWNANDPREDPRECISION;
        default:
          return REPETITIONMODE.NOREUSE;
      }
    }

    private boolean reuseCPA() {
      return mode == REPETITIONMODE.CONTINUE
          || mode == REPETITIONMODE.REUSECPA_OWNPRECISION
          || mode == REPETITIONMODE.REUSECPA_PREDPRECISION
          || mode == REPETITIONMODE.REUSECPA_OWNANDPREDPRECISION;
    }

    private boolean reusePrecision() {
      return reuseOwnPrecision() || reusePredecessorPrecision();
    }

    private boolean reuseOwnPrecision() {
      return mode == REPETITIONMODE.REUSEOWNPRECISION
          || mode == REPETITIONMODE.REUSEOWNANDPREDPRECISION
          || mode == REPETITIONMODE.REUSECPA_OWNPRECISION
          || mode == REPETITIONMODE.REUSECPA_OWNANDPREDPRECISION;
    }

    private boolean reusePredecessorPrecision() {
      return mode == REPETITIONMODE.REUSEPREDPRECISION
          || mode == REPETITIONMODE.REUSEOWNANDPREDPRECISION
          || mode == REPETITIONMODE.REUSECPA_PREDPRECISION
          || mode == REPETITIONMODE.REUSECPA_OWNANDPREDPRECISION;
    }

    public void resetProgress() {
      progress = -1.0;
    }

    public void adaptTimeLimit(final int newTimeLimit) {
      timeLimit = Math.max(DEFAULT_TIME_LIMIT, newTimeLimit);
    }

    public void setProgress(final double pProgress) {
      progress = pProgress;
    }

    public double getProgress() {
      return progress;
    }
  }

  private static final int DEFAULT_TIME_LIMIT = 10;

  @Option(
    secure = true,
    required = true,
    description =
        "list of files with configurations to use, which are optionally suffixed "
            + "according to one of the followig schemes:"
            + "either ::MODE or ::MODE_LIMIT, where MODE and LIMIT are place holders."
            + "MODE may take one of the following values continue (i.e., continue analysis with same CPA and reached set), "
            + "reuse-precision (i.e., reuse the aggregation of the precisions from the previous analysis run), "
            + "noreuse (i.e., start from scratch)."
            + "LIMIT is a positive integer number specifying the time limit of the analysis in each round."
            + "If no (correct) limit is given a default limit is used."
  )
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<AnnotatedValue<Path>> configFiles;

  @Option(
    secure = true,
    description =
        "If adaptTimeLimits is set and all configurations support progress reports, "
            + "in each cycle the time limits per configuration are newly calculated based on the progress"
  )
  private boolean adaptTimeLimits = false;

  public enum INTERMEDIATESTATSOPT {
    EXECUTE, NONE, PRINT
  }

  @Option(
    secure = true,
    description =
        "print the statistics of each component of the interleaved algorithm"
            + " directly after the component's computation is finished"
  )
  private  INTERMEDIATESTATSOPT intermediateStatistics = INTERMEDIATESTATSOPT.NONE;

  @Option(
    secure = true,
    description =
        "let each analysis part of the interleaved algorithm write output files"
            + " and not only the last one that is executed"
  )
  private boolean writeIntermediateOutputFiles = true;

  @Option(
    secure = true,
    name = "initCondition",
    description =
        "Whether or not to create an initial condition, that excludes no paths, "
            + "before first analysis is run."
            + "Required when first analysis uses condition from conditional model checking"
  )
  private boolean generateInitialFalseCondition = false;

  @Option(
    secure = true,
    name = "propertyChecked",
    description = "Enable when interleaved algorithm is used to check a specification"
  )
  private boolean isPropertyChecked = true;

  @Option(
    secure = true,
    name = "condition.file",
    description = "where to store initial condition, when generated"
  )
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path initialCondition = Paths.get("AssumptionAutomaton.txt");

  private final CFA cfa;
  private final Configuration globalConfig;
  private final LogManager logger;
  private final ShutdownRequestListener logShutdownListener;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;
  private final InterleavedAlgorithmStatistics stats;

  public InterleavedAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    if (configFiles.isEmpty()) {
      throw new InvalidConfigurationException(
          "Need at least one configuration for interleaved algorithm!");
    }
    cfa = pCfa;
    globalConfig = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    specification = checkNotNull(pSpecification);
    stats = new InterleavedAlgorithmStatistics();

    logShutdownListener =
        reason ->
            logger.logf(
                Level.WARNING,
                "Shutdown of analysis %d requested (%s).",
                stats.noOfCurrentAlgorithm,
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

  @SuppressFBWarnings(
    value = "DM_DEFAULT_ENCODING",
    justification = "Encoding is irrelevant for null output stream"
  )
  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
    checkArgument(
        pReached instanceof ForwardingReachedSet,
        "InterleavedAlgorithm needs ForwardingReachedSet");
    checkArgument(
        pReached.size() <= 1,
        "InterleavedAlgorithm does not support being called several times with the same reached set");
    checkArgument(!pReached.isEmpty(), "InterleavedAlgorithm needs non-empty reached set");

    stats.totalTimer.start();
    try {

      ForwardingReachedSet fReached = (ForwardingReachedSet) pReached;

      Iterable<CFANode> initialNodes = AbstractStates.extractLocations(pReached.getFirstState());
      CFANode mainFunction = Iterables.getOnlyElement(initialNodes);

      List<AlgorithmContext> algorithmContexts = new ArrayList<>(configFiles.size());
      for (int i = 0; i < configFiles.size(); i++) {
        AnnotatedValue<Path> singleConfigFile = configFiles.get(i);
        Timer timer = stats.timersPerAlgorithm.get(i);

        algorithmContexts.add(new AlgorithmContext(singleConfigFile, timer));
      }

      AlgorithmStatus status;
      if (isPropertyChecked) {
        status = AlgorithmStatus.UNSOUND_AND_PRECISE;
      } else {
        status = AlgorithmStatus.NO_PROPERTY_CHECKED;
      }

      Iterator<AlgorithmContext> algorithmContextCycle =
          Iterables.cycle(algorithmContexts).iterator();
      AlgorithmContext previousContext = null;
      AlgorithmContext currentContext = null;

      while (!shutdownNotifier.shouldShutdown() && algorithmContextCycle.hasNext()) {

        // retrieve context from last execution of current algorithm
        previousContext = currentContext;
        currentContext = algorithmContextCycle.next();
        boolean analysisFinishedWithResult = false;

        currentContext.timer.start();
        try {

          if (stats.noOfCurrentAlgorithm == stats.noOfAlgorithms) {
            stats.noOfCurrentAlgorithm = 1;
            stats.noOfRounds++;
            logger.log(
                Level.INFO, "InterleavedAlgorithm switches to the next interleave iteration...");
            if (adaptTimeLimits) {
              computeAndSetNewTimeLimits(algorithmContexts);
            }
            for (AlgorithmContext tempContext : algorithmContexts) {
              tempContext.resetProgress();
            }
          } else {
            stats.noOfCurrentAlgorithm++;
          }

          if (currentContext.config == null) {
            readConfig(currentContext);

            // if configuration is still null, skip it in this iteration
            if (currentContext.config == null) {
              continue;
            }
          }

          try {
            createNextAlgorithm(currentContext, mainFunction, previousContext);

          } catch (CPAException | InterruptedException | InvalidConfigurationException e) {
            logger.logUserException(
                Level.WARNING,
                e,
                "Problem during creation of analysis " + stats.noOfCurrentAlgorithm);
            continue;
          }

          if (fReached instanceof HistoryForwardingReachedSet) {
            ((HistoryForwardingReachedSet) fReached).saveCPA(currentContext.cpa);
          }
          fReached.setDelegate(currentContext.reached);

          shutdownNotifier.shutdownIfNecessary();

          logger.logf(Level.INFO, "Starting analysis %d ...", stats.noOfCurrentAlgorithm);
          status = currentContext.algorithm.run(currentContext.reached);

          if (status.wasPropertyChecked() != isPropertyChecked) {
            logger.logf(
                Level.WARNING,
                "Component algorithm and interleaved algorithm do not agree on property checking (%b, %b).",
                status.wasPropertyChecked(),
                isPropertyChecked);
          }

          if (from(currentContext.reached).anyMatch(IS_TARGET_STATE) && status.isPrecise()) {
            analysisFinishedWithResult = true;
            return status;
          }
          if (status.wasPropertyChecked() && !status.isSound()) {
            logger.logf(
                Level.FINE,
                "Analysis %d terminated, but result is unsound.",
                stats.noOfCurrentAlgorithm);

          } else if (currentContext.reached.hasWaitingState()) {
            logger.logf(
                Level.FINE,
                "Analysis %d terminated but did not finish: There are still states to be processed.",
                stats.noOfCurrentAlgorithm);

          } else if (!(from(currentContext.reached).anyMatch(IS_TARGET_STATE)
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
              Level.WARNING, e, "Analysis " + stats.noOfCurrentAlgorithm + " not completed.");

        } catch (InterruptedException e) {
          logger.logUserException(
              Level.FINE, e, "Analysis " + stats.noOfCurrentAlgorithm + " stopped.");

          shutdownNotifier.shutdownIfNecessary();

        } finally {
          if (currentContext.config != null) {
            currentContext.localShutdownManager.getNotifier().unregister(logShutdownListener);
            currentContext.localShutdownManager.requestShutdown("Analysis terminated.");

            if (!analysisFinishedWithResult
                && !shutdownNotifier.shouldShutdown()
                && algorithmContextCycle.hasNext()) {

              switch (intermediateStatistics) {
                case PRINT:
                  stats.printIntermediateStatistics(
                      System.out, Result.UNKNOWN, currentContext.reached);
                  break;
                case EXECUTE:
                  @SuppressWarnings("checkstyle:IllegalInstantiation") // ok for statistics
                  final PrintStream dummyStream = new PrintStream(ByteStreams.nullOutputStream());
                  stats.printIntermediateStatistics(
                      dummyStream, Result.UNKNOWN, currentContext.reached);
                  break;
                default: // do nothing
              }

              if (writeIntermediateOutputFiles) {
                stats.writeOutputFiles(Result.UNKNOWN, pReached);
              }

              stats.resetSubStatistics();

              if (!currentContext.reuseCPA() && currentContext.cpa != null) {
                CPAs.closeCpaIfPossible(currentContext.cpa, logger);
              }

              if (adaptTimeLimits
                  && currentContext.algorithm instanceof ProgressReportingAlgorithm) {
                currentContext.setProgress(
                    ((ProgressReportingAlgorithm) currentContext.algorithm).getProgress());
              }

              CPAs.closeIfPossible(currentContext.algorithm, logger);
            }
          }

          currentContext.timer.stop();
        }
      }

      for (AlgorithmContext context : algorithmContexts) {
        if (context != currentContext
            && context != null
            && context.cpa != null
            && context.reuseCPA()) {
          CPAs.closeCpaIfPossible(context.cpa, logger);
        }
      }

      logger.log(Level.INFO, "Shutdown of interleaved algorithm, analysis not finished yet.");
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

  private void readConfig(AlgorithmContext pContext) {

    Path singleConfigFileName = pContext.configFile;
    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    singleConfigBuilder.clearOption("interleavedAlgorithm.configFiles");
    singleConfigBuilder.clearOption("analysis.useInterleavedAnalyses");

    try { // read config file
      singleConfigBuilder.loadFromFile(singleConfigFileName);
      logger.logf(
          Level.INFO,
          "Loading analysis %d from file %s ...",
          stats.noOfCurrentAlgorithm,
          singleConfigFileName);

      pContext.config = singleConfigBuilder.build();

    } catch (InvalidConfigurationException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Skipping one analysis because the configuration file "
              + singleConfigFileName.toString()
              + " is invalid");

    } catch (IOException e) {
      String message =
          "Skipping one analysis because the configuration file "
              + singleConfigFileName.toString()
              + " could not be read";
      if (shutdownNotifier.shouldShutdown() && e instanceof ClosedByInterruptException) {
        logger.log(Level.WARNING, message);
      } else {
        logger.logUserException(Level.WARNING, e, message);
      }
    }
  }

  private void createNextAlgorithm(
      final AlgorithmContext pCurrentContext,
      final CFANode pMainFunction,
      final AlgorithmContext pPreviousContext)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    pCurrentContext.localShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    List<ResourceLimit> limits = new ArrayList<>();
    try {
      limits.add(ProcessCpuTimeLimit.fromNowOn(TimeSpan.ofSeconds(pCurrentContext.timeLimit)));
    } catch (JMException e) {
      logger.log(
          Level.SEVERE,
          "Your Java VM does not support measuring the cpu time. Ignore time limit.",
          e);
    }

    ResourceLimitChecker singleLimits =
        new ResourceLimitChecker(pCurrentContext.localShutdownManager, limits);
    singleLimits.start();
    pCurrentContext.localShutdownManager.getNotifier().register(logShutdownListener);

    AggregatedReachedSets aggregateReached = new AggregatedReachedSets();
    CoreComponentsFactory localCoreComponents =
        new CoreComponentsFactory(
            pCurrentContext.config,
            logger,
            pCurrentContext.localShutdownManager.getNotifier(),
            aggregateReached);

    boolean newReachedSet = false;

    if (pCurrentContext.reuseCPA()) {
      if (pCurrentContext.cpa == null) {
        // create cpa only once when not initialized, use global limits (i.e. shutdownNotifier)
        CoreComponentsFactory globalCoreComponents =
            new CoreComponentsFactory(
                pCurrentContext.config, logger, shutdownNotifier, aggregateReached);
        pCurrentContext.cpa = globalCoreComponents.createCPA(cfa, specification);
        if (!pCurrentContext.reusePrecision()) {
          // create reached set only once, continue analysis
          newReachedSet = true;
        }
      }

    } else {
      // do not reuse cpa, and, thus reached set
      try {
        pCurrentContext.cpa = localCoreComponents.createCPA(cfa, specification);
        newReachedSet = true;
      } catch (InvalidConfigurationException e) {
        pCurrentContext.cpa = null;
        throw e;
      }
    }

    if (pCurrentContext.reusePrecision()) {
      // start with new reached set each time, but precision from previous analysis if possible
      List<ReachedSet> previousResults = new ArrayList<>(2);
      FormulaManagerView fmgr = null;

      if (pCurrentContext.reuseOwnPrecision()) {
        previousResults.add(pCurrentContext.reached);
      }

      if (pCurrentContext.reusePredecessorPrecision() && pPreviousContext != null) {
        previousResults.add(pPreviousContext.reached);
        PredicateCPA predCPA = CPAs.retrieveCPA(pPreviousContext.cpa, PredicateCPA.class);
        if (predCPA != null) {
          fmgr = predCPA.getSolver().getFormulaManager();
        }
      }

      pCurrentContext.reached =
          createInitialReachedSet(
              pCurrentContext.cpa,
              pMainFunction,
              localCoreComponents,
              previousResults,
              fmgr,
              pCurrentContext.config);
    } else {
      if (newReachedSet) {
        pCurrentContext.reached =
            createInitialReachedSet(
                pCurrentContext.cpa,
                pMainFunction,
                localCoreComponents,
                null,
                null,
                pCurrentContext.config);
      }
    }

    // always create algorithm with new "local" shutdown manager
    pCurrentContext.algorithm =
        localCoreComponents.createAlgorithm(pCurrentContext.cpa, cfa, specification);

    if (pCurrentContext.algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) pCurrentContext.algorithm).collectStatistics(stats.getSubStatistics());
    }

    if (pCurrentContext.cpa instanceof StatisticsProvider) {
      ((StatisticsProvider) pCurrentContext.cpa).collectStatistics(stats.getSubStatistics());
    }

    if (pCurrentContext.algorithm instanceof InterleavedAlgorithm) {
      // To avoid accidental infinitely-recursive nesting.
      throw new InvalidConfigurationException(
          "Interleaved analysis parts may not be interleaved analyses theirselves.");
    }
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

    ReachedSet reached = pFactory.createReachedSet();
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

    PredicatePrecision predPrec;
    LoopBoundPrecision loopPrec;
    ConstraintsPrecision constrPrec;
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

              predPrec = Precisions.extractPrecisionByType(resultPrec, PredicatePrecision.class);
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

    constrPrec = Precisions.extractPrecisionByType(resultPrec, ConstraintsPrecision.class);
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
            if (constrPrecInter != null && !(constrPrecInter instanceof FullConstraintsPrecision)) {
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

    loopPrec = Precisions.extractPrecisionByType(resultPrec, LoopBoundPrecision.class);
    if (loopPrec != null && pPreviousReachedSets.get(0) != null) {
      resultPrec =
          Precisions.replaceByType(
              resultPrec, loopPrec, Predicates.instanceOf(LoopBoundPrecision.class));
    }

    predPrec = Precisions.extractPrecisionByType(resultPrec, PredicatePrecision.class);

    if (predPrec != null && pPreviousReachedSets.get(0) != null) {
      Collection<PredicatePrecision> predPrecs =
          new HashSet<>(pPreviousReachedSets.get(0).getPrecisions().size());
      predPrecs.add(predPrec);
      for (Precision prec : pPreviousReachedSets.get(0).getPrecisions()) {
        predPrec = Precisions.extractPrecisionByType(prec, PredicatePrecision.class);
        predPrecs.add(predPrec);
      }

      resultPrec =
          Precisions.replaceByType(
              resultPrec,
              PredicatePrecision.unionOf(predPrecs),
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
    CFANode dummyNode = new CFANode("dummy");

    for (AbstractionPredicate pred : predicates) {
      for (String var : pFMgr.extractVariables(pred.getSymbolicVariable()).keySet()) {
          trackedVariables.put(dummyNode, MemoryLocation.valueOf(var));
      }
    }

    return trackedVariables;
  }

  private void computeAndSetNewTimeLimits(final List<AlgorithmContext> pAlgorithmContexts) {
    long totalDistributableTimeBudget = 0;
    double totalRelativeProgress = 0.0;
    boolean mayAdapt = true;

    for (AlgorithmContext context : pAlgorithmContexts) {
      totalDistributableTimeBudget += context.timeLimit - DEFAULT_TIME_LIMIT;
      totalRelativeProgress += (context.getProgress() / context.timeLimit);
      mayAdapt &= context.getProgress() >= 0;
    }

    if (totalDistributableTimeBudget <= pAlgorithmContexts.size() || totalRelativeProgress <= 0) {
      mayAdapt = false;
    }

    for (AlgorithmContext context : pAlgorithmContexts) {
      if (mayAdapt) {
        context.adaptTimeLimit(
        DEFAULT_TIME_LIMIT
            + (int)
                Math.round(
                    ((context.getProgress() / context.timeLimit) / totalRelativeProgress)
                        * totalDistributableTimeBudget));
      }
      context.resetProgress();
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
