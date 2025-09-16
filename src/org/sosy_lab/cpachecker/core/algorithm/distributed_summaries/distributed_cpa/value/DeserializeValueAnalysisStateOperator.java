// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import com.google.common.base.Splitter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeParser;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.FunctionValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaToValueVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DeserializeValueAnalysisStateOperator implements DeserializeOperator {

  private final Map<MemoryLocation, CType> variableTypes;
  private final FormulaManagerView formulaManager;
  private final CFA cfa;

  private static final String BOOLEAN_VALUE_PREFIX = "BooleanValue";
  private static final String NUMERIC_VALUE_PREFIX = "NumericValue";
  private static final String FUNCTION_VALUE_PREFIX = "FunctionValue";

  public DeserializeValueAnalysisStateOperator(
      CFA pCFA, Map<MemoryLocation, CType> pVariableTypes, Solver pSolver) {
    variableTypes = pVariableTypes;
    formulaManager = pSolver.getFormulaManager();
    cfa = pCFA;
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    ContentReader reader = pMessage.getAbstractStateContent(ValueAnalysisState.class);
    String valueAnalysisString = reader.get(STATE_KEY);

    if (valueAnalysisString.isEmpty()) {
      return new ValueAnalysisState(cfa.getMachineModel());
    } else {
      Map<MemoryLocation, ValueAndType> constantsMap = new HashMap<>();

      for (String constant : Splitter.on(" && ").split(valueAnalysisString)) {
        List<String> parts = Splitter.on("->").splitToList(constant);
        String identifier = parts.get(0);
        List<String> valueParts = Splitter.on('=').splitToList(parts.get(1));

        if (valueParts.get(1).isEmpty()) {
          continue;
        }

        String valueString = valueParts.get(1);
        Value value = extractValueFromString(valueString);

        if (value != null) {
          constantsMap.put(
              MemoryLocation.forIdentifier(identifier),
              new ValueAndType(value, getSimpleTypeFromString(valueParts.getFirst())));
        }
      }

      return new ValueAnalysisState(
          Optional.of(cfa.getMachineModel()), PathCopyingPersistentTreeMap.copyOf(constantsMap));
    }
  }

  @Override
  public ValueAnalysisState deserializeFromFormula(BooleanFormula pFormula) {
    FormulaToValueVisitor visitor = new FormulaToValueVisitor(variableTypes, formulaManager);
    formulaManager.visit(pFormula, visitor);
    Map<MemoryLocation, ValueAndType> constantsMap = visitor.getConstantsMap();

    return new ValueAnalysisState(
        Optional.of(cfa.getMachineModel()), PathCopyingPersistentTreeMap.copyOf(constantsMap));
  }

  private Value extractValueFromString(String valueString) {
    if (valueString.startsWith(BOOLEAN_VALUE_PREFIX)) {
      long boolNumericValue =
          Long.parseLong(
              valueString.substring(valueString.indexOf('(') + 1, valueString.length() - 1));
      return BooleanValue.valueOf(boolNumericValue != 0);

    } else if (valueString.startsWith(NUMERIC_VALUE_PREFIX)) {
      long numericValue =
          Long.parseLong(
              valueString.substring(valueString.indexOf('(') + 1, valueString.length() - 1));
      return new NumericValue(BigInteger.valueOf(numericValue));

    } else if (valueString.startsWith(FUNCTION_VALUE_PREFIX)) {
      String functionName =
          valueString.substring(valueString.indexOf('(') + 1, valueString.length() - 1);
      return new FunctionValue(functionName);
    }
    return null;
  }

  private CSimpleType getSimpleTypeFromString(String typeString) {
    boolean isConst = false;
    boolean isVolatile = false;
    boolean isLong = false;
    boolean isShort = false;
    boolean isSigned = false;
    boolean isUnsigned = false;
    boolean isComplex = false;
    boolean isImaginary = false;
    boolean isLongLong = false;
    CBasicType basicType = CBasicType.UNSPECIFIED;

    for (String typePart : Splitter.on(' ').split(typeString)) {
      switch (typePart) {
        case "const" -> isConst = true;
        case "volatile" -> isVolatile = true;
        case "long" -> {
          if (isLong) {
            isLongLong = true;
            isLong = false;
          } else {
            isLong = true;
          }
        }
        case "short" -> isShort = true;
        case "signed" -> isSigned = true;
        case "unsigned" -> isUnsigned = true;
        case "_Complex" -> isComplex = true;
        case "_Imaginary" -> isImaginary = true;
        default -> basicType = getTypeFromString(typePart);
      }
    }

    return new CSimpleType(
        CTypeParser.getQualifiers(isConst, isVolatile),
        basicType,
        isLong,
        isShort,
        isSigned,
        isUnsigned,
        isComplex,
        isImaginary,
        isLongLong);
  }

  private static CBasicType getTypeFromString(String typeStr) {
    return switch (typeStr) {
      case "_Bool" -> CBasicType.BOOL;
      case "char" -> CBasicType.CHAR;
      case "int" -> CBasicType.INT;
      case "__int128" -> CBasicType.INT128;
      case "float" -> CBasicType.FLOAT;
      case "double" -> CBasicType.DOUBLE;
      case "__float128" -> CBasicType.FLOAT128;
      default -> CBasicType.UNSPECIFIED;
    };
  }
}
