// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.CLangStackFrame;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

/** This utility class computes MemoryPaths for a SMG. */
public class SMGMemoryPathCollector {

  private enum SMGObjectPosition {
    STACK,
    HEAP,
    GLOBAL
  }

  private final UnmodifiableCLangSMG smg;

  public SMGMemoryPathCollector(UnmodifiableCLangSMG pSmg) {
    smg = pSmg;
  }

  public Set<SMGMemoryPath> getMemoryPaths() {

    Set<SMGMemoryPath> result = new LinkedHashSet<>();
    Set<SMGObject> reached = new LinkedHashSet<>();

    getMemoryPathsFromGlobalVariables(result, reached);
    getMemoryPathsFromStack(result, reached);

    return Collections.unmodifiableSet(result);
  }

  public Map<SMGObject, SMGMemoryPath> getHeapObjectMemoryPaths() {

    Map<SMGObject, SMGMemoryPath> result = new LinkedHashMap<>();
    Set<SMGObject> reached = new LinkedHashSet<>();

    getHeapObjectMemoryPathsFromGlobalVariables(result, reached);
    getHeapObjectMemoryPathsFromStack(result, reached);

    return Collections.unmodifiableMap(result);
  }

  private void getMemoryPathsFromGlobalVariables(
      Set<SMGMemoryPath> pResult, Set<SMGObject> pReached) {
    for (Entry<String, SMGRegion> entry : smg.getGlobalObjects().entrySet()) {
      getMemoryPathsFromObject(
          entry.getValue(),
          pResult,
          pReached,
          SMGObjectPosition.GLOBAL,
          null,
          null,
          null,
          entry.getKey());
    }
  }

  private void getMemoryPathsFromStack(Set<SMGMemoryPath> pResult, Set<SMGObject> pReached) {

    int pLocationOnStack = 0;

    for (CLangStackFrame frame : smg.getStackFrames()) {
      String functionName = frame.getFunctionDeclaration().getName();

      for (Entry<String, SMGRegion> entry : frame.getVariables().entrySet()) {
        getMemoryPathsFromObject(
            entry.getValue(),
            pResult,
            pReached,
            SMGObjectPosition.STACK,
            null,
            functionName,
            pLocationOnStack,
            entry.getKey());
      }

      if (frame.getReturnObject() != null) {
        getMemoryPathsFromObject(
            frame.getReturnObject(),
            pResult,
            pReached,
            SMGObjectPosition.STACK,
            null,
            functionName,
            pLocationOnStack,
            frame.getReturnObject().getLabel());
      }

      pLocationOnStack = pLocationOnStack + 1;
    }
  }

