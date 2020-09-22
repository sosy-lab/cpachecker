// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.numeric.visitor.NumericAssumptionHandler;
import org.sosy_lab.cpachecker.cpa.numeric.visitor.NumericDeclarationVisitor;
import org.sosy_lab.cpachecker.cpa.numeric.visitor.NumericRightHandSideVisitor;
import org.sosy_lab.cpachecker.cpa.numeric.visitor.NumericStatementVisitor;
import org.sosy_lab.cpachecker.cpa.numeric.visitor.PartialState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.numericdomains.Value.NewVariableValue;
import org.sosy_lab.numericdomains.constraint.TreeConstraint;
import org.sosy_lab.numericdomains.environment.Environment;
import org.sosy_lab.numericdomains.environment.Variable;

public class NumericTransferRelation
    extends ForwardingTransferRelation<
        Collection<NumericState>, NumericState, VariableTrackingPrecision> {

  private final LogManager logger;

  NumericTransferRelation(LogManager logManager) {
    logger = logManager;
  }

  @Override
  protected Collection<NumericState> handleBlankEdge(BlankEdge cfaEdge)
      throws CPATransferException {
    return ImmutableSet.of(state);
  }

  @Override
  protected Collection<NumericState> handleAssumption(
      CAssumeEdge cfaEdge, CExpression cExpression, boolean truthAssumption)
      throws UnrecognizedCodeException {
    return cExpression.accept(new NumericAssumptionHandler(state, truthAssumption, logger));
  }

  @Override
  protected Collection<NumericState> handleDeclarationEdge(
      CDeclarationEdge cfaEdge, CDeclaration declaration) throws UnrecognizedCodeException {
    NumericState successor = declaration.accept(new NumericDeclarationVisitor(state, logger));

    if (successor != null) {
      return ImmutableSet.of(successor);
    } else {
      return ImmutableSet.of(state);
    }
  }

  @Override
  protected Collection<NumericState> handleStatementEdge(
      CStatementEdge cfaEdge, CStatement statement) throws UnrecognizedCodeException {
    return statement.accept(new NumericStatementVisitor(state, logger));
  }

  @Override
  protected Collection<NumericState> handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws UnrecognizedCodeException {
    // Collect all variables to add them in one native function call
    ImmutableSet.Builder<Variable> integerVariables = new ImmutableSet.Builder<>();
    for (CParameterDeclaration declaration : parameters) {
      Variable variable = createVariableFromDeclaration(declaration);
      integerVariables.add(variable);
      if (state.getValue().getEnvironment().containsVariable(variable)) {
        throw new IllegalStateException("Variable is already contained in environment.");
      }
    }

    NumericState extendedState =
        state.addVariables(
            integerVariables.build(), ImmutableSet.of(), NewVariableValue.UNCONSTRAINED);
    Environment extendedEnvironment = extendedState.getValue().getEnvironment();

    // Set values of the variables one by one
    Collection<NumericState> successors = ImmutableSet.of(extendedState);

    for (int i = 0; i < parameters.size(); i++) {
      Collection<NumericState> tempStates =
          applyAssignmentConstraints(
              arguments.get(i), parameters.get(i), successors, extendedEnvironment);
      disposeAll(successors);
      successors = tempStates;
    }
    return successors;
  }

  private Collection<NumericState> applyAssignmentConstraints(
      CExpression expression,
      CParameterDeclaration declaration,
      Collection<NumericState> pStates,
      Environment pEnvironment)
      throws UnrecognizedCodeException {
    if (expression == null || declaration == null || pStates == null) {
      throw new IllegalArgumentException("Parameters can not be null.");
    }

    Variable variable = createVariableFromDeclaration(declaration);
    logger.log(Level.FINEST, "ApplyAssignmentConstraints", variable, expression);

    ImmutableSet.Builder<NumericState> statesBuilder = new ImmutableSet.Builder<>();
    Collection<PartialState> partialAssignments =
        expression.accept(new NumericRightHandSideVisitor(pEnvironment, null, logger));
    for (PartialState partialAssignment : partialAssignments) {
      ImmutableList.Builder<NumericState> successorCandidates = new ImmutableList.Builder<>();
      for (NumericState current : pStates) {
        successorCandidates.add(current);
      }
      Collection<TreeConstraint> assignmentConstraints =
          partialAssignment.assignToVariable(pEnvironment, variable);
      for (NumericState tempState : successorCandidates.build()) {
        tempState.meet(assignmentConstraints).ifPresent(statesBuilder::add);
      }
    }
    return statesBuilder.build();
  }

  private void disposeAll(Collection<NumericState> states) {
    for (NumericState tempState : states) {
      tempState.getValue().dispose();
    }
  }

  @Override
  protected Collection<NumericState> handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws UnrecognizedCodeException {
    Optional<CAssignment> assignment = cfaEdge.asAssignment();

    if (assignment.isPresent()) {
      Optional<? extends AVariableDeclaration> declaration =
          cfaEdge.getSuccessor().getEntryNode().getReturnVariable();

      if (declaration.isPresent() && declaration.get() instanceof CVariableDeclaration) {
        NumericState intermediateStates =
            ((CDeclaration) declaration.get()).accept(new NumericDeclarationVisitor(state, logger));
        Collection<NumericState> statesAfterAssignment =
            assignment.get().accept(new NumericStatementVisitor(intermediateStates, logger));
        // Dispose of value in intermediate state
        if (!statesAfterAssignment.contains(intermediateStates)) {
          intermediateStates.getValue().dispose();
        }
        return statesAfterAssignment;
      }
    }

    return ImmutableSet.of(state);
  }

  @Override
  protected Collection<NumericState> handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String callerFunctionName)
      throws UnrecognizedCodeException {
    CFunctionCall expr = fnkCall.getExpression();
    if (expr instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assignment = (CFunctionCallAssignmentStatement) expr;
      Optional<CVariableDeclaration> returnVarDeclaration =
          fnkCall.getFunctionEntry().getReturnVariable();
      if (!returnVarDeclaration.isPresent()) {
        throw new IllegalStateException("Can not handle CAssignment without return value.");
      }

      Variable returnVariable = createVariableFromDeclaration(returnVarDeclaration.get());
      Collection<NumericState> tempStates =
          assignment.accept(new NumericStatementVisitor(state, returnVariable, logger));
      ImmutableSet.Builder<NumericState> successorsBuilder = new ImmutableSet.Builder<>();
      for (NumericState tempState : tempStates) {
        successorsBuilder.add(tempState.removeVariables(ImmutableSet.of(returnVariable)));
        tempState.getValue().dispose();
      }
      return successorsBuilder.build();
    } else {
      // nothing to do
      return ImmutableSet.of(state);
    }
  }

  private Collection<Variable> createOutOfScopeVariables(
      Collection<CSimpleDeclaration> declarations) {
    ImmutableSet.Builder<Variable> outOfScopeBuilder = new ImmutableSet.Builder<>();
    for (CSimpleDeclaration declaration : declarations) {
      outOfScopeBuilder.add(createVariableFromDeclaration(declaration));
    }
    return outOfScopeBuilder.build();
  }

  /**
   * Returns the states with the variables removed.
   *
   * <p>If at least one variable is given, the returned state is a copy of the original state and
   * all original states will be disposed.
   *
   * @param pVariables variables that will be removed
   * @param pStates states from which the variables will be removed
   * @return collection of states with the variables removed
   */
  private Collection<NumericState> removeVariablesFromEachAndDispose(
      Collection<Variable> pVariables, Collection<NumericState> pStates) {
    if (pVariables.isEmpty()) {
      return pStates;
    }

    ImmutableSet.Builder<NumericState> successorsBuilder = new ImmutableSet.Builder<>();

    for (NumericState newState : pStates) {
      successorsBuilder.add(newState.removeVariables(pVariables));
      newState.getValue().dispose();
    }

    return successorsBuilder.build();
  }

  @Override
  protected Collection<NumericState> postProcessing(
      Collection<NumericState> successors, CFAEdge edge) {
    if (successors == null) {
      logger.log(Level.FINEST, edge, "has no successors");
      return ImmutableSet.of();
    } else {
      // Remove out of scope variables from each successor
      Collection<Variable> outOfScopeVariables =
          createOutOfScopeVariables(edge.getSuccessor().getOutOfScopeVariables());
      Collection<NumericState> newSuccessors =
          removeVariablesFromEachAndDispose(outOfScopeVariables, successors);

      if (logger.wouldBeLogged(Level.FINEST)) {
        for (NumericState successor : newSuccessors) {
          StringBuilder builder = new StringBuilder();
          for (Variable var : successor.getValue().getEnvironment().getIntVariables()) {
            builder.append(var).append("=").append(successor.getValue().getBounds(var)).append(";");
          }
          for (Variable var : successor.getValue().getEnvironment().getRealVariables()) {
            builder.append(var).append("=").append(successor.getValue().getBounds(var)).append(";");
          }
          logger.log(
              Level.FINEST,
              edge.getEdgeType(),
              edge.getCode(),
              "successor:",
              successor,
              "intervals:",
              builder.toString());
        }
      }
      return ImmutableSet.copyOf(newSuccessors);
    }
  }

  /**
   * Creates the name of the variable by its declaration.
   *
   * @param pDeclaration declaration from which the variable should be created
   * @return variable which is described by the declaration
   */
  public static Variable createVariableFromDeclaration(CSimpleDeclaration pDeclaration) {
    return new Variable(pDeclaration.getQualifiedName());
  }

  /**
   * Represents a variable substitution.
   *
   * <p>The variable substitution consists of the variable which should be substituted and the
   * substitute.
   */
  public static class VariableSubstitution {
    private static final String SUBSTITUTE_SUFFIX = "__SUBSTITUTE__";

    /**
     * Create a substitution for the variable.
     *
     * @param pVariable variable for which the substitution is created
     * @param additionalSuffix suffix that is added to the name of the variable in addition to the
     *     default suffix {@link VariableSubstitution#SUBSTITUTE_SUFFIX}
     * @return variable substitution consisting of the source variable and the substitute
     */
    public static VariableSubstitution createSubstitutionOf(
        Variable pVariable, @Nullable String additionalSuffix) {
      String name =
          pVariable.getName()
              + SUBSTITUTE_SUFFIX
              + (additionalSuffix != null ? additionalSuffix : "");
      return new VariableSubstitution(pVariable, new Variable(name));
    }

    private final Variable from;
    private final Variable substitute;
    /** Tracks whether the substitution was applied. */
    private boolean wasUsed;

    /**
     * Creates a substitution.
     *
     * @param pFrom variable that should be substituted
     * @param pSubstitute variable with which pFrom is substituted
     */
    private VariableSubstitution(Variable pFrom, Variable pSubstitute) {
      from = pFrom;
      substitute = pSubstitute;
    }

    /**
     * Applies the substitution for the variable.
     *
     * <p>The variable is only substituted if the variable equals the {@link
     * VariableSubstitution#from} variable that was used to create this substitution.
     *
     * @param pVariable variable that may be substituted
     * @return substitute if the pVariable is the variable that should be substituted, pVariable
     *     otherwise
     */
    public Variable applyTo(Variable pVariable) {
      if (pVariable.equals(from)) {
        wasUsed = true;
        return substitute;
      } else {
        return pVariable;
      }
    }

    /**
     * Checks whether the substitution was used.
     *
     * @return {@code true} if the substitution was used, {@code false} otherwise
     */
    public boolean wasUsed() {
      return wasUsed;
    }

    /** Returns the variable which will be substituted. */
    public Variable getFrom() {
      return from;
    }

    /** Returns the substitute. */
    public Variable getSubstitute() {
      return substitute;
    }
  }
}
