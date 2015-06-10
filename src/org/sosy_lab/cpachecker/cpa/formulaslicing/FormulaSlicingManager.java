/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Collection;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Optional;

public class FormulaSlicingManager implements IFormulaSlicingManager {
  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;

  public FormulaSlicingManager(PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr) {
    pfmgr = pPfmgr;
    fmgr = pFmgr;
  }

  @Override
  public FormulaSlicingState join(FormulaSlicingState oldState,
      FormulaSlicingState newState) throws CPAException, InterruptedException {
    return null;
  }

  @Override
  public Collection<? extends FormulaSlicingState> getAbstractSuccessors(
      FormulaSlicingState state, CFAEdge edge)
      throws CPAException, InterruptedException {
    return null;
  }

  @Override
  public Collection<? extends FormulaSlicingState> strengthen(
      FormulaSlicingState state, List<AbstractState> otherState,
      CFAEdge pCFAEdge) throws CPATransferException, InterruptedException {
    return null;
  }

  @Override
  public FormulaSlicingState getInitialState(CFANode node) {
    return null;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(FormulaSlicingState state,
      UnmodifiableReachedSet states, AbstractState pArgState)
      throws CPAException, InterruptedException {
    return null;
  }

  @Override
  public boolean isLessOrEqual(FormulaSlicingState pState1,
      FormulaSlicingState pState2) {
    return false;
  }
}
