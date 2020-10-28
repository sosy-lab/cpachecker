// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Splitter;
import com.google.common.base.VerifyException;
import com.google.common.collect.FluentIterable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.FaultLocalizerWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants.ErrorInvariantsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants.IntervalReportWriter;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings.CallHierarchyScoring;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings.EdgeTypeScoring;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings.InformationProvider;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.Selector;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat.ModifiedMaxSatAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat.OriginalMaxSatAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat.SingleUnsatCoreAlgorithm;
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
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.MinimalLineDistanceScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.OverallOccurrenceScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.SetSizeScoring;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "faultLocalization.by_traceformula")
public class FaultLocalizationWithTraceFormula
    implements Algorithm, StatisticsProvider, Statistics {

  enum AlgorithmTypes {
    UNSAT, MAXSAT, MAXORG, ERRINV
  }

  private final Algorithm algorithm;
  private final LogManager logger;
  private final BooleanFormulaManager bmgr;
  private final FormulaContext context;
  private final PathFormulaManagerImpl manager;
  private final TraceFormulaOptions options;

  private final FaultLocalizerWithTraceFormula faultAlgorithm;
  private final StatTimer totalTime = new StatTimer("Total time for fault localization");

  @Option(secure=true, name="type",
      description="which algorithm to use")
  private AlgorithmTypes algorithmType = AlgorithmTypes.UNSAT;

  @Option(
      secure = true,
      name = "errorInvariants.disableFSTF",
      description =
          "disable flow-sensitive trace formula (may increase runtime)") // can decrease runtime
  private boolean disableFSTF = false;

  @Option(secure=true, name="maxsat.ban",
      description="ban faults with certain variables")
  private String ban = "";

  public FaultLocalizationWithTraceFormula(
      final Algorithm pStoreAlgorithm,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    if (!(pStoreAlgorithm instanceof CounterexampleStoreAlgorithm)){
      throw new InvalidConfigurationException("option CounterexampleStoreAlgorithm required");
    }

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
    context = new FormulaContext(solver, manager, pCfa, logger, pConfig, pShutdownNotifier);

    switch (algorithmType){
      case MAXORG:
        faultAlgorithm = new OriginalMaxSatAlgorithm();
        break;
      case MAXSAT:
        faultAlgorithm = new ModifiedMaxSatAlgorithm();
        break;
      case ERRINV:
        faultAlgorithm = new ErrorInvariantsAlgorithm(pShutdownNotifier, pConfig, logger);
        break;
      case UNSAT:
        faultAlgorithm = new SingleUnsatCoreAlgorithm();
        break;
      default: throw new AssertionError("The specified algorithm type does not exist");
    }
  }

  public void checkOptions () throws InvalidConfigurationException {
    if (!algorithmType.equals(AlgorithmTypes.ERRINV) && disableFSTF) {
      throw new InvalidConfigurationException(
          "The option flow-sensitive trace formula will be ignored since the error invariants"
              + " algorithm is not selected");
    }
    if (algorithmType.equals(AlgorithmTypes.ERRINV) && !ban.isBlank()) {
      throw new InvalidConfigurationException(
          "The option ban will be ignored since the error invariants algorithm is not selected");
    }
    if (!algorithmType.equals(AlgorithmTypes.MAXSAT) && options.isReduceSelectors()) {
      throw new InvalidConfigurationException(
          "The option reduceselectors will be ignored since MAX-SAT is not selected");
    }
    if (!options.getDisable().isBlank() && algorithmType.equals(AlgorithmTypes.ERRINV)) {
      throw new InvalidConfigurationException(
          "The option ban will be ignored because it is not applicable on the error invariants"
              + " algorithm");
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {

    AlgorithmStatus status = algorithm.run(reachedSet);
    totalTime.start();
    try {
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
    } finally{
      totalTime.stop();
    }
    return status;
  }

  private void runAlgorithm(
      CounterexampleInfo pInfo, FaultLocalizerWithTraceFormula pAlgorithm)
      throws CPAException, InterruptedException {

    // Run the algorithm and create a CFAPathWithAssumptions to the last reached state.
    CFAPathWithAssumptions assumptions = pInfo.getCFAPathWithAssignments();
    if (assumptions.isEmpty()) {
      logger.log(
          Level.INFO, "The analysis returned no assumptions. Fault localization not possible.");
      return;
    }

    try {
      // Collect all edges that do not evaluate to true
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

      //Find correct ranking and correct trace formula for the specified algorithm
      TraceFormula tf;
      FaultScoring ranking;
      switch(algorithmType){
        case MAXORG:
        case MAXSAT: {
          tf = new TraceFormula.SelectorTrace(context, options, edgeList);
          ranking =  FaultRankingUtils.concatHeuristics(
              new EdgeTypeScoring(),
              new SetSizeScoring(),
              new OverallOccurrenceScoring(),
              new MinimalLineDistanceScoring(edgeList.get(edgeList.size()-1)),
              new CallHierarchyScoring(edgeList, tf.getPostConditionOffset()));
          break;
        }
        case ERRINV: {
          tf = disableFSTF ? new TraceFormula.DefaultTrace(context, options, edgeList) : new TraceFormula.FlowSensitiveTrace(context, options, edgeList);
          ranking = FaultRankingUtils.concatHeuristics(
              new EdgeTypeScoring(),
              new CallHierarchyScoring(edgeList, tf.getPostConditionOffset()));
          break;
        }
        case UNSAT: {
          tf = new TraceFormula.DefaultTrace(context, options, edgeList);
          ranking = FaultRankingUtils.concatHeuristics(
              new EdgeTypeScoring(),
              new CallHierarchyScoring(edgeList, tf.getPostConditionOffset()));
          break;
        }
        default: throw new AssertionError("The specified algorithm type does not exist");
      }

      if (!tf.isCalculationPossible()) {
        logger.log(
            Level.INFO,
            "Pre- and post-condition are unsatisfiable. No further analysis required. Most likely"
                + " the variables in your post-condition never change their value.");
        return;
      }

      Set<Fault> errorIndicators = pAlgorithm.run(context, tf);

      if(!algorithmType.equals(AlgorithmTypes.ERRINV)) {
        ban(errorIndicators);
      }

      InformationProvider.searchForAdditionalInformation(errorIndicators, edgeList);
      InformationProvider.addDefaultPotentialFixesToFaults(errorIndicators, 3);
      InformationProvider.propagatePreCondition(errorIndicators, tf, context.getSolver().getFormulaManager());
      FaultLocalizationInfo info = new FaultLocalizationInfo(errorIndicators, ranking, pInfo);

      if (algorithmType.equals(AlgorithmTypes.ERRINV)) {
        info.replaceHtmlWriter(new IntervalReportWriter());
        info.sortIntended();
      }

      info.getHtmlWriter().hideTypes(InfoType.RANK_INFO);
      info.apply();
      logger.log(
          Level.INFO,
          "Running " + pAlgorithm.getClass().getSimpleName() + ":\n" + info.toString());

    } catch (SolverException sE) {
      throw new CPAException(
          "The solver was not able to find the UNSAT-core of the path formula.", sE);
    } catch (VerifyException vE) {
      throw new CPAException(
          "No bugs found because the trace formula is satisfiable or the counterexample is"
              + " spurious.",
          vE);
    } catch (InvalidConfigurationException iE) {
      throw new CPAException( "Incomplete analysis because of invalid configuration.", iE);
    } catch (IllegalStateException iE) {
      throw new CPAException(
          "The counterexample is spurious. Calculating interpolants is not possible.", iE);
    } finally{
      context.getSolver().close();
      context.getProver().close();
    }
  }

  /**
   * Ban all faults containing certain variables defined in the option ban.
   * @param pErrorIndicators result set
   */
  private void ban(Set<Fault> pErrorIndicators) {
    List<String> banned = Splitter.on(",").splitToList(ban);
    Set<Fault> copy = new HashSet<>(pErrorIndicators);
    for (Fault errorIndicator : copy) {
      for (FaultContribution faultContribution : errorIndicator) {
        BooleanFormula curr = ((Selector)faultContribution).getEdgeFormula();
        for (String b: banned) {
          if (b.contains("::")){
            if (curr.toString().contains(b + "@")){
              pErrorIndicators.remove(errorIndicator);
              break;
            }
          } else {
            if (curr.toString().contains("::"+b +"@")){
              pErrorIndicators.remove(errorIndicator);
              break;
            }
          }
        }
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
    if (algorithm instanceof Statistics) {
      statsCollection.add((Statistics) algorithm);
    }
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
    if (faultAlgorithm instanceof Statistics) {
      statsCollection.add((Statistics) faultAlgorithm);
    }
    if (faultAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) faultAlgorithm).collectStatistics(statsCollection);
    }
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out).put(totalTime);
  }

  @Override
  public @Nullable String getName() {
    return getClass().getSimpleName();
  }

}
