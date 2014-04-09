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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

/**
 * Instances of implementing classes are visitors for invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the visited
 * formulae.
 * @param <ReturnType> the type of the visit results.
 */
public interface InvariantsFormulaVisitor<ConstantType, ReturnType> {

  /**
   * Visits the given addition invariants formula.
   *
   * @param pAdd the addition invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(Add<ConstantType> pAdd);

  /**
   * Visits the given binary and invariants formula.
   *
   * @param pAnd the binary and invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(BinaryAnd<ConstantType> pAnd);

  /**
   * Visits the given binary negation invariants formula.
   *
   * @param pNot the binary negation invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(BinaryNot<ConstantType> pNot);

  /**
   * Visits the given binary or invariants formula.
   *
   * @param pOr the binary or invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(BinaryOr<ConstantType> pOr);

  /**
   * Visits the given binary exclusive or invariants formula.
   *
   * @param pXor the binary exclusive or invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(BinaryXor<ConstantType> pXor);

  /**
   * Visits the given constant invariants formula.
   *
   * @param pConstant the constant invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(Constant<ConstantType> pConstant);

  /**
   * Visits the given fraction invariants formula.
   *
   * @param pDivide the fraction invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(Divide<ConstantType> pDivide);

  /**
   * Visits the given equation invariants formula.
   *
   * @param pEqual the equation invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(Equal<ConstantType> pEqual);

  /**
   * Visits the given less-than inequation invariants formula.
   *
   * @param pLessThan the less-than inequation invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(LessThan<ConstantType> pLessThan);

  /**
   * Visits the given logical conjunction invariants formula.
   *
   * @param pAnd the logical conjunction invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(LogicalAnd<ConstantType> pAnd);

  /**
   * Visits the given logical negation invariants formula.
   *
   * @param pNot the logical negation invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(LogicalNot<ConstantType> pNot);

  /**
   * Visits the given modulo invariants formula.
   *
   * @param pModulo the modulo invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(Modulo<ConstantType> pModulo);

  /**
   * Visits the given multiplication invariants formula.
   *
   * @param pMultiply the multiplication invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(Multiply<ConstantType> pMultiply);

  /**
   * Visits the given left shift invariants formula.
   *
   * @param pMultiply the left shift invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(ShiftLeft<ConstantType> pShiftLeft);

  /**
   * Visits the given right shift invariants formula.
   *
   * @param pMultiply the right shift invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(ShiftRight<ConstantType> pShiftRight);

  /**
   * Visits the given union invariants formula.
   *
   * @param pMultiply the union invariants formula to visit.
   *
   * @return the result of the visit.
   */
  ReturnType visit(Union<ConstantType> pUnion);

  /**
   * Visits the given variable invariants formula.
   *
   * @param pMultiply the variable invariants formula to visit..
   *
   * @return the result of the visit.
   */
  ReturnType visit(Variable<ConstantType> pVariable);

}
