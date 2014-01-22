/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
 * Instances of this class are visitors used to check if the visited formula is
 * a boolean formula. While numerical evaluations of formulae can be
 * interpreted as boolean values with <code>0</code> meaning <code>false</code>
 * and <code>1</code> meaning <code>true</code>, only formulae with operators
 * producing genuine boolean results are considered as boolean formula.
 *
 * @param <T> the type of the constants used in the visited formulae.
 */
public class IsBooleanFormulaVisitor<T> implements InvariantsFormulaVisitor<T, Boolean> {

  @Override
  public Boolean visit(Add<T> pAdd) {
    return false;
  }

  @Override
  public Boolean visit(BinaryAnd<T> pAnd) {
    return false;
  }

  @Override
  public Boolean visit(BinaryNot<T> pNot) {
    return false;
  }

  @Override
  public Boolean visit(BinaryOr<T> pOr) {
    return false;
  }

  @Override
  public Boolean visit(BinaryXor<T> pXor) {
    return false;
  }

  @Override
  public Boolean visit(Constant<T> pConstant) {
    return false;
  }

  @Override
  public Boolean visit(Divide<T> pDivide) {
    return false;
  }

  @Override
  public Boolean visit(Equal<T> pEqual) {
    return true;
  }

  @Override
  public Boolean visit(LessThan<T> pLessThan) {
    return true;
  }

  @Override
  public Boolean visit(LogicalAnd<T> pAnd) {
    return true;
  }

  @Override
  public Boolean visit(LogicalNot<T> pNot) {
    return true;
  }

  @Override
  public Boolean visit(Modulo<T> pModulo) {
    return false;
  }

  @Override
  public Boolean visit(Multiply<T> pMultiply) {
    return false;
  }

  @Override
  public Boolean visit(ShiftLeft<T> pShiftLeft) {
    return false;
  }

  @Override
  public Boolean visit(ShiftRight<T> pShiftRight) {
    return false;
  }

  @Override
  public Boolean visit(Union<T> pUnion) {
    return false;
  }

  @Override
  public Boolean visit(Variable<T> pVariable) {
    return false;
  }

}
