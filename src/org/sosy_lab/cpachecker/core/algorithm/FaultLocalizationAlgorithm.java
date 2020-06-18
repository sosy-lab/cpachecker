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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.ErrorInvariantsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.FaultLocalizationAlgorithmInterface;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.MaxSatAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.SingleUnsatCoreAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.ExpressionConverter;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.rankings.CallHierarchyRanking;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.rankings.EdgeTypeRanking;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.rankings.ForwardPreConditionRanking;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.rankings.InformationProvider;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.HintRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.MinimalLineDistanceRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.OverallOccurrenceRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.SetSizeRanking;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix="faultlocalization")
public class FaultLocalizationAlgorithm implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final LogManager logger;
  private final BooleanFormulaManager bmgr;
  private final FormulaContext context;
  private final PathFormulaManagerImpl manager;
  private final TraceFormulaOptions options;

  private final FaultLocalizationAlgorithmInterface faultAlgorithm;
  private final StatTimer totalTime = new StatTimer("Total time");

  @Option(secure=true, name="type", toUppercase=true, values={"UNSAT", "MAXSAT", "ERRINV"},
      description="which algorithm to use")
  private String algorithmType = "UNSAT";

  @Option(secure=true, name="maintainhierarchy",
      description="sort by call order")
  private boolean maintainCallHierarchy = false;

  public FaultLocalizationAlgorithm(
      final Algorithm pStoreAlgorithm,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    logger = pLogger;

    //Options
    pConfig.inject(this);
    options = new TraceFormulaOptions(pConfig);
    checkOptions();

    // Parent algorithm
    algorithm = pStoreAlgorithm;

    // Create formula context
    Solver solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    manager =
        new PathFormulaManagerImpl(
            solver.getFormulaManager(),
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    context = new FormulaContext(solver, manager, new ExpressionConverter(pConfig));

    switch (algorithmType){
      case "MAXSAT":
        faultAlgorithm = new MaxSatAlgorithm();
        break;
      case "ERRINV":
        faultAlgorithm = new ErrorInvariantsAlgorithm(pShutdownNotifier, pConfig, logger);
        break;
      default:
        faultAlgorithm = new SingleUnsatCoreAlgorithm();
        break;
    }
  }

  public boolean checkOptions(){
    boolean correctConfiguration = true;
    if (!algorithmType.equals("ERRINV") && maintainCallHierarchy) {
      logger.log(Level.SEVERE, "The optiion maintainhierarchy will be ignored since the error invariants algorithm is not selected");
      maintainCallHierarchy = false;
      correctConfiguration = false;
    }
    if (!options.getBan().isBlank() && algorithmType.equals("ERRINV")) {
      logger.log(Level.SEVERE, "The option ban will be ignored because it is not applicable on the error invariants algorithm");
      correctConfiguration = false;
    }
    return correctConfiguration;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {

    totalTime.start();
    // Find error labels
    AlgorithmStatus status = algorithm.run(reachedSet);
    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));


    // run algorithm for every error
    logger.log(Level.INFO, "Starting fault localization...");
    for (CounterexampleInfo info : counterExamples) {
      logger.log(Level.INFO, "Find explanations for fault #" + info.getUniqueId());
      runAlgorithm(info, faultAlgorithm);
    }
    logger.log(Level.INFO, "Stopping fault localization...");
    totalTime.stop();
    return status;
  }

  private void runAlgorithm(
      CounterexampleInfo pInfo, FaultLocalizationAlgorithmInterface pAlgorithm)
      throws CPAException, InterruptedException {

    // Run the algorithm and create a CFAPathWithAssumptions to the last reached state.
    CFAPathWithAssumptions assumptions = pInfo.getCFAPathWithAssignments();
    if (assumptions.isEmpty()) {
      logger.log(Level.INFO, "The analysis returned no assumptions.");
      logger.log(Level.INFO, "No bugs found.");
      return;
    }

    try {
      // Collect all edges that do not evaluate to true
      List<CFAEdge> edgeList = new ArrayList<>();
      Map<CFAEdge, Integer> mapEdgeToIndex = new HashMap<>();
      int i = 0;
      for (CFAEdgeWithAssumptions assumption : assumptions) {
        if (!bmgr.isTrue(
            manager
                .makeFormulaForPath(Collections.singletonList(assumption.getCFAEdge()))
                .getFormula())) {
          edgeList.add(assumption.getCFAEdge());
          mapEdgeToIndex.put(assumption.getCFAEdge(), i);
          i++;
        }
      }

      if(edgeList.isEmpty()){
        logger.log(Level.INFO, "Can't find relevant edges in the error trace.");
        return;
      }

      TraceFormula tf = new TraceFormula(context, options, edgeList);
      if(tf.isAlwaysUnsat()){
        logger.log(Level.INFO, "Pre and post condition are unsatisfiable when conjugated. This means the initial variable assignment contradicts the post condition. No further analysis required.");
        return;
      }

      Set<Fault> errorIndicators = pAlgorithm.run(context, tf);

      //Find correct ranking
      FaultRanking ranking;
      switch(algorithmType){
        case "MAXSAT": {
          ranking =  FaultRankingUtils.concatHeuristicsDefaultFinalScoring(
              new ForwardPreConditionRanking(tf, context),
              new EdgeTypeRanking(),
              new SetSizeRanking(),
              new HintRanking(3),
              new OverallOccurrenceRanking(),
              new MinimalLineDistanceRanking(edgeList.get(edgeList.size()-1)),
              new CallHierarchyRanking(edgeList, tf.getNegated().size()));
          break;
        }
        case "ERRINV": {
          ranking = FaultRankingUtils.concatHeuristicsDefaultFinalScoring(
              new ForwardPreConditionRanking(tf, context),
              new EdgeTypeRanking(),
              new HintRanking(3),
              // new MinimalLineDistanceRanking(edgeList.get(edgeList.size()-1)),
              new CallHierarchyRanking(edgeList, tf.getNegated().size()));
          break;
        }
        default: {
          ranking = FaultRankingUtils.concatHeuristicsDefaultFinalScoring(
              new ForwardPreConditionRanking(tf, context),
              new EdgeTypeRanking(),
              new HintRanking(-1),
              new CallHierarchyRanking(edgeList, tf.getNegated().size()));
        }
      }

      InformationProvider.searchForAdditionalInformation(errorIndicators, edgeList);
      FaultLocalizationInfo info;
      if (maintainCallHierarchy && algorithmType.equals("ERRINV")) {
        List<Fault> faults = ranking.rank(errorIndicators);
        faults.forEach(FaultRankingUtils::assignScoreTo);
        faults.sort(Comparator.comparingInt(f -> mapEdgeToIndex.get(f.iterator().next().correspondingEdge())));
        info = new FaultLocalizationInfo(faults, pInfo);
      } else {
        info = new FaultLocalizationInfo(errorIndicators, ranking, pInfo);
      }
      //info.getHtmlWriter().hideTypes(InfoType.RANK_INFO);
      info.apply();
      logger.log(
          Level.INFO,
          "Running " + pAlgorithm.getClass().getSimpleName() + ":\n" + info.toString());

    } catch (SolverException sE) {
      logger.log(Level.INFO, "The solver was not able to find the UNSAT-core of the path formula.");
    } catch (VerifyException vE) {
      logger.log(Level.INFO, "No bugs found because the trace formula is satisfiable or the counterexample is spurious.");
    } catch (InvalidConfigurationException iE) {
      logger.log(Level.INFO, "Incomplete analysis because of invalid configuration.");
    } catch (IllegalStateException iE) {
      logger.log(
          Level.INFO, "The counterexample is spurious. Calculating interpolants is not possible.");
    } finally{
      context.getSolver().close();
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
    if(algorithm instanceof Statistics){
      statsCollection.add((Statistics)algorithm);
    }
    if(faultAlgorithm instanceof Statistics){
      statsCollection.add((Statistics)faultAlgorithm);
    }
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter w0  = StatisticsWriter.writingStatisticsTo(out);
   w0.put("Total time", totalTime);
  }

  @Override
  public @Nullable String getName() {
    return "Fault Localization";
  }
}
