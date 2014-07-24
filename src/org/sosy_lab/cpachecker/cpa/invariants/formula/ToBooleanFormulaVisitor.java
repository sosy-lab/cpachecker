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

import java.math.BigInteger;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Instances of this class are compound state invariants visitors used to
 * convert the visited invariants formulae into boolean formulae. This visitor
 * always coexists with an instance of {@link ToFormulaVisitor} for value type
 * formulae.
 */
public class ToBooleanFormulaVisitor<ValueFormulaType> implements ToFormulaVisitor<CompoundInterval, BooleanFormula> {

  /**
   * The boolean formula manager used.
   */
  private final BooleanFormulaManager bfmgr;

  private final MachineModel machineModel;

  private final Map<String, CType> types;

  /**
   * The formula evaluation visitor used to evaluate compound state invariants
   * formulae to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  private final boolean useBitvectors;

  private final ToValueFormulaVisitorProvider<ValueFormulaType> toValueFormulaVisitorProvider;

  public static ToFormulaVisitor<CompoundInterval, BooleanFormula> getVisitor(final FormulaManagerView pFmgr,
        final FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
        boolean pUseBitvectors,
        final MachineModel pMachineModel,
        final Map<String, CType> pVariableTypes) {
    if (pUseBitvectors) {
      return new ToBooleanFormulaVisitor<>(pFmgr, pUseBitvectors, pEvaluationVisitor, pMachineModel, pVariableTypes, new ToValueFormulaVisitorProvider<BitvectorFormula>() {

        @Override
        public ToFormulaVisitor<CompoundInterval, BitvectorFormula> getValueVisitor(Integer pSize, ToBooleanFormulaVisitor<BitvectorFormula> pToBooleanFormulaVisitor) {
          return new ToBitvectorFormulaVisitor(pFmgr, pToBooleanFormulaVisitor, pEvaluationVisitor, pSize, pVariableTypes, pMachineModel);
        }
      });
    } else {
      return new ToBooleanFormulaVisitor<>(pFmgr, pUseBitvectors, pEvaluationVisitor, pMachineModel, pVariableTypes, new ToValueFormulaVisitorProvider<NumeralFormula>() {

        @Override
        public ToFormulaVisitor<CompoundInterval, NumeralFormula> getValueVisitor(Integer pSize,
            ToBooleanFormulaVisitor<NumeralFormula> pToBooleanFormulaVisitor) {
          NumeralFormulaManager<NumeralFormula, ? extends NumeralFormula> mgr = pFmgr.getRationalFormulaManager();
          return new ToNumeralFormulaVisitor<>(pFmgr, mgr, pToBooleanFormulaVisitor, pEvaluationVisitor);
        }
      });
    }

  }

  /**
   * Creates a new visitor for converting compound state invariants formulae to
   * boolean formulae by using the given formula manager, value to formula
   * converter and evaluation visitor.
   *
   * @param pFmgr the formula manager used.
   * @param evaluationVisitor the evaluation visitor used to evaluate compound
   * state invariants formulae to compound states.
   */
  private ToBooleanFormulaVisitor(FormulaManagerView pFmgr,
      boolean pUseBitvectors,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      MachineModel pMachineModel,
      Map<String, CType> pTypes,
      ToValueFormulaVisitorProvider<ValueFormulaType> pToValueFormulaVisitorProvider) {
    this.toValueFormulaVisitorProvider = pToValueFormulaVisitorProvider;
    this.machineModel = pMachineModel;
    this.types = pTypes;
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.evaluationVisitor = pEvaluationVisitor;
    this.useBitvectors = pUseBitvectors;
  }

  /**
   * Gets the corresponding visitor for converting compound state invariants
   * formulae to value type formulae.
   *
   * @return a visitor for converting compound state invariants formulae to
   * value type formulae.
   */
  private ToFormulaVisitor<CompoundInterval, ValueFormulaType> getValueVisitor(InvariantsFormula<CompoundInterval> pFormula) {
    Integer size = ToBitvectorFormulaVisitor.getSize(pFormula, types, machineModel);
    return getValueVisitor(size);
  }

  /**
   * Gets the corresponding visitor for converting compound state invariants
   * formulae to value type formulae.
   *
   * @return a visitor for converting compound state invariants formulae to
   * value type formulae.
   */
  private ToFormulaVisitor<CompoundInterval, ValueFormulaType> getValueVisitor(Integer pSize) {
    return this.toValueFormulaVisitorProvider.getValueVisitor(pSize, this);
  }

  private static interface ToValueFormulaVisitorProvider<ValueFormulaType> {

    ToFormulaVisitor<CompoundInterval, ValueFormulaType> getValueVisitor(Integer pSize, ToBooleanFormulaVisitor<ValueFormulaType> pToBooleanFormulaVisitor);

  }

