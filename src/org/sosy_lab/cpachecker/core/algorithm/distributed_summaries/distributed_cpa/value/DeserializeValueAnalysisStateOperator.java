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
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.FunctionValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaToValueVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DeserializeValueAnalysisStateOperator implements DeserializeOperator {

  private CFA cfa;
  private final Map<MemoryLocation, CType> variableTypes;
  private final Solver solver;
  private final FormulaManagerView formulaManager;
  private final BooleanFormulaManagerView booleanFormulaManager;

  public DeserializeValueAnalysisStateOperator(
      CFA pCFA,
      Map<MemoryLocation, CType> pVariableTypes,
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    cfa = pCFA;
    variableTypes = pVariableTypes;
    solver = Solver.create(config, pLogger, shutdownNotifier);
    formulaManager = solver.getFormulaManager();
    booleanFormulaManager = formulaManager.getBooleanFormulaManager();
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    Optional<Object> abstractStateOptional = pMessage.getAbstractState(ValueAnalysisCPA.class);
    if (!abstractStateOptional.isPresent()) {
      return new ValueAnalysisState(cfa.getMachineModel());
    }
    String valueAnalysisString = (String) abstractStateOptional.orElseThrow();

    if (valueAnalysisString.equals("No constants")) {
      return new ValueAnalysisState(cfa.getMachineModel());
    } else {
      Map<MemoryLocation, ValueAndType> constantsMap = new HashMap<>();

      for (String constant : Splitter.on(" && ").split(valueAnalysisString)) {
        List<String> parts = Splitter.on("->").splitToList(constant);
        String identifier = parts.get(0);
        List<String> valueParts = Splitter.on('=').splitToList(parts.get(1));

        if (valueParts.get(1).equals("")) {
          continue;
        }

        String valueString = valueParts.get(1);
        Value value = extractValueFromString(valueString);

        if (value != null) {
          constantsMap.put(
              MemoryLocation.forIdentifier(identifier),
              new ValueAndType(value, getSimpleTypeFromString(valueParts.get(0))));
        }
      }
      ValueAnalysisState state =
          new ValueAnalysisState(
              Optional.of(cfa.getMachineModel()),
              PathCopyingPersistentTreeMap.copyOf(constantsMap));

      BooleanFormula formula = state.getFormulaApproximation(formulaManager);
      FormulaToValueVisitor visitor = new FormulaToValueVisitor(variableTypes, formulaManager);
      formulaManager.visit(formula, visitor);
      Map<MemoryLocation, ValueAndType> constants = visitor.getConstantsMap();
      return state;
    }
  }

  private Value extractValueFromString(String valueString) {
    if (valueString.startsWith("BooleanValue")) {
      long boolNumericValue =
          Long.parseLong(
              valueString.substring(valueString.indexOf('(') + 1, valueString.length() - 1));
      return BooleanValue.valueOf(boolNumericValue != 0);

    } else if (valueString.startsWith("NumericValue")) {
      long numericValue =
          Long.parseLong(
              valueString.substring(valueString.indexOf('(') + 1, valueString.length() - 1));
      return new NumericValue(BigInteger.valueOf(numericValue));

    } else if (valueString.startsWith("FunctionValue")) {
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
        case "const":
          isConst = true;
          break;
        case "volatile":
          isVolatile = true;
          break;
        case "long":
          if (isLong) {
            isLongLong = true;
            isLong = false;
          } else {
            isLong = true;
          }
          break;
        case "short":
          isShort = true;
          break;
        case "signed":
          isSigned = true;
          break;
        case "unsigned":
          isUnsigned = true;
          break;
        case "_Complex":
          isComplex = true;
          break;
        case "_Imaginary":
          isImaginary = true;
          break;
        default:
          basicType = getTypeFromString(typePart);
      }
    }

    return new CSimpleType(
        isConst,
        isVolatile,
        basicType,
        isLong,
        isShort,
        isSigned,
        isUnsigned,
        isComplex,
        isImaginary,
        isLongLong);
  }

  private CBasicType getTypeFromString(String typeStr) {
    switch (typeStr) {
      case "_Bool":
        return CBasicType.BOOL;
      case "char":
        return CBasicType.CHAR;
      case "int":
        return CBasicType.INT;
      case "__int128":
        return CBasicType.INT128;
      case "float":
        return CBasicType.FLOAT;
      case "double":
        return CBasicType.DOUBLE;
      case "__float128":
        return CBasicType.FLOAT128;
      default:
        return CBasicType.UNSPECIFIED;
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
}
