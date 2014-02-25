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

import java.util.Iterator;

public class EAPair {

  private Condition cond;
  private Solution soln;

  public EAPair() {}

  public EAPair(Condition c, Solution s) {
    cond = c; soln = s;
  }

  public void setCondition(Condition c) {
    cond = c;
  }

  public void setSolution(Solution s) {
    soln= s;
  }

  public Condition getCondition() {
    return cond;
  }

  public Solution getSolution() {
    return soln;
  }

  public Iterator<Equation> equationIterator() {
    return soln.iterator();
  }

  // TODO: Reimplement all this, after repairing TreeReader.
  /**
  public String makeVarChoiceFormula(String[] params) {
    // We write a formula which can be sent to redlog in order to
    // get it to choose particular values for the variables that
    // appear in the RHSs of the equations for the parameters.

    ParameterManager PM = new ParameterManager(this, params);

    String psi = "ex({";

    // We do three things at once:
    // 1. Build q, the list of variables to quantify.
    // 2. Count the infinity variables.
    // 3. Build p, the statements that the infinity vars are
    //    positive.
    Iterator<String> varsit = PM.getAllVars().iterator();
    String v;
    String q = "", p = ""; int n = 0;
    while (varsit.hasNext()) {
      v = varsit.next();
      q += ","+v;
      if (v.startsWith("infinity")) {
        p += " and "+v+" > 0";
        n += 1;
      }
    }
    psi += q.substring(1)+"},"+p.substring(5);

    // 4. Build b, the statements that the variables are bounded.
    String b = "";
    String ns = Integer.toString(n);
    varsit = PM.getAllVars().iterator();
    while (varsit.hasNext()) {
      v = varsit.next();
      b += " and "+v+" <= "+ns;
    }
    psi += b;

    /////////////////////////////////////////////////////////////////
    // Now must handle the denominators. Probably we need to write
    // a series of optional methods for this.

    // First alternative:
    // 5A. Say that the denominators are nonzero.

    //Iterator denomit = getAllDenominators().iterator();
    //String d = "";
    //CAstNode N;
    //while (denomit.hasNext()) {
    //  N = (CAstNode) denomit.next();
    //  d += " and "+N.getRawSignature()+" <> 0";
    //}
    //psi += d;


    // Second alternative:
    // 5B. Say that the denominators are equal to 1.
    Iterator<CAstNode> denomit = getAllDenominators().iterator();
    String d = "";
    CAstNode N;
    while (denomit.hasNext()) {
      N = denomit.next();
      d += " and "+N.getRawSignature()+" = 1";
    }
    psi += d;

    /////////////////////////////////////////////////////////////////

    psi += ")";
    return psi;
  }

  public Vector<CAstNode> getAllDenominators() {
    // Checks for syntactical identity between denominators and
    // only counts them once.
    HashSet<String> seen = new HashSet<>();
    Vector<CAstNode> denoms = new Vector<>();
    Vector<CAstNode> found;
    int n = soln.getNumEquations();
    Equation E;
    String s;
    Iterator<CAstNode> it;
    CAstNode N;
    for (int i = 0; i < n; i++) {
      E = soln.getEquation(i);
      s = E.getRawSignature();
      CExpressionAssignmentStatement EAS =
        (CExpressionAssignmentStatement) E.getTree();
      CExpression RHS = EAS.getRightHandSide();
      found = TreeReader.findDenominators(RHS);
      it = found.iterator();
      while (it.hasNext()) {
        N = it.next();
        s = N.getRawSignature();
        if (!seen.contains(s)) {
          denoms.add(N);
          seen.add(s);
        }
      }
    }
    return denoms;
  }
  */

}