  /**
   * Interprets the given value invariants formula as a boolean formula or
   * <code>null</code> if it was not possible to interpret the formula as a
   * boolean formula.
   *
   * @param pValueFormula the value invariants formula to interpret.
   * @param pEnvironment the environment used to evaluate the given formula or
   * parts of it in if necessary.
   *
   * @return a boolean interpretation of the given value invariants formula
   * or <code>null</code> if it was not possible to interpret the formula as a
   * boolean formula.
   */
  private @Nullable BooleanFormula fromValueFormula(InvariantsFormula<CompoundInterval> pValueFormula, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    ValueFormulaType valueFormula = pValueFormula.accept(getValueVisitor(pValueFormula), pEnvironment);
    if (valueFormula == null) {
      return evaluateAsBoolean(pValueFormula, pEnvironment);
    }
    /*
     * Zero stands for false, everything else for true, so a value formula
     * r can be represented as a boolean formula as (r != zero) or
     * (!(r == zero)).
     */
    return getValueVisitor(pValueFormula).asBoolean(valueFormula);
  }

  /**
   * Tries to evaluate the given compound state invariants formula as a boolean
   * value, which means that the formula is evaluated using the evaluation
   * visitor and if it represents definitely either <code>true</code> or
   * <code>false</code>, the corresponding boolean formula is returned;
   * otherwise <code>null</code> is returned.
   *
   * @param pValueFormula the compound state invariants formula to evaluate.
   * @param pEnvironment the environment to evaluate the formula in.
   * @return a boolean formula representing the evaluated formula or
   * <code>null</code> if the evaluated formula does not clearly represent
   * either <code>true</code> or <code>false</code>.
   */
  private BooleanFormula evaluateAsBoolean(InvariantsFormula<CompoundInterval> pValueFormula, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval value = pValueFormula.accept(this.evaluationVisitor, pEnvironment);
    if (value.isDefinitelyFalse()) {
      return this.bfmgr.makeBoolean(false);
    }
    if (value.isDefinitelyTrue()) {
      return this.bfmgr.makeBoolean(true);
    }
    return null;
  }

  @Override
  public BooleanFormula visit(Add<CompoundInterval> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pAdd, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryAnd<CompoundInterval> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pAnd, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryNot<CompoundInterval> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pNot, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryOr<CompoundInterval> pOr, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pOr, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryXor<CompoundInterval> pXor, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pXor, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Constant<CompoundInterval> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval value = pConstant.getValue();
    if (value.isDefinitelyTrue()) {
      return this.bfmgr.makeBoolean(true);
    } else if (value.isDefinitelyFalse()) {
      return this.bfmgr.makeBoolean(false);
    }
    int size = 0;
    if (value.hasLowerBound()) {
      size = value.getLowerBound().bitLength();
    }
    if (value.hasUpperBound()) {
      size = Math.max(size, value.getUpperBound().bitLength());
    }
    ValueFormulaType valueFormula = pConstant.accept(getValueVisitor(size), pEnvironment);
    if (valueFormula == null) {
      return null;
    }
    /*
     * Zero stands for false, everything else for true, so a value formula
     * r can be represented as a boolean formula as (r != zero) or
     * (!(r == zero)).
     */
    return getValueVisitor(pConstant).asBoolean(valueFormula);
  }

  @Override
  public BooleanFormula visit(Divide<CompoundInterval> pDivide, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pDivide, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Equal<CompoundInterval> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    Integer size = ToBitvectorFormulaVisitor.getSize(pEqual, types, machineModel);
    if (size == null) {
      if (useBitvectors) {
        return null;
      }
      size = 0;
    }
    ValueFormulaType operand1 = pEqual.getOperand1().accept(getValueVisitor(pEqual), pEnvironment);
    ValueFormulaType operand2 = pEqual.getOperand2().accept(getValueVisitor(pEqual), pEnvironment);
    if (operand1 == null && operand2 == null) {
      return evaluateAsBoolean(pEqual, pEnvironment);
    }
    if (operand1 == null || operand2 == null) {
      final ValueFormulaType left;
      final InvariantsFormula<CompoundInterval> right;
      if (operand1 != null) {
        left = operand1;
        right = pEqual.getOperand2();
      } else {
        left = operand2;
        right = pEqual.getOperand1();
      }
      CompoundInterval rightValue = right.accept(evaluationVisitor, pEnvironment);
      BooleanFormula bf = this.bfmgr.makeBoolean(false);
      for (SimpleInterval interval : rightValue.getIntervals()) {
        BooleanFormula intervalFormula = this.bfmgr.makeBoolean(true);
        if (interval.isSingleton()) {
          ValueFormulaType value = getValueFormula(interval.getLowerBound(), pEnvironment, size);
          intervalFormula = this.bfmgr.and(intervalFormula, getValueVisitor(pEqual).equal(left, value));
        } else {
          if (interval.hasLowerBound()) {
            ValueFormulaType lb = getValueFormula(interval.getLowerBound(), pEnvironment, size);
            intervalFormula = this.bfmgr.and(intervalFormula, getValueVisitor(pEqual).greaterOrEqual(left, lb));
          }
          if (interval.hasUpperBound()) {
            ValueFormulaType ub = getValueFormula(interval.getUpperBound(), pEnvironment, size);
            intervalFormula = this.bfmgr.and(intervalFormula, getValueVisitor(pEqual).lessOrEqual(left, ub));
          }
        }
        bf = this.bfmgr.or(bf, intervalFormula);
      }
      return bf;
    }
    return getValueVisitor(pEqual).equal(operand1, operand2);
  }

