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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.harness.ComparableFunctionDeclaration;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
public class HarnessState implements AbstractState {

  private final PersistentMap<String, MemoryLocation> pointerVariableAssignments;
  private final MemoryLocationPartition locationEqualityAssumptions;
  private final PersistentList<MemoryLocation> orderedExternallyKnownLocations;
  private final PersistentMap<ComparableFunctionDeclaration, PersistentList<MemoryLocation>> externFunctionCalls;

  public HarnessState() {
    pointerVariableAssignments = PathCopyingPersistentTreeMap.of();
    locationEqualityAssumptions = new MemoryLocationPartition();
    orderedExternallyKnownLocations = PersistentLinkedList.of();
    externFunctionCalls = PathCopyingPersistentTreeMap.of();
  }

  public HarnessState(
      PersistentMap<String, MemoryLocation> pPointerVariableAssignments,
      MemoryLocationPartition pLocationEqualityAssumptions,
      PersistentList<MemoryLocation> pOrderedExternallyKnownLocations,
      PersistentMap<ComparableFunctionDeclaration, PersistentList<MemoryLocation>> pExternFunctionCalls) {
    pointerVariableAssignments = pPointerVariableAssignments;
    locationEqualityAssumptions = pLocationEqualityAssumptions;
    orderedExternallyKnownLocations = pOrderedExternallyKnownLocations;
    externFunctionCalls = pExternFunctionCalls;
  }



  public HarnessState
      addFunctionCall(CExpression pFunctionNameExpression, MemoryLocation pLocation) {
    if (pFunctionNameExpression instanceof CIdExpression) {
      CIdExpression functionIdExpression = (CIdExpression) pFunctionNameExpression;
      CFunctionDeclaration functionDeclaration =
          (CFunctionDeclaration) functionIdExpression.getDeclaration();
      ComparableFunctionDeclaration comparableDeclaration =
          new ComparableFunctionDeclaration(functionDeclaration);
      PersistentList<MemoryLocation> currentFunctionCallsList =
          externFunctionCalls.get(comparableDeclaration);
      PersistentList<MemoryLocation> newFunctionCallsList =
          currentFunctionCallsList.with(pLocation);
      PersistentMap<ComparableFunctionDeclaration, PersistentList<MemoryLocation>> newExternFunctionCalls =
          externFunctionCalls.putAndCopy(comparableDeclaration, newFunctionCallsList);
      HarnessState newState =
          new HarnessState(
              pointerVariableAssignments,
              locationEqualityAssumptions,
              orderedExternallyKnownLocations,
              newExternFunctionCalls);
      return newState;
    }
    return this;
  }

  public HarnessState addPointerTypeEqualityAssumption(
      CExpression firstOperand,
      CExpression secondOperand) {
    MemoryLocation firstLocation = getLocationValueFromPointerTypeExpression(firstOperand);
    MemoryLocation secondLocation = getLocationValueFromPointerTypeExpression(secondOperand);

    MemoryLocationPartition newLocationEqualityAssumptions =
        locationEqualityAssumptions.mergeAndCopy(firstLocation, secondLocation);

    return new HarnessState(
        pointerVariableAssignments,
        newLocationEqualityAssumptions,
        orderedExternallyKnownLocations,
        externFunctionCalls);
  }

  private String getNameFromExpression(CRightHandSide pExpression) {
    // TODO: Check if cast + getQualifiedName even does anything more than toQualifiedASTString
    String name;
    if (pExpression instanceof CIdExpression) {
      CIdExpression idExpression = (CIdExpression) pExpression;
      CSimpleDeclaration declaration = idExpression.getDeclaration();
      name = declaration.getQualifiedName();
    } else {
      name = pExpression.toQualifiedASTString();
    }
    return name;
  }

  private MemoryLocation getLocationValueFromPointerTypeExpression(CExpression pExpression) {
    String name = getNameFromExpression(pExpression);
    MemoryLocation value = pointerVariableAssignments.get(name);
    if (value == null) {
      value = MemoryLocation.valueOf(name);
    }
    return value;
  }

