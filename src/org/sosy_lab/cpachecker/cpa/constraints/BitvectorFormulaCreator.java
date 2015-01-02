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

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstantConstraintExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicIdentifier;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Creator for {@link Formula}s using bitvectors.
 */
public class BitvectorFormulaCreator implements FormulaCreator<Formula> {

  private static final boolean SIGNED = true;

  /**
   * Default bitvector size to use (in byte).
   */
  private static final int DEFAULT_BITVECTOR_SIZE = 4;

  private final FormulaManagerView formulaManager;
  private final BitvectorFormulaManagerView bitvectorFormulaManager;
  
  private final MachineModel machineModel;

  public BitvectorFormulaCreator(FormulaManagerView pFormulaManager, MachineModel pMachineModel) {
    formulaManager = pFormulaManager;
    bitvectorFormulaManager = formulaManager.getBitvectorFormulaManager();
    machineModel = pMachineModel;
  }


  @Override
  public BitvectorFormula visit(AdditionExpression pAdd) {
    final BitvectorFormula op1 = (BitvectorFormula) pAdd.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pAdd.getOperand2().accept(this);

    return bitvectorFormulaManager.add(op1, op2);
  }

  @Override
  public BitvectorFormula visit(BinaryAndExpression pAnd) {
    final BitvectorFormula op1 = (BitvectorFormula) pAnd.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pAnd.getOperand2().accept(this);

    return bitvectorFormulaManager.and(op1, op2);
  }

  @Override
  public BitvectorFormula visit(BinaryNotExpression pNot) {
    final BitvectorFormula op = (BitvectorFormula) pNot.getOperand().accept(this);

    return bitvectorFormulaManager.not(op);
  }

  @Override
  public BitvectorFormula visit(BinaryOrExpression pOr) {
    final BitvectorFormula op1 = (BitvectorFormula) pOr.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pOr.getOperand2().accept(this);

    return bitvectorFormulaManager.or(op1, op2);
  }

  @Override
  public BitvectorFormula visit(BinaryXorExpression pXor) {
    final BitvectorFormula op1 = (BitvectorFormula) pXor.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pXor.getOperand2().accept(this);

    return bitvectorFormulaManager.xor(op1, op2);
  }

  @Override
  public Formula visit(ConstantConstraintExpression pConstant) {
    Value constantValue = pConstant.getValue();

    if (constantValue.isNumericValue()) {
      return bitvectorFormulaManager.makeBitvector((FormulaType<BitvectorFormula>) getFormulaType(pConstant.getExpressionType()),
          ((NumericValue) constantValue).longValue());

    } else if (constantValue instanceof BooleanValue) {
      return formulaManager.getBooleanFormulaManager().makeBoolean(((BooleanValue) constantValue).isTrue());

    } else if (constantValue instanceof SymbolicValue) {
      return ((SymbolicValue) constantValue).accept(this);

    }

    return null; // if we can't handle it, 'abort'
  }

  @Override
  public Formula visit(SymbolicIdentifier pValue) {
    return formulaManager.makeVariable(getFormulaType(pValue.getType()), pValue.toString());
  }

  @Override
  public Formula visit(SymbolicExpression pValue) {
    return pValue.getExpression().accept(this);
  }

  private FormulaType<?> getFormulaType(Type pType) {
    if (pType instanceof JSimpleType) {
      int sizeInByte;

      switch (((JSimpleType) pType).getType()) {
        case BOOLEAN:
          return FormulaType.BooleanType;
        case BYTE:
          sizeInByte = 1;
          break;
        case CHAR:
          sizeInByte = 2;
          break;
        case SHORT:
          sizeInByte = 2;
          break;
        case FLOAT:
        case INT:
          sizeInByte = 4;
          break;
        case DOUBLE:
        case LONG:
          sizeInByte = 8;
          break;
        case UNSPECIFIED:
          sizeInByte = DEFAULT_BITVECTOR_SIZE;
          break;
        default:
          throw new AssertionError("Unhandled type " + pType);
      }

      int sizeInBit = sizeInByte * machineModel.getSizeofCharInBits();
      return FormulaType.getBitvectorTypeWithSize(sizeInBit);

    } else if (pType instanceof CType) {
      int sizeInBit = machineModel.getSizeof((CType) pType) * machineModel.getSizeofCharInBits();

      return FormulaType.getBitvectorTypeWithSize(sizeInBit);
    } else {
      throw new AssertionError("Unhandled type " + pType);
    }
  }

  @Override
  public BitvectorFormula visit(DivisionExpression pDivide) {
    final BitvectorFormula op1 = (BitvectorFormula) pDivide.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pDivide.getOperand2().accept(this);

    return bitvectorFormulaManager.divide(op1, op2, SIGNED);
  }

  @Override
  public BooleanFormula visit(EqualsExpression pEqual) {
    final BitvectorFormula op1 = (BitvectorFormula) pEqual.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pEqual.getOperand2().accept(this);

    return bitvectorFormulaManager.equal(op1, op2);
  }

  @Override
  public BooleanFormula visit(LessThanExpression pLessThan) {
    final BitvectorFormula op1 = (BitvectorFormula) pLessThan.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pLessThan.getOperand2().accept(this);

    return bitvectorFormulaManager.lessThan(op1, op2, SIGNED);
  }

  @Override
  public Formula visit(LogicalOrExpression pExpression) {
    final BitvectorFormula op1 = (BitvectorFormula) pExpression.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pExpression.getOperand2().accept(this);

    return bitvectorFormulaManager.or(op1, op2);
  }

  @Override
  public BooleanFormula visit(LogicalAndExpression pAnd) {
    final BitvectorFormula op1 = (BitvectorFormula) pAnd.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pAnd.getOperand2().accept(this);

    return (BooleanFormula) formulaManager.makeAnd(op1, op2);
  }

  @Override
  public BooleanFormula visit(LogicalNotExpression pNot) {
    final BooleanFormula op = (BooleanFormula) pNot.getOperand().accept(this);

    return formulaManager.getBooleanFormulaManager().not(op);
  }

  @Override
  public Formula visit(LessThanOrEqualExpression pExpression) {
    final Formula op1 = pExpression.getOperand1().accept(this);
    final Formula op2 = pExpression.getOperand2().accept(this);

    return formulaManager.makeLessOrEqual(op1, op2, SIGNED);
  }

  @Override
  public BitvectorFormula visit(ModuloExpression pModulo) {
    final BitvectorFormula op1 = (BitvectorFormula) pModulo.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pModulo.getOperand2().accept(this);

    return bitvectorFormulaManager.modulo(op1, op2, SIGNED);
  }

  @Override
  public BitvectorFormula visit(MultiplicationExpression pMultiply) {
    final BitvectorFormula op1 = (BitvectorFormula) pMultiply.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pMultiply.getOperand2().accept(this);

    return bitvectorFormulaManager.multiply(op1, op2);
  }

  @Override
  public BitvectorFormula visit(ShiftLeftExpression pShiftLeft) {
    final BitvectorFormula op1 = (BitvectorFormula) pShiftLeft.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pShiftLeft.getOperand2().accept(this);

    return bitvectorFormulaManager.shiftLeft(op1, op2);
  }

  @Override
  public BitvectorFormula visit(ShiftRightExpression pShiftRight) {
    final BitvectorFormula op1 = (BitvectorFormula) pShiftRight.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pShiftRight.getOperand2().accept(this);

    return bitvectorFormulaManager.shiftRight(op1, op2, SIGNED);
  }
}
