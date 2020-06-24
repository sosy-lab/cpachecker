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

  private final SLFormulaManager slMgr;

  public SLFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      SLFormulaManager pFormulaManager) {
    super(pWrappingHandler);
    slMgr = pFormulaManager;
  }

  @Override
  public BooleanFormula makeStar(BooleanFormula pF1, BooleanFormula pF2) {
    return slMgr.makeStar((BooleanFormula) unwrap(pF1), (BooleanFormula) unwrap(pF2));
  }

  @Override
  public <AF extends Formula, VF extends Formula> BooleanFormula makePointsTo(AF pPtr, VF pTo) {
    return slMgr.makePointsTo(unwrap(pPtr), unwrap(pTo));
  }

  @Override
  public BooleanFormula makeMagicWand(BooleanFormula pF1, BooleanFormula pF2) {
    return slMgr.makeMagicWand(pF1, pF2);
  }

  @Override
  public <AF extends Formula, VF extends Formula, AT extends FormulaType<AF>, VT extends FormulaType<VF>>
      BooleanFormula makeEmptyHeap(AT pAdressType, VT pValueType) {
    return slMgr.makeEmptyHeap(pAdressType, pValueType);
  }

  @Override
  public <AF extends Formula, AT extends FormulaType<AF>> AF makeNilElement(AT pAdressType) {
    return slMgr.makeNilElement(pAdressType);
  }
}
