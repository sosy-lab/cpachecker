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
package org.sosy_lab.cpachecker.util.invariants.templates;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Coeff;
import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.LinearInequality;
import org.sosy_lab.cpachecker.util.invariants.balancer.IRMatrix;
import org.sosy_lab.cpachecker.util.invariants.balancer.Matrix;
import org.sosy_lab.cpachecker.util.invariants.balancer.RationalFunction;
import org.sosy_lab.cpachecker.util.invariants.balancer.Variable;
import org.sosy_lab.cpachecker.util.invariants.interfaces.Constraint;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

public class TemplateLinearizer {

  /**
   * This method is intended to be used when you have one or more
   * templates t1, t2, ..., tn, all using the same variable manager
   * vmgr. Each template should be conjunctive,
   *      ti = c1 ^ c2 ^ ... ^ cm.
   * You pass the templates one at a time. When you pass ti, you will
   * get back a matrix representation of all the constraints c1, c2, ..., cm
   * in ti. It will be up to you to append all these matrix representations
   * one after another if you want to solve them all at once, using the
   * LinearInequality object's 'append' (alter in-place) or 'combine'
   * (create a new object) methods.
   *
   * Regarding behavior if your templates are not conjunctive: note that
   * the constraints will be extracted simply by calling the template's
   * 'getConstraints' method.
   *
   * @param t The Template to be linearized.
   * @param vmgr A VariableManager containing the list of all variables
   *              that may appear in any of the templates to be put
   *              together into a single inequality representation.
   */
  public static LinearInequality linearize(TemplateFormula t, VariableManager vmgr) {

    if (t.isTrue()) {
      return booleanLineq(t.getFormulaType(), vmgr, true);
    }

    if (t.isFalse()) {
      return booleanLineq(t.getFormulaType(), vmgr, false);
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
      rhs = cons.getNormalFormConstant(VariableWriteMode.REDLOG);
      reln = cons.getInfixReln();
      if (reln != InfixReln.EQUAL) {
        // The infix relation is LEQ or LT.
        lineq.addIneq(coeffs, reln, rhs);
      } else {
        // In this case the infix relation is EQUAL.
        // Really this corresponds to two lax inequalities.
        lineq.addIneq(coeffs, InfixReln.LEQ, rhs);
        coeffs = negative(coeffs);
        rhs = rhs.negative();
        lineq.addIneq(coeffs, InfixReln.LEQ, rhs);
      }
    }

    return lineq;
  }

  public static IRMatrix buildIRMatrix(TemplateFormula t, VariableManager vmgr, Map<String, Variable> paramVars) {
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

  public static Matrix buildMatrix(TemplateFormula t, VariableManager vmgr, Map<String, Variable> paramVars) {
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
    for (int i = 0; i < constraints.size(); i++) {

      cons = constraints.get(i);
      coeffs = cons.getNormalFormCoeffs(vmgr, VariableWriteMode.REDLOG);
      rhs = cons.getNormalFormConstant(VariableWriteMode.REDLOG);
      coeffs.add(rhs.negative());
      rfs = makeRationalFunctions(coeffs, paramVars);
      cols.add(new Matrix(rfs));

      reln = cons.getInfixReln();
      if (reln == InfixReln.EQUAL) {
        // We consider EQUAL to be two LEQs, which means that
        // in addition to the column itself, we add its negation.
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

  private static List<RationalFunction> makeRationalFunctions(List<Coeff> clist, Map<String, Variable> paramVars) {
    List<RationalFunction> rfs = new Vector<>(clist.size());
    for (Coeff c : clist) {
      rfs.add(c.makeRationalFunction(paramVars));
    }
    return rfs;
  }

  /**
   * Create the inequality 0 <= 0 for a true statement, 0 < 0 for a false one.
   * @param vmgr the VariableManager, which says how many zero coeffs we need on the LHS
   * @param trueStatement a boolean which says if we want a true or a false statement
   * @return
   */
  private static LinearInequality booleanLineq(FormulaType<?> type, VariableManager vmgr, boolean trueStatement) {
    LinearInequality lineq = new LinearInequality(vmgr);
    int n = vmgr.getNumVars();
    List<Coeff> coeffs = Collections.nCopies(n, new Coeff(type, "0"));
    Coeff rhs = new Coeff(type, "0");
    InfixReln reln;
    if (trueStatement) {
      reln = InfixReln.LEQ;
    } else {
      reln = InfixReln.LT;
    }
    lineq.addIneq(coeffs, reln, rhs);
    return lineq;
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

  private static Matrix booleanMatrix(VariableManager vmgr, boolean trueStatement) {
    int n = vmgr.getNumVars();
    List<RationalFunction> rfs = Collections.nCopies(n, new RationalFunction(0));
    RationalFunction constant;
    if (trueStatement) {
      constant = new RationalFunction(-1);
    } else {
      constant = new RationalFunction(1);
    }
    rfs.add(constant);
    return new Matrix(rfs);
  }

  /**
   * @param P A list of coefficients
   * @return The list of all passed coefficients negated
   */
  private static List<Coeff> negative(List<Coeff> P) {
    Vector<Coeff> N = new Vector<>();
    Coeff C;
    for (int i = 0; i < P.size(); i++) {
      C = P.get(i);
      N.add(C.negative());
    }
    return N;
  }

}
