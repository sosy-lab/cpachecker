/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.ufCheckingProver;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.ufCheckingProver.UFCheckingBasicProverEnvironment.UFCheckingProverOptions;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

import java.math.BigInteger;
import java.util.logging.Level;

/** This class contains code for a better evaluation of UFs. */
public class FunctionApplicationManager {

  private final FormulaManagerView fmgr;
  private final LogManager logger;
  private final UFCheckingProverOptions options;

  public FunctionApplicationManager(FormulaManagerView pFmgr, LogManager pLogger,
      UFCheckingProverOptions pOptions) {
    this.fmgr = pFmgr;
    this.logger = pLogger;
    this.options = pOptions;
  }

  /**
   * For a UF (with a matching signature and name), we produce the correct result,
   * and build an assignment (equality) of the UF and the correct result and return it.
   * If we cannot compute a result or UF is unknown, we return TRUE.
   */
  public BooleanFormula evaluate(
      ValueAssignment entry,
      Object value) {
    String functionName = entry.getName();

    // Stateful shortcut.

    switch (functionName) {
      case "Integer__*_": {
        return INTEGER_MULT.apply(entry, value);
      }
      case "Integer__/_": {
        return INTEGER_DIV.apply(entry, value);
      }
      case "Integer__%_": {
        return INTEGER_MOD.apply(entry, value);
      }
      case "_<<_": {
        return INTEGER_SHIFT_LEFT.apply(entry, value);
      }
      case "_>>_": {
        return INTEGER_SHIFT_RIGHT.apply(entry, value);
      }
      case "_&_": {
        return INTEGER_AND.apply(entry, value);
      }
      case "_!!_": {
        return INTEGER_OR.apply(entry, value);
      }
      case "_^_": {
        return INTEGER_XOR.apply(entry, value);
      }
      case "_~_": {
        return INTEGER_NOT.apply(entry, value);
      }
      default:
        // $FALL-THROUGH$
    }

    if (functionName.startsWith("_overflow")) {
      return OVERFLOW.apply(entry, value);
    }

    logger.logf(Level.ALL, "ignoring UF '%s' with value '%s'.", entry, value);
    return fmgr.getBooleanFormulaManager().makeTrue();
  }

  /** if the new valid result is equal to the old value, we return just TRUE, else we return the new assignment. */
  private BooleanFormula makeAssignmentOrTrue(Number validResult, Object value, Formula uf, BooleanFormula newAssignment) {
    if (!validResult.equals(value)) {
      logger.logf(Level.ALL, "replacing UF '%s' with value '%s' through '%s'.", uf, value, newAssignment);
      return newAssignment;
    } else {
      return fmgr.getBooleanFormulaManager().makeTrue();
    }
  }

  /** common interface for all function-evaluators. */
  private interface FunctionApplication {

    /**
     * returns a constraint "UF(params) == result"
     * or TRUE if we cannot evaluate the UF.
     */
    BooleanFormula apply(ValueAssignment func, Object pValue);
  }

  private abstract class BinaryArithmeticFunctionApplication implements FunctionApplication {

    @Override
    public final BooleanFormula apply(ValueAssignment func, Object value) {
      assert value instanceof BigInteger;
      BigInteger arg1 = (BigInteger) func.getArgInterpretation(0);
      BigInteger arg2 = (BigInteger) func.getArgInterpretation(1);

      BigInteger validResult = compute(arg1, arg2);

      if (validResult == null) {
        // evaluation not possible, ignore UF
        return fmgr.getBooleanFormulaManager().makeTrue();
      }

      Formula uf = fmgr.getFunctionFormulaManager().declareAndCallUF(
          func.getName(),
          getType(),
          fmgr.makeNumber(getType(), arg1),
          fmgr.makeNumber(getType(), arg2));

      BooleanFormula newAssignment = fmgr.makeEqual(uf, fmgr.makeNumber(getType(), validResult));

      return makeAssignmentOrTrue(validResult, value, uf, newAssignment);
    }

    /** get FormulaType of parameters and return-type of function. */
    FormulaType<?> getType() {
      return FormulaType.IntegerType;
    }

    /** returns the correct result of the computation. */
    abstract BigInteger compute(BigInteger p1, BigInteger p2);

  }

  private abstract class UnaryFunctionApplication implements FunctionApplication {

