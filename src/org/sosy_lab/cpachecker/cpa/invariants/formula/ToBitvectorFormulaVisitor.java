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

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

import java.math.BigInteger;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Instances of this class are compound state invariants visitors used to
 * convert the visited invariants formulae into bit vector formulae.
 */
public class ToBitvectorFormulaVisitor implements
    ParameterizedNumeralFormulaVisitor<CompoundInterval, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>, BitvectorFormula>,
    ParameterizedBooleanFormulaVisitor<CompoundInterval, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>>, BooleanFormula> {

  /**
   * The boolean formula manager used.
   */
  private final BooleanFormulaManager bfmgr;

  /**
   * The bit vector formula manager used.
   */
  private final BitvectorFormulaManager bvfmgr;

  /**
   * The formula evaluation visitor used to evaluate compound state invariants
   * formulae to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  /**
   * Creates a new visitor for converting compound state invariants formulae to
   * bit vector formulae by using the given formula manager, and evaluation visitor.
   *
   * @param pFmgr the formula manager used.
   * @param pEvaluationVisitor the formula evaluation visitor used to evaluate
   * compound state invariants formulae to compound states.
   */
  public ToBitvectorFormulaVisitor(FormulaManagerView pFmgr,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.bvfmgr = pFmgr.getBitvectorFormulaManager();
    this.evaluationVisitor = pEvaluationVisitor;
  }

  /**
   * Evaluates the given compound state invariants formula and tries to convert
   * the resulting value into a bit vector formula.
   *
   * @param pFormula the formula to evaluate.
   * @param pEnvironment the environment to evaluate the formula in.
   *
   * @return a bit vector formula representing the evaluation of the given
   * formula or <code>null</code> if the evaluation of the given formula could
   * not be represented as a bit vector formula.
   */
  private @Nullable BitvectorFormula evaluate(NumeralFormula<CompoundInterval> pFormula, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval intervals = pFormula.accept(this.evaluationVisitor, pEnvironment);
    if (intervals.isSingleton()) {
      TypeInfo typeInfo = pFormula.getTypeInfo();
      if (typeInfo instanceof BitVectorInfo) {
        return asBitVectorFormula(pFormula.getTypeInfo(), intervals.getValue());
      }
    }
    return null;
  }

  /**
   * Encodes the given value as a bit vector formula using the given bit vector
   * information.
   *
   * @param pTypeInfo the bit vector information.
   * @param pValue the value.
   *
   * @return a bit vector formula representing the given value as a bit vector
   * with the given size.
   */
  private BitvectorFormula asBitVectorFormula(TypeInfo pTypeInfo, Number pValue) {
    if (!(pTypeInfo instanceof BitVectorInfo && pValue instanceof BigInteger)) {
      return null;
    }
    int size = ((BitVectorInfo) pTypeInfo).getSize();
    BigInteger value = (BigInteger) pValue;
    // Get only the [size] least significant bits
    BigInteger upperExclusive = BigInteger.valueOf(2).pow(size);
    boolean negative = value.signum() < 0;
    if (negative && !value.equals(upperExclusive.shiftRight(1).negate())) {
      value = value.negate();
      value = value.and(BigInteger.valueOf(2).pow(size - 1).subtract(BigInteger.valueOf(1))).negate();
    } else if (!negative) {
      value = value.and(BigInteger.valueOf(2).pow(size).subtract(BigInteger.valueOf(1)));
    }
    return this.bvfmgr.makeBitvector(size, value);
  }

  @Override
  public BitvectorFormula visit(Add<CompoundInterval> pAdd, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BitvectorFormula summand1 = pAdd.getSummand1().accept(this, pEnvironment);
    BitvectorFormula summand2 = pAdd.getSummand2().accept(this, pEnvironment);
    if (summand1 == null || summand2 == null) {
      return evaluate(pAdd, pEnvironment);
    }
    return this.bvfmgr.add(summand1, summand2);
  }

  @Override
  public BitvectorFormula visit(BinaryAnd<CompoundInterval> pAnd, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pAnd, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(BinaryNot<CompoundInterval> pNot, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pNot, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(BinaryOr<CompoundInterval> pOr, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pOr, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(BinaryXor<CompoundInterval> pXor, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pXor, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(Constant<CompoundInterval> pConstant, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pConstant, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(Divide<CompoundInterval> pDivide, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BitvectorFormula numerator = pDivide.getNumerator().accept(this, pEnvironment);
    BitvectorFormula denominator = pDivide.getDenominator().accept(this, pEnvironment);
    if (numerator == null || denominator == null) {
      return evaluate(pDivide, pEnvironment);
    }
    return this.bvfmgr.divide(numerator, denominator, true);
  }

  @Override
  public BitvectorFormula visit(Exclusion<CompoundInterval> pExclusion,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pExclusion, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(Modulo<CompoundInterval> pModulo, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BitvectorFormula numerator = pModulo.getNumerator().accept(this, pEnvironment);
    BitvectorFormula denominator = pModulo.getDenominator().accept(this, pEnvironment);
    if (numerator == null || denominator == null) {
      return evaluate(pModulo, pEnvironment);
    }
    return this.bvfmgr.modulo(numerator, denominator, true);
  }

  @Override
  public BitvectorFormula visit(Multiply<CompoundInterval> pMultiply, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BitvectorFormula factor1 = pMultiply.getFactor1().accept(this, pEnvironment);
    BitvectorFormula factor2 = pMultiply.getFactor2().accept(this, pEnvironment);
    if (factor1 == null || factor2 == null) {
      return evaluate(pMultiply, pEnvironment);
    }
    return this.bvfmgr.multiply(factor1, factor2);
  }

  @Override
  public BitvectorFormula visit(ShiftLeft<CompoundInterval> pShiftLeft, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pShiftLeft, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(ShiftRight<CompoundInterval> pShiftRight, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pShiftRight, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(Union<CompoundInterval> pUnion, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pUnion, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(Variable<CompoundInterval> pVariable, Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    TypeInfo typeInfo = pVariable.getTypeInfo();
    if (typeInfo instanceof BitVectorInfo) {
      return this.bvfmgr.makeVariable(
          ((BitVectorInfo) typeInfo).getSize(), pVariable.getMemoryLocation().getAsSimpleString());
    }
    return null;
  }

  @Override
  public BitvectorFormula visit(Cast<CompoundInterval> pCast,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BitvectorFormula sourceFormula = pCast.getCasted().accept(this, pEnvironment);
    if (sourceFormula == null) {
      return sourceFormula;
    }
    TypeInfo sourceInfo = pCast.getCasted().getTypeInfo();
    TypeInfo targetInfo = pCast.getTypeInfo();
    if (sourceInfo instanceof BitVectorInfo && targetInfo instanceof BitVectorInfo) {
      int sourceSize = ((BitVectorInfo) sourceInfo).getSize();
      int targetSize = ((BitVectorInfo) targetInfo).getSize();
      if (sourceSize < targetSize) {
        return bvfmgr.extend(sourceFormula, targetSize - sourceSize, targetInfo.isSigned());
      }
      return bvfmgr.extract(sourceFormula, targetSize - 1, 0, targetInfo.isSigned());
    }
    return null;
  }

  @Override
  public BitvectorFormula visit(IfThenElse<CompoundInterval> pIfThenElse,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    BooleanConstant<CompoundInterval> conditionEval = pIfThenElse.getCondition().accept(evaluationVisitor, pEnvironment);
    if (BooleanConstant.isTrue(conditionEval)) {
      return pIfThenElse.getPositiveCase().accept(this, pEnvironment);
    }
    if (BooleanConstant.isFalse(conditionEval)) {
      return pIfThenElse.getNegativeCase().accept(this, pEnvironment);
    }

    BooleanFormula conditionFormula = pIfThenElse.getCondition().accept(this, pEnvironment);
    if (conditionFormula == null) {
      return InvariantsFormulaManager.INSTANCE.union(pIfThenElse.getPositiveCase(), pIfThenElse.getNegativeCase()).accept(this, pEnvironment);
    }

    BitvectorFormula positiveCaseFormula = pIfThenElse.getPositiveCase().accept(this, pEnvironment);
    if (positiveCaseFormula == null) {
      return null;
    }
    BitvectorFormula negativeCaseFormula = pIfThenElse.getNegativeCase().accept(this, pEnvironment);
    if (negativeCaseFormula == null) {
      return null;
    }
    return this.bfmgr.ifThenElse(
        conditionFormula,
        positiveCaseFormula,
        negativeCaseFormula);
  }

  @Override
  public BooleanFormula visit(Equal<CompoundInterval> pEqual,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    TypeInfo typeInfo = pEqual.getOperand1().getTypeInfo();
    BitvectorFormula operand1 = pEqual.getOperand1().accept(this, pEnvironment);
    BitvectorFormula operand2 = pEqual.getOperand2().accept(this, pEnvironment);
    if (operand1 == null && operand2 == null) {
      return null;
    }
    if (operand1 == null || operand2 == null) {
      final BitvectorFormula left;
      final NumeralFormula<CompoundInterval> right;
      if (operand1 != null) {
        left = operand1;
        right = pEqual.getOperand2();
      } else {
        left = operand2;
        right = pEqual.getOperand1();
      }
      CompoundInterval rightValue = right.accept(evaluationVisitor, pEnvironment);
      BooleanFormula bf = bfmgr.makeFalse();
      for (SimpleInterval interval : rightValue.getIntervals()) {
        BooleanFormula intervalFormula = bfmgr.makeTrue();
        if (interval.isSingleton()) {
          BitvectorFormula value = asBitVectorFormula(typeInfo, interval.getLowerBound());
          intervalFormula = bfmgr.and(intervalFormula, bvfmgr.equal(left, value));
        } else {
          if (interval.hasLowerBound()) {
            BitvectorFormula lb = asBitVectorFormula(typeInfo, interval.getLowerBound());
            intervalFormula =
                bfmgr.and(intervalFormula, bvfmgr.greaterOrEquals(left, lb, typeInfo.isSigned()));
          }
          if (interval.hasUpperBound()) {
            BitvectorFormula ub = asBitVectorFormula(typeInfo, interval.getUpperBound());
            intervalFormula =
                bfmgr.and(intervalFormula, bvfmgr.lessOrEquals(left, ub, typeInfo.isSigned()));
          }
        }
        bf = bfmgr.or(bf, intervalFormula);
      }
      return bf;
    }
    return bvfmgr.equal(operand1, operand2);
  }

  @Override
  public BooleanFormula visit(LessThan<CompoundInterval> pLessThan,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    TypeInfo typeInfo = pLessThan.getOperand1().getTypeInfo();
    BitvectorFormula operand1 = pLessThan.getOperand1().accept(this, pEnvironment);
    BitvectorFormula operand2 = pLessThan.getOperand2().accept(this, pEnvironment);
    if (operand1 == null && operand2 == null) {
      return null;
    }
    if (operand1 == null || operand2 == null) {
      final BitvectorFormula left;
      final NumeralFormula<CompoundInterval> right;
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
        return bfmgr.makeFalse();
      }
      if (lessThan) {
        if (rightValue.hasUpperBound()) {
          return bvfmgr.lessThan(
              left, asBitVectorFormula(typeInfo, rightValue.getUpperBound()), typeInfo.isSigned());
        }
      } else if (rightValue.hasLowerBound()) {
        return bvfmgr.greaterThan(
            left, asBitVectorFormula(typeInfo, rightValue.getLowerBound()), typeInfo.isSigned());
      }
      return null;
    }
    return bvfmgr.lessThan(operand1, operand2, typeInfo.isSigned());
  }

  @Override
  public BooleanFormula visit(LogicalAnd<CompoundInterval> pAnd,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return this.bfmgr.and(
        pAnd.getOperand1().accept(this, pEnvironment),
        pAnd.getOperand2().accept(this, pEnvironment));
  }

  @Override
  public BooleanFormula visit(LogicalNot<CompoundInterval> pNot,
      Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return this.bfmgr.not(pNot.getNegated().accept(this, pEnvironment));
  }

  @Override
  public BooleanFormula visitFalse(Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return bfmgr.makeFalse();
  }

  @Override
  public BooleanFormula visitTrue(Map<? extends MemoryLocation, ? extends NumeralFormula<CompoundInterval>> pEnvironment) {
    return bfmgr.makeTrue();
  }

}
