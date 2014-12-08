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

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintOperand;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.EqualConstraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.LessConstraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.LessOrEqualConstraint;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Add;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BinaryAnd;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BinaryNot;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BinaryOr;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BinaryXor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Divide;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Equal;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Exclusion;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LessThan;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalAnd;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalNot;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Modulo;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Multiply;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ShiftLeft;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ShiftRight;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Union;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicFormula;
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
  private static final FormulaType<BitvectorFormula> BITVECTOR_TYPE_DEFAULT = FormulaType.getBitvectorTypeWithSize(64);

  private final FormulaManagerView formulaManager;
  private final BitvectorFormulaManagerView bitvectorFormulaManager;

  public BitvectorFormulaCreator(FormulaManagerView pFormulaManager) {
    formulaManager = pFormulaManager;
    bitvectorFormulaManager = formulaManager.getBitvectorFormulaManager();
  }


  @Override
  public BitvectorFormula visit(Add<Value> pAdd) {
    final BitvectorFormula op1 = (BitvectorFormula) pAdd.getSummand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pAdd.getSummand2().accept(this);

    return bitvectorFormulaManager.add(op1, op2);
  }

  @Override
  public BitvectorFormula visit(BinaryAnd<Value> pAnd) {
    final BitvectorFormula op1 = (BitvectorFormula) pAnd.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pAnd.getOperand2().accept(this);

    return bitvectorFormulaManager.and(op1, op2);
  }

  @Override
  public BitvectorFormula visit(BinaryNot<Value> pNot) {
    final BitvectorFormula op = (BitvectorFormula) pNot.getFlipped().accept(this);

    return bitvectorFormulaManager.not(op);
  }

  @Override
  public BitvectorFormula visit(BinaryOr<Value> pOr) {
    final BitvectorFormula op1 = (BitvectorFormula) pOr.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pOr.getOperand2().accept(this);

    return bitvectorFormulaManager.or(op1, op2);
  }

  @Override
  public BitvectorFormula visit(BinaryXor<Value> pXor) {
    final BitvectorFormula op1 = (BitvectorFormula) pXor.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pXor.getOperand2().accept(this);

    return bitvectorFormulaManager.xor(op1, op2);
  }

  @Override
  public Formula visit(Constant<Value> pConstant) {
    Value constantValue = pConstant.getValue();

    if (constantValue.isNumericValue()) {
      return bitvectorFormulaManager.makeBitvector(BITVECTOR_TYPE_DEFAULT,
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
  public Formula visit(SymbolicFormula pValue) {
    return pValue.getFormula().accept(this);
  }

  private FormulaType<?> getFormulaType(Type pType) {
    if (pType instanceof JSimpleType) {
      switch (((JSimpleType) pType).getType()) {
        case BOOLEAN:
          return FormulaType.BooleanType;
        case BYTE:
        case CHAR:
        case SHORT:
        case INT:
        case LONG:
        case FLOAT:
        case DOUBLE:
        case UNSPECIFIED:
          return BITVECTOR_TYPE_DEFAULT;
        default:
          throw new AssertionError("Unhandled type " + pType);
      }
    } else if (pType instanceof CSimpleType) {
      switch (((CSimpleType) pType).getType()) {
        case BOOL:
        case CHAR:
        case INT:
        case UNSPECIFIED:
        case FLOAT:
        case DOUBLE:
          return BITVECTOR_TYPE_DEFAULT;
        default:
          throw new AssertionError("Unhandled type " + pType);
      }
    } else if (pType instanceof CNumericTypes || pType instanceof CPointerType) {
      return BITVECTOR_TYPE_DEFAULT;
    } else {
      throw new AssertionError("Unhandled type " + pType);
    }
  }

  @Override
  public BitvectorFormula visit(Divide<Value> pDivide) {
    final BitvectorFormula op1 = (BitvectorFormula) pDivide.getNumerator().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pDivide.getDenominator().accept(this);

    return bitvectorFormulaManager.divide(op1, op2, SIGNED);
  }

  @Override
  public BooleanFormula visit(Equal<Value> pEqual) {
    final BitvectorFormula op1 = (BitvectorFormula) pEqual.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pEqual.getOperand2().accept(this);

    return bitvectorFormulaManager.equal(op1, op2);
  }

  @Override
  public BooleanFormula visit(LessThan<Value> pLessThan) {
    final BitvectorFormula op1 = (BitvectorFormula) pLessThan.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pLessThan.getOperand2().accept(this);

    return bitvectorFormulaManager.lessThan(op1, op2, SIGNED);
  }

  @Override
  public BooleanFormula visit(LogicalAnd<Value> pAnd) {
    final BitvectorFormula op1 = (BitvectorFormula) pAnd.getOperand1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pAnd.getOperand2().accept(this);

    return (BooleanFormula) formulaManager.makeAnd(op1, op2);
  }

  @Override
  public BitvectorFormula visit(Exclusion<Value> pExclusion) {
    throw new AssertionError("Unhandled formula type 'Exclusion' for object " + pExclusion);
  }

  @Override
  public BooleanFormula visit(LogicalNot<Value> pNot) {
    final BooleanFormula op = (BooleanFormula) pNot.getNegated().accept(this);

    return formulaManager.getBooleanFormulaManager().not(op);
  }

  @Override
  public BitvectorFormula visit(Modulo<Value> pModulo) {
    final BitvectorFormula op1 = (BitvectorFormula) pModulo.getNumerator().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pModulo.getDenominator().accept(this);

    return bitvectorFormulaManager.modulo(op1, op2, SIGNED);
  }

  @Override
  public BitvectorFormula visit(Multiply<Value> pMultiply) {
    final BitvectorFormula op1 = (BitvectorFormula) pMultiply.getFactor1().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pMultiply.getFactor2().accept(this);

    return bitvectorFormulaManager.multiply(op1, op2);
  }

  @Override
  public BitvectorFormula visit(ShiftLeft<Value> pShiftLeft) {
    final BitvectorFormula op1 = (BitvectorFormula) pShiftLeft.getShifted().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pShiftLeft.getShiftDistance().accept(this);

    return bitvectorFormulaManager.shiftLeft(op1, op2);
  }

  @Override
  public BitvectorFormula visit(ShiftRight<Value> pShiftRight) {
    final BitvectorFormula op1 = (BitvectorFormula) pShiftRight.getShifted().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pShiftRight.getShiftDistance().accept(this);

    return bitvectorFormulaManager.shiftRight(op1, op2, SIGNED);
  }

  @Override
  public BooleanFormula visit(Union<Value> pUnion) {
    throw new AssertionError("Unhandled formula type 'Union' for object " + pUnion);
  }

  @Override
  public Formula visit(Variable<Value> pVariable) {
    return bitvectorFormulaManager.makeVariable(BITVECTOR_TYPE_DEFAULT, pVariable.getName());
  }

  @Override
  public BooleanFormula visit(LessConstraint pConstraint) {
    final FinalFormulaCreator creator = new FinalFormulaCreator() {

      @Override
      public BooleanFormula create(BitvectorFormula pLeft, BitvectorFormula pRight) {
        return bitvectorFormulaManager.lessThan(pLeft, pRight, SIGNED);
      }
    };

    return (BooleanFormula) transformConstraint(pConstraint, creator);
  }

  /**
   * Transforms a given constraint to a {@link Formula}.
   *
   * <p>The type of the resulting formula depends on the given
   * {@link FinalFormulaCreator} object, not the concrete type of the constraint. All other characteristics depend on the
   * constraint, though.
   *
   * @param pConstraint the constraint whose attributes are used for creating the formula
   * @param pCreator the creator that decides what kind of formula is created
   *
   * @return a formula based on the given parameters.
   */
  // This is done so everything around the concrete formula creation does not have to be redundant
  private Formula transformConstraint(Constraint pConstraint, FinalFormulaCreator pCreator) {
    final BitvectorFormula op1 = (BitvectorFormula) pConstraint.getLeftOperand().accept(this);
    final BitvectorFormula op2 = (BitvectorFormula) pConstraint.getRightOperand().accept(this);

    Formula finalFormula = pCreator.create(op1, op2);

    if (!pConstraint.isPositiveConstraint()) {
      finalFormula = createNot(finalFormula);
    }

    return finalFormula;
  }

  private BooleanFormula createNot(Formula pFormula) {
    return (BooleanFormula) formulaManager.makeNot(pFormula);
  }

  @Override
  public BooleanFormula visit(LessOrEqualConstraint pConstraint) {
    final FinalFormulaCreator creator = new FinalFormulaCreator() {

      @Override
      public BooleanFormula create(BitvectorFormula pLeft, BitvectorFormula pRight) {
        return bitvectorFormulaManager.lessOrEquals(pLeft, pRight, SIGNED);
      }
    };

    return (BooleanFormula) transformConstraint(pConstraint, creator);
  }

  @Override
  public BooleanFormula visit(EqualConstraint pConstraint) {
    final FinalFormulaCreator creator = new FinalFormulaCreator() {

      @Override
      public BooleanFormula create(BitvectorFormula pLeft, BitvectorFormula pRight) {
        return bitvectorFormulaManager.equal(pLeft, pRight);
      }
    };

    return (BooleanFormula) transformConstraint(pConstraint, creator);
  }

  @Override
  public Formula visit(ConstraintOperand pConstraintOperand) {
    return pConstraintOperand.getFormula().accept(this);
  }

  /**
   * Creates a new formula based on two given formulas.
   *
   * The created formula depends on the implementation.
   * This interface is used for creating anonymous classes that are given to {@link #transformConstraint}.
   */
  private interface FinalFormulaCreator {
    Formula create(BitvectorFormula pLeft, BitvectorFormula pRight);
  }
}
