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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.core.counterexample.RichModel;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.ImmutableMap;

public class SMGConcreteErrorPathAllocator {

  @SuppressWarnings("unused")
  private final LogManager logger;

  private MemoryName memoryName = new MemoryName() {

    @Override
    public String getMemoryName(CRightHandSide pExp, Address pAddress) {
      return "SMG_Analysis_Heap";
    }
  };

  public SMGConcreteErrorPathAllocator(LogManager pLogger) {
    logger = pLogger;
  }

  public ConcreteStatePath allocateAssignmentsToPath(ARGPath pPath) {

    List<Pair<SMGState, CFAEdge>> path = new ArrayList<>(pPath.size());

    PathIterator it = pPath.pathIterator();

    while (it.hasNext()) {
      it.advance();
      SMGState state = AbstractStates.extractStateByType(it.getAbstractState(), SMGState.class);
      CFAEdge edge = it.getIncomingEdge();

      if (state == null) {
        return null;
      }

      path.add(Pair.of(state, edge));
    }

    return createConcreteStatePath(path);
  }

  public RichModel allocateAssignmentsToPath(List<Pair<SMGState, CFAEdge>> pPath, MachineModel pMachineModel) {

    pPath.remove(pPath.size() - 1);

    ConcreteStatePath concreteStatePath = createConcreteStatePath(pPath);

    CFAPathWithAssumptions pathWithAssignments =
        CFAPathWithAssumptions.of(concreteStatePath, logger, pMachineModel);

    RichModel model = RichModel.empty();

    return model.withAssignmentInformation(pathWithAssignments);
  }

  private ConcreteStatePath createConcreteStatePath(List<Pair<SMGState, CFAEdge>> pPath) {

    List<ConcerteStatePathNode> result = new ArrayList<>(pPath.size());

    // Until SMGObjects are comparable for persistant maps, this object is mutable
    // and depends on side effects
    SMGObjectAddressMap variableAddresses = new SMGObjectAddressMap();

    for (Pair<SMGState, CFAEdge> edgeStatePair : pPath) {

      SMGState pSMGState = edgeStatePair.getFirst();
      CFAEdge edge = edgeStatePair.getSecond();

      ConcerteStatePathNode node;

      if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {

        node = createMultiEdge(pSMGState, (MultiEdge) edge, variableAddresses);
      } else {
        ConcreteState concreteState = createConcreteState(pSMGState, variableAddresses);
        node = ConcreteStatePath.valueOfPathNode(concreteState, edge);
      }

      result.add(node);
    }


    return new ConcreteStatePath(result);
  }

  private ConcerteStatePathNode createMultiEdge(SMGState pSMGState, MultiEdge multiEdge,
      SMGObjectAddressMap pVariableAddresses) {

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
        state = createConcreteState(pSMGState, pVariableAddresses);
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

  //TODO move to util?
  private ConcreteState createConcreteState(SMGState pSMGState,
      SMGObjectAddressMap pAdresses) {


    Map<LeftHandSide, Object> variables = ImmutableMap.of();
    Map<String, Memory> allocatedMemory = allocateAddresses(pSMGState, pAdresses);
    // We assign every variable to the heap, thats why the variable map is empty.
    return new ConcreteState(variables, allocatedMemory, pAdresses.getAddressMap(), memoryName);
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

    for (SMGEdgeHasValue hvEdge : symbolicValues) {

      int symbolicValue = hvEdge.getValue();
      BigInteger value = null;

      if(symbolicValue == 0) {
        value = BigInteger.ZERO;
      } else if (pSMGState.isPointer(symbolicValue)) {
        SMGEdgePointsTo pointer;
        try {
          pointer = pSMGState.getPointerFromValue(symbolicValue);
        } catch (SMGInconsistentException e) {
          throw new AssertionError();
        }


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