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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Coeff;
import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.interfaces.Constraint;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConstraint;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;


public class IRFormulaMatriciser extends FormulaMatriciser {

  @Override
  public IRMatrix buildMatrix(TemplateFormula t, VariableManager vmgr, Map<String, Variable> paramVars, boolean prependTrue) {
    if (t.isTrue()) {
      return booleanIRMatrix(vmgr, true);
    }

    if (t.isFalse()) {
      return booleanIRMatrix(vmgr, false);
    }

    List<TemplateConstraint> constraints = t.getConstraints();
    if (constraints.size() < 1) {
      System.err.println("Tried to build matrix on no constraints.");
      return null;
    }

    // Make a column matrix for each constraint.
    Constraint cons;
    List<Coeff> coeffs;
    List<RationalFunction> rfs;
    Coeff rhs;
    InfixReln reln;
    List<IRMatrix> cols = new Vector<>();

    // Prepend a "true" column, if requested.
    if (prependTrue) {
      cols.add(booleanIRMatrix(vmgr, true));
    }

    for (int i = 0; i < constraints.size(); i++) {
      cons = constraints.get(i);
      coeffs = cons.getNormalFormCoeffs(vmgr, VariableWriteMode.REDLOG);
      rhs = cons.getNormalFormConstant(VariableWriteMode.REDLOG);
      rfs = makeRationalFunctions(coeffs, paramVars);
      rfs.add(rhs.makeRationalFunction(paramVars));
      reln = cons.getInfixReln();
      if (reln != InfixReln.EQUAL) {
        // The infix relation is LEQ or LT.
        cols.add(new IRMatrix(rfs, reln));
      } else {
        // In this case the infix relation is EQUAL.
        // Really this corresponds to two lax inequalities.
        cols.add(new IRMatrix(rfs, InfixReln.LEQ));
        coeffs = negative(coeffs);
        rhs = rhs.negative();
        rfs = makeRationalFunctions(coeffs, paramVars);
        rfs.add(rhs.makeRationalFunction(paramVars));
        cols.add(new IRMatrix(rfs, InfixReln.LEQ));
      }
    }

    // Put the columns together.
    IRMatrix a = cols.get(0);
    for (int i = 1; i < cols.size(); i++) {
      a = IRMatrix.concat(a, cols.get(i));
    }

    return a;
  }

  private static IRMatrix booleanIRMatrix(VariableManager vmgr, boolean trueStatement) {
    int n = vmgr.getNumVars();
    List<RationalFunction> rfs = Collections.nCopies(n+1, new RationalFunction(0));
    InfixReln reln;
    if (trueStatement) {
      reln = InfixReln.LEQ;
    } else {
      reln = InfixReln.LT;
    }
    return new IRMatrix(rfs, reln);
  }

}
