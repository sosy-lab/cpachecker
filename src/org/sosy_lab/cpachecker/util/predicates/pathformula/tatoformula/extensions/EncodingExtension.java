// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface EncodingExtension {
  BooleanFormula makeAutomatonStep(TaDeclaration pAutomaton, int pLastReachedIndex);

  BooleanFormula makeInitialFormula(TaDeclaration pAutomaton, int pInitialIndex);

  BooleanFormula makeFinalConditionForAutomaton(TaDeclaration pAutomaton, int pHighestReachedIndex);

  BooleanFormula makeDiscreteStep(TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge);

  BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex);

  BooleanFormula makeIdleTransition(TaDeclaration pAutomaton, int pLastReachedIndex);
}
