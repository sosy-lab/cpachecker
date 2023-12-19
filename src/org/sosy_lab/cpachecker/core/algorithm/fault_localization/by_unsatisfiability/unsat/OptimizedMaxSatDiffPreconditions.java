package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.unsat;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.FaultLocalizerWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.InvalidCounterexampleException;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition.PreCondition;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition.VariableAssignmentPreConditionComposer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.java_smt.api.SolverException;

public class OptimizedMaxSatDiffPreconditions
    implements FaultLocalizerWithTraceFormula, StatisticsProvider {

  private final MaxSatStatistics stats = new MaxSatStatistics();

  private final boolean stopAfterFirstFault;

  public OptimizedMaxSatDiffPreconditions(boolean pStopAfterFirstFault) {
    stopAfterFirstFault = pStopAfterFirstFault;
  }

  @Override
  public Collection<Fault> run(FormulaContext pContext, TraceFormula pTf)
      throws CPAException, InterruptedException, SolverException, InvalidConfigurationException {

    VariableAssignmentPreConditionComposer composer =
        new VariableAssignmentPreConditionComposer(
            pContext, new TraceFormulaOptions(pContext.getConfiguration()), false, true);

    List<Fault> results = new ArrayList<>();

    ImmutableList.Builder<PreCondition> preconditions = ImmutableList.builder();

    preconditions.add(pTf.getPrecondition());

    ImmutableList<CFAEdge> cfaCounter = buildCfaCounter(pTf.getPrecondition());

    Set<Fault> result = runOptimizedMaxSatAlgorithm(pContext, pTf);
    results.addAll(result);
    PreCondition newPreCondition = pTf.getPrecondition();

    for (int i = 0; i < 4; i++) {
      try {
        newPreCondition = composer.createNondetPrecondition(cfaCounter, preconditions.build());
        // Error happens here, when calling the prover
      } catch (InvalidCounterexampleException e) {
        throw new CPAException("Could not create new precondition", e);
      }

      TraceFormula newTraceFormula =
          TraceFormula.instantiate(
              pContext, newPreCondition, pTf.getTrace(), pTf.getPostCondition());
      result = runOptimizedMaxSatAlgorithm(pContext, newTraceFormula);
      results.addAll(result);
      preconditions.add(newPreCondition);
    }

    return ImmutableList.copyOf(results);
  }

  private ImmutableList<CFAEdge> buildCfaCounter(PreCondition preCondition) {
    return ImmutableList.<CFAEdge>builder()
        .addAll(preCondition.getEdgesForPrecondition())
        .addAll(preCondition.getRemainingCounterexample())
        .build();
  }

  private Set<Fault> runOptimizedMaxSatAlgorithm(FormulaContext pContext, TraceFormula traceFormula)
      throws CPAException, InterruptedException, SolverException {
    OptimizedMaxSatAlgorithm algorithm = new OptimizedMaxSatAlgorithm(stopAfterFirstFault);
    return algorithm.run(pContext, traceFormula);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
