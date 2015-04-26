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

import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;


public abstract class CartesianRequirementsTranslator<T extends AbstractState> extends AbstractRequirementsTranslator<T> {

  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;
  protected final FormulaManagerView fmgr;

  public CartesianRequirementsTranslator(final Class<T> pAbstractStateClass, final Configuration config,
      final ShutdownNotifier shutdownNotifier, final LogManager log) {
    super(pAbstractStateClass);
    this.config = config;
    this.shutdownNotifier = shutdownNotifier;
    logger = log;
    fmgr = GlobalInfo.getInstance().getFormulaManagerView();
  }

  private List<String> writeVarDefinition(List<String> vars, SSAMap ssaMap) {
    // TODO
    return null;
  }

  protected abstract List<String> getVarsInRequirements(T requirement);

  @Override
  protected Pair<List<String>, String> convertToFormula(T requirement, SSAMap indices) {
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

    secReturn = "(define_fun req Bool() " + secReturn + ")";
    return Pair.of(firstReturn, secReturn);
  }

  private String computeConjunction(List<String> list) {
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

  protected abstract List<String> getListOfIndependentRequirements(T requirement, SSAMap indices);

  public static String getVarWithIndex(String var, SSAMap indices) {
    if (indices.getIndex(var) == 0){
      return var;
    } else {
      return var + "@" + indices.getIndex(var);
    }
  }

}
