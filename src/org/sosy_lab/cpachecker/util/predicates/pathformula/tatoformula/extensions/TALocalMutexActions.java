// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.Sets;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TAActions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TALocalMutexActions extends TAEncodingExtensionBase {
  private final TAActions actions;
  private final TimedAutomatonView automata;

  public TALocalMutexActions(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata, TAActions pActions) {
    super(pFmgr);
    actions = pActions;
    automata = pAutomata;
  }

  @Override
  public BooleanFormula makeAutomatonStep(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var actionDoesNotOccurFormulas =
        from(automata.getActionsByAutomaton(pAutomaton))
            .transform(
                action -> actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action))
            .transform(bFmgr::not)
            .toSet();

    var actionDoesNotOccurFormulaPairs =
        Sets.cartesianProduct(actionDoesNotOccurFormulas, actionDoesNotOccurFormulas);
    var atLeastOneInPairDoesNotOccurFormulas =
        from(actionDoesNotOccurFormulaPairs)
            .filter(pair -> !pair.get(0).equals(pair.get(1)))
            .transform(bFmgr::or);
    return bFmgr.and(atLeastOneInPairDoesNotOccurFormulas.toSet());
  }
}
