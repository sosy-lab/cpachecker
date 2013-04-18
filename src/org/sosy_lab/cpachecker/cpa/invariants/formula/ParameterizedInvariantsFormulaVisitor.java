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

public interface ParameterizedInvariantsFormulaVisitor<ConstantType, ParameterType, ReturnType> {

  ReturnType visit(Add<ConstantType> pAdd, ParameterType pParameter);

  ReturnType visit(BinaryAnd<ConstantType> pAnd, ParameterType pParameter);

  ReturnType visit(BinaryNot<ConstantType> pNot, ParameterType pParameter);

  ReturnType visit(BinaryOr<ConstantType> pOr, ParameterType pParameter);

  ReturnType visit(BinaryXor<ConstantType> pXor, ParameterType pParameter);

  ReturnType visit(Constant<ConstantType> pConstant, ParameterType pParameter);

  ReturnType visit(Divide<ConstantType> pDivide, ParameterType pParameter);

  ReturnType visit(Equal<ConstantType> pEqual, ParameterType pParameter);

  ReturnType visit(LessThan<ConstantType> pLessThan, ParameterType pParameter);

  ReturnType visit(LogicalAnd<ConstantType> pAnd, ParameterType pParameter);

  ReturnType visit(LogicalNot<ConstantType> pNot, ParameterType pParameter);

  ReturnType visit(Modulo<ConstantType> pModulo, ParameterType pParameter);

  ReturnType visit(Multiply<ConstantType> pMultiply, ParameterType pParameter);

  ReturnType visit(Negate<ConstantType> pNegate, ParameterType pParameter);

  ReturnType visit(ShiftLeft<ConstantType> pShiftLeft, ParameterType pParameter);

  ReturnType visit(ShiftRight<ConstantType> pShiftRight, ParameterType pParameter);

  ReturnType visit(Union<ConstantType> pUnion, ParameterType pParameter);

  ReturnType visit(Variable<ConstantType> pVariable, ParameterType pParameter);

}
