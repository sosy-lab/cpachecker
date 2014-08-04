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


public class BasicFormulaMatriciser extends FormulaMatriciser {

  @Override
  public Matrix buildMatrix(TemplateFormula t, VariableManager vmgr, Map<String, Variable> paramVars, boolean prependTrue) {
    if (t.isTrue()) {
      return booleanMatrix(vmgr, true);
    }

    if (t.isFalse()) {
      return booleanMatrix(vmgr, false);
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
    List<Matrix> cols = new Vector<>();

    // Prepend a "true" column, if requested.
    if (prependTrue) {
      cols.add(booleanMatrix(vmgr, true));
    }

    for (int i = 0; i < constraints.size(); i++) {

      // Get the constraint, its infix reln, and the coeffs of all its variable terms.
      cons = constraints.get(i);
      reln = cons.getInfixReln();
      coeffs = cons.getNormalFormCoeffs(vmgr, VariableWriteMode.REDLOG);

      // If the relation is strict <, then since we are assuming that our
      // program variables are integers, we transform this into weak <= by
      // subtracting one from the normal form constant. Else, we take the
      // constant as it is.
      if (reln == InfixReln.LT) {
        rhs = cons.getNormalFormConstantMinusOne(VariableWriteMode.REDLOG);
      } else {
        rhs = cons.getNormalFormConstant(VariableWriteMode.REDLOG);
      }
      // Now bring the constant over to the LHS, by negating it.
      coeffs.add(rhs.negative());

      // Make all the coeffs into rational functions.
      rfs = makeRationalFunctions(coeffs, paramVars);

      // Add a column.
      cols.add(new Matrix(rfs));

      // We consider EQUAL to be two LEQs, which means that
      // in addition to the column itself, we adjoin its negation.
      if (reln == InfixReln.EQUAL) {
        coeffs = negative(coeffs);
        rfs = makeRationalFunctions(coeffs, paramVars);
        cols.add(new Matrix(rfs));
      }
    }

    // Put the columns together.
    Matrix a = cols.get(0);
    for (int i = 1; i < cols.size(); i++) {
      a = a.concat(cols.get(i));
    }

    return a;
  }

  private static Matrix booleanMatrix(VariableManager vmgr, boolean trueStatement) {
    int n = vmgr.getNumVars();
    List<RationalFunction> rfs = new Vector<>(Collections.nCopies(n, new RationalFunction(0)));
    RationalFunction constant;
    if (trueStatement) {
      constant = new RationalFunction(-1);
    } else {
      constant = new RationalFunction(1);
    }
    rfs.add(constant);
    return new Matrix(rfs);
  }

}
