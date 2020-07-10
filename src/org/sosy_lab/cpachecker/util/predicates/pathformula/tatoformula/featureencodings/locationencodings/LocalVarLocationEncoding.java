// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.LocalVarDiscreteFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class LocalVarLocationEncoding extends LocalVarDiscreteFeatureEncoding<TCFANode>
    implements LocationEncoding {

  public LocalVarLocationEncoding(FormulaManagerView pFmgr, CFA pCfa) {
    super(pFmgr, "location");

    var allLocations = from(pCfa.getAllNodes()).filter(instanceOf(TCFANode.class));
    allLocations.forEach(location -> addEntry((TCFANode) location));
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
