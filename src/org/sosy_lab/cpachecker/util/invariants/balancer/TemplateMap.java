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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.invariants.Rational;
import org.sosy_lab.cpachecker.util.invariants.templates.Purification;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateBoolean;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariable;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;

public class TemplateMap {

  private HashMap<Location, Template> map;

  public TemplateMap() {
    map = new HashMap<>();
  }

  public void put(Location L, Template T) {
    map.put(L, T);
  }

  /**
   * @param N a CFANode.
   * @return the TemplateFormula for the Location whose CFANode is N.
   */
  public Template getTemplate(CFANode N) {
    Template T = null;
    for (Location L : map.keySet()) {
      if (L.getNode() == N) {
        T = map.get(L);
        break;
      }
    }
    return T;
  }

  public Template getTemplate(Location L) {
    Template T = null;
    if (map.containsKey(L)) {
      T = map.get(L);
    }
    return T;
  }

  public Set<TemplateVariable> getAllVariables() {
    Set<TemplateVariable> vars = new HashSet<>();
    Collection<Template> range = map.values();
    for (Template t : range) {
      vars.addAll(t.getAllVariables());
    }
    return vars;
  }

  public Purification purify(Purification pur) {
    Collection<Template> range = map.values();
    for (Template t : range) {
      pur = t.purify(pur);
    }
    return pur;
  }

  public boolean evaluate(Map<String, Rational> vals) {
    boolean ans = true;
    for (Template t : map.values()) {
      ans &= t.evaluate(vals);
    }
    return ans;
  }

  public String dumpTemplates() {
    String s = "";
    Template t;
    for (Location l : map.keySet()) {
      t = map.get(l);
      s += l.toString()+": "+t.toString()+"\n";
    }
    return s;
  }

  public Vector<TemplateBoolean> getAllNonzeroParameterClauses() {
    Vector<TemplateBoolean> clauses = new Vector<>();
    for (Template t : map.values()) {
      clauses.add(t.getNonzeroParameterClause());
    }
    return clauses;
  }

  public Set<String> writeAllParameters(VariableWriteMode vwm) {
    Set<String> params = new HashSet<>();
    for (Template t : map.values()) {
      params.addAll(t.writeAllParameters(vwm));
    }
    return params;
  }

}
