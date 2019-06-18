/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.util.predicates.smt;

import org.sosy_lab.java_smt.api.Formula;
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
  public Formula makeStar(Formula pF1, Formula pF2) {
    return slMgr.makeStar(unwrap(pF1), unwrap(pF2));
  }

  @Override
  public Formula makePointsTo(Formula pPtr, Formula pTo) {
    return slMgr.makePointsTo(unwrap(pPtr), unwrap(pTo));
  }

  @Override
  public Formula makeMagicWand(Formula pF1, Formula pF2) {
    return slMgr.makeMagicWand(pF1, pF2);
  }

  @Override
  public Formula makeEmptyHeap(Formula pFormula, Formula pFormula2) {
    return slMgr.makeEmptyHeap(pFormula, pFormula2);
  }

  @Override
  public Formula makeNilElement(Formula pT) {
    return slMgr.makeNilElement(pT);
  }

}
