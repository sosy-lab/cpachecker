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
package org.sosy_lab.cpachecker.cpa.constraints;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.ForwardingMap;

/**
 * Map for storing variable names and their corresponding formulas.
 */
public class VariableMap extends ForwardingMap<String, Formula> {

  private Map<String, Formula> variableMap;

  public VariableMap(Map<String, Formula> pVariableMap) {
    variableMap = new HashMap<>();

    for (Map.Entry<String, Formula> entry : pVariableMap.entrySet()) {
      String variableName = entry.getKey();
      // variable name without SSA index
      String normalizedVariableName = FormulaManagerView.parseName(variableName).getFirst();

      variableMap.put(normalizedVariableName, entry.getValue());
    }
  }

  @Override
  protected Map<String, Formula> delegate() {
    return variableMap;
  }

  public Formula get(String pVariableName) {
    checkNotNull(pVariableName);

    return checkNotNull(super.get(pVariableName));
  }
}
