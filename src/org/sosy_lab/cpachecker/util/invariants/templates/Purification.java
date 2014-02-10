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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

public class Purification {

  private final String prefix;
  private AtomicInteger nextUsableIndex = new AtomicInteger(0);

  // map from UIF signature to fresh variable:
  private HashMap<String, TemplateTerm> defs = new HashMap<>();

  // map from fresh variable subscript to a UIF that the fresh variable stands for:
  private HashMap<Integer, TemplateUIF> UIFByIndex = new HashMap<>();

  // map from fresh variable string to a UIF that the fresh variable stands for:
  private HashMap<String, TemplateUIF> UIFByVarString = new HashMap<>();

  // map from fresh variable string to TemplateTerm representing it:
  private HashMap<String, TemplateTerm> TTByVarString = new HashMap<>();

  // map from function name to the set of indices of fresh variables for UIFs with this function
  private HashMap<String, Set<String>> varsByFunctionName = new HashMap<>();

  public Purification() {
    prefix = "u";
  }

  public Purification(String prefix) {
    this.prefix = prefix;
  }

  public int size() {
    return defs.size();
  }

  public Collection<TemplateTerm> getAllFreshVariables() {
    return defs.values();
  }

  public Collection<String> getFunctionNames() {
    return varsByFunctionName.keySet();
  }

  public Set<String> getVarsForFunction(String func_name) {
    Set<String> vars = new HashSet<>();
    if (varsByFunctionName.containsKey(func_name)) {
      vars = varsByFunctionName.get(func_name);
    }
    return vars;
  }

  /**
   * Returns one of the TemplateUIF objects that has been assigned
   * the index n, or null if there is none.
   */
  public TemplateUIF getUIFByIndex(int n) {
    Integer N = Integer.valueOf(n);
    TemplateUIF F = null;
    if (UIFByIndex.containsKey(N)) {
      F = UIFByIndex.get(N);
    }
    return F;
  }

  public TemplateUIF getUIFByVarString(String ui) {
    TemplateUIF F = null;
    if (UIFByVarString.containsKey(ui)) {
      F = UIFByVarString.get(ui);
    }
    return F;
  }

  public TemplateTerm getTTByVarString(String ui) {
    TemplateTerm T = null;
    if (TTByVarString.containsKey(ui)) {
      T = TTByVarString.get(ui);
    }
    return T;
  }

  /**
   * Gets the purename for the passed UIF F, sets this value within F,
   * and adds a value to UIFByIndex, so that F, or another TemplateUIF
   * object that is equivalent to it (in the sense of having the same
   * signature), can be looked up by index.
   */
  public void purify(TemplateUIF F) {
    String signature = F.toString(VariableWriteMode.PLAIN);
    TemplateTerm A = getPurename(F.getFormulaType(), signature);
    F.setPurifiedName(A);
    Integer I = A.getVariableIndex();
    UIFByIndex.put(I, F);
    String ui = A.toString(VariableWriteMode.PLAIN);
    partitionByName(ui, F);
    UIFByVarString.put(ui, F);
    TTByVarString.put(ui, A);
  }

  /**
   * Maintains sets of variables corresponding to function names, in
   * the varsByFunctionName map.
   */
  private void partitionByName(String ui, TemplateUIF F) {
    String name = F.getName();
    if (varsByFunctionName.containsKey(name)) {
      varsByFunctionName.get(name).add(ui);
    } else {
      Set<String> S = new HashSet<>();
      S.add(ui);
      varsByFunctionName.put(name, S);
    }
  }

  /**
   * Checks whether signature is already present, and if so returns the fresh
   * variable it has been assigned; if not, assigns one and returns that.
   */
  private TemplateTerm getPurename(FormulaType<?> type, String signature) {
    TemplateTerm T = null;
    if (defs.containsKey(signature)) {
      T = defs.get(signature);
    } else {
      T = nextAlias(type);
      defs.put(signature, T);
    }
    return T;
  }

  /**
   * Creates the next fresh variable (in the form of a TemplateTerm).
   */
  private TemplateTerm nextAlias(FormulaType<?> type) {
    int n = nextUsableIndex.incrementAndGet();
    TemplateVariable V = new TemplateVariable(type, prefix, n);
    TemplateTerm T = new TemplateTerm(type);
    T.setVariable(V);
    return T;
  }

  @Override
  public String toString() {
    String s = "";
    String t;
    Set<String> signatures = defs.keySet();
    for (String sig : signatures) {
      t = defs.get(sig).toString();
      s += t+" = "+sig+"\n";
    }
    return s;
  }

}
