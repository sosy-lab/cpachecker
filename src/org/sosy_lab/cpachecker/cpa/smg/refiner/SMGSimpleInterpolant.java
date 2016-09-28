/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGStateInterpolant.SMGPrecisionIncrement;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Set;

public class SMGSimpleInterpolant implements SMGInterpolant {

  public static final SMGInterpolant FALSE = new SMGInterpolant() {

    @Override
    public SMGInterpolant join(SMGInterpolant pOther) {
      return this;
    }

    @Override
    public boolean isTrue() {
      return false;
    }

    @Override
    public boolean isTrivial() {
      return true;
    }

    @Override
    public boolean isFalse() {
      return true;
    }

    @Override
    public SMGPrecisionIncrement getPrecisionIncrement() {
      throw new IllegalStateException();
    }

    @Override
    public Set<SMGMemoryPath> getMemoryLocations() {
      throw new IllegalStateException();
    }
  };

  private static final SMGSimpleInterpolant TRUE = new SMGSimpleInterpolant();

  private final Set<SMGAbstractionBlock> abstractionBlock;
  private final Set<SMGMemoryPath> trackedMemoryPaths;
  private final Set<MemoryLocation> trackedStackVariables;

  private SMGSimpleInterpolant() {
    abstractionBlock = ImmutableSet.of();
    trackedMemoryPaths = ImmutableSet.of();
    trackedStackVariables = ImmutableSet.of();
  }

  public SMGSimpleInterpolant(Set<SMGAbstractionBlock> pAbstractionBlock,
      Set<SMGMemoryPath> pTrackedMemoryPaths, Set<MemoryLocation> pTrackedStackVariables) {
    abstractionBlock = ImmutableSet.copyOf(pAbstractionBlock);
    trackedMemoryPaths = ImmutableSet.copyOf(pTrackedMemoryPaths);
    trackedStackVariables = ImmutableSet.copyOf(pTrackedStackVariables);
  }

  @Override
  public Set<SMGMemoryPath> getMemoryLocations() {
    return trackedMemoryPaths;
  }

  @Override
  public boolean isTrue() {
    return trackedMemoryPaths.isEmpty() && trackedStackVariables.isEmpty();
  }

  @Override
  public boolean isFalse() {
    return false;
  }

  @Override
  public boolean isTrivial() {
    return isTrue() || isFalse();
  }

  @Override
  public SMGInterpolant join(SMGInterpolant pOtherInterpolant) {
    if (pOtherInterpolant.isFalse()) {
      return FALSE;
    }

    SMGPrecisionIncrement incr = pOtherInterpolant.getPrecisionIncrement();

    Builder<SMGAbstractionBlock> abstractionBlockUnion = ImmutableSet.builder();
    Builder<SMGMemoryPath> trackedMemoryPathsUnion = ImmutableSet.builder();
    Builder<MemoryLocation> trackedStackVariablesUnion = ImmutableSet.builder();

    abstractionBlockUnion.addAll(abstractionBlock);
    trackedMemoryPathsUnion.addAll(trackedMemoryPaths);
    trackedStackVariablesUnion.addAll(trackedStackVariables);

    abstractionBlockUnion.addAll(incr.getAbstractionBlock());
    trackedMemoryPathsUnion.addAll(incr.getPathsToTrack());
    trackedStackVariablesUnion.addAll(incr.getStackVariablesToTrack());

    return new SMGSimpleInterpolant(abstractionBlockUnion.build(), trackedMemoryPathsUnion.build(),
        trackedStackVariablesUnion.build());
  }

  public static SMGInterpolant createInitial() {
    return TRUE;
  }

  public static SMGInterpolant getFalseInterpolant() {
    return FALSE;
  }

  @Override
  public String toString() {

    if (isFalse()) {
      return "FALSE";
    } else {
      return "Tracked memory paths: " + trackedMemoryPaths.toString() + "\nstack variables: "
          + trackedStackVariables.toString() + "\nAbstraction locks: "
          + abstractionBlock.toString();
    }
  }

  public static SMGInterpolant getTrueInterpolant() {
    return TRUE;
  }

  @Override
  public SMGPrecisionIncrement getPrecisionIncrement() {
    ImmutableList.Builder<SMGAbstractionBlock> abstractionBlockB = ImmutableList.builder();
    ImmutableList.Builder<SMGMemoryPath> trackedMemoryPathsB = ImmutableList.builder();
    ImmutableList.Builder<MemoryLocation> trackedStackVariablesB = ImmutableList.builder();

    abstractionBlockB.addAll(abstractionBlock);
    trackedMemoryPathsB.addAll(trackedMemoryPaths);
    trackedStackVariablesB.addAll(trackedStackVariables);

    return new SMGPrecisionIncrement(trackedMemoryPathsB.build(), abstractionBlockB.build(), trackedStackVariablesB.build());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((abstractionBlock == null) ? 0 : abstractionBlock.hashCode());
    result = prime * result + ((trackedMemoryPaths == null) ? 0 : trackedMemoryPaths.hashCode());
    result =
        prime * result + ((trackedStackVariables == null) ? 0 : trackedStackVariables.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SMGSimpleInterpolant other = (SMGSimpleInterpolant) obj;
    if (abstractionBlock == null) {
      if (other.abstractionBlock != null) {
        return false;
      }
    } else if (!abstractionBlock.equals(other.abstractionBlock)) {
      return false;
    }
    if (trackedMemoryPaths == null) {
      if (other.trackedMemoryPaths != null) {
        return false;
      }
    } else if (!trackedMemoryPaths.equals(other.trackedMemoryPaths)) {
      return false;
    }
    if (trackedStackVariables == null) {
      if (other.trackedStackVariables != null) {
        return false;
      }
    } else if (!trackedStackVariables.equals(other.trackedStackVariables)) {
      return false;
    }
    return true;
  }
}