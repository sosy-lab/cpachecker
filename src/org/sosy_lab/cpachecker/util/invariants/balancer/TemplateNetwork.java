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

import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateBoolean;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;

public class TemplateNetwork {

  private AssumptionSet currentAssumptions = new AssumptionSet();
  private Vector<Transition> trans;
  private TemplateMap tmap;

  public TemplateNetwork(TemplateMap tmap, Transition... trans) {
    Vector<Transition> tvect = new Vector<>();
    for (int i = 0; i < trans.length; i++) {
      tvect.add(trans[i]);
    }
    build(tmap, tvect);
  }

  public TemplateNetwork(TemplateMap tmap, Vector<Transition> trans) {
    build(tmap, trans);
  }

  private void build(TemplateMap tmap, Vector<Transition> trans) {
    this.trans = trans;
    this.tmap = tmap;
  }

  public void setAssumptions(AssumptionSet aset) {
    currentAssumptions = aset;
  }

  public AssumptionSet getAssumptions() {
    return currentAssumptions;
  }

  public Vector<Transition> getTransitions() {
    return trans;
  }

  public TemplateMap getTemplateMap() {
    return tmap;
  }

  public Template getTemplate(CFANode n) {
    return tmap.getTemplate(n);
  }

  public boolean evaluate(Map<String, Rational> vals) {
    boolean ans = tmap.evaluate(vals);
    return ans;
  }

  public String dumpTemplates() {
    return tmap.dumpTemplates();
  }

  public Vector<TemplateBoolean> getAllNonzeroParameterClauses() {
    return tmap.getAllNonzeroParameterClauses();
  }

  public Set<String> writeAllParameters(VariableWriteMode vwm) {
    return tmap.writeAllParameters(vwm);
  }

}
