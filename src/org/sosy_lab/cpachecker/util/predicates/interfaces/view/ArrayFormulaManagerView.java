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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

public class ArrayFormulaManagerView
  extends BaseManagerView
  implements ArrayFormulaManager {

  private ArrayFormulaManager manager;

  ArrayFormulaManagerView(FormulaManagerView pViewManager, ArrayFormulaManager pManager) {
    super(pViewManager);
    this.manager = pManager;
  }

  @Override
  public <T extends Formula> ArrayFormula<T> select(ArrayFormula<T> pArray, Formula pIndex) {
    return manager.select(pArray, pIndex);
  }

  @Override
  public <T extends Formula> ArrayFormula<T> store(ArrayFormula<T> pArray, Formula pIndex, Formula pValue) {
    return manager.store(pArray, pIndex, pValue);
  }

  @Override
  public <T extends Formula> ArrayFormula<T> makeArray(String pName, FormulaType<T> pElementType) {
    return manager.makeArray(pName, pElementType);
  }



}
