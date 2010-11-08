/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.mathsat;

import java.util.Collection;

import org.sosy_lab.cpachecker.util.symbpredabstraction.Model;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver.AllSatResult;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.TheoremProver;

public class MathsatTheoremProver implements TheoremProver {
  
  private final org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatTheoremProver mInternalSFM;

  public MathsatTheoremProver(MathsatSymbolicFormulaManager pMgr) {
    mInternalSFM = new org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatTheoremProver(pMgr.mInternalSFM);
  }

  @Override
  public boolean isUnsat(SymbolicFormula f) {
    return mInternalSFM.isUnsat(f);
  }

  @Override
  public void pop() {
    mInternalSFM.pop();
  }

  @Override
  public void push(SymbolicFormula f) {
    mInternalSFM.push(f);
  }

  @Override
  public void init() {
    mInternalSFM.init();
  }
  
  @Override
  public void reset() {
    mInternalSFM.reset();
  }
  
  @Override
  public AllSatResult allSat(SymbolicFormula f, Collection<SymbolicFormula> important, 
                             FormulaManager fmgr, AbstractFormulaManager amgr) {
    return mInternalSFM.allSat(f, important, fmgr, amgr);
  }

  @Override
  public Model getModel() {
    return mInternalSFM.getModel();
  }
}
