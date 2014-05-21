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
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Instances of this class are compound state invariants visitors used to
 * convert the visited invariants formulae into bit vector formulae.
 * This visitor always coexists with an instance of
 * {@link ToBooleanFormulaVisitor}, which should also be used to obtain an
 * instance of this visitor.
 */
public class ToBitvectorFormulaVisitor implements ToFormulaVisitor<CompoundInterval, BitvectorFormula> {

  /**
   * The boolean formula manager used.
   */
  private final BooleanFormulaManager bfmgr;

  /**
   * The bit vector formula manager used.
   */
  private final BitvectorFormulaManager bvfmgr;

  /**
   * The corresponding compound state invariants formula visitor used to
   * convert visited formulae into boolean formulae.
   */
  private final ToFormulaVisitor<CompoundInterval, BooleanFormula> toBooleanFormulaVisitor;

  /**
   * The formula evaluation visitor used to evaluate compound state invariants
   * formulae to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  private final int size;

  /**
   * Creates a new visitor for converting compound state invariants formulae to
   * bit vector formulae by using the given formula manager,
   * {@link ToBooleanFormulaVisitor} and evaluation visitor.
   *
   * @param pFmgr the formula manager used.
   * @param pToBooleanFormulaVisitor the compound state invariants formula
   * visitor used to convert invariants formulae to boolean formulae.
   * @param pEvaluationVisitor the formula evaluation visitor used to evaluate
   * compound state invariants formulae to compound states.
   * @param pSize the bit vector size.
   */
  ToBitvectorFormulaVisitor(FormulaManagerView pFmgr,
      ToFormulaVisitor<CompoundInterval, BooleanFormula> pToBooleanFormulaVisitor,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor,
      int pSize) {
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.bvfmgr = pFmgr.getBitvectorFormulaManager();
    this.toBooleanFormulaVisitor = pToBooleanFormulaVisitor;
    this.evaluationVisitor = pEvaluationVisitor;
    this.size = pSize;
  }

