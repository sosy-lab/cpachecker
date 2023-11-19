// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdateListener;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdater;
import org.sosy_lab.cpachecker.core.algorithm.pcc.PartialARGsCombiner;
import org.sosy_lab.cpachecker.core.defaults.MultiStatistics;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

@Options(prefix = "restartAlgorithm")
public class RestartAlgorithm extends NestingAlgorithm implements ReachedSetUpdater {

  private static class RestartAlgorithmStatistics extends MultiStatistics {

    private final List<String> algorithms;
    private int noOfAlgorithmsUsed = 0;
    private Timer totalTime = new Timer();

    public RestartAlgorithmStatistics(List<String> pAlgorithms, LogManager pLogger) {
      super(pLogger);
      algorithms = pAlgorithms;
    }

    @Override
    public void resetSubStatistics() {
      super.resetSubStatistics();
      totalTime = new Timer();
    }

    @Override
    public String getName() {
      return "Restart Algorithm";
    }

    private void printIntermediateStatistics(PrintStream out, Result result, ReachedSet reached) {

      String text = "Statistics for algorithm " + noOfAlgorithmsUsed + " of " + algorithms.size();
      out.println(text);
      out.println("=".repeat(text.length()));

      printSubStatistics(out, result, reached);
      out.println();
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {

      out.println("Number of algorithms provided:    " + algorithms.size());
      out.println("Number of algorithms used:        " + noOfAlgorithmsUsed);
      out.println("Last algorithm used:        " + getLastAlgorithmUsed());

      printSubStatistics(out, result, reached);
    }

    private String getLastAlgorithmUsed() {
      if (noOfAlgorithmsUsed > 0) {
        return algorithms.get(noOfAlgorithmsUsed - 1);
      } else {
        return "none";
      }
    }

    private void printSubStatistics(
        PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Total time for algorithm " + noOfAlgorithmsUsed + ": " + totalTime);
      super.printStatistics(out, result, reached);
    }
  }

