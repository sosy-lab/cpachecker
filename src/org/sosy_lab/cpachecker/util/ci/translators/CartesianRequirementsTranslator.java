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
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;


public abstract class CartesianRequirementsTranslator<T extends AbstractState> extends AbstractRequirementsTranslator<T> {

  protected FormulaManagerView fmgr; // TODO: wird der wirklich benötigt?

  public CartesianRequirementsTranslator(final Class<T> pAbstractStateClass, final Configuration config,
      final ShutdownNotifier shutdownNotifier, final LogManager log) {
    super(pAbstractStateClass);
    fmgr = GlobalInfo.getInstance().getFormulaManagerView();
    if (fmgr==null) {
      // TODO: wirft Exception!
//      fmgr = Solver.create(config, log, shutdownNotifier).getFormulaManager();
    }
  }

  private List<String> writeVarDefinition(final List<String> vars, final SSAMap ssaMap) {
    List<String> list = new ArrayList<>();

    String def;
    for (String var : vars) {
      def = "(declare-fun " + getVarWithIndex(var, ssaMap);
      def += "() Int)";
      list.add(def);
    }
    return list;
  }

  protected abstract List<String> getVarsInRequirements(final T requirement);

  @Override
  protected Pair<List<String>, String> convertToFormula(final T requirement, final SSAMap indices) {
    List<String> firstReturn = writeVarDefinition(getVarsInRequirements(requirement), indices);

    String secReturn;
    List<String> listOfIndependentRequirements = getListOfIndependentRequirements(requirement, indices);
    if (listOfIndependentRequirements.isEmpty()) {
      secReturn = "true";
    } else if (listOfIndependentRequirements.size() == 1) {
      secReturn = listOfIndependentRequirements.get(0);
    } else {
      secReturn = computeConjunction(listOfIndependentRequirements);
    }

    secReturn = "(define-fun req () Bool " + secReturn + ")";
    return Pair.of(firstReturn, secReturn);
  }

  private String computeConjunction(final List<String> list) {
    StringBuilder sb = new StringBuilder();
    int BracketCounter = 0;

    String last = list.get(list.size()-1);
    for (String var : list) {
      if (var.equals(last)) {
        sb.append(var);
      } else {
        sb.append("(and ");
        sb.append(var);
        BracketCounter++;
      }
    }

    for (int i=0; i<BracketCounter; i++) {
      sb.append(")");
    }
    return sb.toString();
  }

  protected abstract List<String> getListOfIndependentRequirements(final T requirement, final SSAMap indices);

  public static String getVarWithIndex(final String var, final SSAMap indices) {
    assert (indices != null);
    assert (var != null);

    int index = indices.getIndex(var);

    if (index == 0){
      return var;
    } else {
      return var + "@" + index;
    }
  }

}
