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

import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.QuantifiedFormulaManager;


public class QuantifiedFormulaManagerView
  extends BaseManagerView<BooleanFormula, BooleanFormula>
  implements QuantifiedFormulaManager {

  private final QuantifiedFormulaManager manager;

  public QuantifiedFormulaManagerView(FormulaManagerView pViewManager,
      QuantifiedFormulaManager pManager) {
    super(pViewManager);
    this.manager = pManager;
  }

  @Override
  public BooleanFormula exists(List<Formula> pVariables, BooleanFormula pBody) {
    return wrapInView(manager.exists(pVariables, extractFromView(pBody)));
  }

  @Override
  public BooleanFormula forall(List<Formula> pVariables, BooleanFormula pBody) {
    return wrapInView(manager.forall(pVariables, extractFromView(pBody)));
  }

  @Override
  public BooleanFormula eliminateQuantifiers(BooleanFormula pF) throws InterruptedException, SolverException {
    return wrapInView(manager.eliminateQuantifiers(extractFromView(pF)));
  }

}
