/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.VerifyException;
import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.*;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristicUtils;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.OverallAppearanceHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.SingleEdgeHintHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.Selector;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.CallHierarchyHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.ErrorLocationNearestHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.SetSizeHeuristic;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class FaultLocalizationAlgorithm implements Algorithm {

  private final Algorithm algorithm;
  private final LogManager logger;
  private final BooleanFormulaManager bmgr;
  private final FormulaContext context;
  private final PathFormulaManagerImpl manager;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  //private final CFA cfa;

  private final boolean useSingleUnsat = false;
  private final boolean useMaxSat = true;
  private final boolean useErrInv = false;
  // private final boolean useErrInvOptim = false;

  public FaultLocalizationAlgorithm(
      final Algorithm pStoreAlgorithm,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    algorithm = pStoreAlgorithm;
    Solver solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    manager =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);
    logger = pLogger;
    //cfa = pCfa;
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    context = new FormulaContext(solver, manager);
    shutdownNotifier = pShutdownNotifier;
    config = pConfig;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {

    // Run the algorithm and create a CFAPathWithAssumptions to the last reached state.
    logger.log(Level.INFO, "Starting fault localization...");

    // Find error labels
    AlgorithmStatus status = algorithm.run(reachedSet);
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));

    List<FaultLocalizationAlgorithmInterface> algorithms = new ArrayList<>();
    if (useSingleUnsat) algorithms.add(new SingleUnsatCoreAlgorithm());
    if (useMaxSat) algorithms.add(new MaxSatAlgorithm());
    if (useErrInv)
      algorithms.add(new ErrorInvariantsAlgorithm(shutdownNotifier, config, logger));

    // run algorithm for every error
    for (CounterexampleInfo info : counterExamples) {
      runAlgorithm(info, algorithms);
    }

    logger.log(Level.INFO, "Stopping fault localization...");
    return status;
  }

  private void runAlgorithm(
      CounterexampleInfo pInfo, List<FaultLocalizationAlgorithmInterface> pAlgorithms)
      throws CPAException, InterruptedException {
    if (pAlgorithms.size() == 0) {
      logger.log(
          Level.INFO,
          "No algorithm passed for fault localization. Using SingleUnsatCoreAlgorithm as default.");
      pAlgorithms.add(new SingleUnsatCoreAlgorithm());
    }

    CFAPathWithAssumptions assumptions = pInfo.getCFAPathWithAssignments();
    if (assumptions.size() == 0) {
      logger.log(Level.INFO, "The analysis returned no assumptions.");
      logger.log(Level.INFO, "No bugs found.");
      return;
    }

    try {
      // Conjunct all but the last assumption and ignore BlankEdges and DeclarationEdges because
      // they evaluate to true
      List<CFAEdge> edgeList = new ArrayList<>();
      for (CFAEdgeWithAssumptions assumption : assumptions) {
        if (!bmgr.isTrue(
            manager
                .makeFormulaForPath(Collections.singletonList(assumption.getCFAEdge()))
                .getFormula())) {
          edgeList.add(assumption.getCFAEdge());
        }
      }

      TraceFormula tf = new TraceFormula(context, edgeList);

      for (FaultLocalizationAlgorithmInterface locAlgorithm : pAlgorithms) {
        ErrorIndicatorSet<Selector> errorIndicators = locAlgorithm.run(context, tf);
        // FaultLocalizationInfo<Selector> info =
        // FaultLocalizationInfo.withPredefinedHeuristics(result, RankingMode.OVERALL);

        FaultLocalizationHeuristic<Selector> concat =
            FaultLocalizationHeuristicUtils.concatHeuristics(List.of(
                new SingleEdgeHintHeuristic<>(),
                new OverallAppearanceHeuristic<>(),
                new ErrorLocationNearestHeuristic<>(edgeList.get(edgeList.size() - 1)),
                new CallHierarchyHeuristic<>(edgeList, tf.getNegated().size())));
        FaultLocalizationInfo<Selector> info =
            new FaultLocalizationInfo<>(errorIndicators, pInfo, Optional.empty(), Optional.of(new SetSizeHeuristic<>()));
        pInfo.getTargetPath().getLastState().replaceCounterexampleInformation(info);
        logger.log(
            Level.INFO,
            "Running " + locAlgorithm.getClass().getSimpleName() + ":\n" + info.toString());
      }

    } catch (SolverException sE) {
      logger.log(Level.SEVERE, "SolverException: " + sE.getMessage());
      logger.log(Level.INFO, "The solver was not able to find the UNSAT-core of the path formula.");
    } catch (VerifyException vE) {
      logger.log(Level.INFO, "No bugs found because the trace formula is satisfiable.");
    } catch (InvalidConfigurationException iE) {
      logger.log(Level.INFO, "Incomplete analysis because of invalid configuration");
    } catch (IllegalStateException iE) {
      logger.log(
          Level.INFO, "The counterexample is spurious. Calculating interpolants is not possible.");
    }
  }
}
