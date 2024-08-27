// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CFloatType;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CIntegerType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointRoundingMode;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class SMTFloat extends CFloat {
  private static final Solver solver;
  private static final FormulaManagerView fmgr;
  private static final BooleanFormulaManagerView bfmgr;
  private static final FloatingPointFormulaManagerView fpfmgr;

  private static int counter = 0;

  static {
    Configuration config = Configuration.defaultConfiguration();
    ShutdownNotifier notifier = ShutdownNotifier.createDummy();
    LogManager logger = LogManager.createTestLogManager();
    try {
      solver = Solver.create(config, logger, notifier);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    fpfmgr = fmgr.getFloatingPointFormulaManager();
  }

  private final Iterable<BooleanFormula> constraints;
  private final FloatingPointFormula formula;

  /** Stores the value of the formula after it was evaluated */
  private Optional<Double> value = Optional.empty();

  public SMTFloat(String pValue, Format pFormat) {
    Preconditions.checkArgument(pFormat.equals(Format.Float32) || pFormat.equals(Format.Float64));
    formula = fromString(pValue, pFormat);
    constraints = ImmutableList.of();
  }

  private FloatingPointFormula fromString(String pInput, Format pFormat) {
    FloatingPointType floatType =
        FormulaType.getFloatingPointType(pFormat.expBits(), pFormat.sigBits());
    if ("inf".equals(pInput)) {
      return fpfmgr.makePlusInfinity(floatType);
    } else if ("-inf".equals(pInput)) {
      return fpfmgr.makeMinusInfinity(floatType);
    } else if ("nan".equals(pInput) || "-nan".equals(pInput)) {
      return fpfmgr.makeNaN(floatType);
    } else {
      return fpfmgr.makeNumber(pInput, floatType, FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
    }
  }

  public SMTFloat(FloatingPointFormula pFormula, Iterable<BooleanFormula> pConstraints) {
    formula = pFormula;
    constraints = pConstraints;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof SMTFloat otherFloat
        && evalPredicate(constraints, fpfmgr.assignment(formula, otherFloat.formula));
  }

  @Override
  public int hashCode() {
    return Objects.hash(formula, constraints);
  }

  @Override
  public int compareTo(CFloat other) {
    // (This comment is needed to silence the CI)
    if (other instanceof SMTFloat otherFloat) {
      IntegerFormulaManagerView imgr = fmgr.getIntegerFormulaManager();
      IntegerFormula zero = imgr.makeNumber(0);
      IntegerFormula one = imgr.makeNumber(1);
      return evalIntegerFunction(
          constraints,
          bfmgr.ifThenElse(
              // Check if the numbers are the same
              // We use "representational equality" where "0.0 != -0.0" and "NaN == NaN" hold
              fpfmgr.assignment(formula, otherFloat.formula),
              zero,
              bfmgr.ifThenElse(
                  // Special case: They are both zero, but the sign is different
                  bfmgr.and(fpfmgr.isZero(formula), fpfmgr.isZero(otherFloat.formula)),
                  bfmgr.ifThenElse(fpfmgr.isNegative(formula), imgr.negate(one), one),
                  bfmgr.ifThenElse(
                      // Special case: NaN > everything else
                      fpfmgr.isNaN(formula),
                      one,
                      bfmgr.ifThenElse(
                          // Special case: everything else < NaN
                          fpfmgr.isNaN(otherFloat.formula),
                          imgr.negate(one),
                          // Otherwise just compare the values
                          bfmgr.ifThenElse(
                              fpfmgr.lessThan(formula, otherFloat.formula),
                              imgr.negate(one),
                              one))))));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat difference(CFloat pOther) {
    if (pOther instanceof SMTFloat otherFloat) {
      FloatingPointFormula zero = fpfmgr.makeNumber(0, getFloatingPointType());
      return new SMTFloat(
          bfmgr.ifThenElse(
              fpfmgr.lessOrEquals(formula, otherFloat.formula),
              zero,
              fpfmgr.subtract(formula, otherFloat.formula)),
          constraints);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat min(CFloat pOther) {
    if (pOther instanceof SMTFloat otherFloat) {
      return new SMTFloat(
          bfmgr.ifThenElse(
              bfmgr.and(
                  fpfmgr.isZero(formula),
                  fpfmgr.isZero(otherFloat.formula),
                  bfmgr.not(fpfmgr.assignment(formula, otherFloat.formula))),
              bfmgr.ifThenElse(fpfmgr.isNegative(formula), formula, otherFloat.formula),
              fpfmgr.min(formula, otherFloat.formula)),
          constraints);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat max(CFloat pOther) {
    if (pOther instanceof SMTFloat otherFloat) {
      return new SMTFloat(
          bfmgr.ifThenElse(
              bfmgr.and(
                  fpfmgr.isZero(formula),
                  fpfmgr.isZero(otherFloat.formula),
                  bfmgr.not(fpfmgr.assignment(formula, otherFloat.formula))),
              bfmgr.ifThenElse(fpfmgr.isNegative(formula), otherFloat.formula, formula),
              fpfmgr.max(formula, otherFloat.formula)),
          constraints);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat fraction() {
    FloatingPointFormula zero = fpfmgr.makeNumber(0, getFloatingPointType());

    FloatingPointFormula integer = fpfmgr.round(formula, FloatingPointRoundingMode.TOWARD_ZERO);
    FloatingPointFormula fraction = fpfmgr.subtract(formula, integer);

    SMTFloat result =
        new SMTFloat(bfmgr.ifThenElse(fpfmgr.isInfinity(formula), zero, fraction), constraints);
    return result.copySignFrom(this);
  }

  @Override
  public String toString() {
    FloatValue r = FloatValue.fromDouble(getValue());
    if (getFloatingPointType().equals(FormulaType.getSinglePrecisionFloatingPointType())) {
      return r.withPrecision(Format.Float32).toString();
    } else if (getFloatingPointType().equals(FormulaType.getDoublePrecisionFloatingPointType())) {
      return r.toString();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public CFloat add(CFloat pSummand) {
    if (pSummand instanceof SMTFloat other) {
      return new SMTFloat(
          fpfmgr.add(formula, other.formula),
          FluentIterable.concat(constraints, other.constraints));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat add(CFloat... pSummands) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat multiply(CFloat pFactor) {
    if (pFactor instanceof SMTFloat other) {
      return new SMTFloat(
          fpfmgr.multiply(formula, other.formula),
          FluentIterable.concat(constraints, other.constraints));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat multiply(CFloat... pFactors) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat subtract(CFloat pSubtrahend) {
    if (pSubtrahend instanceof SMTFloat other) {
      return new SMTFloat(
          fpfmgr.subtract(formula, other.formula),
          FluentIterable.concat(constraints, other.constraints));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat divideBy(CFloat pDivisor) {
    if (pDivisor instanceof SMTFloat other) {
      return new SMTFloat(
          fpfmgr.divide(formula, other.formula),
          FluentIterable.concat(constraints, other.constraints));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat modulo(CFloat pDivisor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat remainder(CFloat pDivisor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat ln() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat exp() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat powTo(CFloat exponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat powToIntegral(int exponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat sqrt() {
    return new SMTFloat(fpfmgr.sqrt(formula), constraints);
  }

  @Override
  public CFloat round() {
    FloatingPointFormula zero = fpfmgr.makeNumber(0, getFloatingPointType());
    FloatingPointFormula fp_half = fpfmgr.makeNumber(0.5, getFloatingPointType());
    FloatingPointFormula fp_neg_half = fpfmgr.makeNumber(-0.5, getFloatingPointType());

    FloatingPointFormula integral = fpfmgr.round(formula, FloatingPointRoundingMode.TOWARD_ZERO);
    FloatingPointFormula rounded_negative_Infinity =
        fpfmgr.round(formula, FloatingPointRoundingMode.TOWARD_NEGATIVE);
    FloatingPointFormula rounded_positive_Infinity =
        fpfmgr.round(formula, FloatingPointRoundingMode.TOWARD_POSITIVE);

    FloatingPointFormula rounded =
        bfmgr.ifThenElse(
            fpfmgr.greaterThan(formula, zero),
            bfmgr.ifThenElse(
                fpfmgr.greaterOrEquals(fpfmgr.subtract(formula, integral), fp_half),
                rounded_positive_Infinity,
                integral),
            bfmgr.ifThenElse(
                fpfmgr.lessOrEquals(fpfmgr.subtract(formula, integral), fp_neg_half),
                rounded_negative_Infinity,
                integral));

    return new SMTFloat(rounded, constraints);
  }

  @Override
  public CFloat trunc() {
    return new SMTFloat(fpfmgr.round(formula, FloatingPointRoundingMode.TOWARD_ZERO), constraints);
  }

  @Override
  public CFloat ceil() {
    return new SMTFloat(
        fpfmgr.round(formula, FloatingPointRoundingMode.TOWARD_POSITIVE), constraints);
  }

  @Override
  public CFloat floor() {
    return new SMTFloat(
        fpfmgr.round(formula, FloatingPointRoundingMode.TOWARD_NEGATIVE), constraints);
  }

  @Override
  public CFloat abs() {
    return new SMTFloat(fpfmgr.abs(formula), constraints);
  }

  @Override
  public boolean isZero() {
    return evalPredicate(constraints, fpfmgr.isZero(formula));
  }

  @Override
  public boolean isOne() {
    FloatingPointFormula one = fpfmgr.makeNumber(1.0, getFloatingPointType());
    return evalPredicate(constraints, fpfmgr.equalWithFPSemantics(one, fpfmgr.abs(formula)));
  }

  @Override
  public boolean isNan() {
    return evalPredicate(constraints, fpfmgr.isNaN(formula));
  }

  @Override
  public boolean isInfinity() {
    return evalPredicate(constraints, fpfmgr.isInfinity(formula));
  }

  @Override
  public boolean isNegative() {
    return evalPredicate(constraints, fpfmgr.isNegative(formula));
  }

  /** Returns a fresh variable name */
  private static String makeVariableName(String prefix) {
    return prefix + counter++;
  }

  @Override
  public CFloat copySignFrom(CFloat source) {
    if (source instanceof SMTFloat otherFloat) {
      FloatingPointFormula maybeNegated =
          fpfmgr.makeVariable(makeVariableName("__nondetSign_"), getFloatingPointType());

      BooleanFormula assertion =
          bfmgr.or(
              fpfmgr.assignment(maybeNegated, formula),
              fpfmgr.assignment(maybeNegated, fpfmgr.negate(formula)));

      FloatingPointFormula copied =
          bfmgr.ifThenElse(
              fpfmgr.isNaN(formula),
              formula,
              bfmgr.ifThenElse(
                  fpfmgr.isNaN(otherFloat.formula),
                  maybeNegated,
                  bfmgr.ifThenElse(
                      bfmgr.equivalence(
                          fpfmgr.isNegative(formula), fpfmgr.isNegative(otherFloat.formula)),
                      formula,
                      fpfmgr.negate(formula))));

      return new SMTFloat(copied, FluentIterable.concat(FluentIterable.of(assertion), constraints));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloat castTo(CFloatType toType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Number> castToOther(CIntegerType toType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloatWrapper copyWrapper() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected CFloatWrapper getWrapper() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFloatType getType() {
    FloatingPointType floatType = getFloatingPointType();
    if (floatType.equals(FormulaType.getSinglePrecisionFloatingPointType())) {
      return CFloatType.SINGLE;
    } else if (floatType.equals(FormulaType.getDoublePrecisionFloatingPointType())) {
      return CFloatType.DOUBLE;
    } else if (floatType.equals(FormulaType.getFloatingPointType(15, 63))) {
      return CFloatType.LONG_DOUBLE;
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public boolean equalTo(CFloat other) {
    if (other instanceof SMTFloat otherFloat) {
      return evalPredicate(constraints, fpfmgr.equalWithFPSemantics(formula, otherFloat.formula));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean lessOrGreater(CFloat other) {
    if (other instanceof SMTFloat otherFloat) {
      return evalPredicate(
          constraints,
          bfmgr.or(
              fpfmgr.lessThan(formula, otherFloat.formula),
              fpfmgr.greaterThan(formula, otherFloat.formula)));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean greaterThan(CFloat other) {
    if (other instanceof SMTFloat otherFloat) {
      return evalPredicate(constraints, fpfmgr.greaterThan(formula, otherFloat.formula));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean greaterOrEqual(CFloat other) {
    if (other instanceof SMTFloat otherFloat) {
      return evalPredicate(constraints, fpfmgr.greaterOrEquals(formula, otherFloat.formula));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean lessThan(CFloat other) {
    if (other instanceof SMTFloat otherFloat) {
      return evalPredicate(constraints, fpfmgr.lessThan(formula, otherFloat.formula));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean lessOrEqual(CFloat other) {
    if (other instanceof SMTFloat otherFloat) {
      return evalPredicate(constraints, fpfmgr.lessOrEquals(formula, otherFloat.formula));
    }
    throw new UnsupportedOperationException();
  }

  FloatingPointType getFloatingPointType() {
    return fmgr.getFormulaType(formula);
  }

  static boolean evalPredicate(Iterable<BooleanFormula> pConstraints, BooleanFormula pFormula) {
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      try {
        prover.push();
        try {
          // Push constraints and formula
          for (BooleanFormula c : pConstraints) {
            prover.addConstraint(c);
          }
          prover.addConstraint(pFormula);
          return !prover.isUnsat();
        } catch (SolverException e) {
          throw new RuntimeException(e);
        } catch (InterruptedException e) {
          // Skip
        } finally {
          prover.pop();
        }
      } catch (InterruptedException e) {
        // Skip
      }
      return false;
    }
  }

  static int evalIntegerFunction(Iterable<BooleanFormula> pConstraints, IntegerFormula pFormula) {
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      try {
        prover.push();
        try {
          // Push constraints and formula
          for (BooleanFormula c : pConstraints) {
            prover.addConstraint(c);
          }
          Preconditions.checkArgument(!prover.isUnsat());
          return prover.getModel().evaluate(pFormula).intValueExact();
        } catch (SolverException e) {
          throw new RuntimeException(e);
        } catch (InterruptedException e) {
          // Skip
        } finally {
          prover.pop();
        }
      } catch (InterruptedException e) {
        // Skip
      }
      return 0;
    }
  }

  double getValue() {
    if (value.isPresent()) {
      return value.orElseThrow();
    }
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      try {
        prover.push();
        try {
          for (BooleanFormula c : constraints) {
            prover.addConstraint(c);
          }
          if (!prover.isUnsat()) {
            Object object = prover.getEvaluator().evaluate(formula);
            if (object instanceof Float floatValue) {
              value = Optional.of((double) floatValue);
            } else if (object instanceof Double doubleValue) {
              value = Optional.of(doubleValue);
            } else {
              // Should be unreachable, as guaranteed by the precondition in the constructor
              throw new UnsupportedOperationException();
            }
            return value.orElseThrow();
          }
        } catch (SolverException e) {
          throw new RuntimeException(e);
        } catch (InterruptedException e) {
          // Skip
        } finally {
          prover.pop();
        }
      } catch (InterruptedException e) {
        // skip
      }
      return Double.NaN;
    }
  }
}
