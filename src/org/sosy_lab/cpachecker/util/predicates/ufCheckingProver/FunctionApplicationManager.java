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

import java.math.BigInteger;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm.Function;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/** This class contains code for a better evaluation of UFs. */
public class FunctionApplicationManager {

  private final FormulaManagerView fmgr;
  private final LogManager logger;

  public FunctionApplicationManager(FormulaManagerView pFmgr, LogManager pLogger) {
    this.fmgr = pFmgr;
    this.logger = pLogger;
  }

  /**
   * For a UF (with a matching signature and name), we produce the correct result,
   * and build an assignment (equality) of the UF and the correct result and return it.
   * If we cannot compute a result or UF is unknown, we return TRUE.
   */
  public BooleanFormula evaluate(Function func, Object value) {
    switch (func.getName()) {
    case "Integer__*_": {
      return INTEGER_MULT.apply(func, value);
    }
    case "Integer__/_": {
      return INTEGER_DIV.apply(func, value);
    }
    case "Integer__%_": {
      return INTEGER_MOD.apply(func, value);
    }
    case "_<<_": {
      return INTEGER_SHIFT_LEFT.apply(func, value);
    }
    case "_>>_": {
      return INTEGER_SHIFT_RIGHT.apply(func, value);
    }
    case "_&_": {
      return INTEGER_AND.apply(func, value);
    }
    case "_!!_": {
      return INTEGER_OR.apply(func, value);
    }
    case "_^_": {
      return INTEGER_XOR.apply(func, value);
    }
    case "_~_": {
      return INTEGER_NOT.apply(func, value);
    }
    }

    if (func.getName().startsWith("__overflow_")) {
      return OVERFLOW.apply(func, value);
    }

    logger.logf(Level.ALL, "ignoring UF '%s' with value '%s'.", func, value);
    return fmgr.getBooleanFormulaManager().makeBoolean(true);
  }

  /** if the new valid result is equal to the old value, we return just TRUE, else we return the new assignment. */
  private BooleanFormula makeAssignmentOrTrue(Number validResult, Object value, Formula uf, BooleanFormula newAssignment) {
    if (!validResult.equals(value)) {
      logger.logf(Level.ALL, "replacing UF '%s' with value '%s' through '%s'.", uf, value, newAssignment);
      return newAssignment;
    } else {
      return fmgr.getBooleanFormulaManager().makeBoolean(true);
    }
  }

  /** common interface for all function-evaluators. */
  private static interface FunctionApplication {

    /**
     * returns a constraint "UF(params) == result"
     * or TRUE if we cannot evaluate the UF.
     */
    public BooleanFormula apply(Function func, Object pValue);
  }

  private abstract class BinaryArithmeticFunctionApplication implements FunctionApplication {

    @Override
    public final BooleanFormula apply(Function func, Object value) {
      assert func.getArity() == 2;
      assert value instanceof BigInteger;

      BigInteger p1 = (BigInteger) func.getArgument(0);
      BigInteger p2 = (BigInteger) func.getArgument(1);
      BigInteger validResult = compute(p1, p2);

      if (validResult == null) {
        // evaluation not possible, ignore UF
        return fmgr.getBooleanFormulaManager().makeBoolean(true);
      }

      Formula uf = fmgr.getFunctionFormulaManager().declareAndCallUninterpretedFunction(
          func.getName(),
          getType(),
          fmgr.makeNumber(getType(), p1),
          fmgr.makeNumber(getType(), p2));

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
    public final BooleanFormula apply(Function func, Object value) {
      assert func.getArity() == 1;
      assert value instanceof BigInteger;

      BigInteger p1 = (BigInteger) func.getArgument(0);
      BigInteger validResult = compute(func, p1);

      Formula uf = fmgr.getFunctionFormulaManager().declareAndCallUninterpretedFunction(
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
    abstract BigInteger compute(Function func, BigInteger p2);

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
    BigInteger compute(Function pFunc, BigInteger p1) {
      return p1.not();
    }
  };

  private final FunctionApplication OVERFLOW = new UnaryFunctionApplication() {

    @Override
    BigInteger compute(Function func, BigInteger p1) {
      String[] parts = func.getName().split("_");
      assert parts.length == 5 : "we expect a function-name like '__overflow_signed_32_'.";
      assert "signed".equals(parts[3]) || "unsigned".equals(parts[3]);

      return overflow("signed".equals(parts[3]), Integer.parseInt(parts[4]), p1);
    }

    private BigInteger overflow(boolean signed, int bitsize, BigInteger value) {
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