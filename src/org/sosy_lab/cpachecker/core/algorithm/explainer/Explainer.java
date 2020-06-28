/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expre`ss or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.explainer;

// TODO: NEW CLASS FOR THAT ?
//import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "explainer")
public class Explainer extends NestingAlgorithm implements Algorithm {

  @Option(secure = true, name = "secondStep", description = "Configuration of the first step of the Explainer Algorithm")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  private Path secondStepConfig;


  private Algorithm secondStepAlgorithm;

  private final ExplainerAlgorithmStatistics stats;


  public Explainer(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa) throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pSpecification, pCfa);
    pConfig.inject(this);
    stats = new ExplainerAlgorithmStatistics(pLogger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    ForwardingReachedSet reached = (ForwardingReachedSet) reachedSet;
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> secondAlg = null;
    ReachedSet currentReached;

    try {
      ShutdownManager shutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
      secondAlg =
          createAlgorithm(secondStepConfig, cfa.getMainFunction(), shutdownManager, reached);
    } catch (IOException pE) {

    } catch (InvalidConfigurationException pE) {
    } catch (InterruptedException pE) {
    } catch (CPAException pE) {
    }


    currentReached = secondAlg.getThird();

    secondStepAlgorithm = secondAlg.getFirst();

    // currentReached
    AlgorithmStatus status;
    status = secondStepAlgorithm.run(currentReached);
    int i = 0;
    while (currentReached.hasWaitingState() && i < 40) {
      status = secondStepAlgorithm.run(currentReached);
      i++;
    }
    reached.setDelegate(currentReached);

    // All Targets
    ImmutableList<ARGState> allTargets = from(currentReached)
        .transform(s -> AbstractStates.extractStateByType(s, ARGState.class))
        .filter(ARGState::isTarget)
        .toList();
    if (allTargets.isEmpty()) {
      return status;
    }


    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reached).filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));

    ARGPath targetPath = counterExamples.get(0).getTargetPath();


    // Find All Safe Nodes
    List<ARGState> safeLeafNodes = from(currentReached)
        .transform(x -> AbstractStates.extractStateByType(x, ARGState.class))
        .filter(x -> x.getChildren().isEmpty())
        .filter(x -> !x.isTarget()).toList();


    //Find the Root Node
    ARGState rootNode = from(currentReached)
        .transform(x -> AbstractStates.extractStateByType(x, ARGState.class))
        .filter(x -> x.getParents().isEmpty()).toList().get(0);

    Collection<ARGState> statesOnPathTo = null;


    List<ARGPath> safePaths = new ArrayList<>();
    for (ARGState safeLeaf : safeLeafNodes) {
      statesOnPathTo = ARGUtils.getAllStatesOnPathsTo(safeLeaf);
      // path reconstruction
      safePaths = createPath(statesOnPathTo, rootNode);
    }


    // TODO: I need this later
    ControlFLowDistanceMetric metric = new ControlFLowDistanceMetric();
    List<CFAEdge> closestSuccessfulExecution = null;
    // TODO: Bring that back to life
    try {
      // Compare all paths with the CE
      closestSuccessfulExecution = metric.startDistanceMetric(safePaths, targetPath);
      // Generate the closest path to the CE with respect to the distance metric
      //closestSuccessfulExecution = metric.startPathGenerator(safePaths, targetPath);
    } catch (SolverException pE) {
    }



    // create a SOLVER
    Solver solver;
    PredicateCPA cpa = null;
    try {
      cpa = CPAs.retrieveCPAOrFail(secondAlg.getSecond(), PredicateCPA.class,
          ConfigurationException.class);
    } catch (InvalidConfigurationException pE) {
    }
    solver = cpa.getSolver();
    BooleanFormulaManagerView bfmgr = solver.getFormulaManager().getBooleanFormulaManager();

    //AbstractDistanceMetric metric2 = new AbstractDistanceMetric(bfmgr);
    //closestSuccessfulExecution = metric2.startDistanceMetric(safePaths, targetPath);

    if (closestSuccessfulExecution == null) {
      // EXECUTION COLLAPSED
      logger.log(Level.INFO, "NO SUCCESSFUL EXECUTION WAS FOUND");
      return status;
    }
    ExplainTool.ExplainDeltas(targetPath.getFullPath(), closestSuccessfulExecution, logger);
    return status;
  }


  private List<ARGPath> createPath(Collection<ARGState> pStatesOnPathTo, ARGState root) {
    List<ARGPath> paths = new ArrayList<>();
    List<List<ARGState>> pathNodes = new ArrayList<>();
    pathNodes.add(new ArrayList<>());
    pathNodes.get(0).add(root);
    int currentPathNumber = -1;
    ARGState currentNode = root;
    boolean finished = false;
    for (int i = 0; i < pathNodes.size(); i++) {
      currentPathNumber++;
      finished = false;
      currentNode =
          pathNodes.get(currentPathNumber).get(pathNodes.get(currentPathNumber).size() - 1);
      while (!finished) {
        // FINISH THE CONSTRUCTION OF A WHOLE PATH
        List<ARGState> children = new ArrayList<>(currentNode.getChildren());
        children = filterChildren(children, pStatesOnPathTo);
        if (children.size() == 1) {
          pathNodes.get(currentPathNumber).add(children.get(0));
          currentNode = children.get(0);
        } else if (children.size() > 1) {
          // create a new path for every path
          for (int j = 1; j < children.size(); j++) {
            List<ARGState> anotherPath = new ArrayList<>(pathNodes.get(currentPathNumber));
            anotherPath.add(children.get(j));
            pathNodes.add(anotherPath);
          }
          pathNodes.get(currentPathNumber).add(children.get(0));
          currentNode = children.get(0);
        } else {
          ARGPath targetPath = new ARGPath(pathNodes.get(currentPathNumber));
          paths.add(targetPath);
          finished = true;
        }

      }
    }
    return paths;
  }

  private List<ARGState> filterChildren(List<ARGState> children, Collection<ARGState> safeNodes) {
    List<ARGState> result = new ArrayList<>();
    for (ARGState child : children) {
      if (safeNodes.contains(child)) {
        result.add(child);
      }
    }
    return result;
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

  }

  private static class ExplainerAlgorithmStatistics extends MultiStatistics {

    private final int noOfAlgorithmsUsed = 0;
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
    public void printStatistics(
        PrintStream out, Result result,
        UnmodifiableReachedSet reached) {

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
// #######################################################################################################################################
// #######################################################################################################################################
// #######################################################################################################################################
// #######################################################################################################################################
// #######################################################################################################################################
// #######################################################################################################################################
// #######################################################################################################################################
// #######################################################################################################################################
