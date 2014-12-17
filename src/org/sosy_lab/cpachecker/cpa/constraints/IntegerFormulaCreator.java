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
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;

/**
 * Creator for {@link Formula}s using only integer values.
 */
public class IntegerFormulaCreator implements FormulaCreator<Formula> {

  private static final boolean SIGNED = true;

  private final FormulaManagerView formulaManager;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> numeralFormulaManager;
  private final BooleanFormulaManagerView booleanFormulaManager;

  public IntegerFormulaCreator(FormulaManagerView pFormulaManager) {
    formulaManager = pFormulaManager;
    numeralFormulaManager = formulaManager.getIntegerFormulaManager();
    booleanFormulaManager = formulaManager.getBooleanFormulaManager();
  }


  @Override
  public IntegerFormula visit(Add<Value> pAdd) {
    final IntegerFormula op1 = (IntegerFormula) pAdd.getSummand1().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pAdd.getSummand2().accept(this);

    return numeralFormulaManager.add(op1, op2);
  }

  @Override
  public IntegerFormula visit(BinaryAnd<Value> pAnd) {
    return handleUnsupportedFormula(pAnd);
  }

  private IntegerFormula handleUnsupportedFormula(InvariantsFormula<Value> pFormula) {
    return formulaManager.makeVariable(FormulaType.IntegerType, getVariableNameByFormula(pFormula));
  }

  private String getVariableNameByFormula(InvariantsFormula<Value> pFormula) {
    return pFormula.toString();
  }

  @Override
  public IntegerFormula visit(BinaryNot<Value> pNot) {
    return handleUnsupportedFormula(pNot);
  }

  @Override
  public IntegerFormula visit(BinaryOr<Value> pOr) {
    return handleUnsupportedFormula(pOr);
  }

  @Override
  public IntegerFormula visit(BinaryXor<Value> pXor) {
    return handleUnsupportedFormula(pXor);
  }

  @Override
  public Formula visit(Constant<Value> pConstant) {
    Value value = pConstant.getValue();

    if (value.isNumericValue()) {
      NumericValue valueAsNumeric = (NumericValue) value;
      long longValue = valueAsNumeric.longValue();
      double doubleValue = valueAsNumeric.doubleValue();

      if (doubleValue % 1 == 0 && longValue == doubleValue) {
        return numeralFormulaManager.makeNumber(valueAsNumeric.longValue());
      } else {
        return handleUnsupportedFormula(pConstant);
      }

    } else if (value instanceof BooleanValue) {
      return formulaManager.getBooleanFormulaManager().makeBoolean(((BooleanValue) value).isTrue());

    } else if (value instanceof SymbolicValue) {
      return ((SymbolicValue) value).accept(this);
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
          return FormulaType.IntegerType;
        default:
          throw new AssertionError("Unhandled type " + pType);
      }
    } else if (pType instanceof CSimpleType) {
      switch (((CSimpleType) pType).getType()) {
        case BOOL:
        case CHAR:
        case INT:
        case FLOAT:
        case DOUBLE:
        case UNSPECIFIED:
          return FormulaType.IntegerType;
        default:
          throw new AssertionError("Unhandled type " + pType);
      }
    } else if (pType instanceof CPointerType) {
      return FormulaType.IntegerType;
    } else {
      throw new AssertionError("Unhandled type " + pType);
    }
  }

  @Override
  public IntegerFormula visit(Divide<Value> pDivide) {
    final IntegerFormula op1 = (IntegerFormula) pDivide.getNumerator().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pDivide.getDenominator().accept(this);

    return numeralFormulaManager.divide(op1, op2);
  }

  @Override
  public BooleanFormula visit(Equal<Value> pEqual) {
    final IntegerFormula op1 = (IntegerFormula) pEqual.getOperand1().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pEqual.getOperand2().accept(this);

    return numeralFormulaManager.equal(op1, op2);
  }

  @Override
  public BooleanFormula visit(LessThan<Value> pLessThan) {
    final IntegerFormula op1 = (IntegerFormula) pLessThan.getOperand1().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pLessThan.getOperand2().accept(this);

    return numeralFormulaManager.lessThan(op1, op2);
  }

  @Override
  public BooleanFormula visit(LogicalAnd<Value> pAnd) {
    final BooleanFormula op1 = (BooleanFormula) pAnd.getOperand1().accept(this);
    final BooleanFormula op2 = (BooleanFormula) pAnd.getOperand2().accept(this);

    return booleanFormulaManager.and(op1, op2);
  }

  @Override
  public IntegerFormula visit(Exclusion<Value> pExclusion) {
    throw new AssertionError("Unhandled formula type 'Exclusion' for object " + pExclusion);
  }

  @Override
  public BooleanFormula visit(LogicalNot<Value> pNot) {
    final BooleanFormula op = (BooleanFormula) pNot.getNegated().accept(this);

    return formulaManager.getBooleanFormulaManager().not(op);
  }

  @Override
  public IntegerFormula visit(Modulo<Value> pModulo) {
    final IntegerFormula op1 = (IntegerFormula) pModulo.getNumerator().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pModulo.getDenominator().accept(this);

    return numeralFormulaManager.modulo(op1, op2);
  }

  @Override
  public IntegerFormula visit(Multiply<Value> pMultiply) {
    final IntegerFormula op1 = (IntegerFormula) pMultiply.getFactor1().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pMultiply.getFactor2().accept(this);

    return numeralFormulaManager.multiply(op1, op2);
  }

  @Override
  public IntegerFormula visit(ShiftLeft<Value> pShiftLeft) {
    return handleUnsupportedFormula(pShiftLeft);
  }

  @Override
  public IntegerFormula visit(ShiftRight<Value> pShiftRight) {
    return handleUnsupportedFormula(pShiftRight);
  }

  @Override
  public BooleanFormula visit(Union<Value> pUnion) {
    throw new AssertionError("Unhandled formula type 'Union' for object " + pUnion);
  }

  @Override
  public Formula visit(Variable<Value> pVariable) {
    return numeralFormulaManager.makeVariable(pVariable.getName());
  }

  @Override
  public BooleanFormula visit(LessConstraint pConstraint) {
    final FinalFormulaCreator creator = new FinalFormulaCreator() {

      @Override
      public BooleanFormula create(IntegerFormula pLeft, IntegerFormula pRight) {
        return numeralFormulaManager.lessThan(pLeft, pRight);
      }
    };

    return (BooleanFormula) transformConstraint(pConstraint, creator);
  }

  /**
   * Transforms a given constraint to a {@link org.sosy_lab.cpachecker.util.predicates.interfaces.Formula}.
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
    final IntegerFormula op1 = (IntegerFormula) pConstraint.getLeftOperand().accept(this);
    final IntegerFormula op2 = (IntegerFormula) pConstraint.getRightOperand().accept(this);

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
      public BooleanFormula create(IntegerFormula pLeft, IntegerFormula pRight) {
        return numeralFormulaManager.lessOrEquals(pLeft, pRight);
      }
    };

    return (BooleanFormula) transformConstraint(pConstraint, creator);
  }

  @Override
  public BooleanFormula visit(EqualConstraint pConstraint) {
    final FinalFormulaCreator creator = new FinalFormulaCreator() {

      @Override
      public BooleanFormula create(IntegerFormula pLeft, IntegerFormula pRight) {
        return numeralFormulaManager.equal(pLeft, pRight);
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
    Formula create(IntegerFormula pLeft, IntegerFormula pRight);
  }
}
