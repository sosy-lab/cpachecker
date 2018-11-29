/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.harness;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.util.harness.ComparableFunctionDeclaration;

public class ExternFunctionCallsState {

  private final PersistentMap<ComparableFunctionDeclaration, PersistentList<MemoryLocation>> externFunctionCalls;

  public ExternFunctionCallsState() {
    externFunctionCalls = PathCopyingPersistentTreeMap.of();
  }

  public ExternFunctionCallsState(
      PersistentMap<ComparableFunctionDeclaration, PersistentList<MemoryLocation>> pNewExternFunctionCalls) {
    externFunctionCalls = pNewExternFunctionCalls;
  }

  public ExternFunctionCallsState
      addExternFunctionCall(ComparableFunctionDeclaration pFunctionDeclaration) {
    MemoryLocation newLocation = new MemoryLocation(false);
    return addExternFunctionCall(pFunctionDeclaration, newLocation);
  }

  public ExternFunctionCallsState
      addExternFunctionCall(
          ComparableFunctionDeclaration pFunctionDeclaration,
          MemoryLocation pLocation) {
    PersistentList<MemoryLocation> functionCalls = externFunctionCalls.get(pFunctionDeclaration);
    PersistentList<MemoryLocation> newFunctionCalls;
    if (functionCalls != null) {
      newFunctionCalls = functionCalls.with(pLocation);
    } else {
      newFunctionCalls = PersistentLinkedList.of(pLocation);
    }
    PersistentMap<ComparableFunctionDeclaration, PersistentList<MemoryLocation>> newExternFunctionCalls =
        externFunctionCalls.putAndCopy(pFunctionDeclaration, newFunctionCalls);
    ExternFunctionCallsState newExternFunctionCallsState =
        new ExternFunctionCallsState(newExternFunctionCalls);
    return newExternFunctionCallsState;
  }

  public ExternFunctionCallsState merge(MemoryLocation pLocation1, MemoryLocation pLocation2) {
    if (pLocation1.isPrecise() && pLocation2.isPrecise()) {
      return this;
    }
    MemoryLocation mergeTarget = pLocation1.isPrecise() ? pLocation1 : pLocation2;
    MemoryLocation mergeSource = mergeTarget == pLocation1 ? pLocation2 : pLocation1;

    // TODO: vergleiche Performance von komplett neuer map mit iterator, mit stream()/map Ansatz
    Map<ComparableFunctionDeclaration, PersistentList<MemoryLocation>> newFunctionCalls =
        new HashMap<>();
    Iterator<Entry<ComparableFunctionDeclaration, PersistentList<MemoryLocation>>> MapIterator =
        ((PathCopyingPersistentTreeMap<ComparableFunctionDeclaration, PersistentList<MemoryLocation>>) externFunctionCalls)
            .entryIterator();
    while (MapIterator.hasNext()) {
      List<MemoryLocation> newLocations = new LinkedList<>();
      Entry<ComparableFunctionDeclaration, PersistentList<MemoryLocation>> mapEntry =
          MapIterator.next();
      PersistentList<MemoryLocation> mapValue = mapEntry.getValue();
      Iterator<MemoryLocation> listIterator = mapValue.listIterator();
      while (listIterator.hasNext()) {
        MemoryLocation listElement = listIterator.next();
        if (listElement == mergeSource) {
          newLocations.add(mergeTarget);
        } else {
          newLocations.add(listElement);
        }
      }
      PersistentList<MemoryLocation> persistentLocations =
          PersistentLinkedList.copyOf(newLocations);
      newFunctionCalls.put(mapEntry.getKey(), persistentLocations);
    }
    PersistentMap<ComparableFunctionDeclaration, PersistentList<MemoryLocation>> persistentMap =
        PathCopyingPersistentTreeMap.copyOf(newFunctionCalls);
    ExternFunctionCallsState newFunctionCallsState = new ExternFunctionCallsState(persistentMap);
    return newFunctionCallsState;
  }

  List<MemoryLocation> getCalls(ComparableFunctionDeclaration pFunctionDeclaration) {
    List<MemoryLocation> callsForName = externFunctionCalls.get(pFunctionDeclaration);
    return callsForName;
  }

  Set<ComparableFunctionDeclaration> getKeys() {
    return externFunctionCalls.keySet();
  }

}
