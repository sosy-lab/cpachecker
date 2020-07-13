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
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.LocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.TimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TaInvariants extends EncodingExtensionBase {
  private final TimeEncoding time;
  private final LocationEncoding locations;
  private final TimedAutomatonView automata;

  // Placeholder for config
  private static enum InvariantType {
    LOCAL, // invariant formulas at discrete and delay steps only (i.e. when clocks actually change
           // their value)
    GLOBAL // add one big invariant formula to each transition
  }

  private InvariantType invariantType = InvariantType.LOCAL;

  public TaInvariants(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TimeEncoding pTime,
      LocationEncoding pLocations) {
    super(pFmgr);
    time = pTime;
    locations = pLocations;
    automata = pAutomata;
  }

  @Override
  public BooleanFormula makeAutomatonStep(TaDeclaration pAutomaton, int pLastReachedIndex) {
    if (invariantType == InvariantType.GLOBAL) {
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
    var invariantFormula = bFmgr.makeTrue();
    if (invariantType == InvariantType.LOCAL) {
      Optional<TaVariableCondition> targetInvariant = Optional.absent();
      if (pEdge.getSuccessor() instanceof TCFANode) {
        targetInvariant = ((TCFANode) pEdge.getSuccessor()).getInvariant();
      }
      invariantFormula =
          targetInvariant
              .transform(
                  invariant ->
                      time.makeConditionFormula(pAutomaton, pLastReachedIndex + 1, invariant))
              .or(bFmgr.makeTrue());
    }

    return invariantFormula;
  }

  @Override
  public BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    if (invariantType == InvariantType.LOCAL) {
      return makeAutomatonInvariantFormula(pAutomaton, pLastReachedIndex + 1);
    }

    return bFmgr.makeTrue();
  }

  private BooleanFormula makeAutomatonInvariantFormula(
      TaDeclaration pAutomaton, int pVariableIndex) {
    var invariantFormulas =
        from(automata.getNodesByAutomaton(pAutomaton))
            .filter(node -> node.getInvariant().isPresent())
            .transform(
                node -> {
                  var locationFormula =
                      locations.makeLocationEqualsFormula(pAutomaton, pVariableIndex, node);
                  var invariantFormula =
                      time.makeConditionFormula(
                          pAutomaton, pVariableIndex, node.getInvariant().get());
                  return bFmgr.implication(locationFormula, invariantFormula);
                });

    return bFmgr.and(invariantFormulas.toSet());
  }
}