  private BitvectorFormula makeVariable(String pVariableName) {
    return this.bvfmgr.makeVariable(size, pVariableName);
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
  private @Nullable BitvectorFormula evaluate(InvariantsFormula<CompoundInterval> pFormula, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval value = pFormula.accept(this.evaluationVisitor, pEnvironment);

    if (value.isSingleton()) {
      return this.bvfmgr.makeBitvector(size, value.getLowerBound().longValue());
    }
    return null;
  }

  /**
   * Interprets the given compound state invariants formula as a boolean
   * formula and then reinterprets the boolean formula as a bit vector formula.
   *
   * @param pBooleanFormula the compound state invariants formula to interpret
   * as a boolean formula.
   * @param pEnvironment the environment to perform formula evaluations in if
   * they are required.
   *
   * @return a bit vector formula representing the bit vector re-interpretation
   * of the boolean formula resulting from interpreting the given formula as a
   * boolean invariants formula or <code>null</code> if any of those
   * interpretations fail.
   */
  private @Nullable BitvectorFormula fromBooleanFormula(InvariantsFormula<CompoundInterval> pBooleanFormula, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromBooleanFormula(pBooleanFormula.accept(this.toBooleanFormulaVisitor, pEnvironment));
  }

  /**
   * Interprets the given boolean formula as a bit vector formula by defining
   * that <code>true</code> is <code>1</code> and <code>false</code> is
   * <code>0</code>, so for any given non-<code>null</code> boolean formula
   * <code>b</code> the result is the bit vector formula equivalent of the
   * expression <code>b ? 1 : 0</code>.
   *
   * @param pBooleanFormula
   *
   * @return a bit vector formula representing the bit vector interpretation of
   * the given boolean formula or <code>null</code> if the given boolean
   * formula is <code>null</code>.
   */
  private @Nullable BitvectorFormula fromBooleanFormula(@Nullable BooleanFormula pBooleanFormula) {
    if (pBooleanFormula == null) {
      return null;
    }
    return this.bfmgr.ifThenElse(pBooleanFormula, this.bvfmgr.makeBitvector(this.size, BigInteger.ONE), this.bvfmgr.makeBitvector(this.size, BigInteger.ZERO));
  }

  @Override
  public BitvectorFormula visit(Add<CompoundInterval> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    BitvectorFormula summand1 = pAdd.getSummand1().accept(this, pEnvironment);
    BitvectorFormula summand2 = pAdd.getSummand2().accept(this, pEnvironment);
    if (summand1 == null || summand2 == null) {
      return evaluate(pAdd, pEnvironment);
    }
    return this.bvfmgr.add(summand1, summand2);
  }

  @Override
  public BitvectorFormula visit(BinaryAnd<CompoundInterval> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pAnd, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(BinaryNot<CompoundInterval> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pNot, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(BinaryOr<CompoundInterval> pOr, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pOr, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(BinaryXor<CompoundInterval> pXor, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pXor, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(Constant<CompoundInterval> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pConstant, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(Divide<CompoundInterval> pDivide, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    BitvectorFormula numerator = pDivide.getNumerator().accept(this, pEnvironment);
    BitvectorFormula denominator = pDivide.getDenominator().accept(this, pEnvironment);
    if (numerator == null || denominator == null) {
      return evaluate(pDivide, pEnvironment);
    }
    return this.bvfmgr.divide(numerator, denominator, true);
  }

  @Override
  public BitvectorFormula visit(Equal<CompoundInterval> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromBooleanFormula(pEqual, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(LessThan<CompoundInterval> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromBooleanFormula(pLessThan, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(LogicalAnd<CompoundInterval> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromBooleanFormula(pAnd, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(LogicalNot<CompoundInterval> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromBooleanFormula(pNot, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(Modulo<CompoundInterval> pModulo, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    BitvectorFormula numerator = pModulo.getNumerator().accept(this, pEnvironment);
    BitvectorFormula denominator = pModulo.getDenominator().accept(this, pEnvironment);
    if (numerator == null || denominator == null) {
      return evaluate(pModulo, pEnvironment);
    }
    return this.bvfmgr.modulo(numerator, denominator, true);
  }

  @Override
  public BitvectorFormula visit(Multiply<CompoundInterval> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    BitvectorFormula factor1 = pMultiply.getFactor1().accept(this, pEnvironment);
    BitvectorFormula factor2 = pMultiply.getFactor2().accept(this, pEnvironment);
    if (factor1 == null || factor2 == null) {
      return evaluate(pMultiply, pEnvironment);
    }
    return this.bvfmgr.multiply(factor1, factor2);
  }

  @Override
  public BitvectorFormula visit(ShiftLeft<CompoundInterval> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pShiftLeft, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(ShiftRight<CompoundInterval> pShiftRight, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pShiftRight, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(Union<CompoundInterval> pUnion, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pUnion, pEnvironment);
  }

  @Override
  public BitvectorFormula visit(Variable<CompoundInterval> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return makeVariable(pVariable.getName());
  }

  @Override
  public BooleanFormula lessThan(BitvectorFormula pOp1, BitvectorFormula pOp2) {
    return this.bvfmgr.lessThan(pOp1, pOp2, true);
  }

  @Override
  public BooleanFormula equal(BitvectorFormula pOp1, BitvectorFormula pOp2) {
    return this.bvfmgr.equal(pOp1, pOp2);
  }

  @Override
  public BooleanFormula greaterThan(BitvectorFormula pOp1, BitvectorFormula pOp2) {
    return this.bvfmgr.greaterThan(pOp1, pOp2, true);
  }

  @Override
  public BooleanFormula lessOrEqual(BitvectorFormula pOp1, BitvectorFormula pOp2) {
    return this.bvfmgr.lessOrEquals(pOp1, pOp2, true);
  }

  @Override
  public BooleanFormula greaterOrEqual(BitvectorFormula pOp1, BitvectorFormula pOp2) {
    return this.bvfmgr.greaterOrEquals(pOp1, pOp2, true);
  }

  @Override
  public BooleanFormula asBoolean(BitvectorFormula pOp1) {
    return this.bfmgr.not(this.bvfmgr.equal(pOp1, this.bvfmgr.makeBitvector(this.bvfmgr.getLength(pOp1), BigInteger.ZERO)));
  }

  public static Integer getSize(InvariantsFormula<CompoundInterval> pFormula, Map<String, CType> pTypes, MachineModel pMachineModel) {
    return pFormula.accept(new GetSizeVisitor(pTypes, pMachineModel));
  }

  private static class GetSizeVisitor implements InvariantsFormulaVisitor<CompoundInterval, Integer> {

    private final Map<String, CType> types;

    private final MachineModel machineModel;

    public GetSizeVisitor(Map<String, CType> pTypes, MachineModel pMachineModel) {
      this.types = pTypes;
      this.machineModel= pMachineModel;
    }

    @Override
    public Integer visit(Add<CompoundInterval> pAdd) {
      Integer size1 = pAdd.getSummand1().accept(this);
      Integer size2 = pAdd.getSummand1().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(BinaryAnd<CompoundInterval> pAnd) {
      Integer size1 = pAnd.getOperand1().accept(this);
      Integer size2 = pAnd.getOperand2().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(BinaryNot<CompoundInterval> pNot) {
      return pNot.getFlipped().accept(this);
    }

    @Override
    public Integer visit(BinaryOr<CompoundInterval> pOr) {
      Integer size1 = pOr.getOperand1().accept(this);
      Integer size2 = pOr.getOperand2().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(BinaryXor<CompoundInterval> pXor) {
      Integer size1 = pXor.getOperand1().accept(this);
      Integer size2 = pXor.getOperand2().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(Constant<CompoundInterval> pConstant) {
      return null;
    }

    @Override
    public Integer visit(Divide<CompoundInterval> pDivide) {
      Integer size1 = pDivide.getNumerator().accept(this);
      Integer size2 = pDivide.getDenominator().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(Equal<CompoundInterval> pEqual) {
      Integer size1 = pEqual.getOperand1().accept(this);
      Integer size2 = pEqual.getOperand2().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(LessThan<CompoundInterval> pLessThan) {
      Integer size1 = pLessThan.getOperand1().accept(this);
      Integer size2 = pLessThan.getOperand2().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(LogicalAnd<CompoundInterval> pAnd) {
      Integer size1 = pAnd.getOperand1().accept(this);
      Integer size2 = pAnd.getOperand2().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(LogicalNot<CompoundInterval> pNot) {
      return pNot.getNegated().accept(this);
    }

    @Override
    public Integer visit(Modulo<CompoundInterval> pModulo) {
      Integer size1 = pModulo.getNumerator().accept(this);
      Integer size2 = pModulo.getDenominator().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(Multiply<CompoundInterval> pMultiply) {
      Integer size1 = pMultiply.getFactor1().accept(this);
      Integer size2 = pMultiply.getFactor2().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(ShiftLeft<CompoundInterval> pShiftLeft) {
      Integer size1 = pShiftLeft.getShifted().accept(this);
      Integer size2 = pShiftLeft.getShiftDistance().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(ShiftRight<CompoundInterval> pShiftRight) {
      Integer size1 = pShiftRight.getShifted().accept(this);
      Integer size2 = pShiftRight.getShiftDistance().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(Union<CompoundInterval> pUnion) {
      Integer size1 = pUnion.getOperand1().accept(this);
      Integer size2 = pUnion.getOperand2().accept(this);
      return decideSize(size1, size2);
    }

    @Override
    public Integer visit(Variable<CompoundInterval> pVariable) {
      CType type = types.get(pVariable.getName());
      if (type == null) {
        return null;
      }
      return machineModel.getSizeof(type) * machineModel.getSizeofCharInBits();
    }

  }

  private static Integer decideSize(Integer pSize1, Integer pSize2) {
    assert pSize1 == null || pSize2 == null || pSize1 == pSize2;
    if (pSize1 == null) {
      return pSize2;
    }
    if (pSize2 == null) {
      return pSize1;
    }
    return Math.max(pSize1, pSize2);
  }

}
