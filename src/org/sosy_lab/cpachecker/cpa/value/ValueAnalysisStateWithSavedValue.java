// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueAnalysisStateWithSavedValue extends ValueAnalysisState{
  private Optional<Value> valueFromLastIteration;

  public ValueAnalysisStateWithSavedValue(MachineModel pMachineModel, Value pValueFromLastIteration) {
    super(pMachineModel);
    this.valueFromLastIteration = Optional.ofNullable(pValueFromLastIteration);
  }

  public ValueAnalysisStateWithSavedValue(
      Optional<MachineModel> pMachineModel,
      PersistentMap<MemoryLocation, ValueAndType> pConstantsMap, Value pValueFromLastIteration) {
    super(pMachineModel, pConstantsMap);this.valueFromLastIteration =
        Optional.ofNullable(pValueFromLastIteration);
  }



  public  ValueAnalysisStateWithSavedValue(ValueAnalysisState state, Optional<Value> pValueFromLastIteration ) {
   super(state);
    this.valueFromLastIteration = pValueFromLastIteration;
  }

  public  ValueAnalysisStateWithSavedValue(ValueAnalysisState state ) {
    super(state);
    this.valueFromLastIteration = Optional.empty();
  }

  public static ValueAnalysisStateWithSavedValue copyOf(ValueAnalysisState state) {
    if (state instanceof  ValueAnalysisStateWithSavedValue){
      return  new ValueAnalysisStateWithSavedValue(state, ((ValueAnalysisStateWithSavedValue)state).getValueFromLastIteration());
    }
    return new ValueAnalysisStateWithSavedValue(state);
  }


  public Optional<Value> getValueFromLastIteration() {
    return valueFromLastIteration;
  }
}
