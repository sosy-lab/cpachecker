// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;


import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TALocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables.TAVariables;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TAInvariants extends TAEncodingExtensionBase {
  private final TAVariables time;
  private final TALocations locations;
  private final TimedAutomatonView automata;
  private final boolean useLocalInvariant;

  public TAInvariants(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TAVariables pTime,
      TALocations pLocations,
      boolean pUseLocalInvariant) {
    super(pFmgr);
    time = pTime;
    locations = pLocations;
    automata = pAutomata;
    useLocalInvariant = pUseLocalInvariant;
  }

  @Override
  public BooleanFormula makeAutomatonStep(TaDeclaration pAutomaton, int pLastReachedIndex) {
    if (!useLocalInvariant) {
      // all invariants hold after every transition
      return makeAutomatonInvariantFormula(pAutomaton, pLastReachedIndex + 1);
    }

    return bFmgr.makeTrue();
  }

  @Override
  public BooleanFormula makeInitialFormula(TaDeclaration pAutomaton, int pInitialIndex) {
    return makeAutomatonInvariantFormula(pAutomaton, pInitialIndex);
  }

  @Override
  public BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    if (useLocalInvariant) {
      return makeEdgeInvariantFormula(pAutomaton, pLastReachedIndex, pEdge);
    }

    return bFmgr.makeTrue();
  }

  private BooleanFormula makeEdgeInvariantFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    if (!(pEdge.getSuccessor() instanceof TCFANode)) {
      return bFmgr.makeTrue();
    }

    var successor = (TCFANode) pEdge.getSuccessor();
    var targetInvariant = successor.getInvariant();
    var invariantFormula =
        targetInvariant
            .transform(
                invariant ->
                    time.makeConditionFormula(pAutomaton, pLastReachedIndex + 1, invariant))
            .or(bFmgr.makeTrue());
    return invariantFormula;
  }

  @Override
  public BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    if (useLocalInvariant) {
      return makeAutomatonInvariantFormula(pAutomaton, pLastReachedIndex + 1);
    }

    return bFmgr.makeTrue();
  }

  private BooleanFormula makeAutomatonInvariantFormula(
      TaDeclaration pAutomaton, int pVariableIndex) {
    var invariantFormulas =
        from(automata.getNodesByAutomaton(pAutomaton))
            .filter(node -> node.getInvariant().isPresent())
            .transform(node -> makeLocationInvariantFormula(pAutomaton, pVariableIndex, node));

    return bFmgr.and(invariantFormulas.toSet());
  }

  private BooleanFormula makeLocationInvariantFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TCFANode node) {
    var invariant = node.getInvariant().get();
    var locationIsNode = locations.makeLocationEqualsFormula(pAutomaton, pVariableIndex, node);
    var invariantHolds = time.makeConditionFormula(pAutomaton, pVariableIndex, invariant);
    return bFmgr.implication(locationIsNode, invariantHolds);
  }
}
