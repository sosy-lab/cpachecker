// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.Address;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.IntermediateConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.SingleConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.core.counterexample.LeftHandSide;
import org.sosy_lab.cpachecker.core.counterexample.Memory;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.cpa.value.refiner.ConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SMGConcreteErrorPathAllocator extends ConcreteErrorPathAllocator<SMGState> {

  private static final String MEMORY_NAME = "SMG_Heap";

  private final MachineModel machineModel;

  public SMGConcreteErrorPathAllocator(
      Configuration pConfig, LogManager pLogger, MachineModel pMachineModel)
      throws InvalidConfigurationException {
    super(SMGState.class, AssumptionToEdgeAllocator.create(pConfig, pLogger, pMachineModel));
    machineModel = pMachineModel;
  }

  @Override
  protected ConcreteStatePath createConcreteStatePath(List<Pair<SMGState, List<CFAEdge>>> pPath) {

    List<ConcreteStatePathNode> result = new ArrayList<>(pPath.size());

    /*"We generate addresses for our memory locations.
     * This avoids needing to get the CDeclaration
     * representing each memory location, which would be necessary if we
     * wanted to exactly map each memory location to a LeftHandSide.*/
    Map<LeftHandSide, Address> variableAddresses =
        generateVariableAddresses(FluentIterable.from(pPath).transform(Pair::getFirst));

    for (Pair<SMGState, List<CFAEdge>> edgeStatePair : pPath) {

      SMGState valueState = edgeStatePair.getFirst();
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
                    ImmutableMap.of(),
                    allocateAddresses(valueState, variableAddresses),
                    variableAddresses,
                    exp -> MEMORY_NAME,
                    machineModel)));
      }
    }

    return new ConcreteStatePath(result);
  }

  private ConcreteState createConcreteStateForMultiEdge(
      SMGState pValueState, Set<CLeftHandSide> alreadyAssigned, CFAEdge innerEdge) {
    ConcreteState state;

    // We know only values for LeftHandSides that have not yet been assigned.
    if (allValuesForLeftHandSideKnown(innerEdge, alreadyAssigned)) {
      state = createConcreteState(pValueState, machineModel);
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

  public static ConcreteState createConcreteState(
      SMGState pValueState, MachineModel pMachineModel) {
    Map<LeftHandSide, Address> variableAddresses =
        generateVariableAddresses(Collections.singleton(pValueState));
    // We assign every variable to the heap, thats why the variable map is empty.
    return new ConcreteState(
        ImmutableMap.of(),
        allocateAddresses(pValueState, variableAddresses),
        variableAddresses,
        exp -> MEMORY_NAME,
        pMachineModel);
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

  private boolean isStatementValueKnown(
      CStatementEdge pCfaEdge, Set<CLeftHandSide> pAlreadyAssigned) {

    CStatement stmt = pCfaEdge.getStatement();

    if (stmt instanceof CAssignment) {
      CLeftHandSide leftHandSide = ((CAssignment) stmt).getLeftHandSide();

      return isLeftHandSideValueKnown(leftHandSide, pAlreadyAssigned);
    }

    // If the statement is not an assignment, the lvalue does not exist
    return true;
  }

  private static Map<LeftHandSide, Address> generateVariableAddresses(Iterable<SMGState> pPath) {

    // Get all base IdExpressions for memory locations, ignoring the offset
    Multimap<IDExpression, MemoryLocation> memoryLocationsInPath =
        getAllMemoryLocationsInPath(pPath);

    // Generate consistent Addresses, with non overlapping fields.
    return generateVariableAddresses(memoryLocationsInPath);
  }

  private static Map<LeftHandSide, Address> generateVariableAddresses(
      Multimap<IDExpression, MemoryLocation> pMemoryLocationsInPath) {

    Map<LeftHandSide, Address> result =
        Maps.newHashMapWithExpectedSize(pMemoryLocationsInPath.size());

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

  private static Address generateNextAddresses(
      Collection<MemoryLocation> pCollection, Address pNextAddressToBeAssigned) {

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
      Iterable<SMGState> pPath) {

    Multimap<IDExpression, MemoryLocation> result = HashMultimap.create();

    for (SMGState valueState : pPath) {
      putIfNotExists(valueState, result);
    }
    return result;
  }

  private static void putIfNotExists(
      SMGState pState, Multimap<IDExpression, MemoryLocation> memoryLocationMap) {
    SMGState valueState = pState;

    for (MemoryLocation loc : valueState.getTrackedMemoryLocations()) {
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

  private static Map<String, Memory> allocateAddresses(
      SMGState pValueState, Map<LeftHandSide, Address> pVariableAddressMap) {
    Map<Address, Object> values = createHeapValues(pValueState, pVariableAddressMap);
    return ImmutableMap.of(MEMORY_NAME, new Memory(MEMORY_NAME, values));
  }

  private static Map<Address, Object> createHeapValues(
      SMGState pValueState, Map<LeftHandSide, Address> pVariableAddressMap) {

    Map<Address, Object> result = new HashMap<>();

    // This seems wierd on first glance. We take the non heap memory (local/global variables and all
    // their values, including the arrays etc.) and save it as heap.
    for (Entry<MemoryLocation, ValueAndValueSize> entry :
        pValueState.getMemoryModel().getMemoryLocationsAndValuesForSPCWithoutHeap().entrySet()) {
      MemoryLocation heapLoc = entry.getKey();
      Value valueAsValue = entry.getValue().getValue();

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
