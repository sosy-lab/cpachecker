// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcreteStatePathNode;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.IntermediateConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.SingleConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.FieldReference;
import org.sosy_lab.cpachecker.core.counterexample.IDExpression;
import org.sosy_lab.cpachecker.core.counterexample.LeftHandSide;
import org.sosy_lab.cpachecker.cpa.value.refiner.ConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SMGConcreteErrorPathAllocator extends ConcreteErrorPathAllocator<SMGState> {

  // this analysis puts every object in the same heap
  private static final String MEMORY_NAME = "SMGv2_Analysis_Heap";

  public SMGConcreteErrorPathAllocator(
      Configuration pConfig, LogManager pLogger, MachineModel pMachineModel)
      throws InvalidConfigurationException {
    super(SMGState.class, AssumptionToEdgeAllocator.create(pConfig, pLogger, pMachineModel));
  }

  @Override
  protected ConcreteStatePath createConcreteStatePath(List<Pair<SMGState, List<CFAEdge>>> pPath) {
    Builder<ConcreteStatePathNode> pathBuilder = ImmutableList.builder();

    for (Pair<SMGState, List<CFAEdge>> edgeStatePair : pPath) {
      SMGState state = checkNotNull(edgeStatePair.getFirst());
      List<CFAEdge> edges = checkNotNull(edgeStatePair.getSecond());

      checkState(!edges.isEmpty());
      if (edges.size() == 1) {
        // a normal edge, no special handling required
        pathBuilder.add(new SingleConcreteState(edges.getFirst(), createConcreteStateFrom(state)));
      } else {
        // Multi-edge. E.g. in the beginning of the program declaring all the types etc.
        handleMultiEdge(state, edges, pathBuilder);
      }
    }

    return new ConcreteStatePath(pathBuilder.build());
  }

  private void handleMultiEdge(
      SMGState pState, List<CFAEdge> edges, Builder<ConcreteStatePathNode> pathBuilder) {
    Builder<SingleConcreteState> intermediateStatesBuilder = ImmutableList.builder();
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
    ConcreteState concreteState;

    // We know only values for LeftHandSides that have not yet been assigned.
    if (allValuesForLeftHandSideKnown(innerEdge, alreadyAssigned)) {
      concreteState = createConcreteStateFrom(pState);
    } else {
      concreteState = ConcreteState.empty();
    }

    // add handled edges to alreadyAssigned list if necessary
    if (innerEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      CStatement stmt = ((CStatementEdge) innerEdge).getStatement();

      if (stmt instanceof CAssignment cAssignment) {
        CLeftHandSide lhs = cAssignment.getLeftHandSide();
        alreadyAssigned.add(lhs);
      }
    }

    return concreteState;
  }

  private ConcreteState createConcreteStateFrom(SMGState pSMGState) {
    return new ConcreteState(
        getConcreteValuesForVariables(pSMGState),
        ImmutableMap.of(),
        ImmutableMap.of(),
        exp -> MEMORY_NAME,
        pSMGState.getMachineModel());
  }

  private static Map<LeftHandSide, Object> getConcreteValuesForVariables(SMGState state) {
    ImmutableMap.Builder<LeftHandSide, Object> result = ImmutableMap.builder();
    for (Entry<MemoryLocation, BigInteger> memLocsAndValues :
        state.getVariablesWithConcreteValues().entrySet()) {

      MemoryLocation location = memLocsAndValues.getKey();
      BigInteger value = memLocsAndValues.getValue();

      Optional<LeftHandSide> maybeLhs = createLeftHandSideFor(location);
      // We can't handle local arrays or field references currently, as we only have an offset,
      // and someone decided that THE ONE INFORMATION THAT C NEEDS TO DETERMINE WHERE WE ARE IN
      // MEMORY IS NOT NEEDED IN CPACHECKER
      if (maybeLhs.isPresent()) {
        LeftHandSide lhs = maybeLhs.orElseThrow();
        checkState(lhs.isGlobal() == !location.isOnFunctionStack());
        checkState(!location.isReference() || lhs instanceof FieldReference);
        result.put(lhs, value);
      }
    }

    return result.buildOrThrow();
  }

  private static Optional<LeftHandSide> createLeftHandSideFor(MemoryLocation memLoc) {
    // CType maybeType = state.getMemoryModel().getTypeOfVariable(memLoc).getCanonicalType();
    // MachineModel machineModel = state.getMachineModel();

    if (!memLoc.isReference()) { // offset == null
      if (memLoc.isOnFunctionStack()) {
        return Optional.of(new IDExpression(memLoc.getIdentifier(), memLoc.getFunctionName()));
      } else {
        return Optional.of(new IDExpression(memLoc.getIdentifier()));
      }
    } else {
      // Has offset -> is a reference
      // TODO:
      return Optional.empty();
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
}
