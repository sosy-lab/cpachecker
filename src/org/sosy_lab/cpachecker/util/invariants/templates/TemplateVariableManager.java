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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.interfaces.GeneralVariable;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

public class TemplateVariableManager implements VariableManager {

  private List<TemplateVariable> vars;

  public TemplateVariableManager() {
    vars = new Vector<>();
  }

  public TemplateVariableManager(Collection<TemplateVariable> vars) {
    this.vars = new Vector<>(vars);
  }

  public TemplateVariableManager(FormulaType<?> type,  int n, int m) {
    // Create vars v1, v2, ..., vn, u1, u2, ..., um
    vars = new Vector<>();
    for (int i = 1; i <= n; i++) {
      vars.add(new TemplateVariable(type, "v", i));
    }
    for (int j = 1; j <= m; j++) {
      vars.add(new TemplateVariable(type, "u", j));
    }
  }

  public void merge(TemplateVariableManager tvm) {
    vars.addAll(tvm.getVars());
  }

  public List<TemplateVariable> getVars() {
    return vars;
  }

  /**
   * @return an iterator over the variables
   */
  @Override
  public Iterator<GeneralVariable> iterator() {
    Vector<GeneralVariable> gvars = new Vector<>();
    GeneralVariable GV;
    for (int i = 0; i < vars.size(); i++) {
      GV = vars.get(i);
      gvars.add(GV);
    }
    return gvars.iterator();
  }

  /**
   * @param v the Variable to find
   * @return -1 if the variable is not found, else its index
   */
  @Override
  public int find(GeneralVariable v) {
    return vars.indexOf(v);
  }

  /**
   * @return the number of variables
   */
  @Override
  public int getNumVars() {
    return vars.size();
  }

  @Override
  public String toString() {
  String s = "Variables:";
  Iterator<TemplateVariable> I = vars.iterator();
  TemplateVariable V;
  while (I.hasNext()) {
    V = I.next();
    s += " "+V.toString();
  }
  return s;
  }

}