  @Option(
      secure = true,
      required = true,
      description =
          "List of files with configurations to use. A filename can be suffixed with"
              + " :if-interrupted, :if-failed, and :if-terminated which means that this"
              + " configuration will only be used if the previous configuration ended with a"
              + " matching condition. What also can be added is :use-reached then the reached set"
              + " of the preceding analysis is taken and provided to the next analysis.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<AnnotatedValue<Path>> configFiles;

  @Option(
      secure = true,
      name = "combineARGsAfterRestart",
      description =
          "combine (partial) ARGs obtained by restarts of the analysis after an unknown result with"
              + " a different configuration")
  private boolean useARGCombiningAlgorithm = false;

  @Option(
      secure = true,
      description =
          "print the statistics of each component of the restart algorithm"
              + " directly after the components computation is finished")
  private boolean printIntermediateStatistics = true;

  @Option(
      secure = true,
      description =
          "let each component of the restart algorithm write output files"
              + " and not only the last one that is excuted")
  private boolean writeIntermediateOutputFiles = false;

  /* The option is useful for some preanalysis,
   * for instance, the first analysis is fast and provides some hints to the next ones
   * Is used, for example, in CPALockator
   *
   * TODO It might be better to have two lists of algorithms given to the RestartAlgorithm.
   * One list for analysis with no result expected except information about the program
   * and a second list for the real analyses to be restarted if necessary.
   * This would allow to combine the pre-computation of information and
   * the normal sequential composition of algorithms in a more flexible way.
   */
  @Option(
      secure = true,
      description = "wether to start next algorithm independently from the previous result")
  private boolean alwaysRestart = false;

  enum LastAnalysisResult {
    /** Analysis terminated regularly. */
    TERMINATED,
    /** Analysis was interrupted with a shutdown signal. */
    INTERUPTED,
    /** Analysis produced a CPAException */
    FAILED,
  }

  private final ShutdownRequestListener logShutdownListener;
  private final RestartAlgorithmStatistics stats;
  private final CFA cfa;
  private Algorithm currentAlgorithm;

  private final List<ReachedSetUpdateListener> reachedSetUpdateListeners =
      new CopyOnWriteArrayList<>();

  private final Collection<ReachedSetUpdateListener> reachedSetUpdateListenersAdded =
      new ArrayList<>();

  private RestartAlgorithm(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(config, pLogger, pShutdownNotifier, pSpecification);
    config.inject(this);

    cfa = pCfa;

    if (configFiles.isEmpty()) {
      throw new InvalidConfigurationException(
          "Need at least one configuration for restart algorithm!");
    }

    List<String> configNames =
        configFiles.stream().map(AnnotatedValue::value).map(Path::toString).toList();
    stats = new RestartAlgorithmStatistics(configNames, pLogger);

    logShutdownListener =
        reason ->
            logger.logf(
                Level.WARNING,
                "Shutdown of analysis %d requested (%s).",
                stats.noOfAlgorithmsUsed,
                reason);
  }

  public static Algorithm create(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    RestartAlgorithm algorithm =
        new RestartAlgorithm(pConfig, pLogger, pShutdownNotifier, pSpecification, pCfa);
    if (algorithm.useARGCombiningAlgorithm) {
      return new PartialARGsCombiner(algorithm, pConfig, pLogger, pShutdownNotifier);
    }
    return algorithm;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
    checkArgument(
        pReached instanceof ForwardingReachedSet, "RestartAlgorithm needs ForwardingReachedSet");
    checkArgument(
        pReached.size() <= 1,
        "RestartAlgorithm does not support being called several times with the same reached set");
    checkArgument(!pReached.isEmpty(), "RestartAlgorithm needs non-empty reached set");

    ForwardingReachedSet reached = (ForwardingReachedSet) pReached;

    Iterable<CFANode> initialNodes = AbstractStates.extractLocations(pReached.getFirstState());
    CFANode initialNode = Iterables.getOnlyElement(initialNodes);

    PeekingIterator<AnnotatedValue<Path>> configFilesIterator =
        Iterators.peekingIterator(configFiles.iterator());

    AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_PRECISE;
    boolean isLastReachedSetUsable = false;
    final List<ConfigurableProgramAnalysis> cpasToClose = new ArrayList<>();

    while (configFilesIterator.hasNext()) {
      stats.totalTime.start();
      @Nullable ConfigurableProgramAnalysis currentCpa = null;
      ReachedSet currentReached;
      ShutdownManager singleShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);

      try {
        final AnnotatedValue<Path> singleConfig = configFilesIterator.next();
        final Path singleConfigFileName = singleConfig.value();
        logger.logf(
            Level.INFO,
            "Loading analysis %d from file %s ...",
            stats.noOfAlgorithmsUsed + 1,
            singleConfigFileName);

        try {
          NestedAnalysis currentAlg =
              createNextAlgorithm(
                  singleConfigFileName,
                  initialNode,
                  cfa,
                  singleShutdownManager,
                  analysisWantsPreviousReachedSet(singleConfig),
                  // we can only use the reached set if the last analysis terminated without
                  // exception
                  isLastReachedSetUsable ? reached.getDelegate() : null);
          currentAlgorithm = currentAlg.algorithm();
          currentCpa = currentAlg.cpa();
          currentReached = currentAlg.reached();
        } catch (InvalidConfigurationException e) {
          logger.logUserException(
              Level.WARNING,
              e,
              "Skipping one analysis because the configuration file "
                  + singleConfigFileName
                  + " is invalid");
          continue;
        } catch (IOException e) {
          String message =
              "Skipping one analysis because the configuration file "
                  + singleConfigFileName
                  + " could not be read";
          if (shutdownNotifier.shouldShutdown() && e instanceof ClosedByInterruptException) {
            logger.log(Level.WARNING, message);
          } else {
            logger.logUserException(Level.WARNING, e, message);
          }
          continue;
        }

        if (reached instanceof HistoryForwardingReachedSet) {
          ((HistoryForwardingReachedSet) reached).saveCPA(currentCpa);
        }
        reached.setDelegate(currentReached);

        if (currentAlgorithm instanceof StatisticsProvider) {
          ((StatisticsProvider) currentAlgorithm).collectStatistics(stats.getSubStatistics());
        }
        shutdownNotifier.shutdownIfNecessary();

        stats.noOfAlgorithmsUsed++;

        // run algorithm
        registerReachedSetUpdateListeners();
        try {
          logger.logf(Level.INFO, "Starting analysis %d ...", stats.noOfAlgorithmsUsed);
          status = currentAlgorithm.run(currentReached);

          isLastReachedSetUsable = true;
          skipNextAnalysesIfRequired(
              configFilesIterator, LastAnalysisResult.TERMINATED, false, false);

          if (status.isPrecise() && currentReached.wasTargetReached()) {

            // If the algorithm is not _precise_, verdict "false" actually means "unknown".
            return status;
          }

          if (!status.isSound()) {
            // if the analysis is not sound and we can proceed with
            // another algorithm, continue with the next algorithm
            logger.logf(
                Level.INFO,
                "Analysis %d terminated, but result is unsound.",
                stats.noOfAlgorithmsUsed);

          } else if (currentReached.hasWaitingState()) {
            // if there are still states in the waitlist, the result is unknown
            // continue with the next algorithm
            logger.logf(
                Level.INFO,
                "Analysis %d terminated but did not finish: There are still states to be"
                    + " processed.",
                stats.noOfAlgorithmsUsed);

          } else if (!(!status.isPrecise() && currentReached.wasTargetReached())) {

            if (!(alwaysRestart && configFilesIterator.hasNext())) {
              // sound analysis and completely finished, terminate
              return status;
            }
          }

        } catch (CPAException e) {
          isLastReachedSetUsable = false;
          skipNextAnalysesIfRequired(
              configFilesIterator,
              LastAnalysisResult.FAILED,
              e.getMessage().contains("recursion"),
              e.getMessage().contains("pthread_create"));

          if (e instanceof CounterexampleAnalysisFailed
              || e instanceof RefinementFailedException
              || e instanceof InfeasibleCounterexampleException) {
            status = status.withPrecise(false);
          }
          if (configFilesIterator.hasNext()) {
            logger.logUserException(
                Level.WARNING, e, "Analysis " + stats.noOfAlgorithmsUsed + " not completed.");
          } else {
            throw e;
          }
        } catch (InterruptedException e) {
          isLastReachedSetUsable = false;
          skipNextAnalysesIfRequired(
              configFilesIterator, LastAnalysisResult.INTERUPTED, false, false);

          if (configFilesIterator.hasNext()) {
            logger.logUserException(
                Level.WARNING, e, "Analysis " + stats.noOfAlgorithmsUsed + " stopped.");
            shutdownNotifier.shutdownIfNecessary(); // check if we should also stop
          } else {
            throw e;
          }
        }
      } finally {
        unregisterReachedSetUpdateListeners();
        singleShutdownManager.getNotifier().unregister(logShutdownListener);
        singleShutdownManager.requestShutdown(
            "Analysis terminated"); // shutdown any remaining components
        stats.totalTime.stop();
      }

      shutdownNotifier.shutdownIfNecessary();

      if (configFilesIterator.hasNext()) {
        printIntermediateStatistics(currentReached);
        stats.resetSubStatistics();

        if (currentCpa != null && !analysisWantsPreviousReachedSet(configFilesIterator.peek())) {
          CPAs.closeCpaIfPossible(currentCpa, logger);
        } else {
          cpasToClose.add(currentCpa);
        }
        CPAs.closeIfPossible(currentAlgorithm, logger);

        logger.log(Level.INFO, "RestartAlgorithm switches to the next configuration...");
      }
    }

    for (ConfigurableProgramAnalysis cpa : cpasToClose) {
      CPAs.closeCpaIfPossible(cpa, logger);
    }

    // no further configuration available, and analysis has not finished
    logger.log(Level.INFO, "No further configuration available.");
    return status;
  }

