// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.explainer;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.NestingAlgorithm;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.MultiStatistics;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;

@Options(prefix = "faultLocalization.by_distance")
public class Explainer extends NestingAlgorithm {

  private enum Metric {
    ADM,
    CFDM,
    PG;
  }

  @Option(
      secure = true,
      name = "analysis",
      description = "Configuration to use for initial program-state exploration")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path firstStepConfig = null;

  @Option(
      secure = true,
      name = "metric",
      description = "The distance metric that ought to be used for the computation of the distance")
  private Metric distanceMetric = Metric.ADM;

  @Option(
      secure = true,
      name = "stopAfter",
      description =
          "Maximum number of explorations to run for collecting error paths, before performing"
              + " fault localization.  Exploration runs stop when the program under analysis is"
              + " fully explored or the specified number of runs is reached. Fault localization may"
              + " be more precise if more error paths are available.")
  private int stopAfter = 40;

  private PredicateCPA cpa;

  private final ExplainerAlgorithmStatistics stats;
  private final CFA cfa;

  public Explainer(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pSpecification);
    pConfig.inject(this);
    cfa = pCfa;
    stats = new ExplainerAlgorithmStatistics(pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {

    ForwardingReachedSet reached = (ForwardingReachedSet) reachedSet;
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> secondAlg = null;
    ReachedSet currentReached;

    try {
      ShutdownManager shutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
      secondAlg = createAlgorithm(firstStepConfig, cfa, shutdownManager, reached);
      cpa = CPAs.retrieveCPAOrFail(secondAlg.getSecond(), PredicateCPA.class, Explainer.class);
    } catch (IOException pE) {
      throw new AssertionError(pE);
    } catch (InvalidConfigurationException pE) {
      throw new CPAException("First Step Configuration File is invalid", pE);
    }

    currentReached = secondAlg.getThird();

    Algorithm firstStepAlgorithm = secondAlg.getFirst();
    assert firstStepAlgorithm != null;
    // currentReached
    AlgorithmStatus status;
    status = firstStepAlgorithm.run(currentReached);
    int i = 0;
    while (currentReached.hasWaitingState() && i < stopAfter) {
      status = firstStepAlgorithm.run(currentReached);
      i++;
    }
    reached.setDelegate(currentReached);

    // Find All Targets
    ImmutableList<ARGState> allTargets =
        from(currentReached)
            .transform(s -> AbstractStates.extractStateByType(s, ARGState.class))
            .filter(ARGState::isTarget)
            .toList();

    if (allTargets.isEmpty()) {
      return status;
    }

    // Get a Path to the Target
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            FluentIterable.from(allTargets).transform(ARGState::getCounterexampleInformation));

    ARGPath targetPath = counterExamples.get(0).getTargetPath();

    // Find All Safe Nodes
    List<ARGState> safeLeafNodes =
        from(currentReached)
            .transform(x -> AbstractStates.extractStateByType(x, ARGState.class))
            .filter(x -> x.getChildren().isEmpty())
            .filter(x -> !x.isTarget())
            .filter(x -> x.wasExpanded())
            .toList();

    ARGState rootNode =
        AbstractStates.extractStateByType(currentReached.getFirstState(), ARGState.class);

    List<CFAEdge> closestSuccessfulExecution = null;

    if (distanceMetric.equals(Metric.PG)) {
      startPathGeneration(targetPath, counterExamples.get(0));
      return status;
    } else {
      List<ARGPath> safePaths = findAllSafePaths(safeLeafNodes, rootNode);
      switch (distanceMetric) {
        case ADM:
          closestSuccessfulExecution = startADM(safePaths, targetPath);
          break;
        case CFDM:
          closestSuccessfulExecution = startCFDM(safePaths, targetPath);
          break;
        default:
          logger.log(Level.WARNING, "NO DISTANCE METRIC WAS GIVEN");
          return status;
      }
    }

    if (closestSuccessfulExecution == null) {
      // EXECUTION COLLAPSED
      logger.log(Level.INFO, "NO SUCCESSFUL EXECUTION WAS FOUND");
      return status;
    }
    new ExplainTool()
        .explainDeltas(
            new DistanceCalculationHelper().cleanPath(targetPath.getFullPath()),
            closestSuccessfulExecution,
            counterExamples.get(0));

    return status;
  }

