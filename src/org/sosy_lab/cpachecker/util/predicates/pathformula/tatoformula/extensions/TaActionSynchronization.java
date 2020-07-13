// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.ActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TaActionSynchronization extends EncodingExtensionBase {
  // CONFIG
  private boolean actionDetachedDelayTransition = false;
  private boolean actionDetachedIdleTransition = true;
  private boolean useDelayAction = true;
  private boolean useIdleAction = false;

  private boolean noTwoActions = true;

  private final ActionEncoding actions;
  private final TimedAutomatonView automata;

  public TaActionSynchronization(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata, ActionEncoding pActions) {
    super(pFmgr);
    actions = pActions;
    automata = pAutomata;
  }

  @Override
  public BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    return pEdge
        .getAction()
        .transform(action -> actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action))
        .or(actions.makeLocalDummyActionFormula(pAutomaton, pLastReachedIndex));
  }

  @Override
  public BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();
    if (useDelayAction) {
      result = bFmgr.and(actions.makeDelayActionFormula(pAutomaton, pLastReachedIndex), result);
    }

    if (actionDetachedDelayTransition) {
      if (useIdleAction) {
        result =
            bFmgr.and(
                bFmgr.not(actions.makeIdleActionFormula(pAutomaton, pLastReachedIndex)), result);
      }

      var notActionOccurs =
          from(automata.getActionsByAutomaton(pAutomaton))
              .transform(
                  action ->
                      bFmgr.not(
                          actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action)));
      var notDummyAction =
          bFmgr.not(actions.makeLocalDummyActionFormula(pAutomaton, pLastReachedIndex));
      result = bFmgr.and(bFmgr.and(notActionOccurs.toSet()), notDummyAction, result);
    }

    return result;
  }

  @Override
  public BooleanFormula makeIdleTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();

    if (useIdleAction) {
      result = bFmgr.and(actions.makeIdleActionFormula(pAutomaton, pLastReachedIndex), result);
    }

    if (actionDetachedIdleTransition) {
      if (useDelayAction) {
        result =
            bFmgr.and(
                bFmgr.not(actions.makeDelayActionFormula(pAutomaton, pLastReachedIndex)), result);
      }

      var notActionOccurs =
          from(automata.getActionsByAutomaton(pAutomaton))
              .transform(
                  action ->
                      bFmgr.not(
                          actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action)));
      var notDummyAction =
          bFmgr.not(actions.makeLocalDummyActionFormula(pAutomaton, pLastReachedIndex));
      result = bFmgr.and(bFmgr.and(notActionOccurs.toSet()), notDummyAction, result);
    }

    return result;
  }

  @Override
  public BooleanFormula makeStepFormula(int pLastReachedIndex) {
    if (noTwoActions) {
      var allActions =
          ImmutableSet.copyOf(
              actions.makeAllActionFormulas(pLastReachedIndex, useDelayAction, useIdleAction));
      var actionPairs =
          from(Sets.cartesianProduct(allActions, allActions))
              .filter(pair -> !pair.get(0).equals(pair.get(1)))
              .transform(pair -> bFmgr.and(pair.get(0), pair.get(1)));
      return bFmgr.not(bFmgr.or(actionPairs.toSet()));
    }

    return bFmgr.makeTrue();
  }
}
