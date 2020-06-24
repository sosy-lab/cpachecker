// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AlwaysTrueEncoding implements TAFormulaEncoding {
  private final FormulaManagerView fmgr;

  AlwaysTrueEncoding(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  @Override
  public BooleanFormula getInitialFormula(CFANode pInitialNode) {
    return fmgr.getBooleanFormulaManager().makeTrue();
  }

  @Override
  public Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula pPredecessor, int pStepCount, CFAEdge pEdge) {
    return Collections.singletonList(fmgr.getBooleanFormulaManager().makeTrue());
  }

  @Override
  public Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula pPredecessor, int pStepCount) {
    return Collections.singletonList(fmgr.getBooleanFormulaManager().makeTrue());
  }

  @Override
  public BooleanFormula getFormulaFromReachedSet(Iterable<AbstractState> pReachedSet) {
    return fmgr.getBooleanFormulaManager().makeTrue();
  }
}
