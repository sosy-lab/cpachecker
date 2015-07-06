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


public class FunctionApplicationManager {

  private final FormulaManagerView fmgr;
  private final LogManager logger;

  public FunctionApplicationManager(FormulaManagerView pFmgr, LogManager pLogger) {
    this.fmgr = pFmgr;
    this.logger = pLogger;
  }

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
    default:
      return fmgr.getBooleanFormulaManager().makeBoolean(true);
    }
  }

  /** common interface for all function-evaluators. */
  private static interface FunctionApplication {

    /**
     * returns a pair consisting of
     * -- a constraint "UF(params) == result",
     * -- a boolean flag, whether the result already was correct.
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

      Formula uf = fmgr.getFunctionFormulaManager().declareAndCallUninterpretedFunction(
          func.getName(),
          getType(),
          fmgr.makeNumber(getType(), p1),
          fmgr.makeNumber(getType(), p2));

      BooleanFormula newAssignment = fmgr.makeEqual(uf, fmgr.makeNumber(getType(), validResult));

      if (!validResult.equals(value)) {
        logger.logf(Level.INFO, "replacing UF '%s' with value '%s' through '%s'.", uf, value, newAssignment);
      }

      return newAssignment;
    }

    /** get FormulaType of parameters and return-type of function. */
    FormulaType<?> getType() {
      return FormulaType.IntegerType;
    }

    /** returns the correct result of the computation. */
    abstract BigInteger compute(BigInteger p1, BigInteger p2);

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
      return p1.divide(p2);
    }
  };

  private final FunctionApplication INTEGER_MOD = new BinaryArithmeticFunctionApplication() {

    @Override
    BigInteger compute(BigInteger p1, BigInteger p2) {
      return p1.remainder(p2);
    }
  };
}