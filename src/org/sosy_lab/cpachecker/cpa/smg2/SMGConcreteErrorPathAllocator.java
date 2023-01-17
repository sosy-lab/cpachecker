// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.base.Preconditions;
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
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
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
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.value.refiner.ConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGConcreteErrorPathAllocator extends ConcreteErrorPathAllocator<SMGState> {

  // this analysis puts every object in the same heap
  private static final String MEMORY_NAME = "SMGv2_Analysis_Heap";

  protected SMGConcreteErrorPathAllocator(AssumptionToEdgeAllocator pAssumptionToEdgeAllocator) {
    super(SMGState.class, pAssumptionToEdgeAllocator);
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
        Map<LeftHandSide, Address> variableAddresses = new HashMap<>();
        result.add(
            new SingleConcreteState(
                Iterables.getOnlyElement(edges),
                new ConcreteState(
                    ImmutableMap.of(),
                    allocateAddresses(valueState, variableAddresses),
                    variableAddresses,
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

  public ConcreteState createConcreteState(SMGState pValueState) {
    Map<LeftHandSide, Address> variableAddresses = new HashMap<>();
    // We assign every variable to the heap, thats why the variable map is empty.
    return new ConcreteState(
        ImmutableMap.of(),
        allocateAddresses(pValueState, variableAddresses),
        variableAddresses,
        exp -> MEMORY_NAME);
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

  private Map<String, Memory> allocateAddresses(
      SMGState pValueState, Map<LeftHandSide, Address> pVariableAddressMap) {
    Map<Address, Object> values = new HashMap<>();
    fillAddressAndValueMaps(pValueState, pVariableAddressMap, values);
    return ImmutableMap.of(MEMORY_NAME, new Memory(MEMORY_NAME, values));
  }

  /*
   * Map<Address, Object> with below Addresses
   * Map<LeftHandSide, Address> with IDExpression as LeftHandSide
   */
  private void fillAddressAndValueMaps(
      SMGState state, Map<LeftHandSide, Address> lfhsToAddressMap, Map<Address, Object> valuesMap) {
    @SuppressWarnings("unused")
    Set<SMGObject> todo = new HashSet<>();
    @SuppressWarnings("unused")
    Set<SMGObject> alreadyVisited = new HashSet<>();

    StackFrame currentStackFrame = state.getMemoryModel().getStackFrames().peek();
    String functionName = currentStackFrame.getFunctionDefinition().getName();
    // Start with Address 0
    Address nextAddressToBeAssigned = Address.valueOf(BigInteger.ZERO);
    // Value and the old SMG analysis put some random values here. I have the feeling that this
    // system is either not explained well or broken
    long spaceForLastValue = 64;
    // Stack variables
    for (Entry<String, SMGObject> var : currentStackFrame.getVariables().entrySet()) {
      // This is the qualified name -> reduce by functionName
      String variableName = var.getKey().replace(functionName + "::", "");
      IDExpression idExp = new IDExpression(variableName, functionName);

      lfhsToAddressMap.put(idExp, nextAddressToBeAssigned);

      SMGObject objectForVar = var.getValue();
      // These values are either alone (i.e. int bla = 5;) or there are multiple for arrays etc.
      long biggestOffset =
          putValuesIntoMap(
              nextAddressToBeAssigned, state, valuesMap, objectForVar, alreadyVisited, todo);
      // Make a new Address for the next variable
      BigInteger offset = BigInteger.valueOf(biggestOffset + spaceForLastValue);

      nextAddressToBeAssigned = nextAddressToBeAssigned.addOffset(offset);
    }
    // Global vars
    PersistentMap<String, SMGObject> globalVarMapping =
        state.getMemoryModel().getGlobalVariableToSmgObjectMap();
    for (Entry<String, SMGObject> var : globalVarMapping.entrySet()) {
      String variableName = var.getKey();
      IDExpression idExp = new IDExpression(variableName);

      lfhsToAddressMap.put(idExp, nextAddressToBeAssigned);

      SMGObject objectForVar = var.getValue();
      // These values are either alone (i.e. int bla = 5;) or there are multiple for arrays etc.
      long biggestOffset =
          putValuesIntoMap(
              nextAddressToBeAssigned, state, valuesMap, objectForVar, alreadyVisited, todo);
      // Make a new Address for the next variable
      BigInteger offset = BigInteger.valueOf(biggestOffset + spaceForLastValue);

      nextAddressToBeAssigned = nextAddressToBeAssigned.addOffset(offset);
    }
  }

  private long putValuesIntoMap(
      Address baseAddress,
      SMGState state,
      Map<Address, Object> valuesMap,
      SMGObject objectForVar,
      Set<SMGObject> alreadyVisited,
      Set<SMGObject> todo) {
    alreadyVisited.add(objectForVar);
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> valuesByObject =
        state.getMemoryModel().getSmg().getSMGObjectsWithSMGHasValueEdges();
    PersistentSet<SMGHasValueEdge> valuesInObject = valuesByObject.get(objectForVar);
    if (valuesInObject == null || valuesInObject.isEmpty()) {
      return 0;
    }
    long biggestOffset = 0;
    for (SMGHasValueEdge hve : valuesInObject) {
      if (hve.getOffset().longValue() > biggestOffset) {
        biggestOffset = hve.getOffset().longValue();
      }
      BigInteger offset = hve.getOffset();
      Optional<Value> value = state.getMemoryModel().getValueFromSMGValue(hve.hasValue());

      if (!value.orElseThrow().isNumericValue()) {
        // This is either a symbolic/unknown value or a pointer
        if (state.getMemoryModel().isPointer(value.orElseThrow())) {
          SMGStateAndOptionalSMGObjectAndOffset target;
          try {
            // We want to use the minimal state (list abstraction might split into 2 states when
            // materializing, we use the shortest)
            List<SMGStateAndOptionalSMGObjectAndOffset> listOfTargets =
                state.dereferencePointer(value.orElseThrow());
            if (listOfTargets.size() == 1) {
              target = listOfTargets.get(0);
            } else {
              Preconditions.checkArgument(
                  listOfTargets.get(0).hasSMGObjectAndOffset()
                      && !alreadyVisited.contains(listOfTargets.get(0).getSMGObject())
                      && !state.getMemoryModel().pointsToZeroPlus(value.orElseThrow()));
              // the first element is the minimal list
              target = listOfTargets.get(0);
            }
            if (target.hasSMGObjectAndOffset() && !alreadyVisited.contains(target.getSMGObject())) {
              todo.add(target.getSMGObject());
            }
          } catch (SMG2Exception e) {
            // Do nothing, should not happen
          }
        }
        continue;
      }
      valuesMap.put(
          baseAddress.addOffset(offset), value.orElseThrow().asNumericValue().bigInteger());
    }
    return biggestOffset;
  }
}
