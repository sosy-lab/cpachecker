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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.Address;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcerteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.core.counterexample.LeftHandSide;
import org.sosy_lab.cpachecker.core.counterexample.Memory;
import org.sosy_lab.cpachecker.core.counterexample.MemoryName;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;


public class ValueAnalysisConcreteErrorPathAllocator {

  @SuppressWarnings("unused")
  private final LogManager logger;
  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  private final MachineModel machineModel;

  private MemoryName memoryName = new MemoryName() {

    @Override
    public String getMemoryName(CRightHandSide pExp, Address pAddress) {
      return "Value_Analysis_Heap";
    }
  };

  public ValueAnalysisConcreteErrorPathAllocator(LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    machineModel = pMachineModel;
  }

  public ConcreteStatePath allocateAssignmentsToPath(ARGPath pPath) {

    List<Pair<ValueAnalysisState, CFAEdge>> path = new ArrayList<>(pPath.size());

    PathIterator it = pPath.pathIterator();

    while (it.hasNext()) {
      it.advance();
      ValueAnalysisState state = AbstractStates.extractStateByType(it.getAbstractState(), ValueAnalysisState.class);
      CFAEdge edge = it.getIncomingEdge();

      if (state == null) {
        return null;
      }

      path.add(Pair.of(state, edge));
    }

    return createConcreteStatePath(path);
  }

  public Model allocateAssignmentsToPath(List<Pair<ValueAnalysisState, CFAEdge>> pPath) {

    pPath.remove(pPath.size() - 1);

    ConcreteStatePath concreteStatePath = createConcreteStatePath(pPath);

    CFAPathWithAssumptions pathWithAssignments =
        CFAPathWithAssumptions.of(concreteStatePath, logger, machineModel);

    Model model = Model.empty();

    return model.withAssignmentInformation(pathWithAssignments);
  }

  private ConcreteStatePath createConcreteStatePath(List<Pair<ValueAnalysisState, CFAEdge>> pPath) {

    List<ConcerteStatePathNode> result = new ArrayList<>(pPath.size());

    /*"We generate addresses for our memory locations.
     * This avoids needing to get the CDeclaration
     * representing each memory location, which would be necessary if we
     * wanted to exactly map each memory location to a LeftHandSide.*/
    Map<LeftHandSide, Address> variableAddresses = generateVariableAddresses(pPath);

    for (Pair<ValueAnalysisState, CFAEdge> edgeStatePair : pPath) {

      ValueAnalysisState valueState = edgeStatePair.getFirst();
      CFAEdge edge = edgeStatePair.getSecond();

      ConcerteStatePathNode node;

      if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {

        node = createMultiEdge(valueState, (MultiEdge) edge, variableAddresses);
      } else {
        ConcreteState concreteState = createConcreteState(valueState, variableAddresses);
        node = ConcreteStatePath.valueOfPathNode(concreteState, edge);
      }

      result.add(node);
    }


    return new ConcreteStatePath(result);
  }

  private ConcerteStatePathNode createMultiEdge(ValueAnalysisState pValueState, MultiEdge multiEdge,
      Map<LeftHandSide, Address> pVariableAddresses) {

    int size = multiEdge.getEdges().size();

    ConcreteState[] singleConcreteStates = new ConcreteState[size];

    ListIterator<CFAEdge> iterator = multiEdge.getEdges().listIterator(size);

    Set<CLeftHandSide> alreadyAssigned = new HashSet<>();

    int index = size - 1;
    while (iterator.hasPrevious()) {
      CFAEdge cfaEdge = iterator.previous();

      ConcreteState state;

      // We know only values for LeftHandSides that have not yet been assigned.
      if (allValuesForLeftHandSideKnown(cfaEdge, alreadyAssigned)) {
        state = createConcreteState(pValueState, pVariableAddresses);
      } else {
        state = ConcreteState.empty();
      }
      singleConcreteStates[index] = state;

      addLeftHandSide(cfaEdge, alreadyAssigned);
      index--;
    }

    return ConcreteStatePath.valueOfPathNode(Arrays.asList(singleConcreteStates), multiEdge);
  }

  private boolean allValuesForLeftHandSideKnown(CFAEdge pCfaEdge, Set<CLeftHandSide> pAlreadyAssigned) {

    if (pCfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return isDeclarationValueKnown((CDeclarationEdge) pCfaEdge, pAlreadyAssigned);
    } else if (pCfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      return isStatementValueKnown((CStatementEdge) pCfaEdge, pAlreadyAssigned);
    }

    return false;
  }

