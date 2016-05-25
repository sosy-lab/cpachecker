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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.Address;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.IntermediateConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.SingleConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.core.counterexample.LeftHandSide;
import org.sosy_lab.cpachecker.core.counterexample.Memory;
import org.sosy_lab.cpachecker.core.counterexample.MemoryName;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class ValueAnalysisConcreteErrorPathAllocator {

  private static final MemoryName MEMORY_NAME = (pExp, pAddress) -> "Value_Analysis_Heap";

  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  public ValueAnalysisConcreteErrorPathAllocator(Configuration pConfig, LogManager pLogger, MachineModel pMachineModel) throws InvalidConfigurationException {
    this.assumptionToEdgeAllocator = new AssumptionToEdgeAllocator(pConfig, pLogger, pMachineModel);
  }

  public ConcreteStatePath allocateAssignmentsToPath(ARGPath pPath) {

    List<Pair<ValueAnalysisState, List<CFAEdge>>> path = new ArrayList<>(pPath.size());

    PathIterator it = pPath.fullPathIterator();

    while (it.hasNext()) {
      List<CFAEdge> innerEdges = new ArrayList<>();

      do {
        it.advance();
        innerEdges.add(it.getIncomingEdge());
      } while (!it.isPositionWithState());

      ValueAnalysisState state =
          AbstractStates.extractStateByType(it.getAbstractState(), ValueAnalysisState.class);

      if (state == null) {
        return null;
      }

      path.add(Pair.of(state, innerEdges));
    }

    return createConcreteStatePath(path);
  }

  public CFAPathWithAssumptions allocateAssignmentsToPath(
      List<Pair<ValueAnalysisState, List<CFAEdge>>> pPath) {
    ConcreteStatePath concreteStatePath = createConcreteStatePath(pPath);
    return CFAPathWithAssumptions.of(concreteStatePath, assumptionToEdgeAllocator);
  }

  private ConcreteStatePath createConcreteStatePath(
      List<Pair<ValueAnalysisState, List<CFAEdge>>> pPath) {

    List<ConcreteStatePathNode> result = new ArrayList<>(pPath.size());

    /*"We generate addresses for our memory locations.
     * This avoids needing to get the CDeclaration
     * representing each memory location, which would be necessary if we
     * wanted to exactly map each memory location to a LeftHandSide.*/
    Map<LeftHandSide, Address> variableAddresses =
        generateVariableAddresses(FluentIterable.from(pPath).transform(Pair::getFirst));

    for (Pair<ValueAnalysisState, List<CFAEdge>> edgeStatePair : pPath) {

      ValueAnalysisState valueState = edgeStatePair.getFirst();
      List<CFAEdge> edges = edgeStatePair.getSecond();

      if (edges.size() > 1) {
        Iterator<CFAEdge> it = Lists.reverse(edges).iterator();
        List<SingleConcreteState> intermediateStates = new ArrayList<>();
        Set<CLeftHandSide> alreadyAssigned = new HashSet<>();
        boolean isFirstIteration = true;
        while (it.hasNext()) {
          CFAEdge innerEdge = it.next();
          ConcreteState state =
              createConcreteStateForMultiEdge(valueState, alreadyAssigned, innerEdge);

          // intermediate edge
          if (isFirstIteration) {
            intermediateStates.add(new SingleConcreteState(innerEdge, state));
            isFirstIteration = false;

            // last edge of (dynamic) multi edge
          } else {
            intermediateStates.add(new IntermediateConcreteState(innerEdge, state));
          }
        }
        result.addAll(Lists.reverse(intermediateStates));

        // a normal edge, no special handling required
      } else {
        result.add(
            new SingleConcreteState(
                Iterables.getOnlyElement(edges),
                new ConcreteState(
                    ImmutableMap.<LeftHandSide, Object>of(),
                    allocateAddresses(valueState, variableAddresses),
                    variableAddresses,
                    MEMORY_NAME)));
      }
    }

    return new ConcreteStatePath(result);
  }

  private ConcreteState createConcreteStateForMultiEdge(
      ValueAnalysisState pValueState, Set<CLeftHandSide> alreadyAssigned, CFAEdge innerEdge) {
    ConcreteState state;

    // We know only values for LeftHandSides that have not yet been assigned.
    if (allValuesForLeftHandSideKnown(innerEdge, alreadyAssigned)) {
      state = createConcreteState(pValueState);
    } else {
      state = ConcreteState.empty();
    }

    // add handled edges to alreadyAssigned list if necessary
    if (innerEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatement stmt = ((CStatementEdge) innerEdge).getStatement();

      if (stmt instanceof CAssignment) {
        CLeftHandSide lhs = ((CAssignment) stmt).getLeftHandSide();
        alreadyAssigned.add(lhs);
      }
    }

    return state;
  }

  public static ConcreteState createConcreteState(ValueAnalysisState pValueState) {
    Map<LeftHandSide, Address> variableAddresses =
        generateVariableAddresses(Collections.singleton(pValueState));
    // We assign every variable to the heap, thats why the variable map is empty.
    return new ConcreteState(
        ImmutableMap.<LeftHandSide, Object>of(),
        allocateAddresses(pValueState, variableAddresses),
        variableAddresses,
        MEMORY_NAME);
  }

  private boolean allValuesForLeftHandSideKnown(
      CFAEdge pCfaEdge, Set<CLeftHandSide> pAlreadyAssigned) {
    if (pCfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      return isDeclarationValueKnown((CDeclarationEdge) pCfaEdge, pAlreadyAssigned);
    } else if (pCfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      return isStatementValueKnown((CStatementEdge) pCfaEdge, pAlreadyAssigned);
    }

    return false;
  }

  private boolean isStatementValueKnown(CStatementEdge pCfaEdge, Set<CLeftHandSide> pAlreadyAssigned) {

    CStatement stmt = pCfaEdge.getStatement();

    if (stmt instanceof CAssignment) {
      CLeftHandSide leftHandSide = ((CAssignment) stmt).getLeftHandSide();

      return isLeftHandSideValueKnown(leftHandSide, pAlreadyAssigned);
    }

    // If the statement is not an assignment, the lvalue does not exist
    return true;
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

    // only variable declaration matter for value analysis
    return true;
  }

  private static Map<LeftHandSide, Address> generateVariableAddresses(Iterable<ValueAnalysisState> pPath) {

    // Get all base IdExpressions for memory locations, ignoring the offset
    Multimap<IDExpression, MemoryLocation> memoryLocationsInPath =
        getAllMemoryLocationsInPath(pPath);

    // Generate consistent Addresses, with non overlapping fields.
    return generateVariableAddresses(memoryLocationsInPath);
  }

  private static Map<LeftHandSide, Address> generateVariableAddresses(Multimap<IDExpression, MemoryLocation> pMemoryLocationsInPath) {

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

  private static Address generateNextAddresses(Collection<MemoryLocation> pCollection, Address pNextAddressToBeAssigned) {

    long biggestStoredOffsetInPath = 0;

    for (MemoryLocation loc : pCollection) {
      if (loc.isReference() && loc.getOffset() > biggestStoredOffsetInPath) {
        biggestStoredOffsetInPath = loc.getOffset();
      }
    }

    // Leave enough space for a long Value
    // TODO find good value
    long spaceForLastValue = 64;
    BigInteger offset = BigInteger.valueOf(biggestStoredOffsetInPath + spaceForLastValue);

    return pNextAddressToBeAssigned.addOffset(offset);
  }

  private static Multimap<IDExpression, MemoryLocation> getAllMemoryLocationsInPath(
      Iterable<ValueAnalysisState> pPath) {

    Multimap<IDExpression, MemoryLocation> result = HashMultimap.create();

    for (ValueAnalysisState valueState : pPath) {
      putIfNotExists(valueState, result);
    }
    return result;
  }

  private static void putIfNotExists(
      ValueAnalysisState pState, Multimap<IDExpression, MemoryLocation> memoryLocationMap) {
    ValueAnalysisState valueState = pState;

    for (MemoryLocation loc : valueState.getConstantsMapView().keySet()) {
      IDExpression idExp = createBaseIdExpresssion(loc);

      if (!memoryLocationMap.containsEntry(idExp, loc)) {
        memoryLocationMap.put(idExp, loc);
      }
    }
  }

  private static IDExpression createBaseIdExpresssion(MemoryLocation pLoc) {

    if (!pLoc.isOnFunctionStack()) {
      return new IDExpression(pLoc.getIdentifier());
    } else {
      return new IDExpression(pLoc.getIdentifier(), pLoc.getFunctionName());
    }
  }

  private static Map<String, Memory> allocateAddresses(ValueAnalysisState pValueState,
      Map<LeftHandSide, Address> pVariableAddressMap) {

    Map<Address, Object> values = createHeapValues(pValueState, pVariableAddressMap);

    // memory name of value analysis does not need to know expression or address
    Memory heap = new Memory(MEMORY_NAME.getMemoryName(null, null), values);

    Map<String, Memory> result = new HashMap<>();

    result.put(heap.getName(), heap);

    return result;
  }

  private static Map<Address, Object> createHeapValues(ValueAnalysisState pValueState,
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
      Address address = baseAddress;
      if (heapLoc.isReference()) {
        address = baseAddress.addOffset(BigInteger.valueOf(heapLoc.getOffset()));
      }
      result.put(address, value);
    }

    return result;
  }
}