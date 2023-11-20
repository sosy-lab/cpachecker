// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
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
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.value.refiner.ConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGConcreteErrorPathAllocator extends ConcreteErrorPathAllocator<SMGState> {

  // this analysis puts every object in the same heap
  private static final String MEMORY_NAME = "SMGv2_Analysis_Heap";

  // Map Object <-> some address distinct from 0
  private Map<SMGObject, Address> addressOfObjectMap = new HashMap<>();
  private Address nextAlloc = Address.valueOf(BigInteger.valueOf(100));
  private Map<LeftHandSide, Address> variableAddressMap = new HashMap<>();

  protected SMGConcreteErrorPathAllocator(
      Configuration pConfig, LogManagerWithoutDuplicates pLogger, MachineModel pMachineModel)
      throws InvalidConfigurationException {
    super(SMGState.class, AssumptionToEdgeAllocator.create(pConfig, pLogger, pMachineModel));
  }

  @Override
  protected ConcreteStatePath createConcreteStatePath(List<Pair<SMGState, List<CFAEdge>>> pPath) {

    List<ConcreteStatePathNode> result = new ArrayList<>(pPath.size());

    for (Pair<SMGState, List<CFAEdge>> edgeStatePair : pPath) {

      SMGState valueState = edgeStatePair.getFirst();
      List<CFAEdge> edges = edgeStatePair.getSecond();

      if (edges.size() > 1) {
        List<SingleConcreteState> intermediateStates = new ArrayList<>();
        Set<CLeftHandSide> alreadyAssigned = new HashSet<>();
        boolean isFirstIteration = true;
        for (CFAEdge innerEdge : Lists.reverse(edges)) {
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
                    allocateAddresses(valueState),
                    ImmutableMap.copyOf(variableAddressMap),
                    exp -> MEMORY_NAME)));
      }
    }

    return new ConcreteStatePath(result);
  }

  private ConcreteState createConcreteStateForMultiEdge(
      SMGState pValueState, Set<CLeftHandSide> alreadyAssigned, CFAEdge innerEdge) {
    ConcreteState state;

    // We know only values for LeftHandSides that have not yet been assigned.
    if (allValuesForLeftHandSideKnown(innerEdge, alreadyAssigned)) {
      state = createConcreteState(pValueState, alreadyAssigned, innerEdge);
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

  private ConcreteState createConcreteState(
      SMGState pSMGState, Set<CLeftHandSide> alreadyAssigned, CFAEdge innerEdge) {
    ConcreteState state;

    // We know only values for LeftHandSides that have not yet been assigned.
    if (allValuesForLeftHandSideKnown(innerEdge, alreadyAssigned)) {
      state =
          new ConcreteState(
              ImmutableMap.of(),
              allocateAddresses(pSMGState),
              ImmutableMap.copyOf(variableAddressMap),
              exp -> MEMORY_NAME);
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

  private Map<String, Memory> allocateAddresses(SMGState pValueState) {
    Map<Address, Object> values = createHeapValues(pValueState);
    return ImmutableMap.of(MEMORY_NAME, new Memory(MEMORY_NAME, values));
  }

  private Map<Address, Object> createHeapValues(SMGState pSMGState) {

    Map<Address, Object> result = new HashMap<>();

    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> entry :
        pSMGState.getMemoryModel().getSmg().getSMGObjectsWithSMGHasValueEdges().entrySet()) {
      for (SMGHasValueEdge hvEdge : entry.getValue()) {

        BigInteger value = null;
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
        result.put(address, value);
      }
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
      BigInteger objectSize = pObject.getSize();

      BigInteger nextAllocOffset = nextAlloc.getAddressValue().add(objectSize).add(BigInteger.TEN);

      nextAlloc = nextAlloc.addOffset(nextAllocOffset);
    }

    return addressOfObjectMap.get(pObject).addOffset(pOffset);
  }

  // Finds the variable names of objects if present
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
