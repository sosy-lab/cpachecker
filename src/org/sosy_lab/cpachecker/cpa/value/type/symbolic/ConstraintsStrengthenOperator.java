/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.value.type.symbolic;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

import com.google.common.base.Optional;

/**
 * Strengthener for ValueAnalysis with ConstraintsCPA.
 */
public class ConstraintsStrengthenOperator {

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;
  
  private ConstraintsStrengthenOperator(
      MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {
    logger = pLogger;
    machineModel = pMachineModel;
  }

  public static ConstraintsStrengthenOperator getInstance(
      MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {

    return new ConstraintsStrengthenOperator(pMachineModel, pLogger);
  }

  /**
   * Strengthen the given {@link ValueAnalysisState} with the given {@link ConstraintsState}.
   *
   * <p>The returned <code>Collection</code> contains all reachable states after strengthening.
   * A returned empty <code>Collection</code> represents 'bottom', a returned <code>null</code>
   * represents that no changes were made to the given <code>ValueAnalysisState</code>.</p>
   *
   *
   * @param pStateToStrengthen the state to strengthen
   * @param pStrengtheningState the state to strengthen the first state with
   * @return <code>null</code> if no changes were made to the given <code>ValueAnalysisState</code>,
   *    an empty <code>Collection</code>, if the resulting state is not reachable and
   *    a <code>Collection</code> containing all reachable states, otherwise
   */
  public Collection<ValueAnalysisState> strengthen(
      ValueAnalysisState pStateToStrengthen, ConstraintsState pStrengtheningState) {

    Optional<ValueAnalysisState> newElement =
        evaluateAssignment(pStrengtheningState.getDefiniteAssignment(), pStateToStrengthen);

    if (newElement == null) {
      return null;
    } else if (newElement.isPresent()) {
      return Collections.singleton(newElement.get());
    } else {
      return Collections.emptySet();
    }
  }

  private Optional<ValueAnalysisState> evaluateAssignment(
      IdentifierAssignment pAssignment, ValueAnalysisState pValueState) {

    ValueAnalysisState newElement = ValueAnalysisState.copyOf(pValueState);
    boolean somethingChanged = false;

    for (Map.Entry<? extends SymbolicIdentifier, Value> onlyValidAssignment : pAssignment.entrySet()) {
      final SymbolicIdentifier identifierToReplace = onlyValidAssignment.getKey();
      final Value newIdentifierValue = onlyValidAssignment.getValue();

      for (Map.Entry<ValueAnalysisState.MemoryLocation, Value> valueEntry : pValueState.getConstantsMapView().entrySet()) {
        final Value currentValue = valueEntry.getValue();
        final ValueAnalysisState.MemoryLocation memLoc = valueEntry.getKey();
        final Type storageType = pValueState.getTypeForMemoryLocation(memLoc);

        if (currentValue instanceof SymbolicValue) {
          IdentifierReplacer replacer = new IdentifierReplacer(identifierToReplace, newIdentifierValue,
              machineModel, logger);

          Value valueAfterReplacingIdentifier = ((SymbolicValue) currentValue).accept(replacer);

          // if the current variable can never obtain the only valid value, the path is infeasible
          /*if (isValueOutOfTypeBounds(newIdentifierValue, storageType)) {
            return Optional.absent();
          }*/ // We can only do this if the memorylocation is exactly the variable/field represented
              // by the ConstraintsCPA's constraint

          newElement.assignConstant(memLoc, valueAfterReplacingIdentifier, storageType);
          somethingChanged = true;
        }
      }
    }

    if (somethingChanged) {
      return Optional.of(newElement);
    } else {
      return null;
    }
  }

  private boolean isValueOutOfTypeBounds(Value pValue, Type pType) {
    if (pType instanceof JSimpleType) {
      return isValueOutOfJTypeBounds(pValue, (JSimpleType) pType);

    } else {
      assert pType instanceof CSimpleType;

      return isValueOutOfCTypeBounds(pValue, (CSimpleType) pType);
    }
  }

  private boolean isValueOutOfJTypeBounds(Value pValue, JSimpleType pType) {
    final JBasicType basicType = pType.getType();

    if (basicType == JBasicType.BOOLEAN) {
      assert pValue instanceof BooleanValue;
      return false;

    } else if (basicType.isFloatingPointType()) {
      if (basicType == JBasicType.DOUBLE) {
        return true;
      } else {
        final double concreteValue = ((NumericValue) pValue).doubleValue();
        float castValue = (float) concreteValue;

        return concreteValue == castValue;
      }
    } else {
      assert basicType.isIntegerType();
      final long concreteValue = pValue.asNumericValue().longValue();

      switch (basicType) {
        case BYTE:
          return concreteValue == ((byte) concreteValue);
        case SHORT:
          return concreteValue == ((short) concreteValue);
        case CHAR:
          return concreteValue == ((char) concreteValue);
        case INT:
          return concreteValue == ((int) concreteValue);
        case LONG:
          return true;

        default:
          throw new AssertionError("Unexpected type " + pType.getType() + " for integer value");
      }
    }
  }

  private boolean isValueOutOfCTypeBounds(Value pValue, CSimpleType pType) {
    final CBasicType basicType = pType.getType();

    if (basicType.isFloatingPointType()) {
      return false; // we can't handle this easily, as C float values can't be easily represented as bits
    } else {
      assert basicType.isIntegerType();

      final BigInteger concreteValue = BigInteger.valueOf(pValue.asNumericValue().longValue());
      final BigInteger maxValue = machineModel.getMaximalIntegerValue(pType);
      final BigInteger minValue = machineModel.getMinimalIntegerValue(pType);

      return concreteValue.compareTo(minValue) < 0 || maxValue.compareTo(concreteValue) < 0;
    }
  }
}
