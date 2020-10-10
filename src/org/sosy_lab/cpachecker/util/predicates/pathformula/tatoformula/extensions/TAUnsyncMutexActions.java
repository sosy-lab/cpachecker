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
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TAActions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TAUnsyncMutexActions extends TAEncodingExtensionBase {
  private final TAActions actions;
  private final TimedAutomatonView automata;

  public TAUnsyncMutexActions(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata, TAActions pActions) {
    super(pFmgr);
    actions = pActions;
    automata = pAutomata;
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

        var exclusiveToAutomaton1 = Sets.difference(automaton1Actions, automaton2Actions);
        var exclusiveToAutomaton2 = Sets.difference(automaton2Actions, automaton1Actions);

        var actions1DoNotOccurFormulas =
            from(exclusiveToAutomaton1)
                .transform(
                    action -> actions.makeActionOccursInStepFormula(pLastReachedIndex, action))
                .transform(bFmgr::not)
                .toSet();

        var actions2DoNotOccurFormulas =
            from(exclusiveToAutomaton2)
                .transform(
                    action -> actions.makeActionOccursInStepFormula(pLastReachedIndex, action))
                .transform(bFmgr::not)
                .toSet();

        var actionDoesNotOccurFormulaPairs =
            Sets.cartesianProduct(actions1DoNotOccurFormulas, actions2DoNotOccurFormulas);
        var atLeastOneInPairDoesNotOccurFormulas =
            from(actionDoesNotOccurFormulaPairs)
                .filter(pair -> !pair.get(0).equals(pair.get(1)))
                .transform(bFmgr::or);

        result = result.append(atLeastOneInPairDoesNotOccurFormulas);
      }
    }
    return bFmgr.and(result.toSet());
  }
}
