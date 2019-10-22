/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.harness;

import java.util.LinkedList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.java_smt.api.Formula;

public class StringReplacer {
  String base;
  int counter;
  List<Formula> newNames;
  private PredicateAbstractState predicateState;
  private PathFormulaManager pathFormulaManager;

  public StringReplacer(
      String pBase,
      PathFormulaManager pPathFormulaManager,
      PredicateAbstractState pPredicateAbstractState) {
    newNames = new LinkedList<>();
    predicateState = pPredicateAbstractState;
    pathFormulaManager = pPathFormulaManager;
    base = pBase;
    counter = 0;
  }

  public List<Formula> getNewNames() {
    return newNames;
  }

  String renameField(String pName) {
    if (pName.equals(base)) {
      String result = base + counter;

      Formula functionFormula =
          pathFormulaManager.makeFormulaForUninstantiatedVariable(
              result,
              CPointerType.POINTER_TO_VOID,
              predicateState.getPathFormula().getPointerTargetSet(),
              false);

      newNames.add(functionFormula);
      counter++;
      return result;
    } else {
      return pName;
    }
  }
}
