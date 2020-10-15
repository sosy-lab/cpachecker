// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric.visitor;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.numeric.NumericState;
import org.sosy_lab.cpachecker.cpa.numeric.NumericVariable;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.numericdomains.Value.NewVariableValue;
import org.sosy_lab.numericdomains.constraint.ConstraintType;
import org.sosy_lab.numericdomains.constraint.TreeConstraint;
import org.sosy_lab.numericdomains.constraint.tree.TreeNode;
import org.sosy_lab.numericdomains.constraint.tree.VariableTreeNode;
import org.sosy_lab.numericdomains.environment.Environment;
import org.sosy_lab.numericdomains.environment.Variable;

public class NumericDeclarationVisitor
    implements CSimpleDeclarationVisitor<Collection<NumericState>, UnrecognizedCodeException> {
  private final NumericState state;
  private final LogManager logger;
  private final CFAEdge edge;
  private final VariableTrackingPrecision precision;

  /**
   * Creates a NumericDeclarationVisitor.
   *
   * @param pState state before the declaration
   * @param pEdge edge of the declaration
   * @param pPrecision precision of the CPA
   */
  public NumericDeclarationVisitor(
      NumericState pState,
      CFAEdge pEdge,
      VariableTrackingPrecision pPrecision,
      LogManager logManager) {
    this.state = pState;
    logger = logManager;
    precision = pPrecision;
    edge = pEdge;
  }

  @Override
  public Collection<NumericState> visit(CVariableDeclaration pDecl)
      throws UnrecognizedCodeException {
    Optional<NumericVariable> variable =
        NumericVariable.valueOf(pDecl, edge.getSuccessor(), precision, state.getManager(), logger);

    if (pDecl.getType() instanceof CSimpleType && variable.isPresent()) {
      CSimpleType simpleType = (CSimpleType) pDecl.getType();

      final ImmutableSet<Variable> intVar;
      final ImmutableSet<Variable> realVar;
      if (simpleType.getType().isFloatingPointType()) {
        intVar = ImmutableSet.of();
        realVar = ImmutableSet.of(variable.get());
      } else {
        intVar = ImmutableSet.of(variable.get());
        realVar = ImmutableSet.of();
      }

      if (intVar.size() == 0 && realVar.size() == 0) {
        return ImmutableSet.of(state.createCopy());
      }

      // Checks the default value of the uninitialized variable
      final NewVariableValue initialValue;

      if (pDecl.isGlobal()) {
        initialValue = NewVariableValue.ZERO;
      } else {
        initialValue = NewVariableValue.UNCONSTRAINED;
      }

      NumericState extendedState = state.addVariables(intVar, realVar, initialValue);

      Collection<NumericState> initializedStates;

      CInitializer initializer = pDecl.getInitializer();
      if (initializer != null) {
        initializedStates =
            applyInitializer(variable, extendedState, (CInitializerExpression) initializer);
      } else {
        initializedStates = ImmutableSet.of(extendedState.createCopy());
      }
      extendedState.getValue().dispose();

      Collection<NumericState> successors = initializedStates;
      if (simpleType.isUnsigned()) {
        successors =
            setVariableUnsigned(
                variable.get(), extendedState.getValue().getEnvironment(), initializedStates);
      }

      if (!successors.isEmpty()) {
        return successors;
      } else {
        // Return the state before the addition of the variable as a fallback
        return ImmutableSet.of(state.createCopy());
      }
    }

    return ImmutableSet.of(state.createCopy());
  }

  public static Collection<NumericState> setVariableUnsigned(
      Variable pVariable, Environment pEnvironment, Collection<NumericState> pStates) {
    ImmutableSet.Builder<NumericState> unsigned = new ImmutableSet.Builder<>();
    for (NumericState pState : pStates) {
      TreeNode rootNode = new VariableTreeNode(pVariable);
      TreeConstraint biggerThanZero =
          new TreeConstraint(pEnvironment, ConstraintType.BIGGER_EQUALS, null, rootNode);

      Optional<NumericState> successor = pState.meetConstraints(ImmutableSet.of(biggerThanZero));
      successor.ifPresent(unsigned::add);
      pState.getValue().dispose();
    }
    return unsigned.build();
  }

  private Collection<NumericState> applyInitializer(
      Optional<NumericVariable> pVariable, NumericState pState, CInitializerExpression pInitializer)
      throws UnrecognizedCodeException {
    ImmutableSet.Builder<NumericState> pSuccessorStates = new ImmutableSet.Builder<>();
    CExpression expr = pInitializer.getExpression();
    Collection<PartialState> init =
        expr.accept(
            new NumericRightHandSideVisitor(
                pState.getValue().getEnvironment(), state.getManager(), edge, precision, logger));

    for (PartialState partialState : init) {
      NumericState tempSuccessor =
          pState.assignTreeExpression(pVariable.get(), partialState.getPartialConstraint());
      if (partialState.getConstraints().isEmpty()) {
        pSuccessorStates.add(tempSuccessor);
      } else {
        Optional<NumericState> temp = pState.meetConstraints(partialState.getConstraints());
        temp.ifPresent(pSuccessorStates::add);
      }
    }

    Collection<NumericState> successors = pSuccessorStates.build();
    if (successors.isEmpty()) {
      return ImmutableSet.of(pState.createCopy());
    } else {
      return successors;
    }
  }

  @Override
  public Collection<NumericState> visit(CFunctionDeclaration pDecl)
      throws UnrecognizedCodeException {
    return ImmutableSet.of(state.createCopy());
  }

  @Override
  public Collection<NumericState> visit(CComplexTypeDeclaration pDecl)
      throws UnrecognizedCodeException {
    return ImmutableSet.of(state.createCopy());
  }

  @Override
  public Collection<NumericState> visit(CTypeDefDeclaration pDecl)
      throws UnrecognizedCodeException {
    return ImmutableSet.of(state.createCopy());
  }

  @Override
  public Collection<NumericState> visit(CParameterDeclaration declaration)
      throws UnrecognizedCodeException {
    // To reduce the amount of operations on the native value, the parameter declarations are pooled
    // and added in one operation.
    throw new UnsupportedOperationException(
        "Parameter declarations should be pooled and added in one operation on the native state.");
  }

  @Override
  public Collection<NumericState> visit(CEnumerator pDecl) throws UnrecognizedCodeException {
    Optional<NumericVariable> variable =
        NumericVariable.valueOf(pDecl, edge.getSuccessor(), precision, state.getManager(), logger);

    if (variable.isPresent()) {
      NumericState newState;
      if (variable.get().getSimpleType().getType().isIntegerType()) {
        newState =
            state.addVariables(
                ImmutableSet.of(variable.get()), ImmutableSet.of(), NewVariableValue.UNCONSTRAINED);
      } else {
        newState =
            state.addVariables(
                ImmutableSet.of(), ImmutableSet.of(variable.get()), NewVariableValue.UNCONSTRAINED);
      }

      return ImmutableSet.of(newState);
    }

    return ImmutableSet.of(state.createCopy());
  }
}
