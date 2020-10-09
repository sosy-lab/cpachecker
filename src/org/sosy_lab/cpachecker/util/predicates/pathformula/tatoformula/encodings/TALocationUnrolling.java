// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.encodings;

import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAEncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TALocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables.TAVariables;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TALocationUnrolling extends TAEncodingBase {
  public TALocationUnrolling(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TAVariables pTime,
      TALocations pLocations,
      TAEncodingExtension pExtensions) {
    super(pFmgr, pAutomata, pTime, pLocations, pExtensions);
  }

  @Override
  protected BooleanFormula makeAutomatonTransitionsFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex) {
    var delayTransitionConstraint = makeDelayTransition(pAutomaton, pLastReachedIndex);
    var idleTransitionConstraint = makeIdleTransition(pAutomaton, pLastReachedIndex);

    var locationStepFormulas =
        from(automata.getAllNodes())
            .transform(
                node ->
                    makeLocationStepFormula(
                        pAutomaton,
                        pLastReachedIndex,
                        node,
                        delayTransitionConstraint,
                        idleTransitionConstraint));

    return bFmgr.and(locationStepFormulas.toSet());
  }

  private BooleanFormula makeLocationStepFormula(
      TaDeclaration pAutomaton,
      int pLastReachedIndex,
      TCFANode pNode,
      BooleanFormula delayTransitionConstraint,
      BooleanFormula idleTransitionConstraint) {
    var locationEqualsNode =
        locations.makeLocationEqualsFormula(pAutomaton, pLastReachedIndex, pNode);

    var discreteTransitionConstraints =
        from(automata.getEdgesByPredecessor(pNode))
            .transform(edge -> makeDiscreteStep(pAutomaton, pLastReachedIndex, edge));
    var discreteTransitionsConstraint = bFmgr.or(discreteTransitionConstraints.toSet());

    var transitionConstraint =
        bFmgr.or(
            delayTransitionConstraint, idleTransitionConstraint, discreteTransitionsConstraint);
    var locationStepFormula = bFmgr.implication(locationEqualsNode, transitionConstraint);

    return locationStepFormula;
  }
}
