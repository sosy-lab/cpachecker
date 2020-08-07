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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.naming.ConfigurationException;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.specification.Specification;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@Options(prefix = "explainer")
public class Explainer extends NestingAlgorithm implements Algorithm {

  @Option(
      secure = true,
      name = "firstStep",
      description = "Configuration of the first step of the Explainer Algorithm")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path firstStepConfig;

  private PredicateCPA cpa;

  private final ExplainerAlgorithmStatistics stats;

  public Explainer(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pSpecification, pCfa);
    pConfig.inject(this);
    stats = new ExplainerAlgorithmStatistics(pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {

    ForwardingReachedSet reached = (ForwardingReachedSet) reachedSet;
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> secondAlg = null;
    ReachedSet currentReached;

    try {
      ShutdownManager shutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
      secondAlg = createAlgorithm(firstStepConfig, cfa.getMainFunction(), shutdownManager, reached);
      cpa =
          CPAs.retrieveCPAOrFail(
              secondAlg.getSecond(), PredicateCPA.class, ConfigurationException.class);
    } catch (IOException | CPAException | InvalidConfigurationException | InterruptedException pE) {
      throw new AssertionError(pE);
    }

    assert secondAlg != null;

    currentReached = secondAlg.getThird();

    Algorithm firstStepAlgorithm = secondAlg.getFirst();
    assert firstStepAlgorithm != null;
    // currentReached
    AlgorithmStatus status;
    status = firstStepAlgorithm.run(currentReached);
    int i = 0;
    // TODO: Review: HAVE TO FIX THIS
    while (currentReached.hasWaitingState() && i < 40) {
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
            from(reached)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));

    ARGPath targetPath = counterExamples.get(0).getTargetPath();

    // TODO: Review: What was the command ?
    // ARGPath targetPath = ARGUtils.getOnePathTo(allTargets.get(0));

    // TODO: Review: Check that x is not in the Waitlist anymore
    // Find All Safe Nodes
    List<ARGState> safeLeafNodes =
        from(currentReached)
            .transform(x -> AbstractStates.extractStateByType(x, ARGState.class))
            .filter(x -> x.getChildren().isEmpty())
            .filter(x -> !x.isTarget())
            .toList();

    // Find the Root Node
    /*ARGState rootNode =
    from(currentReached)
        .transform(x -> AbstractStates.extractStateByType(x, ARGState.class))
        .filter(x -> x.getParents().isEmpty())
        .toList()
        .get(0);*/

    ARGState rootNode =
        AbstractStates.extractStateByType(
            currentReached.getFirstState(),
            ARGState.class); // (ARGState) currentReached.getFirstState();

    Collection<ARGState> statesOnPathTo;

    // Find all ARGStates that are in the Path
    List<ARGPath> safePaths = new ArrayList<>();
    for (ARGState safeLeaf : safeLeafNodes) {
      statesOnPathTo = ARGUtils.getAllStatesOnPathsTo(safeLeaf);
      // path reconstruction
      safePaths = createPath(statesOnPathTo, rootNode);
    }

    // TODO: Turn back on later
    // Constructor for the first 2 Techniques
    ControlFlowDistanceMetric metric =
    new ControlFlowDistanceMetric(new DistanceCalculationHelper());
    List<CFAEdge> closestSuccessfulExecution = null;
    // Compare all paths with the CE
    // Distance Metric No. 1
     closestSuccessfulExecution = metric.startDistanceMetric(safePaths, targetPath);

    // Generate the closest path to the CE with respect to the distance metric
    // Distance Metric No. 2
    // TODO: Change Descriptions, like Distance Metric No 1,2 etc..
    // closestSuccessfulExecution = metric.generateClosestSuccessfulExecution(safePaths,
    // targetPath);

    // create a SOLVER for the 3rd Technique
    try {
      Solver solver = cpa.getSolver();
      BooleanFormulaManagerView bfmgr = solver.getFormulaManager().getBooleanFormulaManager();
      // Create Distance Metric No. 3
      AbstractDistanceMetric metric2 =
          new AbstractDistanceMetric(new DistanceCalculationHelper(bfmgr));
      //closestSuccessfulExecution = metric2.startDistanceMetric(safePaths, targetPath);
    } catch (Exception pE) {
      throw new AssertionError(pE);
    }

    if (closestSuccessfulExecution == null) {
      // EXECUTION COLLAPSED
      logger.log(Level.INFO, "NO SUCCESSFUL EXECUTION WAS FOUND");
      return status;
    }
    ExplainTool.explainDeltas(targetPath.getFullPath(), closestSuccessfulExecution, logger);
    return status;
  }

  /**
   * Find and constructs all Safe Paths Example: When the algorithm constructs the safe path:
   * [1,2,3,4,5] Condition now: [1,2,3], but "3" has 2 Children (1st: "4" and 2nd "9") Then the
   * Algorithm creates a new (copy) List: [1,2,3,4] and put this list in the waitList in order to be
   * examined later. Then adds in the original List the next child [1,2,3,4] and goes on. Terminates
   * when the Wait List is empty
   *
   * @param pStatesOnPathTo the ARGStates on the Path
   * @param root the Beginning of the Path
   * @return a List with all found safe paths
   */
  private List<ARGPath> createPath(Collection<ARGState> pStatesOnPathTo, ARGState root) {
    List<ARGPath> paths = new ArrayList<>();
    List<List<ARGState>> nodeWaitList = new ArrayList<>();
    nodeWaitList.add(new ArrayList<>());
    nodeWaitList.get(0).add(root);
    int currentPathNumber = -1;
    ARGState currentNode = root;
    boolean finished = false;
    for (int i = 0; i < nodeWaitList.size(); i++) {
      currentPathNumber++;
      finished = false;
      currentNode =
          nodeWaitList.get(currentPathNumber).get(nodeWaitList.get(currentPathNumber).size() - 1);
      while (!finished) {
        // FINISH THE CONSTRUCTION OF A WHOLE PATH
        // Get all children that are actually on this path
        ImmutableList<ARGState> children =
            from(currentNode.getChildren()).filter(pStatesOnPathTo::contains).toList();
        if (children.size() == 1) {
          nodeWaitList.get(currentPathNumber).add(children.get(0));
          currentNode = children.get(0);
        } else if (children.size() > 1) {
          // create a new path for every path
          for (int j = 1; j < children.size(); j++) {
            List<ARGState> anotherPath = new ArrayList<>(nodeWaitList.get(currentPathNumber));
            anotherPath.add(children.get(j));
            nodeWaitList.add(anotherPath);
          }
          nodeWaitList.get(currentPathNumber).add(children.get(0));
          currentNode = children.get(0);
        } else {
          ARGPath targetPath = new ARGPath(nodeWaitList.get(currentPathNumber));
          paths.add(targetPath);
          finished = true;
        }
      }
    }
    return paths;
  }

  private Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> createAlgorithm(
      Path singleConfigFileName,
      CFANode mainFunction,
      ShutdownManager singleShutdownManager,
      ReachedSet currentReached)
      throws InvalidConfigurationException, CPAException, IOException, InterruptedException {
    AggregatedReachedSets aggregateReached;
    if (currentReached != null) {
      aggregateReached = new AggregatedReachedSets(Collections.singleton(currentReached));
    } else {
      aggregateReached = new AggregatedReachedSets();
    }
    return super.createAlgorithm(
        singleConfigFileName,
        mainFunction,
        singleShutdownManager,
        aggregateReached,
        ImmutableSet.of("analysis.useExplainer"),
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