  private void addLeftHandSide(CFAEdge pCfaEdge, Set<CLeftHandSide> pAlreadyAssigned) {

    if (pCfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatement stmt = ((CStatementEdge)pCfaEdge).getStatement();

      if(stmt instanceof CAssignment) {
        CLeftHandSide lhs = ((CAssignment) stmt).getLeftHandSide();
        pAlreadyAssigned.add(lhs);
      }
    }
  }

  private boolean isStatementValueKnown(CStatementEdge pCfaEdge, Set<CLeftHandSide> pAlreadyAssigned) {

    CStatement stmt = pCfaEdge.getStatement();

    if (stmt instanceof CAssignment) {
      CLeftHandSide leftHandSide = ((CAssignment) stmt).getLeftHandSide();

      return isLeftHandSideValueKnown(leftHandSide, pAlreadyAssigned);
    }

    return false;
  }

  private boolean isLeftHandSideValueKnown(CLeftHandSide pLHS, Set<CLeftHandSide> pAlreadyAssigned) {

    ValueKnownVisitor v = new ValueKnownVisitor(pAlreadyAssigned);
    return pLHS.accept(v);
  }

  /**
   * Checks, if we know a value. This is the case, if the value will not be assigned in the future.
   * Since we traverse the multi edge from bottom to top, this means if a left hand side, that was already
   * assigned, may not be part of the Left Hand Side we want to know the value of.
   *
   */
  private static class ValueKnownVisitor extends DefaultCExpressionVisitor<Boolean, RuntimeException> {

    private final Set<CLeftHandSide> alreadyAssigned;

    public ValueKnownVisitor(Set<CLeftHandSide> pAlreadyAssigned) {
      alreadyAssigned = pAlreadyAssigned;
    }

    @Override
    protected Boolean visitDefault(CExpression pExp) {
      return true;
    }

    @Override
    public Boolean visit(CArraySubscriptExpression pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CBinaryExpression pE) {
      return pE.getOperand1().accept(this)
          && pE.getOperand2().accept(this);
    }

    @Override
    public Boolean visit(CCastExpression pE) {
      return pE.getOperand().accept(this);
    }

    //TODO Complex Cast
    @Override
    public Boolean visit(CFieldReference pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CIdExpression pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CPointerExpression pE) {
      return !alreadyAssigned.contains(pE);
    }

    @Override
    public Boolean visit(CUnaryExpression pE) {
      return pE.getOperand().accept(this);
    }
  }


  private boolean isDeclarationValueKnown(CDeclarationEdge pCfaEdge, Set<CLeftHandSide> pAlreadyAssigned) {

    CDeclaration dcl = pCfaEdge.getDeclaration();

    if (dcl instanceof CVariableDeclaration) {
      CIdExpression idExp = new CIdExpression(dcl.getFileLocation(), dcl);

      return isLeftHandSideValueKnown(idExp, pAlreadyAssigned);
    }

    return false;
  }

  private Map<LeftHandSide, Address> generateVariableAddresses(List<Pair<ValueAnalysisState, CFAEdge>> pPath) {

    // Get all base IdExpressions for memory locations, ignoring the offset
    Multimap<IDExpression, MemoryLocation> memoryLocationsInPath = getAllMemoryLocationInPath(pPath);

    // Generate consistent Addresses, with non overlapping fields.
    return generateVariableAddresses(memoryLocationsInPath);
  }

  private Map<LeftHandSide, Address> generateVariableAddresses(Multimap<IDExpression, MemoryLocation> pMemoryLocationsInPath) {

    Map<LeftHandSide, Address> result = Maps.newHashMapWithExpectedSize(pMemoryLocationsInPath.size());

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

  private Multimap<IDExpression, MemoryLocation> getAllMemoryLocationInPath(List<Pair<ValueAnalysisState, CFAEdge>> pPath) {

    Multimap<IDExpression, MemoryLocation> result = HashMultimap.create();

    for (Pair<ValueAnalysisState, CFAEdge> edgeStatePair : pPath) {

      ValueAnalysisState valueState = edgeStatePair.getFirst();

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

    for (Entry<MemoryLocation, Value> entry : valueView.entrySet()) {
      MemoryLocation heapLoc = entry.getKey();
      Value valueAsValue = entry.getValue();

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