  /**
   * Advance the given iterator by skipping over analyses where the usage condition does not match,
   * such that the next entry in the iterator is the next analysis that should be used or the
   * iterator is empty.
   */
  private void skipNextAnalysesIfRequired(
      final PeekingIterator<AnnotatedValue<Path>> configFilesIterator,
      final LastAnalysisResult lastAnalysisResult,
      final boolean recursionFound,
      final boolean concurrencyFound) {

    while (configFilesIterator.hasNext()) {
      Optional<String> condition = configFilesIterator.peek().annotation();
      if (condition.isEmpty()) {
        return; // next analysis used unconditionally
      }
      boolean foundConfig =
          switch (condition.orElseThrow()) {
            case "if-interrupted" -> lastAnalysisResult == LastAnalysisResult.INTERUPTED;
            case "if-failed" -> lastAnalysisResult == LastAnalysisResult.FAILED;
            case "if-terminated" -> lastAnalysisResult == LastAnalysisResult.TERMINATED;
            case "if-recursive" -> recursionFound;
            case "if-concurrent" -> concurrencyFound;
            case "use-reached" -> true;
            default -> {
              logger.logf(
                  Level.WARNING,
                  "Ignoring invalid restart condition '%s' for file %s.",
                  condition.orElseThrow(),
                  configFilesIterator.peek().value());
              yield true;
            }
          };
      if (foundConfig) {
        return;
      } else {
        logger.logf(
            Level.INFO,
            "Ignoring restart configuration '%s' because condition %s did not match.",
            configFilesIterator.peek().value(),
            condition.orElseThrow());
        configFilesIterator.next();
        stats.noOfAlgorithmsUsed++;
      }
    }
  }

