// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.encodings;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAEncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.TABooleanVarFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.TADiscreteFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TALocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables.TAVariables;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TATransitionUnrolling extends TAEncodingBase {
  private final TADiscreteFeatureEncoding<TCFAEdge> transitions;
  private final TCFAEdge delayEdge;
  private final Map<TaDeclaration, TCFAEdge> idleEdgesByAutomaton;

  public TATransitionUnrolling(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TAVariables pTime,
      TALocations pLocations,
      TAEncodingExtension pExtensions) {
    super(pFmgr, pAutomata, pTime, pLocations, pExtensions);

    idleEdgesByAutomaton = new HashMap<>();
    automata.getAllAutomata().forEach(this::createIdleEdgeForAutomaton);

    delayEdge = TCFAEdge.createDummyEdge();

    Table<TaDeclaration, TCFAEdge, String> variableNames = HashBasedTable.create();
    var elementsByAutomaton = new HashMap<TaDeclaration, Collection<TCFAEdge>>();
    from(automata.getAllAutomata())
        .transformAndConcat(this::getTransitionVariablesForAutomaton)
        .forEach(
            variable -> {
              variableNames.put(variable.automaton, variable.edge, variable.variableName);
              elementsByAutomaton.computeIfAbsent(variable.automaton, a -> new HashSet<>());
              elementsByAutomaton.get(variable.automaton).add(variable.edge);
            });
    transitions = new TABooleanVarFeatureEncoding<>(pFmgr, variableNames, elementsByAutomaton);
  }

  private void createIdleEdgeForAutomaton(TaDeclaration pAutomaton) {
    var idleEdge = TCFAEdge.createDummyEdge();
    idleEdgesByAutomaton.put(pAutomaton, idleEdge);
  }

  private static class TransitionVariable {
    TaDeclaration automaton;
    TCFAEdge edge;
    String variableName;
  }

  private Iterable<TransitionVariable> getTransitionVariablesForAutomaton(
      TaDeclaration pAutomaton) {
    var idleEdge = idleEdgesByAutomaton.get(pAutomaton);
    return from(automata.getEdgesByAutomaton(pAutomaton))
        .transform(edge -> makeTransitionVariable(pAutomaton, edge, "edge_" + edge.hashCode()))
        .append(makeTransitionVariable(pAutomaton, delayEdge, "delay_edge"))
        .append(makeTransitionVariable(pAutomaton, idleEdge, "idle_" + pAutomaton.getName()));
  }

  private TransitionVariable makeTransitionVariable(
      TaDeclaration automaton, TCFAEdge edge, String variableName) {
    var result = new TransitionVariable();
    result.automaton = automaton;
    result.edge = edge;
    result.variableName = variableName;
    return result;
  }

  @Override
  protected BooleanFormula makeAutomatonTransitionsFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex) {
    var transitionContraints = makeConstraintsFormula(pAutomaton, pLastReachedIndex);

    var idleEdge = idleEdgesByAutomaton.get(pAutomaton);
    var idleTransitionVariable =
        transitions.makeEqualsFormula(pAutomaton, pLastReachedIndex, idleEdge);
    var delayTransitionVariable =
        transitions.makeEqualsFormula(pAutomaton, pLastReachedIndex, delayEdge);
    var discreteTransitionVariables =
        from(automata.getEdgesByAutomaton(pAutomaton))
            .transform(edge -> transitions.makeEqualsFormula(pAutomaton, pLastReachedIndex, edge));
    var transitionVariables =
        discreteTransitionVariables.append(delayTransitionVariable).append(idleTransitionVariable);

    var atLeastOneTransition = bFmgr.or(transitionVariables.toSet());

    var transitionNotTakenFormulas = transitionVariables.transform(bFmgr::not).toSet();
    var transitionNotTakenPairs =
        Sets.cartesianProduct(transitionNotTakenFormulas, transitionNotTakenFormulas);
    var atLeastOneInPairNotTakenFormulas =
        from(transitionNotTakenPairs)
            .filter(pair -> !pair.get(0).equals(pair.get(1)))
            .transform(bFmgr::or);
    var noTwoTransitionsFormula = bFmgr.and(atLeastOneInPairNotTakenFormulas.toSet());

    return bFmgr.and(transitionContraints, atLeastOneTransition, noTwoTransitionsFormula);
  }

  private BooleanFormula makeConstraintsFormula(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var delayTransitionVariable =
        transitions.makeEqualsFormula(pAutomaton, pLastReachedIndex, delayEdge);
    var delayTransitionConstraint = makeDelayTransition(pAutomaton, pLastReachedIndex);
    var delayTransitionFormula =
        bFmgr.implication(delayTransitionVariable, delayTransitionConstraint);

    var idleEdge = idleEdgesByAutomaton.get(pAutomaton);
    var idleTransitionVariable =
        transitions.makeEqualsFormula(pAutomaton, pLastReachedIndex, idleEdge);
    var idleTransitionConstraint = makeIdleTransition(pAutomaton, pLastReachedIndex);
    var idleTransitionFormula = bFmgr.implication(idleTransitionVariable, idleTransitionConstraint);

    var discreteTransitionsFormula = bFmgr.makeTrue();
    for (var edge : automata.getEdgesByAutomaton(pAutomaton)) {
      var discreteTransitionVariable =
          transitions.makeEqualsFormula(pAutomaton, pLastReachedIndex, edge);
      var discreteTransitionConstraint = makeDiscreteStep(pAutomaton, pLastReachedIndex, edge);
      var discreteTransitionFormula =
          bFmgr.implication(discreteTransitionVariable, discreteTransitionConstraint);
      discreteTransitionsFormula = bFmgr.and(discreteTransitionFormula, discreteTransitionsFormula);
    }

    return bFmgr.and(delayTransitionFormula, idleTransitionFormula, discreteTransitionsFormula);
  }
}
