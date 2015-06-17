/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.ci.translators;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;


public class PredicateRequirementsTranslator extends AbstractRequirementsTranslator<PredicateAbstractState>{

  private final FormulaManagerView fmgr;
  private int counter;

  public PredicateRequirementsTranslator() {
    super(PredicateAbstractState.class);
    fmgr = GlobalInfo.getInstance().getFormulaManagerView();
    counter = 0;
  }

  @Override
  protected Pair<List<String>, String> convertToFormula(final PredicateAbstractState pRequirement,
      final SSAMap pIndices) throws CPAException {

    if (!pRequirement.isAbstractionState()) {
      throw new CPAException("The PredicateAbstractState " + pRequirement + " is not an abstractionState.");
    }

    BooleanFormula formulaBool = pRequirement.getAbstractionFormula().asFormula();
    fmgr.instantiate(formulaBool, pIndices);

    Pair<String, List<String>> pair = PredicatePersistenceUtils.splitFormula(fmgr, formulaBool);
    List<String> list = pair.getSecond();
    List<String> removeFromList = new ArrayList<>();
    for (String stmt : list) {
      if (!stmt.startsWith("(declare") || !stmt.startsWith("(define")) {
        removeFromList.add(stmt);
      }
    }
    list.removeAll(removeFromList);

    String secReturn;
    String element = pair.getFirst();
    // element =(assert ...)
    element = element.substring(element.indexOf('t') + 1, element.length() - 1);
    secReturn = "(define-fun .defci" + (counter++) + " Bool() " + element + ")";


    return Pair.of(list, secReturn);
  }


}
