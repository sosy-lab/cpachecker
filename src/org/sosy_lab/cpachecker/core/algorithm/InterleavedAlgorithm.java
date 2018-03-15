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

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
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
import javax.annotation.Nullable;
import javax.management.JMException;
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
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTimeLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
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
      CONTINUE, NOREUSE, REUSEPRECISION;
    }

    private final Path configFile;
    private final int timeLimit;
    private final REPETITIONMODE mode;
    private final Timer timer;

    private Algorithm algorithm;
    private @Nullable ConfigurableProgramAnalysis cpa;
    private Configuration config;
    private ShutdownManager localShutdownManager;
    private ReachedSet reached;

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
        case "reuse-precision":
          return REPETITIONMODE.REUSEPRECISION;
        default:
          return REPETITIONMODE.NOREUSE;
      }
    }

    private boolean reuseCPA() {
      return mode == REPETITIONMODE.CONTINUE || reusePrecision();
    }

    private boolean reusePrecision() {
      return mode == REPETITIONMODE.REUSEPRECISION;
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
        "print the statistics of each component of the interleaved algorithm"
            + " directly after the component's computation is finished"
  )
  private boolean printIntermediateStatistics = false;

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

      AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_PRECISE;

      Iterator<AlgorithmContext> algorithmContextCycle =
          Iterables.cycle(algorithmContexts).iterator();

      while (!shutdownNotifier.shouldShutdown() && algorithmContextCycle.hasNext()) {

        // retrieve context from last execution of current algorithm
        AlgorithmContext currentContext = algorithmContextCycle.next();
        boolean analysisFinishedWithResult = false;

        currentContext.timer.start();
        try {

          if (stats.noOfCurrentAlgorithm == stats.noOfAlgorithms) {
            stats.noOfCurrentAlgorithm = 1;
            stats.noOfRounds++;
            logger.log(
                Level.INFO, "InterleavedAlgorithm switches to the next interleave iteration...");
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
            createNextAlgorithm(currentContext, mainFunction);

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

          if (currentContext.algorithm instanceof StatisticsProvider) {
            ((StatisticsProvider) currentContext.algorithm)
                .collectStatistics(stats.getSubStatistics());
          }

          shutdownNotifier.shutdownIfNecessary();

          logger.logf(Level.INFO, "Starting analysis %d ...", stats.noOfCurrentAlgorithm);
          status = currentContext.algorithm.run(currentContext.reached);

          if (from(currentContext.reached).anyMatch(IS_TARGET_STATE) && status.isPrecise()) {
            analysisFinishedWithResult = true;
            return status;
          }
          if (!status.isSound()) {
            logger.logf(
                Level.INFO,
                "Analysis %d terminated, but result is unsound.",
                stats.noOfCurrentAlgorithm);

          } else if (currentContext.reached.hasWaitingState()) {
            logger.logf(
                Level.INFO,
                "Analysis %d terminated but did not finish: There are still states to be processed.",
                stats.noOfCurrentAlgorithm);

          } else if (!(from(currentContext.reached).anyMatch(IS_TARGET_STATE)
              && !status.isPrecise())) {
            // sound analysis and completely finished, terminate
            analysisFinishedWithResult = true;
            return status;
          }

          shutdownNotifier.shutdownIfNecessary();

          if (printIntermediateStatistics) {
            stats.printIntermediateStatistics(System.out, Result.UNKNOWN, currentContext.reached);
          }
          if (writeIntermediateOutputFiles) {
            stats.writeOutputFiles(Result.UNKNOWN, pReached);
          }

        } catch (CPAException e) {
          if (e instanceof CounterexampleAnalysisFailed || e instanceof RefinementFailedException) {
            status = status.withPrecise(false);
          }

          logger.logUserException(
              Level.WARNING, e, "Analysis " + stats.noOfCurrentAlgorithm + " not completed.");

        } catch (InterruptedException e) {
          logger.logUserException(
              Level.WARNING, e, "Analysis " + stats.noOfCurrentAlgorithm + " stopped.");

          shutdownNotifier.shutdownIfNecessary();

        } finally {
          if (currentContext.config != null) {
            currentContext.localShutdownManager.getNotifier().unregister(logShutdownListener);
            currentContext.localShutdownManager.requestShutdown("Analysis terminated.");

            if (!analysisFinishedWithResult && !shutdownNotifier.shouldShutdown()) {
              stats.resetSubStatistics();
            }

            if (!currentContext.reuseCPA()) {
              CPAs.closeCpaIfPossible(currentContext.cpa, logger);
            }

            CPAs.closeIfPossible(currentContext.algorithm, logger);
          }

          currentContext.timer.stop();
        }
      }

      for (AlgorithmContext context : algorithmContexts) {
        CPAs.closeCpaIfPossible(context.cpa, logger);
      }

      logger.log(Level.INFO, "Shutdown of interleaved algorithm, analysis not finished yet.");
      return status;

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

    } catch (InvalidConfigurationException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Skipping one analysis because the configuration file "
              + singleConfigFileName.toString()
              + " is invalid");
      return;
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
      return;
    }

    pContext.config = singleConfigBuilder.build();
  }

  private void createNextAlgorithm(AlgorithmContext pContext, CFANode pMainFunction)
      throws InvalidConfigurationException, CPAException, InterruptedException {

    pContext.localShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    ArrayList<ResourceLimit> limits = new ArrayList<>();
    try {
      limits.add(ProcessCpuTimeLimit.fromNowOn(TimeSpan.ofSeconds(pContext.timeLimit)));
    } catch (JMException e) {
      logger.log(
          Level.SEVERE,
          "Your Java VM does not support measuring the cpu time. Ignore time limit.",
          e);
    }

    ResourceLimitChecker singleLimits = new ResourceLimitChecker(pContext.localShutdownManager,limits);
    singleLimits.start();
    pContext.localShutdownManager.getNotifier().register(logShutdownListener);

    AggregatedReachedSets aggregateReached = new AggregatedReachedSets();
    CoreComponentsFactory localCoreComponents =
        new CoreComponentsFactory(
            pContext.config, logger, pContext.localShutdownManager.getNotifier(), aggregateReached);

    if (pContext.reuseCPA()) {
      if (pContext.cpa == null) {
        // create cpa only once when not initialized, use global limits (i.e. shutdownNotifier)
        CoreComponentsFactory globalCoreComponents =
            new CoreComponentsFactory(pContext.config, logger, shutdownNotifier, aggregateReached);
        pContext.cpa = globalCoreComponents.createCPA(cfa, specification);
        if (!pContext.reusePrecision()) {
          // create reached set only once, continue analysis
          pContext.reached =
              createInitialReachedSet(pContext.cpa, pMainFunction, globalCoreComponents, null);
        }
      }
      if (pContext.reusePrecision()) {
        // start with new reached set each time, but precision from previous analysis if possible
        pContext.reached =
            createInitialReachedSet(
                pContext.cpa, pMainFunction, localCoreComponents, pContext.reached);
      }
    } else {
      // do not reuse cpa, and, thus reached set
      pContext.cpa = localCoreComponents.createCPA(cfa, specification);
      pContext.reached =
          createInitialReachedSet(pContext.cpa, pMainFunction, localCoreComponents, null);
    }

    // always create algorithm with new "local" shutdown manager
    pContext.algorithm = localCoreComponents.createAlgorithm(pContext.cpa, cfa, specification);

    if (pContext.cpa instanceof StatisticsProvider) {
      ((StatisticsProvider) pContext.cpa).collectStatistics(stats.getSubStatistics());
    }

    if (pContext.algorithm instanceof InterleavedAlgorithm) {
      // To avoid accidental infinitely-recursive nesting.
      throw new InvalidConfigurationException(
          "Interleaved analysis parts may not be interleaved analyses theirselves.");
    }
  }

  private ReachedSet createInitialReachedSet(
      final ConfigurableProgramAnalysis pCpa,
      final CFANode pMainFunction,
      final CoreComponentsFactory pFactory,
      final @Nullable ReachedSet previousReachedSet)
      throws InterruptedException {

    logger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState =
        pCpa.getInitialState(pMainFunction, StateSpacePartition.getDefaultPartition());

    Precision initialPrecision =
        pCpa.getInitialPrecision(pMainFunction, StateSpacePartition.getDefaultPartition());
    if (previousReachedSet != null) {
      initialPrecision = aggregatePrecisionsForReuse(previousReachedSet, initialPrecision);
    }

    ReachedSet reached = pFactory.createReachedSet();
    reached.add(initialState, initialPrecision);
    return reached;
  }

  private Precision aggregatePrecisionsForReuse(
      final ReachedSet pPreviousReachedSet, final Precision pInitialPrecision) {
    Precision resultPrec = pInitialPrecision;

    if (Precisions.extractPrecisionByType(resultPrec, VariableTrackingPrecision.class) != null) {
      resultPrec =
          Precisions.replaceByType(
              resultPrec,
              VariableTrackingPrecision.joinVariableTrackingPrecisionsInReachedSet(
                  pPreviousReachedSet),
              Predicates.instanceOf(VariableTrackingPrecision.class));
    }

    PredicatePrecision predPrec;
    predPrec = Precisions.extractPrecisionByType(resultPrec, PredicatePrecision.class);

    if (predPrec != null) {
      Collection<PredicatePrecision> predPrecs =
          new HashSet<>(pPreviousReachedSet.getPrecisions().size());
      predPrecs.add(predPrec);
      for (Precision prec : pPreviousReachedSet.getPrecisions()) {
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

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
