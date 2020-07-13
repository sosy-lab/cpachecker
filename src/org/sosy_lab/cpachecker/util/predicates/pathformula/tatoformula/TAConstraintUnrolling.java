// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.EncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.LocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.TimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TAConstraintUnrolling extends AutomatonEncoding {
  public TAConstraintUnrolling(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TimeEncoding pTime,
      LocationEncoding pLocations,
      Iterable<EncodingExtension> pExtensions) {
    super(pFmgr, pAutomata, pTime, pLocations, pExtensions);
  }

  @Override
  protected BooleanFormula makeAutomatonTransitionsFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex) {
    var delayFormula = makeDelayTransition(pAutomaton, pLastReachedIndex);
    var idleFormula = makeIdleTransition(pAutomaton, pLastReachedIndex);

    var discreteSteps =
        from(automata.getEdgesByAutomaton(pAutomaton))
            .transform(edge -> makeDiscreteStep(pAutomaton, pLastReachedIndex, edge));

    return bFmgr.or(delayFormula, idleFormula, bFmgr.or(discreteSteps.toSet()));
  }
}
