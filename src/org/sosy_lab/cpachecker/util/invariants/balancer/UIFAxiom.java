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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.Set;

import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.templates.Purification;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConjunction;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConstraint;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateSumList;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTerm;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateUIF;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariableManager;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;

public class UIFAxiom {

  private TemplateConjunction antecedent;
  private TemplateConstraint consequent;

  public UIFAxiom(TemplateUIF F1, TemplateUIF F2) {
    construct(F1, F2);
  }

  public UIFAxiom(TemplateUIF U1, TemplateUIF U2, TemplateTerm T1, TemplateTerm T2) {
    TemplateSumList L1 = U1.getArgs();
    TemplateSumList L2 = U2.getArgs();
    antecedent = new TemplateConjunction(L1, L2);
    consequent = new TemplateConstraint(T1, InfixReln.EQUAL, T2);
  }

  /**
   * Construct the UIF axiom for those UIFs that were assigned
   * the indices i and j in the Purification pur.
   */
  public UIFAxiom(Purification pur, int i, int j) {
    TemplateUIF F1 = pur.getUIFByIndex(i);
    TemplateUIF F2 = pur.getUIFByIndex(j);
    construct(F1, F2);
  }

  private void construct(TemplateUIF F1, TemplateUIF F2) {
    F1 = F1.copy();
    F2 = F2.copy();
    TemplateSumList A1 = F1.getArgs();
    TemplateTerm    U1 = F1.getPurifiedName();
    TemplateSumList A2 = F2.getArgs();
    TemplateTerm    U2 = F2.getPurifiedName();
    antecedent = new TemplateConjunction(A1, A2);
    consequent = new TemplateConstraint(U1, InfixReln.EQUAL, U2);
  }

  public TemplateConjunction getAntecedent() {
    return antecedent;
  }

  public TemplateConstraint getConsequent() {
    return consequent;
  }

  public Set<String> getAllParameters(VariableWriteMode vwm) {
    return antecedent.getAllParameters(vwm);
  }

  public TemplateVariableManager getVariableManager() {
    TemplateVariableManager vmgr = antecedent.getVariableManager();
    vmgr.merge(consequent.getVariableManager());
    return vmgr;
  }

  @Override
  public String toString() {
    String s = "";
    s += antecedent.toString();
    s += " --> ";
    s += consequent.toString();
    return s;
  }

}
