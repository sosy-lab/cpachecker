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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DeserializeValueAnalysisStateOperator implements DeserializeOperator {

  private CFA cfa;
  private ValueAnalysisCPA valueAnalysisCPA;
  private BlockNode blockNode;

  public DeserializeValueAnalysisStateOperator(
      CFA pCFA,
      ValueAnalysisCPA pValueAnalysisCPA,
      BlockNode pBlockNode) {
    cfa = pCFA;
    valueAnalysisCPA = pValueAnalysisCPA;
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    Optional<Object> abstractStateOptional = pMessage.getAbstractState(ValueAnalysisCPA.class);
    if (!abstractStateOptional.isPresent()) {
      return new ValueAnalysisState(cfa.getMachineModel());
    }
    String valueAnalysisString = (String) abstractStateOptional.get();

    if (valueAnalysisString.equals("No constants")) {
      return new ValueAnalysisState(cfa.getMachineModel());
    } else {
      Map<MemoryLocation, ValueAndType> constantsMap = new HashMap<>();

      for (String constant : Splitter.on(" && ").split(valueAnalysisString)) {
        List<String> parts = Splitter.on("->").splitToList(constant);
        List<String> valueParts = Splitter.on('=').splitToList(parts.get(1));

        constantsMap.put(
            MemoryLocation.forIdentifier(parts.get(0)),
            new ValueAndType(
                new NumericValue(BigInteger.valueOf(Integer.parseInt(valueParts.get(1)))),
                getSimpleTypeFromString(valueParts.get(0))));
      }

      return new ValueAnalysisState(
          Optional.of(cfa.getMachineModel()),
          PathCopyingPersistentTreeMap.copyOf(constantsMap));
    }
  }

  private CSimpleType getSimpleTypeFromString(String typeStr) {
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

    for (String typePart : Splitter.on(' ').split(typeStr)) {
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
}
