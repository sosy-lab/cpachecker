// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.states.MemoryLocationValueHandler;
import org.xml.sax.SAXException;

@Options(prefix = "cpa.value")
public class PredefinedValueAssinger implements MemoryLocationValueHandler {
  private final LogManager logger;

  public static final String PATTERN_FOR_RANDOM = "__VERIFIER_nondet_";

  @Option(
      secure = true,
      description =
          "Fixed set of values for function calls to VERIFIER_nondet_*. Does only work, if"
              + " ignoreFunctionValueExceptRandom is enabled ")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path functionValuesForRandom = null;

  @Option(
      secure = true,
      description =
          "If 'ignoreFunctionValueExceptRandom' is set to true, and functionValuesForRandom are"
              + " present,this option determines the analysis behaviour, if all values from the"
              + " file are used and another call to VERIFIER_nondet_* is present. If it is set to"
              + " true, the analysis aborts the computation. If set to false, an unknown value is"
              + " used. functionValuesForRandom ")
  private boolean stopIfAllValuesForUnknownAreUsed = false;

  private Map<Integer, String> valuesFromFile;

  public PredefinedValueAssinger(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    if (functionValuesForRandom != null) {
      setupFunctionValuesForRandom();
    }
  }

  /** Load the FunctionValues for random functinos from the given Testcomp Testcase */
  private void setupFunctionValuesForRandom() {
    try {
      valuesFromFile = TestCompTestcaseLoader.loadTestcase(functionValuesForRandom);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      // Nothing to do here, as we are not able to lead the additional information, hence ignoring
      // the file
      logger.logUserException(
          Level.WARNING,
          e,
          String.format(
              "Ignoring the additionally given file 'functionValuesForRandom' %s due to an error",
              functionValuesForRandom));
      valuesFromFile = new HashMap<>();
    }
  }

  private boolean expressionIsRandomCall(CRightHandSide pExp) {
    if (pExp instanceof CFunctionCallExpression) {
      CFunctionCallExpression call = (CFunctionCallExpression) pExp;
      if (call.getFunctionNameExpression() instanceof CIdExpression
          && ((CIdExpression) call.getFunctionNameExpression())
              .getName()
              .startsWith(PATTERN_FOR_RANDOM)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean handle(
      MemoryLocation pMemLocation,
      Type pType,
      ValueAnalysisState pState,
      ExpressionValueVisitor pValueVisitor,
      @Nullable ARightHandSide pExpression)
      throws UnrecognizedCodeException {
    if (pExpression == null) { // if the expression is null, we do not know the value
      pState.forget(pMemLocation);
    } else if (pExpression instanceof CRightHandSide
        && expressionIsRandomCall((CRightHandSide) pExpression)) {
      if (valuesFromFile.isEmpty()) {
        pState.forget(pMemLocation);
      } else {
        int counter = pState.getCounterForRandomInputValuesUsed().getAndIncrement();
        CFunctionCallExpression call = (CFunctionCallExpression) pExpression;
        if (valuesFromFile.containsKey(counter)) {
          Value value = computeNumericalValue(call, valuesFromFile.get(counter));
          pState.assignConstant(pMemLocation, value, pType);
        } else {
          pState.forget(pMemLocation);
          return this.stopIfAllValuesForUnknownAreUsed;
        }
      }
    }
    return false;
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
