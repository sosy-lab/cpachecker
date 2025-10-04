// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.ValueAndValueSize;
import org.sosy_lab.cpachecker.cpa.value.refiner.ConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

public class SMGConcreteErrorPathAllocator extends ConcreteErrorPathAllocator<SMGState> {

  // this analysis puts every object in the same heap
  private static final String MEMORY_NAME = "SMGv2_Analysis_Heap";

  // Map Object <-> some address distinct from 0
  private final Map<SMGObject, Address> addressOfObjectMap = new HashMap<>();
  private Address nextAlloc = Address.valueOf(BigInteger.valueOf(100));
  private final Map<LeftHandSide, Address> variableAddressMap = new HashMap<>();

  public SMGConcreteErrorPathAllocator(
      Configuration pConfig, LogManager pLogger, MachineModel pMachineModel)
      throws InvalidConfigurationException {
    super(SMGState.class, AssumptionToEdgeAllocator.create(pConfig, pLogger, pMachineModel));
  }

  @Override
  protected ConcreteStatePath createConcreteStatePath(List<Pair<SMGState, List<CFAEdge>>> pPath) {

    // TODO: this is missing heap completely, and is also just BAD....
    ImmutableList.Builder<ConcreteStatePathNode> pathBuilder = ImmutableList.builder();

    /* This is more or less a modified copy of what value does.
     * We generate addresses for our memory locations.
     * This avoids needing to get the CDeclaration
     * representing each memory location, which would be necessary if we
     * wanted to exactly map each memory location to a LeftHandSide.*/
    Map<LeftHandSide, Address> variableAddresses =
        generateVariableAddresses(FluentIterable.from(pPath).transform(Pair::getFirst));

    for (Pair<SMGState, List<CFAEdge>> edgeStatePair : pPath) {

      SMGState smgState = checkNotNull(edgeStatePair.getFirst());
      List<CFAEdge> edges = checkNotNull(edgeStatePair.getSecond());

      if (edges.size() > 1) {
        // Multi-edge. E.g. in the beginning of the program declaring all the types etc.
        handleMultiEdge(smgState, edges, pathBuilder);

      } else {
        // a normal edge, no special handling required
        pathBuilder.add(
            new SingleConcreteState(
                edges.getFirst(),
                new ConcreteState(
                    ImmutableMap.of(),
                    allocateAddresses(smgState, variableAddresses),
                    variableAddresses,
                    exp -> MEMORY_NAME)));
      }
    }

    return new ConcreteStatePath(pathBuilder.build());
  }

  private void handleMultiEdge(
      SMGState pState,
      List<CFAEdge> edges,
      ImmutableList.Builder<ConcreteStatePathNode> pathBuilder) {
    ImmutableList.Builder<SingleConcreteState> intermediateStatesBuilder = ImmutableList.builder();
    Set<CLeftHandSide> alreadyAssigned = new HashSet<>();
    boolean isFirstIteration = true;
    for (CFAEdge innerEdge : edges.reversed()) {
      ConcreteState state = createConcreteStateForMultiEdge(pState, alreadyAssigned, innerEdge);

      // intermediate edge
      if (isFirstIteration) {
        intermediateStatesBuilder.add(new SingleConcreteState(innerEdge, state));
        isFirstIteration = false;

        // last edge of (dynamic) multi edge
      } else {
        intermediateStatesBuilder.add(new IntermediateConcreteState(innerEdge, state));
      }
    }
    pathBuilder.addAll(intermediateStatesBuilder.build().reverse());
  }

  private ConcreteState createConcreteStateForMultiEdge(
      SMGState pState, Set<CLeftHandSide> alreadyAssigned, CFAEdge innerEdge) {
    ConcreteState state;

    // We know only values for LeftHandSides that have not yet been assigned.
    if (allValuesForLeftHandSideKnown(innerEdge, alreadyAssigned)) {
      state = createConcreteState(pState, pState.getMachineModel());
    } else {
      state = ConcreteState.empty();
    }

    // add handled edges to alreadyAssigned list if necessary
    if (innerEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatement stmt = ((CStatementEdge) innerEdge).getStatement();

      if (stmt instanceof CAssignment cAssignment) {
        CLeftHandSide lhs = cAssignment.getLeftHandSide();
        alreadyAssigned.add(lhs);
      }
    }

    return state;
  }

