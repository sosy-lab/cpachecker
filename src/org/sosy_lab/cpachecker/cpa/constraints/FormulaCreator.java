/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints;

import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;


/**
 * Class for creating {@link Formula}s out of {@link Constraint}s
 */
public interface FormulaCreator {

  /**
   * Creates a {@link BooleanFormula} representing the given {@link Constraint}.
   *
   * @param pConstraint the constraint to create a formula of
   * @return a <code>Formula</code> representing the given constraint
   */
  BooleanFormula createFormula(Constraint pConstraint) throws UnrecognizedCCodeException, InterruptedException;

  /**
   * Creates a {@link BooleanFormula} representing the given term-value assignment.
   *
   * <p>These assignments are usually returned by {@link ProverEnvironment#getModel()} after a
   * successful SAT check.</p>
   *
   * <p>Example: Given variable <code>a</code> and <code>5</code>, this method
   * returns the formula <code>a equals 5</code>
   * </p>
   *
   * @param pTerm the term of the assignment
   * @param termAssignment the value of the assignment
   * @return a <code>BooleanFormula</code> representing the given assignment
   */
  BooleanFormula transformAssignment(AssignableTerm pTerm, Object termAssignment, VariableMap pVariables)
  ;
}