  @SuppressFBWarnings(
      value = "DM_DEFAULT_ENCODING",
      justification = "Encoding is irrelevant for null output stream")
  @SuppressForbidden("System.out is correct for statistics")
  private void printIntermediateStatistics(ReachedSet currentReached) {
    if (printIntermediateStatistics) {
      stats.printIntermediateStatistics(System.out, Result.UNKNOWN, currentReached);
    } else {
      @SuppressWarnings("checkstyle:IllegalInstantiation") // ok for statistics
      final PrintStream dummyStream = new PrintStream(ByteStreams.nullOutputStream());
      stats.printIntermediateStatistics(dummyStream, Result.UNKNOWN, currentReached);
    }
    if (writeIntermediateOutputFiles) {
      stats.writeOutputFiles(Result.UNKNOWN, currentReached);
    }
  }

  private NestedAnalysis createNextAlgorithm(
      Path singleConfigFileName,
      CFANode pInitialNode,
      CFA pCfa,
      ShutdownManager singleShutdownManager,
      boolean pProvideReachedForNextAlgorithm,
      ReachedSet pCurrentReached)
      throws InvalidConfigurationException, CPAException, IOException, InterruptedException {

    AggregatedReachedSets aggregateReached;
    if (pProvideReachedForNextAlgorithm && pCurrentReached != null) {
      aggregateReached = AggregatedReachedSets.singleton(pCurrentReached);
    } else {
      aggregateReached = AggregatedReachedSets.empty();
    }

    return super.createAlgorithm(
        singleConfigFileName,
        pInitialNode,
        pCfa,
        singleShutdownManager,
        aggregateReached,
        Sets.newHashSet("restartAlgorithm.configFiles", "analysis.restartAfterUnknown"),
        stats.getSubStatistics());
  }

  private boolean analysisWantsPreviousReachedSet(AnnotatedValue<?> singleConfigEntry) {
    return singleConfigEntry.annotation().equals(Optional.of("use-reached"));
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (currentAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) currentAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  @Override
  public void register(ReachedSetUpdateListener pReachedSetUpdateListener) {
    reachedSetUpdateListeners.add(pReachedSetUpdateListener);
  }

  @Override
  public void unregister(ReachedSetUpdateListener pReachedSetUpdateListener) {
    reachedSetUpdateListeners.remove(pReachedSetUpdateListener);
  }

  private void registerReachedSetUpdateListeners() {
    Preconditions.checkState(reachedSetUpdateListenersAdded.isEmpty());
    if (currentAlgorithm instanceof ReachedSetUpdater algorithm) {
      for (ReachedSetUpdateListener listener : reachedSetUpdateListeners) {
        algorithm.register(listener);
        reachedSetUpdateListenersAdded.add(listener);
      }
    }
  }

  private void unregisterReachedSetUpdateListeners() {
    if (currentAlgorithm instanceof ReachedSetUpdater algorithm) {
      for (ReachedSetUpdateListener listener : reachedSetUpdateListenersAdded) {
        algorithm.unregister(listener);
      }
      reachedSetUpdateListenersAdded.clear();
    } else {
      Preconditions.checkState(reachedSetUpdateListenersAdded.isEmpty());
    }
    assert reachedSetUpdateListenersAdded.isEmpty();
  }
}
