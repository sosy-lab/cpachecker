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
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaVisitor;
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
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicFormula;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicIdentifier;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Creator for {@link BooleanFormula}s.
 */
public class BooleanFormulaCreator
    implements ConstraintVisitor<Formula>, InvariantsFormulaVisitor<Value, Formula> {

  private static final boolean SIGNED = true;

  private FormulaManagerView formulaManager;

  public BooleanFormulaCreator(FormulaManagerView pFormulaManager) {
    formulaManager = pFormulaManager;
  }

  @Override
  public Formula visit(Add<Value> pAdd) {
    final Formula op1 = pAdd.getSummand1().accept(this);
    final Formula op2 = pAdd.getSummand2().accept(this);

    return formulaManager.makePlus(op1, op2);
  }

  @Override
  public Formula visit(BinaryAnd<Value> pAnd) {
    final Formula op1 = pAnd.getOperand1().accept(this);
    final Formula op2 = pAnd.getOperand2().accept(this);

    return formulaManager.makeAnd(op1, op2);
  }

  @Override
  public Formula visit(BinaryNot<Value> pNot) {
    final Formula op = pNot.getFlipped().accept(this);

    return formulaManager.makeNot(op);
  }

  @Override
  public Formula visit(BinaryOr<Value> pOr) {
    final Formula op1 = pOr.getOperand1().accept(this);
    final Formula op2 = pOr.getOperand2().accept(this);

    return formulaManager.makeOr(op1, op2);
  }

  @Override
  public Formula visit(BinaryXor<Value> pXor) {
    final Formula op1 = pXor.getOperand1().accept(this);
    final Formula op2 = pXor.getOperand2().accept(this);

    return formulaManager.makeXor(op1, op2);
  }

  @Override
  public Formula visit(Constant<Value> pConstant) {
    Value constantValue = pConstant.getValue();

    BooleanFormulaManagerView booleanFormulaManager = formulaManager.getBooleanFormulaManager();

    if (constantValue.isNumericValue()) {
      return formulaManager.makeNumber(FormulaType.BooleanType, ((NumericValue) constantValue).longValue());

    } else if (constantValue instanceof BooleanValue) {
      return booleanFormulaManager.makeBoolean(((BooleanValue) constantValue).isTrue());

    } else if (constantValue instanceof SymbolicIdentifier) {
      FormulaType<?> formulaType = getFormulaType(((SymbolicIdentifier) constantValue).getType());
      Formula formula = formulaManager.makeVariable(formulaType, constantValue.toString());
      return formula;
    } else if (constantValue instanceof SymbolicFormula) {
      // TODO
    }

    return null;
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
          return FormulaType.IntegerType;
        case FLOAT:
        case DOUBLE:
          return FormulaType.RationalType;
        default:
          throw new AssertionError("Unhandled type " + pType);
      }
    } else if (pType instanceof CSimpleType) {
      switch (((CSimpleType) pType).getType()) {
        case BOOL:
        case CHAR:
        case INT:
          return FormulaType.IntegerType;
        case FLOAT:
        case DOUBLE:
          return FormulaType.RationalType;
        default:
          throw new AssertionError("Unhandled type " + pType);
      }
    } else if (pType instanceof CNumericTypes || pType instanceof CPointerType) {
      return FormulaType.IntegerType;
    } else {
      throw new AssertionError("Unhandled type " + pType);
    }
  }

  @Override
  public Formula visit(Divide<Value> pDivide) {
    final Formula op1 = pDivide.getNumerator().accept(this);
    final Formula op2 = pDivide.getDenominator().accept(this);

    return formulaManager.makeDivide(op1, op2, SIGNED);
  }

  @Override
  public BooleanFormula visit(Equal<Value> pEqual) {
    final Formula op1 = pEqual.getOperand1().accept(this);
    final Formula op2 = pEqual.getOperand2().accept(this);

    return formulaManager.makeEqual(op1, op2);
  }

  @Override
  public BooleanFormula visit(LessThan<Value> pLessThan) {
    final Formula op1 = pLessThan.getOperand1().accept(this);
    final Formula op2 = pLessThan.getOperand2().accept(this);

    return formulaManager.makeEqual(op1, op2);
  }

  @Override
  public Formula visit(LogicalAnd<Value> pAnd) {
    final Formula op1 = pAnd.getOperand1().accept(this);
    final Formula op2 = pAnd.getOperand2().accept(this);

    return formulaManager.makeAnd(op1, op2);
  }

  @Override
  public Formula visit(Exclusion<Value> pExclusion) {
    throw new AssertionError("Unhandled formula type 'Exclusion' for object " + pExclusion);
  }

  @Override
  public Formula visit(LogicalNot<Value> pNot) {
    final Formula op = pNot.getNegated().accept(this);

    return formulaManager.makeNot(op);
  }

  @Override
  public Formula visit(Modulo<Value> pModulo) {
    final Formula op1 = pModulo.getNumerator().accept(this);
    final Formula op2 = pModulo.getDenominator().accept(this);

    return formulaManager.makeModulo(op1, op2, SIGNED);
  }

  @Override
  public Formula visit(Multiply<Value> pMultiply) {
    final Formula op1 = pMultiply.getFactor1().accept(this);
    final Formula op2 = pMultiply.getFactor2().accept(this);

    return formulaManager.makeMultiply(op1, op2);
  }

  @Override
  public Formula visit(ShiftLeft<Value> pShiftLeft) {
    final Formula op1 = pShiftLeft.getShifted().accept(this);
    final Formula op2 = pShiftLeft.getShiftDistance().accept(this);

    return formulaManager.makeShiftLeft(op1, op2);
  }

  @Override
  public Formula visit(ShiftRight<Value> pShiftRight) {
    final Formula op1 = pShiftRight.getShifted().accept(this);
    final Formula op2 = pShiftRight.getShiftDistance().accept(this);

    return formulaManager.makeShiftRight(op1, op2, SIGNED);
  }

  @Override
  public BooleanFormula visit(Union<Value> pUnion) {
    throw new AssertionError("Unhandled formula type 'Union' for object " + pUnion);
  }

  @Override
  public BooleanFormula visit(Variable<Value> pVariable) {
    return formulaManager.makeVariable(FormulaType.BooleanType, pVariable.getName());
  }

  @Override
  public Formula visit(LessConstraint pConstraint) {
    final FinalFormulaCreator creator = new FinalFormulaCreator() {

      @Override
      public Formula create(Formula pLeft, Formula pRight) {
        return formulaManager.makeLessThan(pLeft, pRight, SIGNED);
      }
    };

    return transformConstraint(pConstraint, creator);
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
    final Formula op1 = pConstraint.getLeftOperand().accept(this);
    final Formula op2 = pConstraintf.getRightOperand().accept(this);

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
  public Formula visit(LessOrEqualConstraint pConstraint) {
    final FinalFormulaCreator creator = new FinalFormulaCreator() {

      @Override
      public Formula create(Formula pLeft, Formula pRight) {
        return formulaManager.makeLessOrEqual(pLeft, pRight, SIGNED);
      }
    };

    return transformConstraint(pConstraint, creator);
  }

  @Override
  public Formula visit(EqualConstraint pConstraint) {
    final FinalFormulaCreator creator = new FinalFormulaCreator() {

      @Override
      public Formula create(Formula pLeft, Formula pRight) {
        return formulaManager.makeEqual(pLeft, pRight);
      }
    };

    return transformConstraint(pConstraint, creator);
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
    Formula create(Formula pLeft, Formula pRight);
  }
}