  public HarnessState addPointerVariableToUndefinedFunctionCallAssignment(
      CLeftHandSide pLeftHandSide,
      CFunctionCallExpression pFunctionCallExpression) {
    String lhsString = getNameFromExpression(pLeftHandSide);
    CFunctionDeclaration functionDeclaration = pFunctionCallExpression.getDeclaration();
    ComparableFunctionDeclaration comparableDeclaration =
        new ComparableFunctionDeclaration(functionDeclaration);
    String functionName = functionDeclaration.getQualifiedName();
    PersistentList<MemoryLocation> defaultCalls = PersistentLinkedList.of();
    PersistentList<MemoryLocation> currentCalls =
        externFunctionCalls.getOrDefault(comparableDeclaration, defaultCalls);
    MemoryLocation returnedValue =
        new IndeterminateMemoryLocation(functionName + currentCalls.size(), 0L);
    PersistentList<MemoryLocation> newCalls = currentCalls.with(returnedValue);
    PersistentMap<ComparableFunctionDeclaration, PersistentList<MemoryLocation>> newExternFunctionCalls =
        externFunctionCalls.putAndCopy(comparableDeclaration, newCalls);
    PersistentMap<String, MemoryLocation> newPointerVariableAssignments =
        pointerVariableAssignments.putAndCopy(lhsString, returnedValue);
    MemoryLocationPartition newLocationEqualityAssumptions =
        locationEqualityAssumptions.addAndCopy(returnedValue);
    return new HarnessState(
        newPointerVariableAssignments,
        newLocationEqualityAssumptions,
        orderedExternallyKnownLocations,
        newExternFunctionCalls);
  }

  public HarnessState
      addPointerVariableAssignment(
          CLeftHandSide pLeftHandSide,
          CRightHandSide pRightHandSide) {

    String lhsString = getNameFromExpression(pLeftHandSide);
    String rhsString = getNameFromExpression(pRightHandSide);
    MemoryLocation rhsValue;

    // Get String representations of both sides of the assignment



    // Get a MemoryLocation representing the value of the RightHandSideExpression
    // TODO: properly handle cases where either of the sides is not an Id Expression. By
    // strengthening with pointerAnalysis.

    if (pRightHandSide instanceof CFunctionCallExpression) {
      rhsValue = MemoryLocation.valueOf(rhsString);
    } else {
      rhsValue = pointerVariableAssignments.getOrDefault(rhsString, MemoryLocation.valueOf(""));
    }
    PersistentMap<String, MemoryLocation> newPointerVariableAssignments =
        pointerVariableAssignments.putAndCopy(lhsString, rhsValue);
    return new HarnessState(
        newPointerVariableAssignments,
        locationEqualityAssumptions,
        orderedExternallyKnownLocations,
        externFunctionCalls);
  }

  private HarnessState addExternallyKnownPointer(MemoryLocation pPointerLocation) {
    PersistentList<MemoryLocation> newExternallyKnownPointers =
        orderedExternallyKnownLocations.with(pPointerLocation);
    return new HarnessState(
        pointerVariableAssignments,
        locationEqualityAssumptions,
        newExternallyKnownPointers,
        externFunctionCalls);
  }

  private boolean isAddressOperation(CExpression pExpression) {
    if (pExpression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression) pExpression;
      UnaryOperator operator = unaryExpression.getOperator();
      return operator.getOperator() == "&";
    }
    return false;
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

  public List<Integer> getIndices(ComparableFunctionDeclaration pFunctionDeclaration) {
    AFunctionDeclaration declaration = pFunctionDeclaration.getDeclaration();
    String qualifiedName = declaration.getQualifiedName();
    List<MemoryLocation> locations = externFunctionCalls.get(pFunctionDeclaration);
    List<Integer> result =
        locations.stream().map(location -> getIndex(location)).collect(Collectors.toList());
    return result;
  }

  public Set<ComparableFunctionDeclaration> getFunctionsWithIndices() {
    Set<ComparableFunctionDeclaration> functionCallsKeys = externFunctionCalls.keySet();
    Set<AFunctionDeclaration> functionCallSet =
        functionCallsKeys.stream()
            .map(comparableDeclaration -> comparableDeclaration.getDeclaration())
            .collect(Collectors.toSet());
    return functionCallsKeys;
  }

  private int getIndex(MemoryLocation pLocation) {
    MemoryLocation root = locationEqualityAssumptions.findRoot(pLocation);
    ListIterator<MemoryLocation> listIterator = orderedExternallyKnownLocations.listIterator();
    while (listIterator.hasNext()) {
      MemoryLocation element = listIterator.next();
      if (element == root) {
        int index = listIterator.previousIndex();
        return index;
      }
    }
    return 0;
  }

  public int getExternPointersArrayLength() {
    return orderedExternallyKnownLocations.size();
  }

  public HarnessState updatePointerTarget(CAssignment pAssignment) {
    return this;
  }

