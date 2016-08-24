/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.lasso_ranker.construction;

import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;
import org.sosy_lab.solver.visitors.TraversalProcess;

import java.util.Set;

class InOutVariablesCollector extends DefaultFormulaVisitor<TraversalProcess> {

  private final FormulaManagerView formulaManagerView;

  private final Set<Formula> inVariables = Sets.newLinkedHashSet();
  private final Set<Formula> outVariables = Sets.newLinkedHashSet();
  private final SSAMap outVariablesSsa;
  private final SSAMap inVariablesSsa;

  public InOutVariablesCollector(
      FormulaManagerView pFormulaManagerView, SSAMap pInVariablesSsa, SSAMap pOutVariablesSsa) {
    formulaManagerView = pFormulaManagerView;
    outVariablesSsa = pOutVariablesSsa;
    inVariablesSsa = pInVariablesSsa;
  }

  @Override
  protected TraversalProcess visitDefault(Formula pF) {
    return TraversalProcess.CONTINUE;
  }

  @Override
  public TraversalProcess visitFreeVariable(Formula pF, String pName) {
    if (!formulaManagerView.isIntermediate(pName, outVariablesSsa)) {
      outVariables.add(pF);
    }
    if (!formulaManagerView.isIntermediate(pName, inVariablesSsa)) {
      inVariables.add(pF);
    }

    return TraversalProcess.CONTINUE;
  }

  public Set<Formula> getInVariables() {
    return inVariables;
  }

  public Set<Formula> getOutVariables() {
    return outVariables;
  }
}
