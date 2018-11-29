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

import com.google.common.collect.FluentIterable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.CFAUtils;
public class HarnessState implements AbstractState {

  private final PointerState pointers;
  private final PersistentList<HarnessMemoryLocation> externallyKnownPointers;
  private final LocationFromCallsState locationFromCalls;
  private final ExternFunctionCallsState externFunctionCalls;


  public HarnessState() {
    pointers = new PointerState();
    externallyKnownPointers = PersistentLinkedList.of();
    locationFromCalls = new LocationFromCallsState();
    externFunctionCalls = new ExternFunctionCallsState();
  }

  public HarnessState(
      PointerState pPointers,
      List<HarnessMemoryLocation> pExternallyKnownPointers,
      LocationFromCallsState pLocationFromCallsState,
      ExternFunctionCallsState pExternFunctionCallsState) {
    pointers = pPointers;
    externallyKnownPointers = PersistentLinkedList.copyOf(pExternallyKnownPointers);
    locationFromCalls = pLocationFromCallsState;
    externFunctionCalls = pExternFunctionCallsState;
  }

  public HarnessState addExternallyKnownPointer(HarnessMemoryLocation pPointerLocation) {
    PersistentList<HarnessMemoryLocation> newExternallyKnownPointers =
        externallyKnownPointers.with(pPointerLocation);
    return new HarnessState(
        pointers,
        newExternallyKnownPointers,
        locationFromCalls,
        externFunctionCalls);
  }

  public HarnessState addPointerDeclaration(CVariableDeclaration pVariableDeclaration) {
    String identifier = pVariableDeclaration.getName();
    HarnessMemoryLocation pointerLocation = new DefinedMemoryLocation(identifier);
    PointerState newPointers =
        pointers.addPointer(pointerLocation, UndefinedMemoryLocation.getInstance());
    return new HarnessState(
        newPointers,
        externallyKnownPointers,
        locationFromCalls,
        externFunctionCalls);
  }

  public HarnessState assignPointer(AAssignment pAssignment) {
    String assignedToIdentifier = ((CIdExpression) pAssignment.getLeftHandSide()).getName();
    Optional<HarnessMemoryLocation> assignedToLocation = pointers.fromIdentifier(assignedToIdentifier);
    if (assignedToLocation.isPresent()) {
      if (pAssignment instanceof CFunctionCallAssignmentStatement) {
        HarnessMemoryLocation newTargetLocation = new DefinedMemoryLocation();
        CFunctionCallAssignmentStatement statement = (CFunctionCallAssignmentStatement) pAssignment;
        CFunctionCallExpression functionCallExpression = statement.getFunctionCallExpression();
        CExpression functionName = functionCallExpression.getFunctionNameExpression();
        String functionNameString = functionName.toString();
        if (isSystemAllocationFunction(functionNameString)) {
          PointerState newPointers = pointers.addPointer(assignedToLocation.get(), newTargetLocation);
          return new HarnessState(
              newPointers,
              externallyKnownPointers,
              locationFromCalls,
              externFunctionCalls);
        } else {
          ExternFunctionCall newExternFunctionCall = new ExternFunctionCall();
          ExternFunctionCallsState newExternFunctionCalls = externFunctionCalls.addCall(newExternFunctionCall);
          LocationFromCallsState newLocationFromCalls =
              locationFromCalls.replace(assignedToLocation, newExternFunctionCall);
          PointerState newPointers = pointers.addPointer(assignedToLocation.get(), newTargetLocation);
          return new HarnessState(
              newPointers,
              externallyKnownPointers,
              newLocationFromCalls,
              newExternFunctionCalls
              );
        }
      }
      if (pAssignment instanceof CExpressionAssignmentStatement) {
        CExpressionAssignmentStatement assignment = (CExpressionAssignmentStatement) pAssignment;
        CExpression rightHandSide = assignment.getRightHandSide();
        HarnessMemoryLocation assignedLocation =
            pointers.fromIdentifier(rightHandSide.toString()).get();
        PointerState newPointers =
            pointers.addPointer(assignedToLocation.get(), assignedLocation);
        return new HarnessState(newPointers, externallyKnownPointers,locationFromCalls, externFunctionCalls);
      }
    }
  }




  public HarnessState addExternFunctionCallNoPointerParam(String pFunctionName) {
    ExternFunctionCallsState newExternFunctionCallsState =
        externFunctionCalls.addExternFunctionCall(pFunctionName);
    return new HarnessState(
        pointers,
        externallyKnownPointers,
        locationFromCalls,
        newExternFunctionCallsState);
  }

  public HarnessState addExternFunctionCallWithPointerParams(CFunctionCallExpression pExpression) {
    List<HarnessMemoryLocation> pointerParams =
        pExpression.getParameterExpressions()
            .stream()
            .filter(param -> param.getExpressionType().getCanonicalType() instanceof CPointerType)
            .map(param -> (CIdExpression) param)
            .map(param -> param.getName())
            .map(param -> pointers.fromIdentifier(param).get())
            .filter(param -> (param != null))
            .collect(Collectors.toList());
    PersistentList<HarnessMemoryLocation> newExternallyKnownPointers =
        externallyKnownPointers.withAll(pointerParams);
    return new HarnessState(
        pointers,
        newExternallyKnownPointers,
        locationFromCalls,
        externFunctionCalls);
  }

  public HarnessState assumePointerEquals(CAssumeEdge pAssumeEdge) {
    FluentIterable<String> identifiers =
        CFAUtils.getVariableNamesOfExpression(pAssumeEdge.getExpression());
    if (identifiers.size() == 2) {
      HarnessMemoryLocation leftPointer = pointers.fromIdentifier(identifiers.get(0)).get();
      HarnessMemoryLocation rightPointer = pointers.fromIdentifier(identifiers.get(1)).get();
      boolean leftIsFromExtern = locationFromCalls.isFromExternCall(leftPointer);
      boolean rightIsFromExtern = locationFromCalls.isFromExternCall(rightPointer);

      if (leftIsFromExtern && rightIsFromExtern) {
        LocationFromCallsState newLocationFromCalls =
            locationFromCalls
                .merge(pointers.getTarget(leftPointer), pointers.getTarget(rightPointer));
        PointerState newPointers =
            pointers.merge(leftPointer, pointers.getTarget(rightPointer));
      } else if (leftIsFromExtern && !rightIsFromExtern) {
        LocationFromCallsState newLocationFromCalls =
            locationFromCalls
                .merge(pointers.getTarget(leftPointer), pointers.getTarget(rightPointer));
        PointerState newPointers =
            pointers.addPointer(leftPointer, pointers.getTarget(rightPointer));
      } else if (!leftIsFromExtern && rightIsFromExtern) {
        LocationFromCallsState newLocationFromCalls =
            locationFromCalls
                .merge(pointers.getTarget(rightPointer), pointers.getTarget(leftPointer));
        PointerState newPointers =
            pointers.addPointer(rightPointer, pointers.getTarget(leftPointer));
      }

    }
    return new HarnessState(
        newPointers,
        externallyKnownPointers,
        newLocationFromCalls,
        externFunctionCalls);

  }


  private boolean isSystemAllocationFunction(String functionName) {
    return false;
  }



}
