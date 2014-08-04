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
package org.sosy_lab.cpachecker.util.invariants.interfaces;

import java.util.List;

import org.sosy_lab.cpachecker.util.invariants.Coeff;
import org.sosy_lab.cpachecker.util.invariants.InfixReln;
import org.sosy_lab.cpachecker.util.invariants.templates.VariableWriteMode;

public interface Constraint {

  /**
   * When the Constraint object is constructed, it should construct
   * and store a VariableManager object that lists its (program)
   * variables.
   * Then, when this method is called, the passed vmgr lists /all/
   * the variables that will appear in any equation or inequality in
   * the overall linear system. Therefore the job of this method is
   * to reconcile the two VariableManagers. Probably you should just
   * copy the method in the ConstraintImpl class. In fact, maybe we
   * will make Constraint into an abstract class instead of an
   * interface, and we will put that method into it.
   *
   * @return List of coeffs <c1, c2, ..., cn> such that if the
   * variables in vmgr are, in order, <v1, v2, ..., vn>, then ci is
   * the coefficient of vi in this Constraint, for i = 1 ... n. Some
   * of the ci may of course be zero, in case variables vi do not
   * appear in this constraint at all.
   */
  public List<Coeff> getNormalFormCoeffs(VariableManager vmgr, VariableWriteMode vwm);

  /**
   * @return the coefficient giving all the constant terms, when the
   * relation is EQUAL or LEQ, and the constants are all gathered on
   * the right-hand side.
   */
  public Coeff getNormalFormConstant(VariableWriteMode vwm);

  /*
   * This method is for use when we assume that the program variables
   * are integers, and we transform strict inequalities into lax.
   */
  public Coeff getNormalFormConstantMinusOne(VariableWriteMode vwm);

  /**
   * @return the infix relation of this constraint.
   */
  public InfixReln getInfixReln();

}