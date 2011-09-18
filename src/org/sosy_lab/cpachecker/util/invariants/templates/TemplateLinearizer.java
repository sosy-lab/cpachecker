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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Coeff;
import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.LinearInequality;
import org.sosy_lab.cpachecker.util.invariants.interfaces.Constraint;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;

public class TemplateLinearizer {

  /**
   * @param t: The Template to be linearized.
   * @param vmgr: A VariableManager containing the list of all variables
   *              that may appear in any of the templates to be put
   *              together into a single inequality representation.
   */
  public static LinearInequality linearize(TemplateFormula t, VariableManager vmgr) {

  	if (t.isTrue()) {
  		return booleanLineq(vmgr, true);
  	}

  	if (t.isFalse()) {
  		return booleanLineq(vmgr, false);
  	}

    LinearInequality lineq = new LinearInequality(vmgr);
    List<TemplateConstraint> constraints = t.getConstraints();

    Constraint cons;
    List<Coeff> coeffs;
    Coeff rhs;
    InfixReln reln;
    for (int i = 0; i < constraints.size(); i++) {
      cons = constraints.get(i);
      coeffs = cons.getNormalFormCoeffs(vmgr, VariableWriteMode.REDLOG);
      // diag:
      //System.out.println("Coeffs: "+Coeff.coeffsToString(coeffs));
      //
      rhs = cons.getNormalFormConstant(VariableWriteMode.REDLOG);
      reln = cons.getInfixReln();
      if (reln == InfixReln.LEQ) {
        // The infix relation is LEQ.
        lineq.addRow(coeffs, rhs);
      } else {
        // In this case it must be EQUAL.
        lineq.addRow(coeffs, rhs);
        coeffs = negative(coeffs);
        rhs = rhs.negative();
        lineq.addRow(coeffs, rhs);
      }
    }

    return lineq;
  }

  /**
   * Create the inequality 0 <= 1 for a true statement, 0 <= -1 for a false one.
   * @param vmgr the VariableManager, which says how many zero coeffs we need on the LHS
   * @param trueStatement a boolean which says if we want a true or a false statement
   * @return
   */
  private static LinearInequality booleanLineq(VariableManager vmgr, boolean trueStatement) {
  	LinearInequality lineq = new LinearInequality(vmgr);
  	int n = vmgr.getNumVars();
  	List<Coeff> coeffs = Collections.nCopies(n, new Coeff("0"));
  	Coeff rhs;
  	if (trueStatement) {
  		rhs = new Coeff("1");
  	} else {
  		rhs = new Coeff("-1");
  	}
  	lineq.addRow(coeffs, rhs);
  	return lineq;
  }

  private static List<Coeff> negative(List<Coeff> P) {
    Vector<Coeff> N = new Vector<Coeff>();
    Coeff C;
    for (int i = 0; i< P.size(); i++) {
      C = P.get(i);
      N.add( C.negative() );
    }
    return N;
  }

}