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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Coeff;
import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.balancer.interfaces.MatrixI;
import org.sosy_lab.cpachecker.util.invariants.interfaces.Constraint;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateConstraint;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;

/*
 * Like BasicFormulaMatriciser, except that we put the "true" column last, we put the
 * constant term first in all columns, and we turn the constraints into columns in reverse order,
 * since the path formula constraints come last, and we want their columns to come first.
 * This is because they have no parameters, so we'll tend to divide by parameters less, and
 * because path formulas often are where the negations are to be found, and negations often
 * involve strict <, which results in +1 to the constant term, and +1 is helpful in trying
 * to derive falsehood (which is represented by 1 <= 0). We want such a constant to be a pivot,
 * so that it is preserved.
 */
public class BasicReversedFormulaMatriciser extends FormulaMatriciser {

  @Override
  public MatrixI buildMatrix(TemplateFormula t, VariableManager vmgr, Map<String, Variable> paramVars, boolean prependTrue) {
    if (t.isTrue()) {
      return booleanMatrix(vmgr, true);
    }

    if (t.isFalse()) {
      return booleanMatrix(vmgr, false);
    }

    // Get the constraints.
    List<TemplateConstraint> constraints = t.getConstraints();
    // Put them in reverse order.
    //Collections.reverse(constraints);

    if (constraints.size() < 1) {
      System.err.println("Tried to build matrix on no constraints.");
      return null;
    }

    // Make a column matrix for each constraint.
    Constraint cons;
    Deque<Coeff> coeffs;
    List<Coeff> coeffList;
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
      coeffs = new ArrayDeque<>(cons.getNormalFormCoeffs(vmgr, VariableWriteMode.REDLOG));

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
      coeffs.addFirst(rhs.negative());
      // And form a list, for later use.
      coeffList = new Vector<>(coeffs);

      // Make all the coeffs into rational functions.
      rfs = makeRationalFunctions(coeffList, paramVars);

      // Add a column.
      cols.add(new Matrix(rfs));

      // We consider EQUAL to be two LEQs, which means that
      // in addition to the column itself, we adjoin its negation.
      if (reln == InfixReln.EQUAL) {
        coeffList = negative(coeffList);
        rfs = makeRationalFunctions(coeffList, paramVars);
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

    List<RationalFunction> rfs = new Vector<>();

    RationalFunction constant;
    if (trueStatement) {
      constant = new RationalFunction(-1);
    } else {
      constant = new RationalFunction(1);
    }
    rfs.add(constant);

    int n = vmgr.getNumVars();
    rfs.addAll(Collections.nCopies(n, new RationalFunction(0)));

    return new Matrix(rfs);
  }

}
