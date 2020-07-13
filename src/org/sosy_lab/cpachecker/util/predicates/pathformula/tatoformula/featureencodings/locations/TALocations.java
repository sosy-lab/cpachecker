// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface TALocations {
  BooleanFormula makeLocationEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TCFANode pNode);

  BooleanFormula makeDoesNotChangeFormula(TaDeclaration pAutomaton, int pIndexBefore);
}
