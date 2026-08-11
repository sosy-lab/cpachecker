// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.CExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public record LoopAccelerationRecovery(
    List<CExpression> numberOfIterations,
    CFANode loopHead,
    CFANode nodeAfterLoop,
    Set<CFAEdge> loopEdges,
    CVariableDeclaration iterVarDeclaration,
    AffineLoopClosedFormRepresentation closedForm)
    implements ProgramTransformationRecovery {

  @Override
  public void revertProgramTransformation(
      AbstractState pBeforeState,
      SubCFA pAfterProgramTransformation,
      ReachedSet reached,
      LocationStateFactory pLocationStateFactory) {

    ARGState previousARGState = (ARGState) pBeforeState;
    ARGState currentARGState = previousARGState.getChildren().getFirst();

    // 1. remove the initial ARGState in the program transformation
    currentARGState =
        ProgramTransformationRecoveryUtils.handleEntry(previousARGState, currentARGState, reached);

    // 2. are we entering the loop?
    boolean insideLoop =
        switch (currentARGState.getChildren().size()) {
          case 1 ->
              AbstractStates.extractLocation(currentARGState.getChildren().getFirst())
                  != pAfterProgramTransformation.subCFAExitNode();
          case 2 ->
              throw new RuntimeException(
                  "Error during loop acceleration recovery! Cannot recreate loop iterations without"
                      + " value state!");
          default ->
              throw new RuntimeException(
                  "Error during loop acceleration recovery! Invalid end of ARG!");
        };

    if (insideLoop) {
      // 3.1. remove the state after passing the initial loop guard
      currentARGState = previousARGState.getChildren().getFirst();
      currentARGState.removeParent(previousARGState);
      currentARGState.getChildren().getFirst().addParent(previousARGState);
      currentARGState.getChildren().getFirst().removeParent(currentARGState);
      reached.remove(currentARGState);
      currentARGState = previousARGState.getChildren().getFirst();

      // 3.1. handle the single state that includes all accelerated states
      ARGState accelerationState = currentARGState;
      ARGState afterAccelerationState = accelerationState.getChildren().getFirst();
      ARGState afterProgramTransformation = afterAccelerationState.getChildren().getFirst();
      Verify.verify(AbstractStates.extractLocation(afterProgramTransformation) == nodeAfterLoop);
      currentARGState = previousARGState;
      accelerationState.removeParent(previousARGState);
      // extract the exact iteration number from the value state
      Optional<Long> numberOfIterationsOptional = getIterationsFromState(accelerationState);
      if (numberOfIterationsOptional.isEmpty()) {
        // try to calculate it manually
        numberOfIterationsOptional =
            calculateIterationsFromState(accelerationState.getParents().getFirst());
        if (numberOfIterationsOptional.isEmpty())
          throw new RuntimeException(
              "Error during loop acceleration recovery! No iterations found in value state!");
      }
      // create symbolic expressions for all assignments in the loop
      ImmutableMap.Builder<CFAEdge, SymbolicAssignmentExpression> assignmentExpressions =
          ImmutableMap.builder();
      for (CFAEdge edge : loopEdges) {
        if (edge instanceof CStatementEdge cStatementEdge) {
          IntegerArithmaticExpressionVisitor visitor = new IntegerArithmaticExpressionVisitor();
          IExpr symbolicExpression;
          try {
            symbolicExpression =
                visitor.visit(
                    ((CExpressionAssignmentStatement) cStatementEdge.getStatement())
                        .getRightHandSide());
          } catch (Exception pE) {
            throw new RuntimeException(
                "Error during loop acceleration recovery! Cannot perform expression analysis.");
          }
          if (visitor.failed())
            throw new RuntimeException(
                "Error during loop acceleration recovery! Cannot perform expression analysis.");
          assignmentExpressions.put(
              edge,
              new SymbolicAssignmentExpression(
                  symbolicExpression, visitor.getEncounteredVariables()));
        }
      }
      // recreate numberOfIterations loop cycles
      if (numberOfIterationsOptional.orElseThrow() > 0) {
        CFAEdge initialLoopEdge =
            (loopHead
                    .getLeavingEdges()
                    .firstMatch((CFAEdge leavingEdge) -> loopEdges.contains(leavingEdge)))
                .orNull();
        if (initialLoopEdge == null)
          throw new RuntimeException(
              "Error during loop acceleration recovery! Could not determine the first loop edge!");
        ImmutableList.Builder<ARGState> newArgStatesBuilder = ImmutableList.builder();
        for (int i = 0; i < numberOfIterationsOptional.orElseThrow(); i++) {
          ARGState newARGState =
              ProgramTransformationRecoveryUtils.argStateWithLocation(
                  currentARGState,
                  pLocationStateFactory.getState(initialLoopEdge.getSuccessor()),
                  currentARGState);
          newArgStatesBuilder.add(newARGState);
          CFANode currentNode = initialLoopEdge.getSuccessor();
          while (currentNode != loopHead) {
            Verify.verify(currentNode.getLeavingEdges().size() == 1);
            // in case of a blank edge copy the state with only a location update
            if (currentNode.getLeavingEdges().get(0) instanceof BlankEdge blankEdge) {
              newARGState =
                  ProgramTransformationRecoveryUtils.argStateWithLocation(
                      newARGState,
                      pLocationStateFactory.getState(blankEdge.getSuccessor()),
                      newARGState);
              newArgStatesBuilder.add(newARGState);
              currentNode = blankEdge.getSuccessor();
              // else handle the assignment in valuestate
            } else if (currentNode.getLeavingEdges().get(0) instanceof CStatementEdge nextEdge) {
              CIdExpression assignedVariable =
                  (CIdExpression)
                      ((CExpressionAssignmentStatement) nextEdge.getStatement()).getLeftHandSide();
              previousARGState = newARGState;
              newARGState =
                  ProgramTransformationRecoveryUtils.argStateWithLocation(
                      newARGState,
                      pLocationStateFactory.getState(nextEdge.getSuccessor()),
                      newARGState);
              // update value state
              ValueAnalysisState previousValueState =
                  AbstractStates.extractStateByType(newARGState, ValueAnalysisState.class);
              if (previousValueState == null)
                throw new RuntimeException(
                    "Error during loop acceleration recovery! Missing value state!.");
              ValueAnalysisState newValueState =
                  calculateNextValueState(
                          previousValueState,
                          (CIdExpression)
                              ((CExpressionAssignmentStatement) nextEdge.getStatement())
                                  .getLeftHandSide(),
                          assignmentExpressions.build().get(nextEdge),
                          AbstractStates.extractLocation(newARGState).getFunctionName())
                      .orElseThrow();
              newARGState.removeParent(previousARGState);
              newARGState =
                  ProgramTransformationRecoveryUtils.argStateWithValue(
                      newARGState, newValueState, previousARGState);
              newArgStatesBuilder.add(newARGState);
              currentNode = nextEdge.getSuccessor();
            } else
              throw new RuntimeException(
                  "Error during loop acceleration recovery! Invalid edge encountered.");
          }
          currentARGState = newARGState;
        }
        // add the new arg states and remove the accelerated state
        ImmutableList<ARGState> newARGStates = newArgStatesBuilder.build();
        for (ARGState newARGState : newARGStates) {
          reached.add(
              newARGState, reached.getPrecision(accelerationState.getChildren().getFirst()));
        }
        currentARGState = newARGStates.getLast();
        afterProgramTransformation.removeParent(afterAccelerationState);
        reached.remove(accelerationState);
        reached.remove(afterAccelerationState);
      }
      afterProgramTransformation.addParent(currentARGState);
    } else {
      // 3.2. remove the next two states
      currentARGState.removeParent(previousARGState);
      ARGState child = currentARGState.getChildren().getFirst();
      ARGState afterProgramTransformation = child.getChildren().getFirst();
      afterProgramTransformation.removeParent(child);
      afterProgramTransformation.addParent(previousARGState);
      reached.remove(currentARGState);
      reached.remove(child);
    }
    // 4. clear the waitlist
    reached.clearWaitlist();
  }

  private record SymbolicAssignmentExpression(
      IExpr symbolicExpression, ImmutableList<CIdExpression> encounteredVariables) {}

  private Optional<Long> getIterationsFromState(ARGState pAcceleratedState) {
    CompositeState compositeState = (CompositeState) pAcceleratedState.getWrappedState();
    Value numberOfIterationsValue;
    MemoryLocation iterationVar = MemoryLocation.forDeclaration(iterVarDeclaration);
    for (AbstractState state : compositeState.getWrappedStates()) {
      if (state instanceof ValueAnalysisState valueState) {
        if (valueState.contains(iterationVar)) {
          numberOfIterationsValue = valueState.getValueFor(iterationVar);
          return Optional.of(numberOfIterationsValue.asLong(CNumericTypes.INT).orElseThrow());
        } else {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }

  private Optional<Long> calculateIterationsFromState(ARGState pInitialState) {
    CompositeState compositeState = (CompositeState) pInitialState.getWrappedState();
    Set<CIdExpression> variables = closedForm.getVariables();
    ImmutableMap.Builder<CIdExpression, Value> variableValues = ImmutableMap.builder();
    for (AbstractState state : compositeState.getWrappedStates()) {
      if (state instanceof ValueAnalysisState valueState) {
        for (CIdExpression var : variables) {
          if (valueState.contains(MemoryLocation.forDeclaration(var.getDeclaration()))) {
            variableValues.put(
                var, valueState.getValueFor(MemoryLocation.forDeclaration(var.getDeclaration())));
          }
        }
        break;
      }
    }

    ExprEvaluator util = new ExprEvaluator();
    ImmutableList.Builder<IExpr> possibleIterationNumbersBuilder = ImmutableList.builder();
    for (CExpression expression : this.numberOfIterations) {
      // parse the expression
      IExpr symbolicExpression = util.eval(expression.toASTString());
      // insert values for all variables
      for (Entry<CIdExpression, Value> entry : variableValues.build().entrySet()) {
        IExpr rule =
            F.Rule(
                F.$s(entry.getKey().getName()),
                F.ZZ(entry.getValue().asLong(CNumericTypes.INT).orElseThrow()));
        symbolicExpression = util.eval("ReplaceAll(" + symbolicExpression + ", " + rule + ")");
      }
      possibleIterationNumbersBuilder.add(symbolicExpression);
    }
    ImmutableList<IExpr> possibleIterations = possibleIterationNumbersBuilder.build();

    if (possibleIterations.isEmpty()) {
      return Optional.empty();
    }

    Predicate<IExpr> isInteger = expr -> expr.isInteger();
    Predicate<IExpr> isPositive = expr -> expr.isPositive();

    ImmutableList<IExpr> nonNegativeIterations =
        ImmutableList.copyOf(
            possibleIterations.stream()
                .filter(expr -> isInteger.and(isPositive).test(expr))
                .toList());
    IExpr smallestPossibleIterationNumber =
        util.eval(F.Min(nonNegativeIterations.toArray(new IExpr[nonNegativeIterations.size()])));
    if (!smallestPossibleIterationNumber.isInteger()) return Optional.empty();
    return Optional.of(smallestPossibleIterationNumber.toLongDefault());
  }

  private static Optional<ValueAnalysisState> calculateNextValueState(
      ValueAnalysisState pPreviousValueState,
      CIdExpression pVariable,
      SymbolicAssignmentExpression pAssignment,
      String pFunctionName) {
    ExprEvaluator util = new ExprEvaluator();
    IExpr symbolicExpression = pAssignment.symbolicExpression();
    List<CIdExpression> encounteredVariables = pAssignment.encounteredVariables();
    HashMap<MemoryLocation, ValueAndType> newState = new HashMap<>();
    Set<Entry<MemoryLocation, ValueAndType>> state = pPreviousValueState.getConstants();
    for (Entry<MemoryLocation, ValueAndType> entry : state) {
      if (!entry.getKey().equals(MemoryLocation.forDeclaration(pVariable.getDeclaration()))) {
        newState.put(entry.getKey(), entry.getValue());
      }
    }
    for (Entry<MemoryLocation, ValueAndType> entry : pPreviousValueState.getConstants()) {
      symbolicExpression =
          util.eval(
              F.ReplaceAll(
                  symbolicExpression,
                  F.Rule(
                      F.$s(entry.getKey().getIdentifier() + "-"),
                      F.ZZ(
                          BigInteger.valueOf(
                              entry
                                  .getValue()
                                  .getValue()
                                  .asLong(CNumericTypes.LONG_INT)
                                  .orElseThrow())))));
      if (symbolicExpression.isInteger()) break;
    }
    if (symbolicExpression.isInteger()) {
      Entry<MemoryLocation, ValueAndType> newConstant =
          new SimpleEntry<>(
              MemoryLocation.forDeclaration(pVariable.getDeclaration()),
              new ValueAndType(
                  new NumericValue(symbolicExpression.toIntDefault()), CNumericTypes.INT));
      newState.put(newConstant.getKey(), newConstant.getValue());
      return Optional.of(
          new ValueAnalysisState(
              Optional.of(MachineModel.LINUX64), PathCopyingPersistentTreeMap.copyOf(newState)));
    }
    // just add the expression as is
    Optional<CExpression> newValueAsExpression =
        LoopAccelerationUtils.expressionFromIExpr(
            symbolicExpression, ImmutableSet.copyOf(encounteredVariables));
    if (newValueAsExpression.isPresent()) {
      CExpressionTransformer expressionToValueTransformer =
          new CExpressionTransformer(
              pFunctionName, pPreviousValueState, MachineModel.LINUX64, null);
      Entry<MemoryLocation, ValueAndType> newConstant;
      Value newValue;
      try {
        newValue = expressionToValueTransformer.transform(newValueAsExpression.orElseThrow());
      } catch (UnrecognizedCodeException pE) {
        return Optional.empty();
      }
      newConstant =
          new SimpleEntry<>(
              MemoryLocation.forDeclaration(pVariable.getDeclaration()),
              new ValueAndType(newValue, CNumericTypes.INT));

      newState.put(newConstant.getKey(), newConstant.getValue());
      return Optional.of(
          new ValueAnalysisState(
              Optional.of(MachineModel.LINUX64), PathCopyingPersistentTreeMap.copyOf(newState)));
    }

    return Optional.empty();
  }
}
