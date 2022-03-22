// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ExpressionValueVisitorWithPredefinedValues extends ExpressionValueVisitor {

  public static final String PATERN_FOR_RANDOM = "__VERIFIER_nondet_";
  private AtomicInteger numReturnedValues;
  private LogManagerWithoutDuplicates logger;
  private Map<Integer, String> valuesFromFile = new HashMap<>();
  private boolean lastRequestSuccessful = true;

  private boolean lastRequestedValue = false;

  public ExpressionValueVisitorWithPredefinedValues(
      ValueAnalysisState pState,
      String pFunctionName,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger) {
    super(pState, pFunctionName, pMachineModel, pLogger);
    this.logger = pLogger;
  }

  /**
   * @param pState see {@link ExpressionValueVisitor#ExpressionValueVisitor(ValueAnalysisState,
   *     String, MachineModel, LogManagerWithoutDuplicates) pState}
   * @param pFunctionName see {@link
   *     ExpressionValueVisitor#ExpressionValueVisitor(ValueAnalysisState, String, MachineModel,
   *     LogManagerWithoutDuplicates) pFunctionName}
   * @param pAtomicInteger the index of the element that should be returned from the file.
   * @param pMachineModel see {@link
   *     ExpressionValueVisitor#ExpressionValueVisitor(ValueAnalysisState, String, MachineModel,
   *     LogManagerWithoutDuplicates) pMachineModel}
   * @param pLogger see {@link ExpressionValueVisitor#ExpressionValueVisitor(ValueAnalysisState,
   *     String, MachineModel, LogManagerWithoutDuplicates) pLogger}
   * @param pValuesFromFile The Map containing the values for the random function.
   */
  public ExpressionValueVisitorWithPredefinedValues(
      ValueAnalysisState pState,
      String pFunctionName,
      AtomicInteger pAtomicInteger,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      Map<Integer, String> pValuesFromFile) {
    super(pState, pFunctionName, pMachineModel, pLogger);
    this.logger = pLogger;
    this.numReturnedValues = pAtomicInteger;
    if (Objects.nonNull(pValuesFromFile)) {
      valuesFromFile = pValuesFromFile;
    }
  }

  @Override
  public Value evaluate(CRightHandSide pExp, CType pTargetType) throws UnrecognizedCodeException {
    if (lastRequestSuccessful && pExp instanceof CFunctionCallExpression) {
      CFunctionCallExpression call = (CFunctionCallExpression) pExp;
      if (call.getFunctionNameExpression() instanceof CIdExpression
          && ((CIdExpression) call.getFunctionNameExpression())
              .getName()
              .startsWith(PATERN_FOR_RANDOM)) {

        // We found a call to random. If available, return a new value from the predefined inputs.
        // Otherwise, delegate to super
        int counter = numReturnedValues.getAndIncrement();
        if (this.valuesFromFile.containsKey(counter)) {
          Value value = computeNumericalValue(call, valuesFromFile.get(counter));

          this.logger.log(
              Level.FINER,
              "Returning value at position %d, for statement " + pExp.toASTString() + " that is: ",
              value);
          lastRequestedValue = true;

          return value;
        } else {
          lastRequestSuccessful = false;
        }
      }
    }
    return super.evaluate(pExp, pTargetType);
  }

  /**
   * @return true, if the value used in the last request was loaded from the testcomp-testcase
   */
  public boolean isLastRequestedValuePresent() {
    return lastRequestedValue;
  }

  @Override
  public void reset() {
    super.reset();
    this.lastRequestedValue = false;
  }

  private Value computeNumericalValue(CFunctionCallExpression call, String pStringValueForNumber) {

    // Determine the type that needs to be returned:
    if (call.getExpressionType() instanceof CSimpleType) {

      CSimpleType type = (CSimpleType) call.getExpressionType();

      if (type.equals(CNumericTypes.BOOL)) {
        return BooleanValue.valueOf(!pStringValueForNumber.equals("0"));
      }
      if (type.equals(CNumericTypes.CHAR) || type.equals(CNumericTypes.SIGNED_CHAR)) {
        if (pStringValueForNumber.startsWith("'")
            && pStringValueForNumber.length() == 3
            && pStringValueForNumber.substring(2).equals("'")) {
          // String has the form 'c'
          final int unicodeNumericValue = pStringValueForNumber.charAt(1);
          return new NumericValue(unicodeNumericValue);
        } else if (pStringValueForNumber.length() == 1 && isInt(pStringValueForNumber)) {
          // String is a number, hence parse as integer
          final int unicodeNumericValue = Integer.parseInt(pStringValueForNumber);
          return new NumericValue(unicodeNumericValue);

        } else {
          this.logger.logf(
              Level.WARNING,
              "Cannot parse type char for value %s, hence returning unknown",
              pStringValueForNumber);
          return new Value.UnknownValue();
        }
      }
      if (type.equals(CNumericTypes.UNSIGNED_CHAR) || type.equals(CNumericTypes.UNSIGNED_INT)) {
        return new NumericValue(Integer.parseUnsignedInt(pStringValueForNumber));
      }
      if (type.equals(CNumericTypes.INT) || type.equals(CNumericTypes.SIGNED_INT)) {
        return new NumericValue(Integer.parseInt(pStringValueForNumber));
      }
      if (type.equals(CNumericTypes.SHORT_INT)) {
        return new NumericValue(Short.parseShort(pStringValueForNumber));
      }
      if (type.equals(CNumericTypes.UNSIGNED_SHORT_INT)) {
        this.logger.log(Level.WARNING, "Cannot parse unsigned short, returning unknown");
        return new UnknownValue();
      }
      if (type.equals(CNumericTypes.UNSIGNED_LONG_INT)) {
        return new NumericValue(Long.parseUnsignedLong(pStringValueForNumber));
      }
      if (type.equals(CNumericTypes.LONG_INT) || type.equals(CNumericTypes.SIGNED_LONG_INT)) {
        return new NumericValue(Long.parseLong(pStringValueForNumber));
      }
      if (type.equals(CNumericTypes.UNSIGNED_LONG_LONG_INT)) {
        this.logger.log(Level.WARNING, "Cannot parse unsigned longlong, returning unknown");
        return new UnknownValue();
      }
      if (type.equals(CNumericTypes.LONG_LONG_INT)
          || type.equals(CNumericTypes.SIGNED_LONG_LONG_INT)) {
        return new NumericValue(new BigInteger(pStringValueForNumber));
      }
      if (type.equals(CNumericTypes.FLOAT)) {
        return new NumericValue(Float.valueOf(pStringValueForNumber));
      }
      if (type.equals(CNumericTypes.DOUBLE)) {
        return new NumericValue(Double.valueOf(pStringValueForNumber));
      }
      if (type.equals(CNumericTypes.LONG_DOUBLE)) {
        return new NumericValue(new BigDecimal(pStringValueForNumber));
      } else {
        this.logger.log(Level.WARNING, "Cannot parse complex types, hence returning unknown");
      }
    }
    return new Value.UnknownValue();
  }

  private boolean isInt(String pStringValueForNumber) {
    try {
      Integer.parseInt(pStringValueForNumber);
      return true;
    } catch (Throwable e) {
      return false;
    }
  }
}
