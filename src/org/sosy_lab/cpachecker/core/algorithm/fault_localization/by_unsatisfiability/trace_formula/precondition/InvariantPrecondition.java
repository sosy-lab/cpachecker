/*
// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.InvalidCounterexampleException;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.TraceFormulaOptions;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.postcondition.FinalAssumeEdgesOnSameLinePostConditionComposer;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.postcondition.PostCondition;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.Trace;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.java_smt.api.BasicProverEnvironment.AllSatCallback;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class InvariantPrecondition implements PreConditionComposer {

  private final FormulaContext context;
  private final TraceFormulaOptions options;

  public InvariantPrecondition(FormulaContext pContext, TraceFormulaOptions pOptions) {
    context = pContext;
    options = pOptions;
  }

  @Override
  public PreCondition extractPreCondition(List<CFAEdge> pCounterexample)
      throws SolverException, InterruptedException, CPAException, InvalidCounterexampleException,
          InvalidConfigurationException {
    InterpolationManager interpolationManager =
        new InterpolationManager(
            context.getManager(),
            context.getSolver(),
            Optional.empty(),
            Optional.empty(),
            context.getConfiguration(),
            context.getShutdownNotifier(),
            context.getLogger());
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    VariableAssignmentPreConditionComposer preConditionComposer =
        new VariableAssignmentPreConditionComposer(context, options, false);
    PreCondition preCondition = preConditionComposer.extractPreCondition(pCounterexample);
    PostCondition postCondition =
        new FinalAssumeEdgesOnSameLinePostConditionComposer(context)
            .extractPostCondition(preCondition.getRemainingCounterexample());
    Trace trace =
        Trace.fromCounterexample(postCondition.getRemainingCounterexample(), context, options);
    TraceFormula errorTrace = TraceFormula.instantiate(context, preCondition, trace, postCondition);
    // allFormulas.add(errorTrace.getPrecondition().getPrecondition());
    List<BooleanFormula> allFormulas = new ArrayList<>(errorTrace.getTrace().toFormulaList());
    allFormulas.add(bmgr.not(errorTrace.getPostCondition().getPostCondition()));
    Collections.reverse(allFormulas);
    CounterexampleTraceInfo counterexampleTraceInfo =
        interpolationManager.buildCounterexampleTrace(new BlockFormulas(allFormulas));
    List<BooleanFormula> interpolants =
        transformedImmutableListCopy(
            counterexampleTraceInfo.getInterpolants(),
            element -> context.getSolver().getFormulaManager().uninstantiate(element));
    List<BooleanFormula> parts = new ArrayList<>(interpolants.size() * 2);
    for (BooleanFormula interpolant : interpolants) {
      parts.addAll(bmgr.toConjunctionArgs(interpolant, true));
    }
    Set<BooleanFormula> possiblePreconditions = new HashSet<>();
    for (BooleanFormula part : parts) {
      if (bmgr.isFalse(part)) {
        continue;
      }
      if (context
          .getSolver()
          .isUnsat(
              bmgr.and(
                  bmgr.not(context
                      .getSolver()
                      .getFormulaManager()
                      .instantiate(part, trace.getInitialSsaMap())),
                  trace.toFormulaList().stream().collect(bmgr.toConjunction()),
                  bmgr.not(postCondition.getPostCondition())))) {
        possiblePreconditions.add(part);
      }
    }
    return possiblePreconditions.isEmpty() ? preCondition : PreCondition.of(Iterables.getFirst(possiblePreconditions, bmgr.makeTrue()));
   }

  public PreCondition allAssignments(List<CFAEdge> pCounterexample)
      throws CPATransferException, InterruptedException, InvalidCounterexampleException,
             SolverException {
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    BooleanFormula precond = bmgr.makeTrue();
    Set<String> nondetVariables = new HashSet<>();
    List<BooleanFormula> f =
        Trace.fromCounterexample(pCounterexample, context, options).stream()
            .map(s -> s.getFormula())
            .collect(Collectors.toList());
    List<BooleanFormula> variables =
        FluentIterable.from(f)
            .transformAndConcat(
                d -> context.getSolver().getFormulaManager().extractVariables(d).values())
            .filter(BooleanFormula.class)
            .toList();

    try (ProverEnvironment prover =
             context.getProver(ProverOptions.GENERATE_MODELS, ProverOptions.GENERATE_ALL_SAT)) {
      prover.push(context.getManager().makeFormulaForPath(pCounterexample).getFormula());
      if (prover.isUnsat()) {
        throw new InvalidCounterexampleException(
            "Precondition cannot be computed since counterexample is not feasible.");
      }
      List<List<BooleanFormula>> model =
          prover.allSat(
              new AllSatCallback<>() {
                private final List<List<BooleanFormula>> models = new ArrayList<>();

                public void apply(List<BooleanFormula> pModel) {
                  this.models.add(pModel);
                }

                public List<List<BooleanFormula>> getResult() {
                  return this.models;
                }
              },
              variables);
      for (ValueAssignment modelAssignment : prover.getModelAssignments()) {
        context.getLogger().log(Level.FINEST, "tfprecondition=" + modelAssignment);
        BooleanFormula formula = modelAssignment.getAssignmentAsFormula();
        if (!Pattern.matches(".+::.+@[0-9]+", modelAssignment.getKey().toString())) {
          precond = bmgr.and(precond, formula);
          FluentIterable.from(
                  context.getSolver().getFormulaManager().extractVariables(formula).keySet())
              .filter(name -> name.contains("__VERIFIER_nondet_"))
              .copyInto(nondetVariables);
        }
      }
      return new PreCondition(
          ImmutableList.of(),
          pCounterexample,
          context.getSolver().getFormulaManager().uninstantiate(precond),
          nondetVariables);
    }
  }
}
*/
