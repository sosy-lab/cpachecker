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

import com.google.common.collect.ArrayListMultimap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Optional;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
public class HarnessState implements AbstractState {

  private final PersistentMap<HarnessMemoryLocation, HarnessMemoryLocation> pointers;
  private final ArrayDeque<HarnessMemoryLocation> externallyKnownPointers;
  private final ArrayListMultimap<HarnessMemoryLocation, ArrayDeque<ExternFunctionCall>> locationReturnedBy;
  private final HashMap<String, ArrayDeque<ExternFunctionCall>> externFunctionCalls;

  public HarnessState() {
    pointers = PathCopyingPersistentTreeMap.of();
    externallyKnownPointers = new ArrayDeque<>();
    locationReturnedBy = ArrayListMultimap.create();
    externFunctionCalls = new HashMap<>();
  }

  public HarnessState(
      PersistentSortedMap<HarnessMemoryLocation, HarnessMemoryLocation> pNewPointerMap) {
    pointers = pNewPointerMap;
    externallyKnownPointers = new ArrayDeque<>();
    locationReturnedBy = ArrayListMultimap.create();
    externFunctionCalls = new HashMap<>();
  }

  private void addExternallyKnownPointer(HarnessMemoryLocation pExternallyKnownPointer) {
    externallyKnownPointers.push(pExternallyKnownPointer);
  }

  public PersistentMap<HarnessMemoryLocation,HarnessMemoryLocation>
    newPointerDeclaration(CVariableDeclaration pVariableDeclaration) {
    String identifier = pVariableDeclaration.getName();
    HarnessMemoryLocation pointerLocation =
        new DefinedMemoryLocation(Optional.of(identifier));
    return pointers.putAndCopy(pointerLocation, UndefinedMemoryLocation.getInstance());
  }
// RStudio
  public void assignPointer(AAssignment pAssignment) {
    String identifier = ((CIdExpression) pAssignment.getLeftHandSide()).getName();
    Optional<HarnessMemoryLocation> pointerLocation =
        getPointerFromIdentifier(identifier);
    if (pointerLocation.isPresent()) {
      if ( pAssignment instanceof CExpressionAssignmentStatement ) {
        CExpressionAssignmentStatement assignment = (CExpressionAssignmentStatement) pAssignment;
        CExpression rightHandSide = assignment.getRightHandSide();
        HarnessMemoryLocation assignedLocation = getPointerFromIdentifier( rightHandSide.toString()).get();
        if (rightHandSide instanceof CIdExpression) {
          PersistentMap<HarnessMemoryLocation, HarnessMemoryLocation> newPointers =
              pointers.removeAndCopy(pointers.get(pointerLocation)).putAndCopy(pointerLocation.get(), assignedLocation);
        }
      } else if ( pAssignment instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement assignment = (CFunctionCallAssignmentStatement) pAssignment;
        CFunctionCallExpression functionCallExpression = assignment.getRightHandSide();
        CExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
        String functionName = functionNameExpression.toString();
        if(isSystemFunction(functionName)) {
          HarnessMemoryLocation newTargetLocation = new DefinedMemoryLocation();
          PersistentMap<HarnessMemoryLocation, HarnessMemoryLocation> newPointers =
              pointers.removeAndCopy(pointers.get(pointerLocation)).putAndCopy(pointerLocation.get(), newTargetLocation);
        } else {
          HarnessMemoryLocation newTargetLocation = new DefinedMemoryLocation();
          addExternFunctionCall(functionName, newTargetLocation);
          PersistentMap<HarnessMemoryLocation, HarnessMemoryLocation> newPointers =
              pointers.removeAndCopy(pointers.get(pointerLocation))
                  .putAndCopy(pointerLocation.get(), newTargetLocation);
        }
      }
    }
  }

  // python dictionary, rna central

  private boolean isSystemAllocationFunction(String functionName) {
    return false;
  }

  private HashMap<String, ArrayDeque<ExternFunctionCall>>
      addExternFunctionCall(String pFunctionName, HarnessMemoryLocation pLocation) {
    ArrayDeque<ExternFunctionCall> functionCallInstances = externFunctionCalls.get(pFunctionName);
    ExternFunctionCall newExternFunctionCall = new ExternFunctionCall(pLocation);
    functionCallInstances.push(newExternFunctionCall);
    return externFunctionCalls;
  }

  private void assignPointerAlias(
      HarnessMemoryLocation pPointerLocation,
      HarnessMemoryLocation pAliasTarget) {
  }

  public void assumePointerEquals(String pIdentifier1, String pIdentifier2) {
    Optional<HarnessMemoryLocation> firstPointer = getPointerFromIdentifier(pIdentifier1);
    Optional<HarnessMemoryLocation> secondPointer = getPointerFromIdentifier(pIdentifier2);

  }

  private Optional<HarnessMemoryLocation> getPointerFromIdentifier(String pIdentifier) {
    return pointers.keySet()
        .stream()
        .filter(key -> key.getIdentifier().get().equals(pIdentifier))
        .findFirst();
  }




}
