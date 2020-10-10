// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TAActions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TAShallowStrictSync extends TAEncodingExtensionBase {
  private final TAActions actions;
  private final TimedAutomatonView automata;
  private final boolean multistep;

  public TAShallowStrictSync(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TAActions pActions,
      boolean pMultistep) {
    super(pFmgr);
    actions = pActions;
    automata = pAutomata;
    multistep = pMultistep;
  }

  @Override
  public BooleanFormula makeStepFormula(int pLastReachedIndex) {
    var automataList = ImmutableList.copyOf(automata.getAllAutomata());
    FluentIterable<BooleanFormula> result = FluentIterable.of();
    for (int i = 0; i < automataList.size(); i++) {
      var automaton1 = automataList.get(i);
      for (int j = i + 1; j < automataList.size(); j++) {
        var automaton2 = automataList.get(j);

        var automaton1Actions = ImmutableSet.copyOf(automata.getActionsByAutomaton(automaton1));
        var automaton2Actions = ImmutableSet.copyOf(automata.getActionsByAutomaton(automaton2));

        var sharedActions = Sets.intersection(automaton1Actions, automaton2Actions);

        var sharedActionsSynchronize =
            from(sharedActions)
                .transform(
                    action -> {
                      var automaton1PerformsAction =
                          actions.makeActionEqualsFormula(automaton1, pLastReachedIndex, action);
                      var automaton2PerformsAction =
                          actions.makeActionEqualsFormula(automaton2, pLastReachedIndex, action);
                      return bFmgr.equivalence(automaton1PerformsAction, automaton2PerformsAction);
                    });
        result = result.append(sharedActionsSynchronize);

        var automaton1PerformsDelay =
            automata
                .getDelayAction(automaton1)
                .transform(
                    delayAction ->
                        actions.makeActionEqualsFormula(automaton1, pLastReachedIndex, delayAction))
                .or(bFmgr.makeFalse());
        var automaton2PerformsDelay =
            automata
                .getDelayAction(automaton2)
                .transform(
                    delayAction ->
                        actions.makeActionEqualsFormula(automaton2, pLastReachedIndex, delayAction))
                .or(bFmgr.makeFalse());
        var delaysAreSynchronized =
            bFmgr.equivalence(automaton1PerformsDelay, automaton2PerformsDelay);
        result = result.append(delaysAreSynchronized);

        if (multistep) {
          continue;
        }

        var localToAutomaton1 = Sets.difference(automaton1Actions, automaton2Actions);
        var localToAutomaton2 = Sets.difference(automaton2Actions, automaton1Actions);

        var onlyOneLocalAction1 =
            from(localToAutomaton1)
                .transform(
                    action ->
                        makeOnlyOneLocalActionFormula(
                            automaton1, automaton2, action, pLastReachedIndex));
        var onlyOneLocalAction2 =
            from(localToAutomaton2)
                .transform(
                    action ->
                        makeOnlyOneLocalActionFormula(
                            automaton2, automaton1, action, pLastReachedIndex));
        result = result.append(onlyOneLocalAction1);
        result = result.append(onlyOneLocalAction2);
      }
    }
    return bFmgr.and(result.toSet());
  }

  private BooleanFormula makeOnlyOneLocalActionFormula(
      TaDeclaration localAutomaton,
      TaDeclaration otherAutomaton,
      TaVariable action,
      int pLastReachedIndex) {
    if (automata.getIdleAction(localAutomaton).transform(idle -> idle == action).or(false)
        || automata.getDelayAction(localAutomaton).transform(delay -> delay == action).or(false)) {
      return bFmgr.makeTrue();
    }

    var automaton1PerformsAction =
        actions.makeActionEqualsFormula(localAutomaton, pLastReachedIndex, action);
    var automaton2PerformsIdle =
        automata
            .getIdleAction(otherAutomaton)
            .transform(
                idleAction ->
                    actions.makeActionEqualsFormula(otherAutomaton, pLastReachedIndex, idleAction))
            .or(bFmgr.makeFalse());
    return bFmgr.implication(automaton1PerformsAction, automaton2PerformsIdle);
  }
}
