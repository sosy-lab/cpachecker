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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
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
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SMGConcreteErrorPathAllocator {

  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  private MemoryName memoryName = new MemoryName() {

    @Override
    public String getMemoryName(CRightHandSide pExp, Address pAddress) {
      return "SMG_Analysis_Heap";
    }
  };

  public SMGConcreteErrorPathAllocator(AssumptionToEdgeAllocator pAssumptionToEdgeAllocator) {
    assumptionToEdgeAllocator = pAssumptionToEdgeAllocator;
  }

  public ConcreteStatePath allocateAssignmentsToPath(ARGPath pPath) {

    List<Pair<SMGState, List<CFAEdge>>> path = new ArrayList<>(pPath.size());

    PathIterator it = pPath.fullPathIterator();

    while (it.hasNext()) {
      List<CFAEdge> innerEdges = new ArrayList<>();

      do {
        it.advance();
        innerEdges.add(it.getIncomingEdge());
      } while (!it.isPositionWithState());

      SMGState state = AbstractStates.extractStateByType(it.getAbstractState(), SMGState.class);

      if (state == null) {
        return null;
      }

      path.add(Pair.of(state, innerEdges));
    }

    return createConcreteStatePath(path);
  }

  public CFAPathWithAssumptions allocateAssignmentsToPath(
      List<Pair<SMGState, List<CFAEdge>>> pPath) {
    ConcreteStatePath concreteStatePath = createConcreteStatePath(pPath);
    return CFAPathWithAssumptions.of(concreteStatePath, assumptionToEdgeAllocator);
  }

  private ConcreteStatePath createConcreteStatePath(List<Pair<SMGState, List<CFAEdge>>> pPath) {

    List<ConcreteStatePathNode> result = new ArrayList<>();

    // Until SMGObjects are comparable for persistant maps, this object is mutable
    // and depends on side effects
    SMGObjectAddressMap variableAddresses = new SMGObjectAddressMap();

    for (Pair<SMGState, List<CFAEdge>> edgeStatePair : pPath) {

      SMGState pSMGState = edgeStatePair.getFirst();
      List<CFAEdge> edges = edgeStatePair.getSecond();

      if (edges.size() > 1) {
        Iterator<CFAEdge> it = Lists.reverse(edges).iterator();
        List<SingleConcreteState> intermediateStates = new ArrayList<>();
        Set<CLeftHandSide> alreadyAssigned = new HashSet<>();
        while (it.hasNext()) {
          CFAEdge innerEdge = it.next();
          ConcreteState state =
              createConcreteState(variableAddresses, pSMGState, alreadyAssigned, innerEdge);

          // intermediate edge
          if (it.hasNext()) {
            intermediateStates.add(new IntermediateConcreteState(innerEdge, state));

            // last edge of (dynamic) multi edge
          } else {
            result.addAll(Lists.reverse(intermediateStates));
            result.add(new SingleConcreteState(innerEdge, state));
          }
        }

        // a normal edge, no special handling required
      } else {
        result.add(
            new SingleConcreteState(
                Iterables.getOnlyElement(edges),
                new ConcreteState(
                    ImmutableMap.<LeftHandSide, Object>of(),
                    allocateAddresses(pSMGState, variableAddresses),
                    variableAddresses.getAddressMap(),
                    memoryName)));
      }
    }


    return new ConcreteStatePath(result);
  }

  private ConcreteState createConcreteState(
      SMGObjectAddressMap variableAddresses,
      SMGState pSMGState,
      Set<CLeftHandSide> alreadyAssigned,
      CFAEdge innerEdge) {
    ConcreteState state;

    // We know only values for LeftHandSides that have not yet been assigned.
    if (allValuesForLeftHandSideKnown(innerEdge, alreadyAssigned)) {
      state =
          new ConcreteState(
              ImmutableMap.<LeftHandSide, Object>of(),
              allocateAddresses(pSMGState, variableAddresses),
              variableAddresses.getAddressMap(),
              memoryName);
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

  private boolean allValuesForLeftHandSideKnown(CFAEdge pCfaEdge, Set<CLeftHandSide> pAlreadyAssigned) {

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

  private Map<String, Memory> allocateAddresses(SMGState pSMGState,
      SMGObjectAddressMap pAdresses) {

    Map<Address, Object> values = createHeapValues(pSMGState, pAdresses);

    // memory name of smg analysis does not need to know expression or address
    Memory heap = new Memory(memoryName.getMemoryName(null, null), values);

    Map<String, Memory> result = new HashMap<>();

    result.put(heap.getName(), heap);

    return result;
  }

  private Map<Address, Object> createHeapValues(SMGState pSMGState,
      SMGObjectAddressMap pAdresses) {

    Set<SMGEdgeHasValue> symbolicValues = pSMGState.getHVEdges();

    Map<Address, Object> result = new HashMap<>();

    for (SMGEdgeHasValue hvEdge : ImmutableSet.copyOf(symbolicValues)) {

      int symbolicValue = hvEdge.getValue();
      BigInteger value = null;

      if (symbolicValue == 0) {
        value = BigInteger.ZERO;
      } else if (pSMGState.isPointer(symbolicValue)) {
        SMGEdgePointsTo pointer = pSMGState.getPointsToEdge(symbolicValue);

        //TODO ugly, use common representation
        value = pAdresses.calculateAddress(pointer.getObject(), pointer.getOffset(), pSMGState).getAddressValue();
      } else if (pSMGState.isExplicit(symbolicValue)) {
        value = BigInteger.valueOf(pSMGState.getExplicit(symbolicValue).getAsLong());
      } else {
        continue;
      }

      Address address = pAdresses.calculateAddress(hvEdge.getObject(), hvEdge.getOffset(), pSMGState);
      result.put(address, value);
    }

    return result;
  }

  private static class SMGObjectAddressMap {

    private Map<SMGObject, Address> objectAddressMap = new HashMap<>();
    private Address nextAlloc = Address.valueOf(BigInteger.valueOf(100));
    private Map<LeftHandSide, Address> variableAddressMap = new HashMap<>();

    public Address calculateAddress(SMGObject pObject, int pOffset,
        SMGState pSMGState) {

      // Create a new base address for the object if necessary
      if (!objectAddressMap.containsKey(pObject)) {
        objectAddressMap.put(pObject, nextAlloc);
        IDExpression lhs = pSMGState.createIDExpression(pObject);
        if (lhs != null) {
          variableAddressMap.put(lhs, nextAlloc);
        }
        BigInteger objectSize = BigInteger.valueOf(pObject.getSize());

        BigInteger nextAllocOffset = nextAlloc.getAddressValue().add(objectSize).add(BigInteger.ONE);

        nextAlloc = nextAlloc.addOffset(nextAllocOffset);
      }

      return objectAddressMap.get(pObject).addOffset(BigInteger.valueOf(pOffset));
    }

    public Map<LeftHandSide, Address> getAddressMap() {
      return ImmutableMap.copyOf(variableAddressMap);
    }
  }
}