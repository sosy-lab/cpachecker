// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings;


import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.LocalVarDiscreteFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class LocalVarLocationEncoding extends LocalVarDiscreteFeatureEncoding<TCFANode>
    implements LocationEncoding {

  public LocalVarLocationEncoding(FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    super(pFmgr, "location");

    pAutomata.getAllNodes().forEach(location -> addEntry(location));
  }

  @Override
  public BooleanFormula makeLocationEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TCFANode pNode) {
    return makeEqualsFormula(pAutomaton, pVariableIndex, pNode);
  }

  @Override
  public BooleanFormula makeDoesNotChangeFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    return makeUnchangedFormula(pAutomaton, pIndexBefore);
  }
}