  private void getMemoryPathsFromObject(
      SMGObject pSmgObject,
      Set<SMGMemoryPath> pResult,
      Set<SMGObject> pReached,
      SMGObjectPosition pPos,
      SMGMemoryPath pParent,
      String pFunctionName,
      Integer pLocationOnStack,
      String pVariableName) {

    List<Long> offsets = new ArrayList<>();
    Map<Long, SMGObject> offsetToRegion = new HashMap<>();
    Map<Long, SMGMemoryPath> offsetToParent = new HashMap<>();

    for (SMGEdgeHasValue objectHve :
        smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pSmgObject))) {
      SMGValue value = objectHve.getValue();
      long offset = objectHve.getOffset();

      SMGMemoryPath path =
          getSMGMemoryPath(pVariableName, offset, pPos, pFunctionName, pLocationOnStack, pParent);
      pResult.add(path);

      if (smg.isPointer(value)) {
        SMGObject rObject = smg.getObjectPointedBy(value);

        if (smg.isHeapObject(rObject) && !pReached.contains(rObject)) {
          pReached.add(rObject);
          offsets.add(offset);
          offsetToRegion.put(offset, rObject);
          offsetToParent.put(offset, path);
        }
      }
    }

    Collections.sort(offsets);

    for (long offset : offsets) {
      SMGObject smgObject = offsetToRegion.get(offset);
      SMGMemoryPath currentPath = offsetToParent.get(offset);
      getMemoryPathsFromObject(
          smgObject, pResult, pReached, SMGObjectPosition.HEAP, currentPath, null, null, null);
    }
  }

  private void getHeapObjectMemoryPathsFromGlobalVariables(
      Map<SMGObject, SMGMemoryPath> pResult, Set<SMGObject> pReached) {
    for (Entry<String, SMGRegion> entry : smg.getGlobalObjects().entrySet()) {
      getHeapObjectMemoryPathsFromObject(
          entry.getValue(),
          pResult,
          pReached,
          SMGObjectPosition.GLOBAL,
          null,
          null,
          null,
          entry.getKey());
    }
  }

  private void getHeapObjectMemoryPathsFromStack(
      Map<SMGObject, SMGMemoryPath> pResult, Set<SMGObject> pReached) {

    int pLocationOnStack = 0;

    for (CLangStackFrame frame : smg.getStackFrames()) {
      String functionName = frame.getFunctionDeclaration().getName();

      for (Entry<String, SMGRegion> entry : frame.getVariables().entrySet()) {
        getHeapObjectMemoryPathsFromObject(
            entry.getValue(),
            pResult,
            pReached,
            SMGObjectPosition.STACK,
            null,
            functionName,
            pLocationOnStack,
            entry.getKey());
      }

      if (frame.getReturnObject() == null) {
        continue;
      }

      getHeapObjectMemoryPathsFromObject(
          frame.getReturnObject(),
          pResult,
          pReached,
          SMGObjectPosition.STACK,
          null,
          functionName,
          pLocationOnStack,
          frame.getReturnObject().getLabel());
      pLocationOnStack = pLocationOnStack + 1;
    }
  }

  private void getHeapObjectMemoryPathsFromObject(
      SMGObject pSmgObject,
      Map<SMGObject, SMGMemoryPath> pResult,
      Set<SMGObject> pReached,
      SMGObjectPosition pPos,
      SMGMemoryPath pParent,
      String pFunctionName,
      Integer pLocationOnStack,
      String pVariableName) {

    List<Long> offsets = new ArrayList<>();
    Map<Long, SMGObject> offsetToRegion = new HashMap<>();
    Map<Long, SMGMemoryPath> offsetToParent = new HashMap<>();

    for (SMGEdgeHasValue objectHve :
        smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pSmgObject))) {
      SMGValue value = objectHve.getValue();

      if (!smg.isPointer(value)) {
        continue;
      }

      SMGObject rObject = smg.getObjectPointedBy(value);
      long offset = objectHve.getOffset();

      if (!smg.isHeapObject(rObject) || pReached.contains(rObject)) {
        continue;
      }

      pReached.add(rObject);
      offsets.add(offset);
      offsetToRegion.put(offset, rObject);

      SMGMemoryPath path =
          getSMGMemoryPath(pVariableName, offset, pPos, pFunctionName, pLocationOnStack, pParent);

      offsetToParent.put(offset, path);
      pResult.put(rObject, path);
    }

    Collections.sort(offsets);

    for (long offset : offsets) {
      SMGObject smgObject = offsetToRegion.get(offset);
      SMGMemoryPath currentPath = offsetToParent.get(offset);
      getHeapObjectMemoryPathsFromObject(
          smgObject, pResult, pReached, SMGObjectPosition.HEAP, currentPath, null, null, null);
    }
  }

  private SMGMemoryPath getSMGMemoryPath(
      String pVariableName,
      long pOffset,
      SMGObjectPosition pPos,
      String pFunctionName,
      Integer pLocationOnStack,
      SMGMemoryPath pParent) {

    switch (pPos) {
      case GLOBAL:
        return SMGMemoryPath.valueOf(pVariableName, pOffset);
      case STACK:
        return SMGMemoryPath.valueOf(pVariableName, pFunctionName, pOffset, pLocationOnStack);
      case HEAP:
        return SMGMemoryPath.valueOf(pParent, pOffset);
      default:
        throw new AssertionError();
    }
  }
}
