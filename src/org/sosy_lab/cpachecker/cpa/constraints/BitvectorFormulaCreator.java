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
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstantConstraintExpression;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpression;
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

  /**
   * Default bitvector size to use (in byte).
   */
  private static final int DEFAULT_BITVECTOR_SIZE = 4;

  /**
   * Size of one byte in bit in Java.
   */
  private static final int JAVA_BYTE_SIZE = 8;

  private final FormulaManagerView formulaManager;
  private final BitvectorFormulaManagerView bitvectorFormulaManager;
  
  private final MachineModel machineModel;

  public BitvectorFormulaCreator(FormulaManagerView pFormulaManager, MachineModel pMachineModel) {
    formulaManager = pFormulaManager;
    bitvectorFormulaManager = formulaManager.getBitvectorFormulaManager();
    machineModel = pMachineModel;
  }


  @Override
  public BitvectorFormula visit(AdditionExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return bitvectorFormulaManager.add(op1, op2);
  }

  private BitvectorFormula getFormula(ConstraintExpression pExpression, Type pToType) {
    BitvectorFormula op1 = (BitvectorFormula) pExpression.accept(this);
    final Type fromType = pExpression.getExpressionType();

    return cast(op1, fromType, pToType);
  }

  private BitvectorFormula cast(BitvectorFormula pFormula, Type pFromType, Type pToType) {
    assert !(pToType instanceof CSimpleType)
        || (!((CSimpleType)pToType).isShort() && !(((CSimpleType)pToType).getType() == CBasicType.CHAR));
    FormulaType<?> fromFormulaType = getFormulaType(pFromType);
    FormulaType<?> toFormulaType = getFormulaType(pToType);

    assert fromFormulaType.isBitvectorType() && toFormulaType.isBitvectorType();

    int fromSize = ((FormulaType.BitvectorType) fromFormulaType).getSize();
    int toSize = ((FormulaType.BitvectorType) toFormulaType).getSize();
    boolean signed = isSigned(pToType);

    if (fromSize == toSize) {
      return pFormula;

    } else if (fromSize < toSize) {
      return formulaManager.makeExtend(pFormula, (toSize - fromSize), signed);

    } else {
      return formulaManager.makeExtract(pFormula, toSize-1, 0);
    }
  }

  private boolean isSigned(Type pType) {
    if (pType instanceof JSimpleType) {
      JBasicType basicType = ((JSimpleType)pType).getType();

      switch (basicType) {
        case BYTE:
        case SHORT:
        case INT:
        case LONG:
        case FLOAT:
        case DOUBLE:
          return true;
        default:
          return false;
      }
    } else {
      return pType instanceof CSimpleType && ((CSimpleType) pType).isSigned();
    }
  }

  @Override
  public BitvectorFormula visit(BinaryAndExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return bitvectorFormulaManager.and(op1, op2);
  }

  @Override
  public BitvectorFormula visit(BinaryNotExpression pExpression) {
    final BitvectorFormula op = (BitvectorFormula) pExpression.getOperand().accept(this);

    return bitvectorFormulaManager.not(op);
  }

  @Override
  public BitvectorFormula visit(BinaryOrExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return bitvectorFormulaManager.or(op1, op2);
  }

  @Override
  public BitvectorFormula visit(BinaryXorExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

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

      int sizeInBit = sizeInByte * JAVA_BYTE_SIZE;
      return FormulaType.getBitvectorTypeWithSize(sizeInBit);

    } else if (pType instanceof CType) {
      int sizeInBit = machineModel.getSizeof((CType) pType) * machineModel.getSizeofCharInBits();

      return FormulaType.getBitvectorTypeWithSize(sizeInBit);
    } else {
      throw new AssertionError("Unhandled type " + pType);
    }
  }

  @Override
  public BitvectorFormula visit(DivisionExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return bitvectorFormulaManager.divide(op1, op2, isSigned(toType));
  }

  @Override
  public BooleanFormula visit(EqualsExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);



    return bitvectorFormulaManager.equal(op1, op2);
  }

  @Override
  public BooleanFormula visit(LessThanExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return bitvectorFormulaManager.lessThan(op1, op2, isSigned(toType));
  }

  @Override
  public Formula visit(LogicalOrExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return bitvectorFormulaManager.or(op1, op2);
  }

  @Override
  public BooleanFormula visit(LogicalAndExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return (BooleanFormula) formulaManager.makeAnd(op1, op2);
  }

  @Override
  public BooleanFormula visit(LogicalNotExpression pExpression) {
    final BooleanFormula op = (BooleanFormula) pExpression.getOperand().accept(this);

    return formulaManager.getBooleanFormulaManager().not(op);
  }

  @Override
  public Formula visit(LessThanOrEqualExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return formulaManager.makeLessOrEqual(op1, op2, isSigned(toType));
  }

  @Override
  public BitvectorFormula visit(ModuloExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return bitvectorFormulaManager.modulo(op1, op2, isSigned(toType));
  }

  @Override
  public BitvectorFormula visit(MultiplicationExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return bitvectorFormulaManager.multiply(op1, op2);
  }

  @Override
  public BitvectorFormula visit(ShiftLeftExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return bitvectorFormulaManager.shiftLeft(op1, op2);
  }

  @Override
  public BitvectorFormula visit(ShiftRightExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return bitvectorFormulaManager.shiftRight(op1, op2, isSigned(toType));
  }
}
