// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * This provider is instantiated by {@link ValueAnalysisTransferRelation} and is then provided to
 * the {@link ExpressionValueVisitor}. It can contain a List of values to be used by the same to
 * assign to an already known Value instead of performing unknown value handling (e.g. to assign a
 * value to a VERIFIER_nondet_* function call)
 */
public class NondeterministicValueProvider {

  private List<Value> knownValues;

  public NondeterministicValueProvider() {
    knownValues = new ArrayList<>();
  }

  public void setKnownValues(List<Value> pKnownValues) {
    knownValues = new ArrayList<>(pKnownValues);
  }

  public void clearKnownValues() {
    if (knownValues != null) {
      knownValues.clear();
    }
  }

  public Optional<Value> getNextNondetValueFor(CType pType) {
    if (this.knownValues.isEmpty()) {
      return Optional.absent();
    }

    if (!isAssignable(this.knownValues.get(0), pType)) {
      return Optional.absent();
    }

    return Optional.of(this.knownValues.remove(0));
  }

  /**
   * Check if value is assignable via the expr (aka. has the correct type)
   *
   * @param value The value to assign.
   * @param expressionType The type the related expression needs to be assignable.
   */
  private Boolean isAssignable(Value value, CType expressionType) {
    // Check basic types
    if (expressionType instanceof CSimpleType) {
      CBasicType type = ((CSimpleType) expressionType).getType();

      if (type == CBasicType.BOOL && value instanceof BooleanValue) {
        return true;
      }
      if (type == CBasicType.CHAR && value instanceof NumericValue) {
        return true;
      }
      if (type == CBasicType.INT && value instanceof NumericValue) {
        return true;
      }
      if (type == CBasicType.FLOAT && value instanceof NumericValue) {
        return true;
      }
      if (type == CBasicType.DOUBLE && value instanceof NumericValue) {
        return true;
      }
    }

    // Ignore complex types for now
    if (!(expressionType instanceof CSimpleType)) {
      return true;
    }

    return false;
  }
}
