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
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions.DIRECTION;
import org.sosy_lab.cpachecker.cpa.value.refiner.ConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

public class SMGConcreteErrorPathAllocator extends ConcreteErrorPathAllocator<SMGState> {

  // this analysis puts every object in the same heap
  private static final String MEMORY_NAME = "SMGv2_Analysis_Heap";

  private final SMGOptions options;

  public SMGConcreteErrorPathAllocator(
      Configuration pConfig, LogManager pLogger, MachineModel pMachineModel, SMGOptions pOptions)
      throws InvalidConfigurationException {
    super(SMGState.class, AssumptionToEdgeAllocator.create(pConfig, pLogger, pMachineModel));
    options = pOptions;
  }

  @Override
  protected ConcreteStatePath createConcreteStatePath(List<Pair<SMGState, List<CFAEdge>>> pPath) {
    return switch (options.getErrorPathConcreteValueAssignmentDirection()) {
      case DIRECTION.BACKWARD -> assignConcreteValuesBackwardsFromFinalAssignment(pPath);
      case DIRECTION.FORWARD -> assignConcreteValuesLinearlyForwardAsFound(pPath);
    };
  }

  /**
   * Goes backwards through the given path (that is supposed to be in forward direction) and assigns
   * concrete values to variables when known. This does remember the last value assignment (produced
   * by a solver) and back-propagates the assignments as far as possible.
   */
  private ConcreteStatePath assignConcreteValuesBackwardsFromFinalAssignment(
      List<Pair<SMGState, List<CFAEdge>>> pForwardPath) {
    ImmutableList.Builder<ConcreteStatePathNode> pathBuilder = ImmutableList.builder();
    List<ValueAssignment> assignmentToUse = ImmutableList.of();
    List<Pair<SMGState, List<CFAEdge>>> backwardsPath = pForwardPath.reversed();
    for (Pair<SMGState, List<CFAEdge>> edgeStatePair : backwardsPath) {
      SMGState state = checkNotNull(edgeStatePair.getFirst());
      List<CFAEdge> edges = checkNotNull(edgeStatePair.getSecond());

      if (assignmentToUse.isEmpty() && !state.getModel().isEmpty()) {
        // We want the "last" model (in forward direction) only
        // TODO: the IDs to assign are extracted from this every time. Do it here once instead.
        assignmentToUse = state.getModel();
      }

      checkState(!edges.isEmpty());
      if (edges.size() == 1) {
        // a normal edge, no special handling required
        pathBuilder.add(
            new SingleConcreteState(
                edges.getFirst(), createConcreteStateFrom(state, assignmentToUse)));
      } else {
        // Multi-edge. E.g. in the beginning of the program declaring all the types etc.
        pathBuilder.addAll(handleMultiEdge(state, assignmentToUse, edges).reversed());
      }
    }
    return new ConcreteStatePath(pathBuilder.build().reverse());
  }

  /**
   * Assigns concrete values (this includes solver models/value assignments) of simple types (and
   * numeric for pointer types) when known to the location. This method does not apply value
   * assignments backwards if a later state finds concrete values that apply before this state.
   *
   * @param pForwardPath path in linear fashion from start of the program to the error location.
   */
  private ConcreteStatePath assignConcreteValuesLinearlyForwardAsFound(
      List<Pair<SMGState, List<CFAEdge>>> pForwardPath) {
    ImmutableList.Builder<ConcreteStatePathNode> pathBuilder = ImmutableList.builder();
    for (Pair<SMGState, List<CFAEdge>> edgeStatePair : pForwardPath) {
      SMGState state = checkNotNull(edgeStatePair.getFirst());
      List<ValueAssignment> assignmentsToUse = state.getModel();
      List<CFAEdge> edges = checkNotNull(edgeStatePair.getSecond());

      checkState(!edges.isEmpty());
      if (edges.size() == 1) {
        // a normal edge, no special handling required
        pathBuilder.add(
            new SingleConcreteState(
                edges.getFirst(), createConcreteStateFrom(state, assignmentsToUse)));
      } else {
        // Multi-edge. E.g. in the beginning of the program declaring all the types etc.
        pathBuilder.addAll(handleMultiEdge(state, assignmentsToUse, edges));
      }
    }
    return new ConcreteStatePath(pathBuilder.build());
  }

  /**
   * Iterates over the edges provided in a reversed order and tries to assign concrete values from
   * the state and model given to the returned states
   */
  private List<SingleConcreteState> handleMultiEdge(
      SMGState pState, List<ValueAssignment> modelToUse, List<CFAEdge> edges) {
    ImmutableList.Builder<SingleConcreteState> intermediateStatesBuilder = ImmutableList.builder();
    Set<CLeftHandSide> alreadyAssigned = new HashSet<>();
    boolean isFirstIteration = true;
    for (CFAEdge innerEdge : edges.reversed()) {
      ConcreteState state =
          createConcreteStateForMultiEdge(pState, modelToUse, alreadyAssigned, innerEdge);

      // intermediate edge
      if (isFirstIteration) {
        intermediateStatesBuilder.add(new SingleConcreteState(innerEdge, state));
        isFirstIteration = false;

        // last edge of (dynamic) multi edge
      } else {
        intermediateStatesBuilder.add(new IntermediateConcreteState(innerEdge, state));
      }
    }
    return intermediateStatesBuilder.build().reverse();
  }

  private ConcreteState createConcreteStateForMultiEdge(
      SMGState pState,
      List<ValueAssignment> modelToUse,
      Set<CLeftHandSide> alreadyAssigned,
      CFAEdge innerEdge) {
    ConcreteState concreteState;

    // We know only values for LeftHandSides that have not yet been assigned.
    if (allValuesForLeftHandSideKnown(innerEdge, alreadyAssigned)) {
      concreteState = createConcreteStateFrom(pState, modelToUse);
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

  private ConcreteState createConcreteStateFrom(
      SMGState pSMGState, List<ValueAssignment> modelToUse) {
    return new ConcreteState(
        getConcreteValuesForVariables(pSMGState, modelToUse, options),
        ImmutableMap.of(),
        ImmutableMap.of(),
        exp -> MEMORY_NAME,
        pSMGState.getMachineModel());
  }

  private static Map<LeftHandSide, Object> getConcreteValuesForVariables(
      SMGState state, List<ValueAssignment> modelToUse, SMGOptions options) {
    ImmutableMap.Builder<LeftHandSide, Object> result = ImmutableMap.builder();
    for (Entry<MemoryLocation, BigInteger> memLocsAndValues :
        state.getVariablesWithConcreteValues(modelToUse).entrySet()) {

      MemoryLocation location = memLocsAndValues.getKey();
      BigInteger value = memLocsAndValues.getValue();

      Optional<LeftHandSide> maybeLhs = createLeftHandSideFor(location, options);
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

  private static Optional<LeftHandSide> createLeftHandSideFor(
      MemoryLocation memLoc, SMGOptions options) {
    // CType maybeType = state.getMemoryModel().getTypeOfVariable(memLoc).getCanonicalType();
    // MachineModel machineModel = state.getMachineModel();

    String variableName = memLoc.getIdentifier();
    if (options.exportInternalVariableAssignments() && variableName.contains("__CPAchecker_TMP_")) {
      return Optional.empty();
    }
    if (!memLoc.isReference()) { // offset == null
      if (memLoc.isOnFunctionStack()) {
        return Optional.of(new IDExpression(variableName, memLoc.getFunctionName()));
      } else {
        return Optional.of(new IDExpression(variableName));
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