  @Override
  public BooleanFormula visit(Exclusion<CompoundInterval> pExclusion,
      Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pExclusion, pEnvironment);
  }

  private ValueFormulaType getValueFormula(BigInteger pValue, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment, int pSize) {
    return CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.singleton(pValue)).accept(getValueVisitor(pSize), pEnvironment);
  }

  @Override
  public BooleanFormula visit(LessThan<CompoundInterval> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    Integer size = ToBitvectorFormulaVisitor.getSize(pLessThan, types, machineModel);
    if (size == null) {
      if (useBitvectors) {
        return null;
      }
      size = 0;
    }
    ValueFormulaType operand1 = pLessThan.getOperand1().accept(getValueVisitor(pLessThan), pEnvironment);
    ValueFormulaType operand2 = pLessThan.getOperand2().accept(getValueVisitor(pLessThan), pEnvironment);
    if (operand1 == null && operand2 == null) {
      return evaluateAsBoolean(pLessThan, pEnvironment);
    }
    if (operand1 == null || operand2 == null) {
      final ValueFormulaType left;
      final InvariantsFormula<CompoundInterval> right;
      final boolean lessThan;
      if (operand1 != null) {
        left = operand1;
        right = pLessThan.getOperand2();
        lessThan = true;
      } else {
        left = operand2;
        right = pLessThan.getOperand1();
        lessThan = false;
      }
      CompoundInterval rightValue = right.accept(evaluationVisitor, pEnvironment);
      if (rightValue.isBottom()) {
        return this.bfmgr.makeBoolean(false);
      }
      if (lessThan) {
        if (rightValue.hasUpperBound()) {
          return getValueVisitor(pLessThan).lessThan(left, getValueFormula(rightValue.getUpperBound(), pEnvironment, size));
        }
      } else {
        if (rightValue.hasLowerBound()) {
          return getValueVisitor(pLessThan).greaterThan(left, getValueFormula(rightValue.getLowerBound(), pEnvironment, size));
        }
      }
      return null;
    }
    return getValueVisitor(pLessThan).lessThan(operand1, operand2);
  }

  @Override
  public BooleanFormula visit(LogicalAnd<CompoundInterval> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    BooleanFormula operand1 = pAnd.getOperand1().accept(this, pEnvironment);
    BooleanFormula operand2 = pAnd.getOperand2().accept(this, pEnvironment);
    if (operand1 == null || operand2 == null) {
      return evaluateAsBoolean(pAnd, pEnvironment);
    }
    return this.bfmgr.and(operand1, operand2);
  }

  @Override
  public BooleanFormula visit(LogicalNot<CompoundInterval> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    BooleanFormula operand = pNot.getNegated().accept(this, pEnvironment);
    if (operand == null) {
      return evaluateAsBoolean(pNot, pEnvironment);
    }
    return this.bfmgr.not(operand);
  }

  @Override
  public BooleanFormula visit(Modulo<CompoundInterval> pModulo, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pModulo, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Multiply<CompoundInterval> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pMultiply, pEnvironment);
  }

  @Override
  public BooleanFormula visit(ShiftLeft<CompoundInterval> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pShiftLeft, pEnvironment);
  }

  @Override
  public BooleanFormula visit(ShiftRight<CompoundInterval> pShiftRight, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pShiftRight, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Union<CompoundInterval> pUnion, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pUnion, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Variable<CompoundInterval> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromValueFormula(pVariable, pEnvironment);
  }

  @Override
  public BooleanFormula lessThan(BooleanFormula pOp1, BooleanFormula pOp2) {
    return this.bfmgr.and(this.bfmgr.not(pOp1), pOp2);
  }

  @Override
  public BooleanFormula equal(BooleanFormula pOp1, BooleanFormula pOp2) {
    return this.bfmgr.equivalence(pOp1, pOp2);
  }

  @Override
  public BooleanFormula greaterThan(BooleanFormula pOp1, BooleanFormula pOp2) {
    return this.bfmgr.and(pOp1, this.bfmgr.not(pOp2));
  }

  @Override
  public BooleanFormula lessOrEqual(BooleanFormula pOp1, BooleanFormula pOp2) {
    return this.bfmgr.not(greaterThan(pOp1, pOp2));
  }

  @Override
  public BooleanFormula greaterOrEqual(BooleanFormula pOp1, BooleanFormula pOp2) {
    return this.bfmgr.not(lessThan(pOp1, pOp2));
  }

  @Override
  public BooleanFormula asBoolean(BooleanFormula pOp1) {
    return pOp1;
  }

}
