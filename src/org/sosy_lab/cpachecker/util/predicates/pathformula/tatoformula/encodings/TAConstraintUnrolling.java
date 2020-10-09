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
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAEncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TALocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables.TAVariables;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TAConstraintUnrolling extends TAEncodingBase {
  public TAConstraintUnrolling(
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
    var delayFormula = makeDelayTransition(pAutomaton, pLastReachedIndex);
    var idleFormula = makeIdleTransition(pAutomaton, pLastReachedIndex);

    var discreteSteps =
        from(automata.getEdgesByAutomaton(pAutomaton))
            .transform(edge -> makeDiscreteStep(pAutomaton, pLastReachedIndex, edge));

    return bFmgr.or(delayFormula, idleFormula, bFmgr.or(discreteSteps.toSet()));
  }
}
