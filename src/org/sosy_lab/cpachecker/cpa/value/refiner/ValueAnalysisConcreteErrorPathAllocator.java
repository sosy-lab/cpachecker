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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Address;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssignments;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcerteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.LeftHandSide;
import org.sosy_lab.cpachecker.core.counterexample.Memory;
import org.sosy_lab.cpachecker.core.counterexample.MemoryName;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.Value;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;


public class ValueAnalysisConcreteErrorPathAllocator {

  @SuppressWarnings("unused")
  private final LogManager logger;
  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  private MemoryName memoryName = new MemoryName() {

    @Override
    public String getMemoryName(CRightHandSide pExp, Address pAddress) {
      return "Value_Analysis_Heap";
    }
  };

  public ValueAnalysisConcreteErrorPathAllocator(LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  public Model allocateAssignmentsToPath(ARGPath pPath, MachineModel pMachineModel)
      throws InterruptedException {

    ConcreteStatePath concreteStatePath = createConcreteStatePath(pPath);

    //TODO After multi edges are implemented, erase
    if (concreteStatePath == null) {
      return null;
    }

    CFAPathWithAssignments pathWithAssignments =
        CFAPathWithAssignments.valueOf(concreteStatePath, logger, pMachineModel);

    Model model = Model.empty();

    return model.withAssignmentInformation(pathWithAssignments);
  }

  private ConcreteStatePath createConcreteStatePath(ARGPath pPath) {

    List<ConcerteStatePathNode> result = new ArrayList<>(pPath.size());

    /*"We generate addresses for our memory locations.
     * This avoids needing to get the CDeclaration
     * representing each memory location, which would be necessary if we
     * wanted to exactly map each memory location to a LeftHandSide.*/
    Map<LeftHandSide, Address> variableAddresses = generateVariableAddresses(pPath);

    for (Pair<ARGState, CFAEdge> edgeStatePair : pPath) {

      ValueAnalysisState valueState =
          AbstractStates.extractStateByType(edgeStatePair.getFirst(), ValueAnalysisState.class);
      CFAEdge edge = edgeStatePair.getSecond();

      //TODO erase after multi Edges are implemented
      if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {
        return null;
      }

      ConcreteState concreteState = createConcreteState(valueState, variableAddresses);
      result.add(ConcreteStatePath.valueOfPathNode(concreteState, edge));
    }


    return new ConcreteStatePath(result);
  }

  private Map<LeftHandSide, Address> generateVariableAddresses(ARGPath pPath) {

    // Get all base IdExpressions for memory locations, ignoring the offset
    Multimap<IDExpression, MemoryLocation> memoryLocationsInPath = getAllMemoryLocationInPath(pPath);

    // Generate consistent Addresses, with non overlapping fields.
    return generateVariableAddresses(memoryLocationsInPath);
  }

  private Map<LeftHandSide, Address> generateVariableAddresses(Multimap<IDExpression, MemoryLocation> pMemoryLocationsInPath) {

    Map<LeftHandSide, Address> result = new HashMap<>(pMemoryLocationsInPath.size());

    // Start with Address 0
    Address nextAddressToBeAssigned = Address.valueOf(BigInteger.ZERO);

    for (IDExpression variable : pMemoryLocationsInPath.keySet()) {
      result.put(variable, nextAddressToBeAssigned);

      // leave enough space for values between addresses
      nextAddressToBeAssigned =
          generateNextAddresses(pMemoryLocationsInPath.get(variable), nextAddressToBeAssigned);

    }

    return result;
  }

  private Address generateNextAddresses(Collection<MemoryLocation> pCollection, Address pNextAddressToBeAssigned) {

    long biggestStoredOffsetInPath = 0;

    for (MemoryLocation loc : pCollection) {
      if (loc.getOffset() > biggestStoredOffsetInPath) {
        biggestStoredOffsetInPath = loc.getOffset();
      }
    }

    // Leave enough space for a long Value
    // TODO find good value
    long spaceForLastValue = 64;
    BigInteger offset = BigInteger.valueOf(biggestStoredOffsetInPath + spaceForLastValue);

    return pNextAddressToBeAssigned.addOffset(offset);
  }

  private Multimap<IDExpression, MemoryLocation> getAllMemoryLocationInPath(ARGPath pPath) {

    Multimap<IDExpression, MemoryLocation> result = HashMultimap.create();

    for (Pair<ARGState, CFAEdge> edgeStatePair : pPath) {

      ValueAnalysisState valueState =
          AbstractStates.extractStateByType(edgeStatePair.getFirst(), ValueAnalysisState.class);

      for (MemoryLocation loc : valueState.getConstantsMapView().keySet()) {
        IDExpression idExp = createBaseIdExpresssion(loc);

        if (!result.containsEntry(idExp, loc)) {
          result.put(idExp, loc);
        }
      }
    }
    return result;
  }

  private IDExpression createBaseIdExpresssion(MemoryLocation pLoc) {

    if (!pLoc.isOnFunctionStack()) {
      return new IDExpression(pLoc.getIdentifier());
    } else {
      return new IDExpression(pLoc.getIdentifier(), pLoc.getFunctionName());
    }
  }

  //TODO move to util? (without param generated addresses)
  private ConcreteState createConcreteState(ValueAnalysisState pValueState,
      Map<LeftHandSide, Address> pVariableAddressMap) {


    Map<LeftHandSide, Object> variables = ImmutableMap.of();
    Map<String, Memory> allocatedMemory = allocateAddresses(pValueState, pVariableAddressMap);
    // We assign every variable to the heap, thats why the variable map is empty.
    return new ConcreteState(variables, allocatedMemory, pVariableAddressMap, memoryName);
  }

  private Map<String, Memory> allocateAddresses(ValueAnalysisState pValueState,
      Map<LeftHandSide, Address> pVariableAddressMap) {

    Map<Address, Object> values = createHeapValues(pValueState, pVariableAddressMap);

    // memory name of value analysis does not need to know expression or address
    Memory heap = new Memory(memoryName.getMemoryName(null, null), values);

    Map<String, Memory> result = new HashMap<>();

    result.put(heap.getName(), heap);

    return result;
  }

  private Map<Address, Object> createHeapValues(ValueAnalysisState pValueState,
      Map<LeftHandSide, Address> pVariableAddressMap) {

    Map<MemoryLocation, Value> valueView = pValueState.getConstantsMapView();

    Map<Address, Object> result = new HashMap<>();

    for (MemoryLocation heapLoc : valueView.keySet()) {
      Value valueAsValue = valueView.get(heapLoc);

      if (!valueAsValue.isNumericValue()) {
        // Skip non numerical values for now
        // TODO Should they also be integrated?
        continue;
      }

      Number value = valueAsValue.asNumericValue().getNumber();
      LeftHandSide lhs = createBaseIdExpresssion(heapLoc);
      assert pVariableAddressMap.containsKey(lhs);
      Address baseAddress = pVariableAddressMap.get(lhs);
      Address address = baseAddress.addOffset( BigInteger.valueOf(heapLoc.getOffset()));
      result.put(address, value);
    }

    return result;
  }
}