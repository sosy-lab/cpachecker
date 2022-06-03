// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ExpressionValueVisitorWithPredefinedValues extends ExpressionValueVisitor {

  public static final String PATERN_FOR_RANDOM = "__VERIFIER_nondet_";
  private AtomicInteger numReturnedValues;
  private LogManagerWithoutDuplicates logger;
  private Map<Integer, String> valuesFromFile = new HashMap<>();
  private boolean lastRequestSuccessful = true;

  public ExpressionValueVisitorWithPredefinedValues(
      ValueAnalysisState pState,
      String pFunctionName,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger) {
    super(pState, pFunctionName, pMachineModel, pLogger);
    logger = pLogger;
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
    logger = pLogger;
    numReturnedValues = pAtomicInteger;

    valuesFromFile = pValuesFromFile;
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
        if (valuesFromFile.containsKey(counter)) {
          Value value = computeNumericalValue(call, valuesFromFile.get(counter));

          logger.log(
              Level.FINER,
              "Returning value at position %d, for statement " + pExp.toASTString() + " that is: ",
              value);
          return value;
        } else {
          lastRequestSuccessful = false;
        }
      }
    }
    return super.evaluate(pExp, pTargetType);
  }

  private Value computeNumericalValue(CFunctionCallExpression call, String pStringValueForNumber) {

    // Determine the type that needs to be returned:
    if (call.getExpressionType() instanceof CSimpleType) {

      // TODO: Add support for other datatypes and unsigneds (if needed), not only signed ints
      // CSimpleType type = (CSimpleType) call.getExpressionType();
      // if (type.isLong()) {
      // if (type.isUnsigned()) {
      // return new NumericValue(Long.parseUnsignedLong(pStringValueForNumber));
      // } else {
      // return new NumericValue(Long.parseLong(pStringValueForNumber));
      // }
      // } else if (type.isShort()) {
      // if (type.isUnsigned()) {
      // this.logger.log(Level.WARNING, "Cannot parse unsigned short, returning unknown");
      // return new Value.UnknownValue();
      // } else {
      // return new NumericValue(Short.parseShort(pStringValueForNumber));
      // }
      // } else if (type.isUnsigned()) {
      // return new NumericValue(Integer.parseUnsignedInt(pStringValueForNumber));
      // } else {
      return new NumericValue(Integer.parseInt(pStringValueForNumber));
      // }

    } else {
      logger.log(Level.WARNING, "Cannot parse complex types, hence returning unknown");
    }
    return new Value.UnknownValue();
  }
}
