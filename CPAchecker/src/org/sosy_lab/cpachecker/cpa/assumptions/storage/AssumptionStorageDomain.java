/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

public class AssumptionStorageDomain implements AbstractDomain {

  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManagerView bfmgr;

  public AssumptionStorageDomain(
      FormulaManagerView pFormulaManager) {
    formulaManager = pFormulaManager;
    bfmgr = formulaManager.getBooleanFormulaManager();
  }

  @Override
  public AbstractState join(AbstractState pElement1, AbstractState pElement2) {

    AssumptionStorageState storageElement1= (AssumptionStorageState)pElement1;
    AssumptionStorageState storageElement2 = (AssumptionStorageState)pElement2;

    // create the disjunction of the stop formulas
    // however, if one of them is true, we would loose the information from the other
    // so handle these special cases separately
    BooleanFormula stopFormula1 = storageElement1.getStopFormula();
    BooleanFormula stopFormula2 = storageElement2.getStopFormula();
    BooleanFormula newStopFormula;
    if (bfmgr.isTrue(stopFormula1)) {
      newStopFormula = stopFormula2;
    } else if (bfmgr.isTrue(stopFormula2)) {
      newStopFormula = stopFormula1;
    } else {
      newStopFormula = bfmgr.or(stopFormula1, stopFormula2);
    }

    return new AssumptionStorageState(
        formulaManager,
        bfmgr.and(storageElement1.getAssumption(),
                               storageElement2.getAssumption()),
        newStopFormula);
  }

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) {
    throw new UnsupportedOperationException();
  }
}