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
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public class ValueInferenceObject implements InferenceObject {

  private final ValueAnalysisState source;
  private final ValueAnalysisInformation diff;

  private ValueInferenceObject(ValueAnalysisState src, ValueAnalysisInformation pDst) {
    Preconditions.checkNotNull(src);
    Preconditions.checkNotNull(pDst);
    source = src;
    diff = pDst;
  }

  public static InferenceObject create(ValueAnalysisState src, ValueAnalysisState dst) {
    ValueAnalysisState newSrc = ValueAnalysisState.copyOf(src);

    Set<MemoryLocation> srcMems = newSrc.getTrackedMemoryLocations();
    Set<MemoryLocation> dstMems = dst.getTrackedMemoryLocations();

    PersistentMap<MemoryLocation, ValueAndType> newMap = PathCopyingPersistentTreeMap.of();

    for (MemoryLocation mem : srcMems) {
      if (mem.isOnFunctionStack()) {
        newSrc.forget(mem);
      }
      if (!dstMems.contains(mem)) {
        newMap =
            newMap.putAndCopy(
                mem,
                new ValueAndType(UnknownValue.getInstance(), src.getTypeForMemoryLocation(mem)));
      }
    }

    for (MemoryLocation mem : dstMems) {
      if (mem.isOnFunctionStack()) {

      } else {
        ValueAndType newVal = dst.getValueAndTypeFor(mem);
        if (newSrc.contains(mem) && newSrc.getValueFor(mem).equals(newVal.getValue())) {

        } else {
          newMap = newMap.putAndCopy(mem, newVal);
        }
      }
    }

    if (newMap.size() == 0) {
      return EmptyInferenceObject.getInstance();
    } else {

      ValueAnalysisInformation info =
          new ValueAnalysisInformation(newMap);
      return new ValueInferenceObject(newSrc, info);
    }
  }

  public ValueInferenceObject merge(ValueInferenceObject pObject) {
    ValueAnalysisState src1 = getSource();
    ValueAnalysisState src2 = pObject.getSource();

    ValueAnalysisInformation diff1 = getDifference();
    ValueAnalysisInformation diff2 = pObject.getDifference();

    Map<MemoryLocation, ValueAndType> map1 = diff1.getAssignments();
    Map<MemoryLocation, ValueAndType> map2 = diff2.getAssignments();

    PersistentMap<MemoryLocation, ValueAndType> newMap = PathCopyingPersistentTreeMap.of();

    Set<MemoryLocation> jointMems = Sets.union(map1.keySet(), map2.keySet());

    for (MemoryLocation mem : jointMems) {
      if (map1.containsKey(mem) && !map2.containsKey(mem)) {
        newMap = newMap.putAndCopy(mem, map1.get(mem));
      } else if (map2.containsKey(mem) && !map1.containsKey(mem)) {
        newMap = newMap.putAndCopy(mem, map2.get(mem));
      } else if (map1.containsKey(mem) && map2.containsKey(mem)) {
        ValueAndType val1 = map1.get(mem);
        ValueAndType val2 = map2.get(mem);

        ValueAndType newValue;

        if (val1.equals(val2)) {
          newValue = val1;
        } else {
          newValue = new ValueAndType(UnknownValue.getInstance(), val1.getType());
        }

        newMap = newMap.putAndCopy(mem, newValue);
      } else {
        assert false;
      }
    }

    ValueAnalysisState newSrc = src1.join(src2);

    if (newMap.equals(map1) && newSrc.equals(src1)) {
      return this;
    } else if (newMap.equals(map2) && newSrc.equals(src2)) {
      return pObject;
    } else {

      ValueAnalysisInformation newDiff = new ValueAnalysisInformation(newMap);
      return new ValueInferenceObject(newSrc, newDiff);
    }
  }

  @Override
  public ValueInferenceObject clone() {
    return new ValueInferenceObject(source, diff);
  }

  public ValueAnalysisInformation getDifference() {
    return diff;
  }

  public ValueAnalysisState getSource() {
    return source;
  }

  public boolean compatibleWith(ValueAnalysisState state) {
    return source.compatibleWith(state);
  }

  @Override
  public boolean hasEmptyAction() {
    assert diff.getAssignments().size() > 0;
    return false;
  }
}