    @Override
    public final BooleanFormula apply(ValueAssignment func, Object value) {
      BigInteger p1 = (BigInteger) func.getArgInterpretation(0);
      BigInteger validResult = compute(func, p1);

      if (validResult == null) {
        // evaluation not possible, ignore UF
        return fmgr.getBooleanFormulaManager().makeTrue();
      }

      Formula uf = fmgr.getFunctionFormulaManager().declareAndCallUF(
          func.getName(),
          getType(),
          fmgr.makeNumber(getType(), p1));

      BooleanFormula newAssignment = fmgr.makeEqual(uf, fmgr.makeNumber(getType(), validResult));

      return makeAssignmentOrTrue(validResult, value, uf, newAssignment);
    }

    /** get FormulaType of parameters and return-type of function. */
    FormulaType<?> getType() {
      return FormulaType.IntegerType;
    }

    /** returns the correct result of the computation. */
    abstract BigInteger compute(ValueAssignment func, BigInteger p2);

  }

  private final FunctionApplication INTEGER_MULT = new BinaryArithmeticFunctionApplication() {

    @Override
    BigInteger compute(BigInteger p1, BigInteger p2) {
      return p1.multiply(p2);
    }
  };

  private final FunctionApplication INTEGER_DIV = new BinaryArithmeticFunctionApplication() {

    @Override
    BigInteger compute(BigInteger p1, BigInteger p2) {
      if (BigInteger.ZERO.equals(p2)) {
        return null;
      } else {
        return p1.divide(p2);
      }
    }
  };

  private final FunctionApplication INTEGER_MOD = new BinaryArithmeticFunctionApplication() {

    @Override
    BigInteger compute(BigInteger p1, BigInteger p2) {
      if (BigInteger.ZERO.equals(p2)) {
        return null;
      } else {
        return p1.remainder(p2);
      }
    }
  };

  private final FunctionApplication INTEGER_SHIFT_LEFT = new BinaryArithmeticFunctionApplication() {

    @Override
    BigInteger compute(BigInteger p1, BigInteger p2) {
      int v = p2.intValue();
      if (v < 0) {
        return null;
      }
      return p1.shiftLeft(v);
    }
  };

  private final FunctionApplication INTEGER_SHIFT_RIGHT = new BinaryArithmeticFunctionApplication() {

    @Override
    BigInteger compute(BigInteger p1, BigInteger p2) {
      int v = p2.intValue();
      if (v < 0) {
        return null;
      }
      return p1.shiftRight(v);
    }
  };

  private final FunctionApplication INTEGER_AND = new BinaryArithmeticFunctionApplication() {

    @Override
    BigInteger compute(BigInteger p1, BigInteger p2) {
      return p1.and(p2);
    }
  };

  private final FunctionApplication INTEGER_OR = new BinaryArithmeticFunctionApplication() {

    @Override
    BigInteger compute(BigInteger p1, BigInteger p2) {
      return p1.or(p2);
    }
  };

  private final FunctionApplication INTEGER_XOR = new BinaryArithmeticFunctionApplication() {

    @Override
    BigInteger compute(BigInteger p1, BigInteger p2) {
      return p1.xor(p2);
    }
  };

  private final FunctionApplication INTEGER_NOT = new UnaryFunctionApplication() {

    @Override
    BigInteger compute(ValueAssignment pFunc, BigInteger p1) {
      return p1.not();
    }
  };

  private final FunctionApplication OVERFLOW = new UnaryFunctionApplication() {

    @Override
    BigInteger compute(ValueAssignment func, BigInteger p1) {
      final String name = func.getName();
      assert name.startsWith("_overflowSigned") || name.startsWith("_overflowUnsigned");
      final boolean signed = name.startsWith("_overflowSigned");
      String length = name.substring(name.indexOf("(") + 1, name.indexOf(")"));
      return overflow(signed, Integer.parseInt(length), p1);
    }

    private BigInteger overflow(boolean signed, int bitsize, BigInteger value) {

      if (signed && !options.isSignedOverflowSafe()) {
        // According to C99-standard, signed integer overflow is not specified.
        // Thus no evaluation is possible, every value is allowed.
        // As the SMT-solver has a satisfiable term with this value, just return NULL to ignore the value.
        return null;
      }

      final BigInteger range = BigInteger.ONE.shiftLeft(bitsize);

      // (value % range) is guaranteed to be in range, and always >=0.
      value = value.mod(range);

      // if (value >= 2**31): value -= 2**31
      final BigInteger max = BigInteger.ONE.shiftLeft(bitsize - 1);
      if (signed && value.compareTo(max) >= 0) {
        value = value.subtract(range);
      }
      return value;
    }
  };
}