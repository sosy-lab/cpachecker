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
package org.sosy_lab.cpachecker.util.invariants.redlog;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.util.invariants.Rational;

public class ParameterManager {

  private final EAPair EAP;
  private String[] params = {};
  private HashMap<String, ParameterAssignment> PAmap = null;

  public ParameterManager(EAPair EAP) {
    this.EAP = EAP;
    PAmap = new HashMap<>();
  }

  public ParameterManager(EAPair EAP, String[] params) {
    this.EAP = EAP;
    PAmap = new HashMap<>();
    this.params = params;
    makePAs();
  }

  public ParameterManager(EAPair EAP, Collection<String> pParams) {
    this.EAP = EAP;
    PAmap = new HashMap<>();
    this.params = new String[pParams.size()];
    int i = 0;
    for (String s : pParams) {
      this.params[i] = s;
      i++;
    }
    makePAs();
  }

  public void setParameters(String[] params) {
    // List the names of the parameters for which we need to
    // determine values.
    this.params = params;
  }

  public ParameterAssignment getParameterAssignment(String param) {
    // For a single parameter name, we return the
    // ParameterAssignment object that it got.
    return PAmap.get(param);
  }

  public HashMap<String, Rational> getRationalValueMap() {
    // Returns a mapping from parameter names to (constant)
    // rational values, where those parameters that did not get a
    // constant will map to null.
    HashMap<String, Rational> map = new HashMap<>();
    if (PAmap != null) {
      Rational R;
      String a;
      for (int i = 0; i < params.length; i++) {
        a = params[i];
        R = PAmap.get(a).getValue();
        map.put(a, R);
      }
    }
    return map;
  }

  public HashSet<String> getAllVars() {
    // Return a set of the names of all variables occurring in the
    // values of all parameters.
    HashSet<String> vars = new HashSet<>();
    Iterator<ParameterAssignment> PAs = PAmap.values().iterator();
    ParameterAssignment PA;
    HashSet<String> pavars;
    while (PAs.hasNext()) {
      PA = PAs.next();
      pavars = PA.getVars();
      vars.addAll(pavars);
    }
    return vars;
  }

  public boolean allAreConstant() {
    // Say whether all the parameters got a constant value.
    boolean ans = true;
    if (PAmap == null) {
      ans = false;
    }

    int n = params.length;
    int i = 0;
    String a;
    ParameterAssignment PA;
    while (ans==true && i < n) {
      a = params[i];
      PA = PAmap.get(a);
      ans &= (PA.hasValue());
      i += 1;
    }

    return ans;
  }

  public void makePAs() {
    // Go through all equations in EAP, looking for those that
    // name any of our parameters on the LHS, and construct
    // ParameterAssignment objects accordingly (even for those
    // parameters that have no equation).
    // Store the results in the private global field PAmap.

    // Initialize the set of parameters.
    HashSet<String> waitlist = new HashSet<>();
    for (int i = 0; i < params.length; i++) {
      waitlist.add(params[i]);
    }

    // Go through the equations in EAP.
    Iterator<Equation> eqnit = EAP.equationIterator();
    CExpression LHS, RHS;
    CIdExpression ID;
    Equation eqn;
    String a;
    ParameterAssignment PA;
    while (eqnit.hasNext()) {
      eqn = eqnit.next();
      LHS = eqn.getLeftHandSide();
      ID = (CIdExpression) LHS;
      a = ID.getName().toString();
      if (waitlist.contains(a)) {
        RHS = eqn.getRightHandSide();
        PA = new ParameterAssignment(a, RHS);
        PAmap.put(a, PA);
        waitlist.remove(a);
      }
    }

    // For any parameters that didn't have an equation, put a
    // dummy ParameterAssignment.
    // These will be of PAType NONE, but their Rational value will
    // not be null (it will be 0/1).
    Iterator<String> it = waitlist.iterator();
    String p;
    ParameterAssignment dummyPA;
    while (it.hasNext()) {
      p = it.next();
      dummyPA = new ParameterAssignment(p);
      PAmap.put(p, dummyPA);
    }
  }

}
