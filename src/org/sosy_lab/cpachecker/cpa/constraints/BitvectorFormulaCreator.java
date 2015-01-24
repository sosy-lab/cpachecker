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
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.SymbolicExpression;
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
  private static final FormulaType<? extends Formula> DEFAULT_BITVECTOR =
      FormulaType.getBitvectorTypeWithSize(DEFAULT_BITVECTOR_SIZE);

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
    final BinaryCreator creator = new BinaryCreator() {
      @Override
      public BitvectorFormula create(BitvectorFormula pOp1, BitvectorFormula pOp2, Type pType) {
        return bitvectorFormulaManager.add(pOp1, pOp2);
      }
    };

    return getCastFormulaFromBinaryExp(creator, pExpression);
  }

  private BitvectorFormula getFormula(SymbolicExpression pExpression, Type pToType) {
    BitvectorFormula op1 = (BitvectorFormula) pExpression.accept(this);
    final Type fromType = pExpression.getType();

    return cast(op1, fromType, pToType);
  }

  private BitvectorFormula cast(BitvectorFormula pFormula, Type pFromType, Type pToType) {
    FormulaType<?> fromFormulaType = getFormulaType(pFromType);
    FormulaType<?> toFormulaType = getFormulaType(pToType);

    assert fromFormulaType.isBitvectorType() && toFormulaType.isBitvectorType();

    int fromSize = ((FormulaType.BitvectorType)fromFormulaType).getSize();
    int toSize = ((FormulaType.BitvectorType)toFormulaType).getSize();
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
      return pType instanceof CSimpleType && ((CSimpleType)pType).isSigned();
    }
  }

  @Override
  public BitvectorFormula visit(BinaryAndExpression pExpression) {
    final BinaryCreator creator = new BinaryCreator() {
      @Override
      public BitvectorFormula create(BitvectorFormula pOp1, BitvectorFormula pOp2, Type pType) {
        return bitvectorFormulaManager.and(pOp1, pOp2);
      }
    };

    return getCastFormulaFromBinaryExp(creator, pExpression);
  }

  @Override
  public BitvectorFormula visit(BinaryNotExpression pExpression) {
    final BitvectorFormula op = (BitvectorFormula)pExpression.getOperand().accept(this);

    return bitvectorFormulaManager.not(op);
  }

  @Override
  public BitvectorFormula visit(BinaryOrExpression pExpression) {
    final BinaryCreator creator = new BinaryCreator() {
      @Override
      public BitvectorFormula create(BitvectorFormula pOp1, BitvectorFormula pOp2, Type pType) {
        return bitvectorFormulaManager.or(pOp1, pOp2);
      }
    };

    return getCastFormulaFromBinaryExp(creator, pExpression);
  }

  @Override
  public BitvectorFormula visit(BinaryXorExpression pExpression) {
    final BinaryCreator creator = new BinaryCreator() {
      @Override
      public BitvectorFormula create(BitvectorFormula pOp1, BitvectorFormula pOp2, Type pType) {
        return bitvectorFormulaManager.xor(pOp1, pOp2);
      }
    };

    return getCastFormulaFromBinaryExp(creator, pExpression);
  }

  @Override
  public Formula visit(ConstantSymbolicExpression pConstant) {
    Value constantValue = pConstant.getValue();

    if (constantValue.isNumericValue()) {
      return bitvectorFormulaManager
          .makeBitvector((FormulaType<BitvectorFormula>)getFormulaType(pConstant.getType()),
              ((NumericValue)constantValue).longValue());

    } else if (constantValue instanceof BooleanValue) {
      return formulaManager.getBooleanFormulaManager().makeBoolean(((BooleanValue)constantValue).isTrue());

    } else if (constantValue instanceof SymbolicValue) {
      return ((SymbolicValue)constantValue).accept(this);

    }

    return null; // if we can't handle it, 'abort'
  }

  @Override
  public Formula visit(SymbolicIdentifier pValue) {
    return formulaManager.makeVariable(DEFAULT_BITVECTOR, getName(pValue));
  }

  private String getName(SymbolicIdentifier pIdentifier) {
    return pIdentifier.toString();
  }

  private FormulaType<?> getFormulaType(Type pType) {
    if (pType instanceof JSimpleType) {
      int sizeInByte;

      switch (((JSimpleType)pType).getType()) {
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
      int sizeInBit = machineModel.getSizeof((CType)pType) * machineModel.getSizeofCharInBits();

      return FormulaType.getBitvectorTypeWithSize(sizeInBit);
    } else {
      throw new AssertionError("Unhandled type " + pType);
    }
  }

  @Override
  public BitvectorFormula visit(DivisionExpression pExpression) {
    final Type calculationType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), calculationType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), calculationType);

    return bitvectorFormulaManager.divide(op1, op2, isSigned(calculationType));
  }

  @Override
  public BooleanFormula visit(EqualsExpression pExpression) {
    final Type calculationType = pExpression.getCalculationType();
    final SymbolicExpression op1 = pExpression.getOperand1();
    final SymbolicExpression op2 = pExpression.getOperand2();
    final Type op1Type = pExpression.getOperand1().getType();
    final Type op2Type = pExpression.getOperand2().getType();

    final BitvectorFormula op1Formula = getFormula(op1, calculationType);
    final BitvectorFormula op2Formula = getFormula(op2, calculationType);

    BooleanFormula formula = formulaManager.makeEqual(op1Formula, op2Formula);

    if (isSigned(op1Type) != isSigned(op2Type)) {
      BooleanFormula msbIsZeroFormula = getMsbIsZeroFormula(op1Formula, op2Formula);
      formula = formulaManager.makeAnd(formula, msbIsZeroFormula);
    }

    return formula;
  }

  // Returns a formula that requires that the most significant bit of both given formulas is 0.
  private BooleanFormula getMsbIsZeroFormula(BitvectorFormula pFormula1, BitvectorFormula pFormula2) {
    return formulaManager.makeAnd(getSingleMsbIsZeroFormula(pFormula1), getSingleMsbIsZeroFormula(pFormula2));
  }

  private BooleanFormula getSingleMsbIsZeroFormula(BitvectorFormula pFormula) {
    final int length = bitvectorFormulaManager.getLength(pFormula);
    final BitvectorFormula mostSignificantBit = bitvectorFormulaManager.extract(pFormula, length, length - 1);

    final BitvectorFormula zero = bitvectorFormulaManager.makeBitvector(1, 0L);

    return formulaManager.makeEqual(mostSignificantBit, zero);
  }

  @Override
  public BooleanFormula visit(LessThanExpression pExpression) {
    final Type calculationType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), calculationType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), calculationType);

    return bitvectorFormulaManager.lessThan(op1, op2, isSigned(calculationType));
  }

  @Override
  public BitvectorFormula visit(LogicalOrExpression pExpression) {
    final BinaryCreator creator = new BinaryCreator() {
      @Override
      public BitvectorFormula create(BitvectorFormula pOp1, BitvectorFormula pOp2, Type pType) {
        return bitvectorFormulaManager.or(pOp1, pOp2);
      }
    };

    return getCastFormulaFromBinaryExp(creator, pExpression);
  }

  @Override
  public BooleanFormula visit(LogicalAndExpression pExpression) {
    final Type calculationType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), calculationType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), calculationType);

    return (BooleanFormula) formulaManager.makeAnd(op1, op2);
  }

  @Override
  public Formula visit(CastExpression pExpression) {
    BitvectorFormula formula = (BitvectorFormula) pExpression.getOperand().accept(this);
    final Type fromType = pExpression.getOperand().getType();
    final Type toType = pExpression.getType();

    return cast(formula, fromType, toType);
  }

  @Override
  public Formula visit(PointerExpression pExpression) {
    return null; // TODO
  }

  @Override
  public BooleanFormula visit(LogicalNotExpression pExpression) {
    final BooleanFormula op = (BooleanFormula)pExpression.getOperand().accept(this);

    return formulaManager.getBooleanFormulaManager().not(op);
  }

  @Override
  public BooleanFormula visit(LessThanOrEqualExpression pExpression) {
    final Type toType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), toType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), toType);

    return formulaManager.makeLessOrEqual(op1, op2, isSigned(toType));
  }

  @Override
  public BitvectorFormula visit(ModuloExpression pExpression) {
    final BinaryCreator creator = new BinaryCreator() {
      @Override
      public BitvectorFormula create(BitvectorFormula pOp1, BitvectorFormula pOp2, Type pType) {
        return bitvectorFormulaManager.modulo(pOp1, pOp2, isSigned(pType));
      }
    };

    return getCastFormulaFromBinaryExp(creator, pExpression);
  }

  @Override
  public BitvectorFormula visit(MultiplicationExpression pExpression) {
    final BinaryCreator creator = new BinaryCreator() {
      @Override
      public BitvectorFormula create(BitvectorFormula pOp1, BitvectorFormula pOp2, Type pType) {
        return bitvectorFormulaManager.multiply(pOp1, pOp2);
      }
    };

    return getCastFormulaFromBinaryExp(creator, pExpression);
  }

  @Override
  public BitvectorFormula visit(ShiftLeftExpression pExpression) {
    final BinaryCreator creator = new BinaryCreator() {
      @Override
      public BitvectorFormula create(BitvectorFormula pOp1, BitvectorFormula pOp2, Type pType) {
        return bitvectorFormulaManager.shiftLeft(pOp1, pOp2);
      }
    };

    return getCastFormulaFromBinaryExp(creator, pExpression);
  }

  @Override
  public BitvectorFormula visit(ShiftRightExpression pExpression) {
    final Type leftOperandType = pExpression.getOperand1().getType();

    final BinaryCreator creator = new BinaryCreator() {
      @Override
      public BitvectorFormula create(BitvectorFormula pOp1, BitvectorFormula pOp2, Type pType) {
        return bitvectorFormulaManager.shiftRight(pOp1, pOp2, isSigned(leftOperandType));
      }
    };

    return getCastFormulaFromBinaryExp(creator, pExpression);
  }

  private BitvectorFormula getCastFormulaFromBinaryExp(BinaryCreator pCreator,
      BinarySymbolicExpression pExpression) {

    final Type calculationType = pExpression.getCalculationType();
    final BitvectorFormula op1 = getFormula(pExpression.getOperand1(), calculationType);
    final BitvectorFormula op2 = getFormula(pExpression.getOperand2(), calculationType);

    final Type expressionType = pExpression.getType();

    BitvectorFormula formula = pCreator.create(op1, op2, calculationType);

    return cast(formula, calculationType, expressionType);
  }

  @Override
  public BooleanFormula transformAssignment(Model.AssignableTerm pTerm, Object termAssignment) {
    return null;
    /*
    Formula variable = createVariable(pTerm);

    final FormulaType<?> type = getFormulaType(pTerm.getType());
    Formula rightFormula = null;

    if (termAssignment instanceof Number) {
      assert type.isIntegerType();

      if (termAssignment instanceof Long) {
        rightFormula = bitvectorFormulaManager.makeNumber((Long) termAssignment);
      } else if (termAssignment instanceof Double) {
        rightFormula = bitvectorFormulaManager.makeNumber((Double) termAssignment);
      } else if (termAssignment instanceof BigInteger) {
        rightFormula = bitvectorFormulaManager.makeNumber((BigInteger) termAssignment);
      } else if (termAssignment instanceof BigDecimal) {
        rightFormula = bitvectorFormulaManager.makeNumber((BigDecimal) termAssignment);
      } else {
        throw new AssertionError("Unhandled assignment number " + termAssignment);
      }

    } else {
      throw new AssertionError("Unhandled assignment object " + termAssignment);
    }

    return formulaManager.makeEqual(variable, rightFormula); */
  }

  private Formula createVariable(Model.AssignableTerm pTerm) {
    final String name = pTerm.getName();
    final FormulaType<?> type = getFormulaType(pTerm.getType());

    return formulaManager.makeVariable(type, name);
  }

  private FormulaType<?> getFormulaType(Model.TermType pType) {
    if (pType.equals(Model.TermType.Boolean)) {
      return FormulaType.BooleanType;

    } else if (pType.equals(Model.TermType.Integer)) {
      return FormulaType.IntegerType;

    } else {
      throw new AssertionError("Unexpected term type " + pType);
    }
  }

  private static interface BinaryCreator {
    BitvectorFormula create(BitvectorFormula pOp1, BitvectorFormula pOp2, Type pType);
  }
}