  private ConcreteState createConcreteState(SMGState pSMGState, MachineModel pMachineModel) {
    Map<LeftHandSide, Address> variableAddresses =
        generateVariableAddresses(Collections.singleton(pSMGState));
    // We assign every variable to the heap, that's why the variable map is empty.
    return new ConcreteState(
        ImmutableMap.of(),
        allocateAddresses(pSMGState, variableAddresses),
        variableAddresses,
        exp -> MEMORY_NAME,
        pMachineModel);
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
      Iterable<SMGState> pStatesOnPath) {

    Multimap<IDExpression, MemoryLocation> result = HashMultimap.create();

    for (SMGState state : pStatesOnPath) {
      putIfNotExists(state, result);
    }
    return result;
  }

  private static void putIfNotExists(
      SMGState pState, Multimap<IDExpression, MemoryLocation> memoryLocationMap) {

    for (MemoryLocation loc : pState.getTrackedMemoryLocations()) {
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

    if (stmt instanceof CAssignment cAssignment) {
      CLeftHandSide leftHandSide = cAssignment.getLeftHandSide();

      return isLeftHandSideValueKnown(leftHandSide, pAlreadyAssigned);
    }

    // If the statement is not an assignment, the lvalue does not exist
    return true;
  }

  // This assigns only stack variables for now!!!!! The term Heap is a lie here!
  private Map<String, Memory> allocateAddresses(
      SMGState pSMGState, Map<LeftHandSide, Address> pVariableAddressMap) {
    Map<Address, Object> values = createHeapValues(pSMGState, pVariableAddressMap);
    return ImmutableMap.of(MEMORY_NAME, new Memory(MEMORY_NAME, values));
  }

  @SuppressWarnings("unused")
  private Map<Address, Object> createHeapValues(SMGState pSMGState) {

    ImmutableMap.Builder<Address, Object> addressToHeapMapBuilder = ImmutableMap.builder();

    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> entry :
        pSMGState.getMemoryModel().getSmg().getSMGObjectsWithSMGHasValueEdges().entrySet()) {
      for (SMGHasValueEdge hvEdge : entry.getValue()) {

        BigInteger value;
        SMGValue smgValue = hvEdge.hasValue();
        Optional<Value> valueForSMGValue =
            pSMGState.getMemoryModel().getValueFromSMGValue(smgValue);
        if (smgValue.isZero()) {
          value = BigInteger.ZERO;
        } else if (valueForSMGValue.isPresent()) {
          Value valueFromSMGValue = valueForSMGValue.orElseThrow();
          if (valueFromSMGValue.isNumericValue()) {
            value = valueForSMGValue.orElseThrow().asNumericValue().bigIntegerValue();
          } else if (pSMGState.getMemoryModel().isPointer(valueFromSMGValue)) {
            Optional<SMGStateAndOptionalSMGObjectAndOffset> target =
                pSMGState.dereferencePointerWithoutMaterilization(valueFromSMGValue);
            if (target.isEmpty()) {
              continue;
            }
            SMGObject targetObject = target.orElseThrow().getSMGObject();
            Value targetOffset = target.orElseThrow().getOffsetForObject();
            if (!targetOffset.isNumericValue()) {
              continue;
            }

            // Pointer to some other obj
            value =
                calculateAddress(
                        targetObject, targetOffset.asNumericValue().bigIntegerValue(), pSMGState)
                    .getAddressValue();

          } else {
            continue;
          }
        } else {
          continue;
        }

        // Value and the obj it is saved in
        Address address = calculateAddress(entry.getKey(), hvEdge.getOffset(), pSMGState);
        addressToHeapMapBuilder.put(address, value);
      }
    }

    return addressToHeapMapBuilder.buildOrThrow();
  }

