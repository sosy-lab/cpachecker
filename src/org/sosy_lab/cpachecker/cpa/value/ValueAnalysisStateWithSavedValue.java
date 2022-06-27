// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class ValueAnalysisStateWithSavedValue extends ValueAnalysisState {
  private static final long serialVersionUID = 6590808222591196385L;
  private final Optional<Value> valueFromLastIteration;

  public ValueAnalysisStateWithSavedValue(
      ValueAnalysisState state, Optional<Value> pValueFromLastIteration) {
    super(state);
    this.valueFromLastIteration = pValueFromLastIteration;
  }

  public ValueAnalysisStateWithSavedValue(ValueAnalysisState state) {
    super(state);
    this.valueFromLastIteration = Optional.empty();
  }

  public ValueAnalysisStateWithSavedValue(ValueAnalysisState pState, Map<Integer, String> pValuesMap) {
    super(pState);
    super.setValueMap(pValuesMap);
    this.valueFromLastIteration = Optional.empty();
  }

  public static ValueAnalysisStateWithSavedValue copyOf(ValueAnalysisState state) {
    if (state instanceof ValueAnalysisStateWithSavedValue) {
      return new ValueAnalysisStateWithSavedValue(
          state, ((ValueAnalysisStateWithSavedValue) state).getValueFromLastIteration());
    }
    return new ValueAnalysisStateWithSavedValue(state);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof ValueAnalysisStateWithSavedValue)) {
      return false;
    }
    if (!super.equals(pO)) {
      return false;
    }
    ValueAnalysisStateWithSavedValue that = (ValueAnalysisStateWithSavedValue) pO;
    return Objects.equals(valueFromLastIteration, that.valueFromLastIteration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), valueFromLastIteration);
  }

  public Optional<Value> getValueFromLastIteration() {
    return valueFromLastIteration;
  }
}
