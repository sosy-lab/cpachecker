// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TAActions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TATransitionActions extends TAEncodingExtensionBase {
  private final boolean actionDetachedDelay;
  private final boolean actionDetachedIdle;

  private final TAActions actions;
  private final TimedAutomatonView automata;

  public TATransitionActions(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TAActions pActions,
      boolean pActionDetachedDelay,
      boolean pActionDetachedIdle) {
    super(pFmgr);
    actions = pActions;
    automata = pAutomata;
    actionDetachedDelay = pActionDetachedDelay;
    actionDetachedIdle = pActionDetachedIdle;
  }

  @Override
  public BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    var action = automata.getActionOrDummy(pEdge);
    return actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action);
  }

  @Override
  public BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var delayAction = automata.getDelayAction(pAutomaton);
    var delayActionFormula = makeSpecialActionFormula(pAutomaton, pLastReachedIndex, delayAction);
    var actionDetachedDelayFormula =
        makeActionDetachedTransitionsFormula(pAutomaton, pLastReachedIndex, actionDetachedDelay);
    return bFmgr.and(delayActionFormula, actionDetachedDelayFormula);
  }

  @Override
  public BooleanFormula makeIdleTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var idleAction = automata.getIdleAction(pAutomaton);
    var idleActionFormula = makeSpecialActionFormula(pAutomaton, pLastReachedIndex, idleAction);
    var actionDetachedIdleFormula =
        makeActionDetachedTransitionsFormula(pAutomaton, pLastReachedIndex, actionDetachedIdle);
    return bFmgr.and(idleActionFormula, actionDetachedIdleFormula);
  }

  private BooleanFormula makeSpecialActionFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, Optional<TaVariable> specialAction) {
    var specialActionFormula =
        specialAction
            .transform(
                action -> actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action))
            .or(bFmgr.makeTrue());
    return specialActionFormula;
  }

  private BooleanFormula makeActionDetachedTransitionsFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, boolean active) {
    if (!active) {
      return bFmgr.makeTrue();
    }

    var actionDoesNotOccurFormulas =
        from(automata.getActionsByAutomaton(pAutomaton))
            .transform(
                action -> actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action))
            .transform(bFmgr::not);
    var actionsDoNotOccurFormula = bFmgr.and(actionDoesNotOccurFormulas.toSet());
    return actionsDoNotOccurFormula;
  }
}