  // This assigns only stack variables for now!!!!! The term Heap is a lie here!
  private static Map<Address, Object> createHeapValues(
      SMGState pState, Map<LeftHandSide, Address> pVariableAddressMap) {

    Map<Address, Object> result = new HashMap<>();

    Map<SymbolicIdentifier, Value> assignment = new HashMap<>();
    for (ValueAssignment va : pState.getModel()) {
      if (SymbolicValues.isSymbolicTerm(va.getName())) {
        SymbolicIdentifier identifier =
            SymbolicValues.convertTermToSymbolicIdentifier(va.getName());
        Value value = SymbolicValues.convertToValue(va);
        assignment.put((SymbolicIdentifier) identifier.copyForLocation(null), value);
      }
    }

    for (Entry<MemoryLocation, ValueAndValueSize> entry :
        pState.getMemoryModel().getMemoryLocationsAndValuesForSPCWithoutHeap().entrySet()) {
      MemoryLocation heapLoc = entry.getKey();
      Value valueAsValue = entry.getValue().getValue();

      if (valueAsValue instanceof SymbolicValue symValue) {
        // TODO: get all symIdents in symbolic expr, if at least one in assignments, use value
        //  visitor and substitute the symIdents with the assignments to get a full evaluation
        if (symValue instanceof ConstantSymbolicExpression constSymExpr
            && constSymExpr.getValue() instanceof SymbolicValue nestedSymValue) {
          symValue = nestedSymValue;
        }
        if (symValue instanceof SymbolicIdentifier symIdent && assignment.containsKey(symIdent)) {
          Value assignedValue = assignment.get(symIdent);
          if (assignedValue != null && assignedValue.isNumericValue()) {
            valueAsValue = assignedValue;
          }
        }
      }

      if (valueAsValue.isNumericValue()) {
        Number num = valueAsValue.asNumericValue().getNumber();
        LeftHandSide lhs = createBaseIdExpresssion(heapLoc);
        assert pVariableAddressMap.containsKey(lhs);
        Address baseAddress = pVariableAddressMap.get(lhs);
        Address address = baseAddress;
        if (heapLoc.isReference()) {
          address = baseAddress.addOffset(BigInteger.valueOf(heapLoc.getOffset()));
        }
        result.put(address, num);
      }

      // Skip non numerical values without assignment
      // TODO: Should they also be integrated?
    }

    return result;
  }

  public Address calculateAddress(SMGObject pObject, BigInteger pOffset, SMGState pSMGState) {

    // Create a new base address for the object if necessary
    if (!addressOfObjectMap.containsKey(pObject)) {
      addressOfObjectMap.put(pObject, nextAlloc);
      IDExpression lhs = createIDExpression(pSMGState, pObject);
      if (lhs != null) {
        variableAddressMap.put(lhs, nextAlloc);
      }
      BigInteger objectSize;
      if (!pObject.getSize().isNumericValue()) {
        // List<ValueAssignment> valuesAss = pSMGState.getModel();
        // TODO: fix with solver assignments
        objectSize = BigInteger.TEN;
        /*    for (ValueAssignment assignment : valuesAss) {
        if (assignment.getKey().equals(pObject.getSize())) {
          objectSize = (BigInteger) assignment.getValue();
        }
                    }*/
      } else {
        objectSize = pObject.getSize().asNumericValue().bigIntegerValue();
      }

      BigInteger nextAllocOffset = nextAlloc.getAddressValue().add(objectSize).add(BigInteger.TEN);

      nextAlloc = nextAlloc.addOffset(nextAllocOffset);
    }

    return addressOfObjectMap.get(pObject).addOffset(pOffset);
  }

  // Finds the variable names of objects if present
  @Nullable
  private static IDExpression createIDExpression(SMGState state, SMGObject pObject) {

    if (state.getMemoryModel().getGlobalVariableToSmgObjectMap().containsValue(pObject)) {
      for (Entry<String, SMGObject> entry :
          state.getMemoryModel().getGlobalVariableToSmgObjectMap().entrySet()) {
        if (entry.getValue().equals(pObject)) {
          return new IDExpression(entry.getKey());
        }
      }
      // TODO Breaks if label is changed
    }

    for (StackFrame frame : state.getMemoryModel().getStackFrames()) {
      if (frame.getVariables().containsValue(pObject)) {
        for (Entry<String, SMGObject> entry : frame.getVariables().entrySet()) {
          if (entry.getValue().equals(pObject)) {
            return new IDExpression(entry.getKey(), frame.getFunctionDefinition().getName());
          }
        }
        // TODO Breaks if label is changed
      }
    }

    return null;
  }
}
