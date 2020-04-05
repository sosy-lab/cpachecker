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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.ErrorInvariantsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.FaultLocalizationAlgorithmInterface;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.MaxSatAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.SingleUnsatCoreAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.rankings.CallHierarchyRanking;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.rankings.EdgeTypeRanking;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.HintRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.MinimalLineDistanceRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.OverallOccurrenceRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.SetSizeRanking;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix="faultlocalization")
public class FaultLocalizationAlgorithm implements Algorithm {

  private final Algorithm algorithm;
  private final LogManager logger;
  private final BooleanFormulaManager bmgr;
  private final FormulaContext context;
  private final PathFormulaManagerImpl manager;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  //private final CFA cfa;

  @Option(secure=true, name="type", toUppercase=true, values={"UNSAT", "MAXSAT", "ERRINV"},
      description="which algorithm to use")
  private String algorithmType = "UNSAT";

  public FaultLocalizationAlgorithm(
      final Algorithm pStoreAlgorithm,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
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

    // Find error labels
    AlgorithmStatus status = algorithm.run(reachedSet);
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));

    FaultLocalizationAlgorithmInterface faultAlgorithm;
    switch (algorithmType){
      case "MAXSAT":
        faultAlgorithm = new MaxSatAlgorithm();
        break;
      case "ERRINV":
        faultAlgorithm = new ErrorInvariantsAlgorithm(shutdownNotifier, config, logger);
        break;
      default:
        faultAlgorithm = new SingleUnsatCoreAlgorithm();
        break;
    }

    // run algorithm for every error
    for (CounterexampleInfo info : counterExamples) {
      runAlgorithm(info, faultAlgorithm);
    }

    logger.log(Level.INFO, "Stopping fault localization...");
    return status;
  }

  private void runAlgorithm(
      CounterexampleInfo pInfo, FaultLocalizationAlgorithmInterface pAlgorithm)
      throws CPAException, InterruptedException {
    // Run the algorithm and create a CFAPathWithAssumptions to the last reached state.
    logger.log(Level.INFO, "Starting fault localization...");

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

      if(edgeList.isEmpty()){
        logger.log(Level.INFO, "Can't find relevant edges in the error trace.");
        return;
      }

      TraceFormula tf = new TraceFormula(context, config, edgeList);
      if(tf.isAlwaysUnsat()){
        logger.log(Level.INFO, "Pre and post condition are unsatisfiable when conjugated. This means the initial variable assignment contradicts the post condition. No further analysis required.");
        return;
      }

      Set<Fault> errorIndicators = pAlgorithm.run(context, tf);
      // FaultLocalizationInfo<Selector> info =
      // FaultLocalizationInfo.withPredefinedHeuristics(result, RankingMode.OVERALL);

      FaultRanking concat =
          FaultRankingUtils.concatHeuristicsDefaultFinalScoring(
              new EdgeTypeRanking(),
              new SetSizeRanking(),
              new HintRanking(3),
              new OverallOccurrenceRanking(),
              new MinimalLineDistanceRanking(edgeList.get(edgeList.size()-1)),
              new CallHierarchyRanking(edgeList, tf.getNegated().size()));

      List<Fault> ranked = concat.rank(errorIndicators);
      ranked.forEach(FaultRankingUtils::assignScoreTo);
      ranked.forEach(l -> l.forEach(FaultRankingUtils::assignScoreTo));
      ranked.sort(Comparator.comparingDouble(Fault::getScore).reversed());

      FaultLocalizationInfo info = new FaultLocalizationInfo(ranked, pInfo);
      info.apply();
      logger.log(
          Level.INFO,
          "Running " + pAlgorithm.getClass().getSimpleName() + ":\n" + info.toString());


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
    } finally{
      context.getSolver().close();
    }
  }
}
