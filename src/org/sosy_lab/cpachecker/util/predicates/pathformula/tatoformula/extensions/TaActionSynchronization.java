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
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.ActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TaActionSynchronization extends EncodingExtensionBase {
  private final boolean actionDetachedDelay;
  private final boolean actionDetachedIdle;
  private final boolean noTwoActions;

  private final ActionEncoding actions;
  private final TimedAutomatonView automata;

  public TaActionSynchronization(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      ActionEncoding pActions,
      boolean pActionDetachedDelay,
      boolean pActionDetachedIdle,
      boolean pNoTwoActions) {
    super(pFmgr);
    actions = pActions;
    automata = pAutomata;
    actionDetachedDelay = pActionDetachedDelay;
    actionDetachedIdle = pActionDetachedIdle;
    noTwoActions = pNoTwoActions;
  }

  @Override
  public BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    var action = automata.getActionOrDummy(pEdge);
    return actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action);
  }

  @Override
  public BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();
    if (automata.getDelayAction(pAutomaton).isPresent()) {
      result =
          bFmgr.and(
              actions.makeActionEqualsFormula(
                  pAutomaton, pLastReachedIndex, automata.getDelayAction(pAutomaton).get()),
              result);
    }

    if (actionDetachedDelay) {
      var notActionOccurs =
          from(automata.getActionsByAutomaton(pAutomaton))
              .transform(
                  action ->
                      bFmgr.not(
                          actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action)));
      result = bFmgr.and(bFmgr.and(notActionOccurs.toSet()), result);
    }

    return result;
  }

  @Override
  public BooleanFormula makeIdleTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();

    if (automata.getIdleAction(pAutomaton).isPresent()) {
      result =
          bFmgr.and(
              actions.makeActionEqualsFormula(
                  pAutomaton, pLastReachedIndex, automata.getIdleAction(pAutomaton).get()),
              result);
    }

    if (actionDetachedIdle) {
      var notActionOccurs =
          from(automata.getActionsByAutomaton(pAutomaton))
              .transform(
                  action ->
                      bFmgr.not(
                          actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action)));
      result = bFmgr.and(bFmgr.and(notActionOccurs.toSet()), result);
    }

    return result;
  }

  @Override
  public BooleanFormula makeStepFormula(int pLastReachedIndex) {
    if (noTwoActions) {
      var actionOccurences =
          from(automata.getAllActions())
              .transform(
                  action ->
                      bFmgr.not(actions.makeActionOccursInStepFormula(pLastReachedIndex, action)))
              .toSet();

      var actionPairs =
          from(Sets.cartesianProduct(actionOccurences, actionOccurences))
              .filter(pair -> !pair.get(0).equals(pair.get(1)))
              .transform(pair -> bFmgr.or(pair.get(0), pair.get(1)));
      return bFmgr.and(actionPairs.toSet());
    }

    return bFmgr.makeTrue();
  }
}