  public HarnessState
      addExternallyKnownLocations(List<CExpression> pFunctionParameters) {
    List<String> locationIdentifiers =
        pFunctionParameters.stream()
            .filter(cExpression -> cExpression.getExpressionType() instanceof CPointerType)
            .map(cExpression -> (CIdExpression) cExpression)
            .map(idExpression -> idExpression.getDeclaration().getQualifiedName())
            .collect(Collectors.toList());
    List<MemoryLocation> pointerArgumentValues =
        locationIdentifiers.stream()
            .map(identifier -> pointerVariableAssignments.get(identifier))
            .collect(
            Collectors.toList());
    List<MemoryLocation> newExternallyKnownPointers =
        new LinkedList<>(orderedExternallyKnownLocations);
    newExternallyKnownPointers.addAll(pointerArgumentValues);
    PersistentList<MemoryLocation> persistentNewExternallyKnownPointers =
        PersistentLinkedList.copyOf(newExternallyKnownPointers);
    HarnessState newState =
        new HarnessState(
            pointerVariableAssignments,
            locationEqualityAssumptions,
            persistentNewExternallyKnownPointers,
            externFunctionCalls);
    return newState;
  }

  private MemoryLocation getLocationValueFromPointerTypeInitializer(CInitializer pInitializer) {
    if (pInitializer instanceof CInitializerExpression) {
      CInitializerExpression initializerExpression = (CInitializerExpression) pInitializer;
      return getLocationValueFromPointerTypeExpression(initializerExpression.getExpression());
    } else {
      return MemoryLocation.valueOf(pInitializer.toQualifiedASTString());
    }
  }

  public HarnessState addPointerVariableInitialization(String pName, CInitializer pInitializer) {

    MemoryLocation initializerValue = getLocationValueFromPointerTypeInitializer(pInitializer);
    PersistentMap<String, MemoryLocation> newPointerVariableAssignments =
        pointerVariableAssignments.putAndCopy(pName, initializerValue);
    return new HarnessState(
        newPointerVariableAssignments,
        locationEqualityAssumptions,
        orderedExternallyKnownLocations,
        externFunctionCalls);
  }

  public HarnessState handleArrayDeclarationWithInitializer(
      String pLhs,
      CInitializer pInitializer) {
    if (pInitializer instanceof CInitializerList) {
      return this;
    }
    return this;
  }

  public HarnessState handleStructDeclarationWithPointerFieldWithInitializer(
      HarnessState pState,
      CDeclarationEdge pEdge) {
    // TODO Auto-generated method stub
    /*
     * CVariableDeclaration declaration = (CVariableDeclaration) pEdge.getDeclaration();
     *
     * String identifier = declaration.getName(); String function =
     * pEdge.getPredecessor().getFunctionName();
     *
     * MemoryLocation structLocation = MemoryLocation.valueOf(function, identifier);
     *
     * CType type = pEdge.getDeclaration().getType();
     *
     * CElaboratedType elaboratedDeclarationType = (CElaboratedType) type; CCompositeType
     * realDeclarationType = (CCompositeType) elaboratedDeclarationType.getRealType();
     * List<CCompositeTypeMemberDeclaration> memberDeclarations = realDeclarationType.getMembers();
     * List<CCompositeTypeMemberDeclaration> memberDeclarationsRelevantType =
     * memberDeclarations.stream() .filter( memberDec -> memberDec.getType() instanceof CPointerType
     * || memberDec.getType() instanceof CArrayType) .collect(Collectors.toList());
     *
     * CInitializer initializer = declaration.getInitializer();
     *
     * if (initializer instanceof CInitializerList) { CInitializerList initializerList =
     * (CInitializerList) initializer; List<CInitializer> initializersList =
     * initializerList.getInitializers(); initializersList.forEach(cInitializer -> { if
     * (cInitializer instanceof CDesignatedInitializer) { CDesignatedInitializer
     * designatedInitializer = (CDesignatedInitializer) cInitializer; CInitializer rightHandSide =
     * designatedInitializer.getRightHandSide(); // if(rightHandSide)
     *
     * } }); }
     *
     *
     * // CElaborated types get here and cannot be cast to composite type
     * List<CCompositeTypeMemberDeclaration> structMembers =
     * ((CCompositeType)pEdge.getDeclaration().getType()).getMembers();
     *
     * List<CCompositeTypeMemberDeclaration> pointerTypeMembers = structMembers.stream() .filter(
     * member -> member.getType() instanceof CArrayType || member.getType() instanceof CPointerType)
     * .collect(Collectors.toList());
     */

    return pState;
  }

}
