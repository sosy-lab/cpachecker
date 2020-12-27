// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SLFormulaManager;

public class SLFormulaManagerView extends BaseManagerView implements SLFormulaManager {

  private final SLFormulaManager manager;

  SLFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler, SLFormulaManager pSlFormulaManager) {
    super(pWrappingHandler);
    manager = pSlFormulaManager;
  }

  @Override
  public BooleanFormula makeStar(BooleanFormula pF1, BooleanFormula pF2) {
    return manager.makeStar(pF1, pF2);
  }

  @Override
  public <AF extends Formula, VF extends Formula> BooleanFormula makePointsTo(AF pPtr, VF pTo) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public BooleanFormula makeMagicWand(BooleanFormula pF1, BooleanFormula pF2) {
    return manager.makeMagicWand(pF1, pF2);
  }

  @Override
  public <
          AF extends Formula,
          VF extends Formula,
          AT extends FormulaType<AF>,
          VT extends FormulaType<VF>>
      BooleanFormula makeEmptyHeap(AT pAdressType, VT pValueType) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public <AF extends Formula, AT extends FormulaType<AF>> AF makeNilElement(AT pAdressType) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("not yet implemented");
  }
}
