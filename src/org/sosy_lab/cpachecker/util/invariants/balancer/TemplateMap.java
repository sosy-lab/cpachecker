/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.invariants.redlog.Rational;
import org.sosy_lab.cpachecker.util.invariants.templates.Purification;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateVariable;

public class TemplateMap {

  private HashMap<Location,TemplateFormula> map;

  public TemplateMap() {
    map = new HashMap<Location,TemplateFormula>();
  }

  public void put(Location L, TemplateFormula T) {
  	map.put(L, T);
  }

  public TemplateFormula get(Location L) {
  	return map.get(L);
  }

  /**
   * @param N a CFANode.
   * @return the TemplateFormula for the Location whose CFANode is N.
   */
  public TemplateFormula get(CFANode N) {
  	TemplateFormula T = null;
  	for (Location L : map.keySet()) {
  		if (L.getNode() == N) {
  			T = map.get(L);
  			break;
  		}
  	}
  	return T;
  }

  public TemplateFormula getTemplate(Location L) {
    TemplateFormula T = null;
    if (map.containsKey(L)) {
      T = map.get(L);
    }
    return T;
  }

  public Set<TemplateVariable> getAllVariables() {
    Set<TemplateVariable> vars = new HashSet<TemplateVariable>();
    Collection<TemplateFormula> range = map.values();
    for (TemplateFormula T : range) {
      vars.addAll(T.getAllVariables());
    }
    return vars;
  }

  public Purification purify(Purification pur) {
    Collection<TemplateFormula> range = map.values();
    for (TemplateFormula T : range) {
      pur = T.purify(pur);
    }
    return pur;
  }

  // TODO: prune.
  /**
  public boolean alias(String prefix, List<String> vars) {
    boolean ans = true;
    Collection<TemplateFormula> range = map.values();
    for (TemplateFormula T : range) {
      ans &= T.alias(prefix, vars);
    }
    return ans;
  }
  */

  public void unalias() {
    Collection<TemplateFormula> range = map.values();
    for (TemplateFormula T : range) {
      T.unalias();
    }
  }

  public boolean evaluate(HashMap<String,Rational> vals) {
    boolean ans = true;
    Iterator<TemplateFormula> I = map.values().iterator();
    TemplateFormula T;
    while (I.hasNext()) {
      T = I.next();
      ans &= T.evaluate(vals);
    }
    return ans;
  }

}
