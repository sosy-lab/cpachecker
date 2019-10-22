/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.harness;

import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class MemoryLocationPartition {

  private final PersistentMap<MemoryLocation, MemoryLocation> partitionElementToParentMap;

  public MemoryLocationPartition() {
    partitionElementToParentMap = PathCopyingPersistentTreeMap.of();
  }

  public MemoryLocationPartition(
      PersistentMap<MemoryLocation, MemoryLocation> pPartitionElementToParentMap) {
    partitionElementToParentMap = pPartitionElementToParentMap;
  }

  public MemoryLocationPartition
      mergeAndCopy(MemoryLocation pFirstLocation, MemoryLocation pSecondLocation) {
    MemoryLocation newRoot, newChild, firstRoot, secondRoot;
    boolean firstRootIsIndeterminate;

    firstRoot = findRoot(pFirstLocation);
    secondRoot = findRoot(pSecondLocation);
    firstRootIsIndeterminate = firstRoot instanceof IndeterminateMemoryLocation;

    if (firstRootIsIndeterminate) {
      newRoot = secondRoot;
      newChild = firstRoot;
    } else {
      newRoot = firstRoot;
      newChild = secondRoot;
    }

    PersistentMap<MemoryLocation, MemoryLocation> newPartitionElementToParentMap =
        partitionElementToParentMap.putAndCopy(newChild, newRoot);
    return new MemoryLocationPartition(newPartitionElementToParentMap);
  }

  public MemoryLocationPartition addAndCopy(MemoryLocation pNewMemoryLocation) {
    PersistentMap<MemoryLocation, MemoryLocation> newPartitionElementToParentMap;
    newPartitionElementToParentMap =
        partitionElementToParentMap.putAndCopy(pNewMemoryLocation, pNewMemoryLocation);
    return new MemoryLocationPartition(newPartitionElementToParentMap);
  }

  public MemoryLocation findRoot(MemoryLocation pLocation) {
    MemoryLocation currentElement = pLocation;
    MemoryLocation currentElementParent = partitionElementToParentMap.get(currentElement);
    while (currentElement != currentElementParent) {
      currentElement = currentElementParent;
      currentElementParent = partitionElementToParentMap.get(currentElement);
    }
    return currentElement;
  }

  public MemoryLocationPartition addIfNotExists(MemoryLocation pInitializerLocation) {
    MemoryLocation currentValue = partitionElementToParentMap.get(pInitializerLocation);
    if (currentValue == null) {
      PersistentMap<MemoryLocation, MemoryLocation> newPartitionElementToParentMap;
      newPartitionElementToParentMap =
          partitionElementToParentMap.putAndCopy(pInitializerLocation, pInitializerLocation);
      return new MemoryLocationPartition(newPartitionElementToParentMap);
    }
    return this;
  }


}
