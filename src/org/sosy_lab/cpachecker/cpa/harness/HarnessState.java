/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.harness.ComparableFunctionDeclaration;
public class HarnessState implements AbstractState {

  private final PointerState pointers;
  private final PersistentList<MemoryLocation> externallyKnownLocations;
  private final ExternFunctionCallsState externFunctionCalls;
  public static Set<CFunctionDeclaration> relevantFunctions = new HashSet<>();


  public HarnessState() {
    pointers = new PointerState();
    externallyKnownLocations = PersistentLinkedList.of();
    externFunctionCalls = new ExternFunctionCallsState();
  }

  public HarnessState(
      PointerState pPointers,
      List<MemoryLocation> pExternallyKnownPointers,
      ExternFunctionCallsState pExternFunctionCallsState) {
    pointers = pPointers;
    externallyKnownLocations = PersistentLinkedList.copyOf(pExternallyKnownPointers);
    externFunctionCalls = pExternFunctionCallsState;
  }

  public HarnessState
      addFunctionCall(CExpression pFunctionNameExpression, MemoryLocation pLocation) {
    if (pFunctionNameExpression instanceof CIdExpression) { // TODO handle other functionNames
      final CIdExpression functionIdExpression = (CIdExpression) pFunctionNameExpression;
      final String functionName = functionIdExpression.getName();
      final CFunctionDeclaration functionDeclaration =
          (CFunctionDeclaration) functionIdExpression.getDeclaration();
      final ComparableFunctionDeclaration comparableFunctionDeclaration =
          new ComparableFunctionDeclaration(functionDeclaration);
      ExternFunctionCallsState newExternFunctionCalls =
          externFunctionCalls.addExternFunctionCall(comparableFunctionDeclaration, pLocation);
      HarnessState newState =
          new HarnessState(pointers, externallyKnownLocations, newExternFunctionCalls);
      return newState;
    }
    return this;
  }

  public HarnessState addExternallyKnownPointer(MemoryLocation pPointerLocation) {
    PersistentList<MemoryLocation> newExternallyKnownPointers =
        externallyKnownLocations.with(pPointerLocation);
    return new HarnessState(
        pointers,
        newExternallyKnownPointers,
        externFunctionCalls);
  }

  public HarnessState addPointerDeclaration(CVariableDeclaration pVariableDeclaration) {
    String identifier = pVariableDeclaration.getName();
    MemoryLocation pointerLocation = new MemoryLocation(identifier);
    MemoryLocation pointerTarget = new MemoryLocation();
    PointerState newPointers =
        pointers.addPointer(pointerLocation, pointerTarget);
    return new HarnessState(
        newPointers,
        externallyKnownLocations,
        externFunctionCalls);
  }

  public HarnessState
      addExternFunctionCallNoPointerParam(ComparableFunctionDeclaration pFunctionDeclaration) {
    ExternFunctionCallsState newExternFunctionCallsState =
        externFunctionCalls.addExternFunctionCall(pFunctionDeclaration);
    return new HarnessState(
        pointers,
        externallyKnownLocations,
        newExternFunctionCallsState);
  }

  public HarnessState addExternFunctionCallWithPointerParams(CFunctionCallExpression pExpression) {
   /* List<MemoryLocation> pointerParams =
        pExpression.getParameterExpressions()
            .stream()
            .filter(param -> param.getExpressionType().getCanonicalType() instanceof CPointerType)
            .map(param -> (CIdExpression) param)
            .map(param -> param.getName())
            .map(param -> pointers.fromIdentifier(param).get())
            .filter(param -> (param != null))
            .collect(Collectors.toList());*/
    /*
     * PersistentList<MemoryLocation> newExternallyKnownPointers =
     * externallyKnownPointers.withAll(pExpression.getParameterExpressions()); return new
     * HarnessState( pointers, newExternallyKnownPointers, externFunctionCalls);
     */
    return this;
  }

  public HarnessState
      addPointsToInformation(CExpression pLeftHandSide, MemoryLocation pLocation) {
    // TODO: handle cases where leftHandSide is not an IdExpression, e.g. a[3] = malloc()
    CIdExpression idExpression = (CIdExpression) pLeftHandSide;
    String lhsName = idExpression.getName();
    MemoryLocation lhsLocation = pointers.fromIdentifier(lhsName);
    PointerState newPointers = pointers.addPointerIfNotExists(lhsLocation, pLocation);
    HarnessState newState =
        new HarnessState(newPointers, externallyKnownLocations, externFunctionCalls);
    return newState;
  }

  public HarnessState merge(String pIdentifier1, String pIdentifier2) {

    MemoryLocation location1 = pointers.fromIdentifier(pIdentifier1);
    MemoryLocation location2 = pointers.fromIdentifier(pIdentifier2);
    MemoryLocation location1Target = pointers.getTarget(location1);
    MemoryLocation location2Target = pointers.getTarget(location2);
    PointerState newPointers = pointers.merge(location1Target, location2Target);
    ExternFunctionCallsState newExternFunctionCallsState =
        externFunctionCalls.merge(location1Target, location2Target);

    return new HarnessState(
        newPointers,
        externallyKnownLocations,
        newExternFunctionCallsState);
  }

  public List<Integer> getIndices(ComparableFunctionDeclaration pFunctionName) {
    List<MemoryLocation> locations = externFunctionCalls.getCalls(pFunctionName);
    List<Integer> result =
        locations.stream().map(location -> getIndex(location)).collect(Collectors.toList());
    return result;
  }

  public Set<ComparableFunctionDeclaration> getFunctionsWithIndices() {
    Set<ComparableFunctionDeclaration> functionCallsKeys = externFunctionCalls.getKeys();
    return functionCallsKeys;
  }

  private int getIndex(MemoryLocation pLocation) {
    int result = externallyKnownLocations.indexOf(pLocation);
    if (result == -1) {
      result = 0;
    }
    return result;
  }

  public int getExternPointersArrayLength() {
    return externallyKnownLocations.size();
  }

  public HarnessState
      addExternallyKnownLocations(List<CExpression> pFunctionParameters) {
    // TODO: handle case that arguments are not given by id expressions, like foo(&i);
    List<String> locationIdentifiers =
        pFunctionParameters.stream()
            .map(cExpression -> (CIdExpression) cExpression)
            .map(idExpression -> idExpression.getName())
            .collect(Collectors.toList());
    List<MemoryLocation> pointerArgumentKeys =
        locationIdentifiers.stream().map(identifier -> pointers.fromIdentifier(identifier)).collect(
            Collectors.toList());
    List<MemoryLocation> pointerArgumentValues =
        pointerArgumentKeys.stream().map(location -> pointers.getTarget(location)).collect(
            Collectors.toList());
    List<MemoryLocation> newExternallyKnownPointers = new LinkedList<>(externallyKnownLocations);
    newExternallyKnownPointers.addAll(pointerArgumentValues);
    PersistentList<MemoryLocation> persistentNewExternallyKnownPointers =
        PersistentLinkedList.copyOf(newExternallyKnownPointers);
    PointerState newPointers = pointers;
    for (MemoryLocation location : pointerArgumentValues) {
      newPointers = newPointers.addPointerIfNotExists(location);
    }
    HarnessState newState =
        new HarnessState(newPointers, persistentNewExternallyKnownPointers, externFunctionCalls);
    return newState;
  }
}
