/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Preconditions;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public class ValueInferenceObject implements InferenceObject {

  private final ValueAnalysisState source;
  private final ValueAnalysisState diff;

  private ValueInferenceObject(ValueAnalysisState src, ValueAnalysisState dst) {
    Preconditions.checkNotNull(src);
    Preconditions.checkNotNull(dst);
    source = src;
    diff = dst;
  }

  public static InferenceObject create(ValueAnalysisState src, ValueAnalysisState dst) {
    ValueAnalysisState newSrc = ValueAnalysisState.copyOf(src);
    ValueAnalysisState newDst = ValueAnalysisState.copyOf(dst);

    Set<MemoryLocation> allMems = newSrc.getTrackedMemoryLocations();

    for (MemoryLocation mem : allMems) {
      if (mem.isOnFunctionStack()) {
        newSrc.forget(mem);
      }
    }

    allMems = newDst.getTrackedMemoryLocations();

    for (MemoryLocation mem : allMems) {
      if (mem.isOnFunctionStack()) {
        newDst.forget(mem);
      } else if (newSrc.contains(mem)) {
        Value oldVal = newSrc.getValueFor(mem);
        Value newVal = newDst.getValueFor(mem);
        if (oldVal.equals(newVal)) {
          newDst.forget(mem);
        }
      }
    }

    if (newSrc.equals(newDst)) {
      return EmptyInferenceObject.getInstance();
    } else {
      return new ValueInferenceObject(newSrc, newDst);
    }
  }

  public ValueAnalysisState getDifference() {
    return diff;
  }

  public ValueAnalysisState getSource() {
    return source;
  }

  public boolean compatibleWith(ValueAnalysisState state) {
    return source.compatibleWith(state);
  }
}