  /**
   * Find all successful program executions
   *
   * @param safeLeafNodes the nodes that the successful runs go through
   * @param rootNode the starting node
   * @return a list with all the safe paths that have been found
   */
  private List<ARGPath> findAllSafePaths(List<ARGState> safeLeafNodes, ARGState rootNode) {
    Collection<ARGState> statesOnPathTo;
    List<ARGPath> safePaths = new ArrayList<>();
    for (ARGState safeLeaf : safeLeafNodes) {
      statesOnPathTo = ARGUtils.getAllStatesOnPathsTo(safeLeaf);
      // path reconstruction
      safePaths =
          new DistanceCalculationHelper(null)
              .generateAllSuccessfulExecutions(statesOnPathTo, rootNode, true);
    }
    return safePaths;
  }

  /**
   * This method starts the AbstractDistanceMetric
   *
   * @param safePaths the safe paths that have to be compared wit the counterexample
   * @param targetPath the counterexample
   * @return the closest to the counterexample successful run
   */
  private List<CFAEdge> startADM(List<ARGPath> safePaths, ARGPath targetPath) {
    DistanceMetric metric;
    @SuppressWarnings("resource")
    BooleanFormulaManagerView bfmgr =
        cpa.getSolver().getFormulaManager().getBooleanFormulaManager();
    metric = new AbstractDistanceMetric(new DistanceCalculationHelper(bfmgr));
    return metric.startDistanceMetric(safePaths, targetPath);
  }

  /**
   * This method starts the ControlFlowDistanceMetric
   *
   * @param safePaths the safe paths that have to be compared wit the counterexample
   * @param targetPath the counterexample
   * @return the closest to the counterexample successful run
   */
  private List<CFAEdge> startCFDM(List<ARGPath> safePaths, ARGPath targetPath) {
    DistanceMetric metric = new ControlFlowDistanceMetric(new DistanceCalculationHelper());
    return metric.startDistanceMetric(safePaths, targetPath);
  }

  /**
   * This method starts the Path Generation technique
   *
   * @param targetPath the counterexample
   * @param ceInfo the Information about the counterexample which we need for the Presentation of
   *     Differences in ExplainTool
   */
  private void startPathGeneration(ARGPath targetPath, CounterexampleInfo ceInfo) {
    ControlFlowDistanceMetric pathGeneration =
        new ControlFlowDistanceMetric(new DistanceCalculationHelper());
    pathGeneration.generateClosestSuccessfulExecution(targetPath, ceInfo);
  }

  private Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> createAlgorithm(
      Path singleConfigFileName,
      CFA pCfa,
      ShutdownManager singleShutdownManager,
      ReachedSet currentReached)
      throws InvalidConfigurationException, CPAException, IOException, InterruptedException {
    AggregatedReachedSets aggregateReached;
    if (currentReached != null) {
      aggregateReached = AggregatedReachedSets.singleton(currentReached);
    } else {
      aggregateReached = AggregatedReachedSets.empty();
    }
    return super.createAlgorithm(
        singleConfigFileName,
        pCfa.getMainFunction(),
        pCfa,
        singleShutdownManager,
        aggregateReached,
        ImmutableSet.of("analysis.algorithm.faultLocalization.by_distance"),
        stats.getSubStatistics());
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(stats);
  }

  private static class ExplainerAlgorithmStatistics extends MultiStatistics {

    private static final int noOfAlgorithmsUsed = 0;
    private Timer totalTime = new Timer();

    public ExplainerAlgorithmStatistics(LogManager pLogger) {
      super(pLogger);
    }

    @Override
    public void resetSubStatistics() {
      super.resetSubStatistics();
      totalTime = new Timer();
    }

    @Override
    public String getName() {
      return "Explainer Algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {

      out.println("Number of algorithms provided:    ");
      out.println("Number of algorithms used:        " + noOfAlgorithmsUsed);

      printSubStatistics(out, result, reached);
    }

    private void printSubStatistics(
        PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Total time for algorithm " + noOfAlgorithmsUsed + ": " + totalTime);
      super.printStatistics(out, result, reached);
    }
  }
}
