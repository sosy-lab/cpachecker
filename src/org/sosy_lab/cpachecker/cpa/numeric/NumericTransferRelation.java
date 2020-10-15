// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
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

@Options(prefix = "cpa.numeric.NumericTransferRelation")
public class NumericTransferRelation
    extends ForwardingTransferRelation<
        Collection<NumericState>, NumericState, VariableTrackingPrecision> {

  private final LogManager logger;
  private boolean useLoopInformation;

  @Option(
      secure = true,
      name = "handledVariableTypes",
      toUppercase = true,
      description = "Use this to set which type of variables should be handled.")
  private HandleNumericTypes handledVariableTypes = HandleNumericTypes.INTEGERS_AND_DOUBLES;

  NumericTransferRelation(Configuration config, LogManager pLogManager)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogManager;
    logger.log(Level.CONFIG, handledVariableTypes);
    useLoopInformation = false;
  }

  void setUseLoopInformation() {
    useLoopInformation = true;
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
    return cExpression.accept(
        new NumericAssumptionHandler(
            state, handledVariableTypes, truthAssumption, cfaEdge, precision, logger));
  }

  @Override
  protected Collection<NumericState> handleDeclarationEdge(
      CDeclarationEdge cfaEdge, CDeclaration declaration) throws UnrecognizedCodeException {
    Collection<NumericState> successors =
        declaration.accept(
            new NumericDeclarationVisitor(state, handledVariableTypes, cfaEdge, precision, logger));
    if (!successors.isEmpty()) {
      return successors;
    } else {
      return ImmutableSet.of(state.createCopy());
    }
  }

  @Override
  protected Collection<NumericState> handleStatementEdge(
      CStatementEdge cfaEdge, CStatement statement) throws UnrecognizedCodeException {
    return statement.accept(
        new NumericStatementVisitor(state, cfaEdge, handledVariableTypes, precision, logger));
  }

  @Override
  protected Collection<NumericState> handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws UnrecognizedCodeException {
    // Collect all variables to add them in one function call
    ImmutableSet.Builder<Variable> integerVariables = new ImmutableSet.Builder<>();
    ImmutableSet.Builder<Variable> realVariables = new ImmutableSet.Builder<>();

    for (CParameterDeclaration declaration : parameters) {
      if (!(declaration.getType() instanceof CSimpleType)) {
        // Do nothing for types that aren't handled by the cpa
        continue;
      }
      Optional<NumericVariable> variable =
          NumericVariable.valueOf(
              declaration, cfaEdge.getSuccessor(), precision, state.getManager(), logger);

      if (variable.isEmpty()) {
        continue;
      }

      CSimpleType declarationType = (CSimpleType) declaration.getType();

      if (declarationType.getType().isFloatingPointType() && handledVariableTypes.handleReals()) {
        realVariables.add(variable.get());
      } else if (declarationType.getType().isIntegerType()
          && handledVariableTypes.handleIntegers()) {
        integerVariables.add(variable.get());
      }
      if (state.getValue().getEnvironment().containsVariable(variable.get())) {
        throw new IllegalStateException("Variable is already contained in environment.");
      }
    }

    NumericState extendedState =
        state.addVariables(
            integerVariables.build(), realVariables.build(), NewVariableValue.UNCONSTRAINED);
    Environment extendedEnvironment = extendedState.getValue().getEnvironment();

    // Set values of the variables one by one
    Collection<NumericState> successors = ImmutableSet.of(extendedState);

    for (int i = 0; i < parameters.size(); i++) {
      Collection<NumericState> tempStates =
          assignParameterValue(
              arguments.get(i), parameters.get(i), successors, cfaEdge, extendedEnvironment);
      if (successors != tempStates) {
        disposeAll(successors);
      }
      successors = tempStates;
    }
    return successors;
  }

  private Collection<NumericState> assignParameterValue(
      CExpression expression,
      CParameterDeclaration declaration,
      Collection<NumericState> pStates,
      CFAEdge pCfaEdge,
      Environment pEnvironment)
      throws UnrecognizedCodeException {
    if (expression == null || declaration == null || pStates == null) {
      throw new IllegalArgumentException("Parameters can not be null.");
    }
    if (pStates.isEmpty()) {
      throw new IllegalArgumentException("pStates can not be empty.");
    }

    Optional<NumericVariable> variable =
        NumericVariable.valueOf(
            declaration, pCfaEdge.getSuccessor(), precision, state.getManager(), logger);

    if (variable.isPresent()) {
      logger.log(Level.FINEST, "ApplyAssignmentConstraints", variable, expression);
      ImmutableSet.Builder<NumericState> statesBuilder = new ImmutableSet.Builder<>();
      Collection<PartialState> partialAssignments =
          expression.accept(
              new NumericRightHandSideVisitor(
                  pEnvironment,
                  state.getManager(),
                  handledVariableTypes,
                  pCfaEdge,
                  precision,
                  logger));
      for (PartialState partialAssignment : partialAssignments) {
        ImmutableList.Builder<NumericState> successorCandidates = new ImmutableList.Builder<>();
        for (NumericState current : pStates) {
          successorCandidates.add(current.createCopy());
        }

        if (pEnvironment.containsVariable(variable.get())) {
          AssignParameter(variable.get(), statesBuilder, partialAssignment, successorCandidates);
        }
      }
      return statesBuilder.build();
    } else {
      return pStates;
    }
  }

  private void AssignParameter(
      Variable pVariable,
      Builder<NumericState> pStatesBuilder,
      PartialState partialAssignment,
      ImmutableList.Builder<NumericState> pSuccessorCandidates) {
    Collection<TreeConstraint> assignmentConstraints = partialAssignment.getConstraints();
    // Meet the constraints and then assign the value

    for (NumericState successorCandidate : pSuccessorCandidates.build()) {
      Optional<NumericState> tempState = successorCandidate.meet(assignmentConstraints);
      Optional<NumericState> out =
          tempState.map(
              (st) -> st.assignTreeExpression(pVariable, partialAssignment.getPartialConstraint()));
      tempState.ifPresent((st) -> st.getValue().dispose());
      out.ifPresent(
          (st) -> {
            if (!st.getValue().isBottom()) {
              pStatesBuilder.add(st);
            }
          });

      if (logger.wouldBeLogged(Level.FINEST)) {
        logger.log(
            Level.FINEST,
            "To",
            successorCandidate,
            "Add parameter: ",
            pVariable,
            "=",
            partialAssignment.getPartialConstraint(),
            " by using: ",
            assignmentConstraints,
            "successors:",
            (out.isPresent() ? out.get() : "EMPTY"));
      }
      successorCandidate.getValue().dispose();
    }
  }

  private void disposeAll(Collection<NumericState> states) {
    for (NumericState tempState : states) {
      tempState.getValue().dispose();
    }
  }

  @Override
  protected Collection<NumericState> handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws UnrecognizedCodeException {
    Optional<CAssignment> assignment = cfaEdge.asAssignment().toJavaUtil();

    if (assignment.isPresent()) {
      Optional<? extends AVariableDeclaration> declaration =
          cfaEdge.getSuccessor().getEntryNode().getReturnVariable().toJavaUtil();

      if (declaration.isPresent() && declaration.get() instanceof CVariableDeclaration) {
        assert ((CVariableDeclaration) declaration.get()).getInitializer() == null;
        Collection<NumericState> intermediateStates =
            ((CDeclaration) declaration.get())
                .accept(
                    new NumericDeclarationVisitor(
                        state, handledVariableTypes, cfaEdge, precision, logger));
        // The declaration of the return variable does not contain an initializer and therefore can
        // only return exactly one state.
        assert intermediateStates.size() == 1;

        ImmutableSet.Builder<NumericState> statesAfterAssignment = new ImmutableSet.Builder<>();
        for (NumericState intermediateState : intermediateStates) {
          Collection<NumericState> assigned =
              assignment
                  .get()
                  .accept(
                      new NumericStatementVisitor(
                          intermediateState, cfaEdge, handledVariableTypes, precision, logger));
          // Dispose of value in intermediate state
          if (!assigned.contains(intermediateState)) {
            intermediateState.getValue().dispose();
          }
          statesAfterAssignment.addAll(assigned);
        }
        return statesAfterAssignment.build();
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
          fnkCall.getFunctionEntry().getReturnVariable().toJavaUtil();
      if (!returnVarDeclaration.isPresent()) {
        throw new IllegalStateException("Can not handle CAssignment without return value.");
      }

      Optional<NumericVariable> returnVariable =
          NumericVariable.valueOf(
              returnVarDeclaration.get(),
              cfaEdge.getSuccessor(),
              precision,
              state.getManager(),
              logger);
      if (returnVariable.isPresent()) {
        Collection<NumericState> tempStates =
            assignment.accept(
                new NumericStatementVisitor(
                    state, handledVariableTypes, returnVariable.get(), cfaEdge, precision, logger));
        ImmutableSet.Builder<NumericState> successorsBuilder = new ImmutableSet.Builder<>();
        for (NumericState tempState : tempStates) {
          successorsBuilder.add(tempState.removeVariables(ImmutableSet.of(returnVariable.get())));
          tempState.getValue().dispose();
        }
        return successorsBuilder.build();
      }
    }

    // nothing to do if return Variable was not tracked
    return ImmutableSet.of(state.createCopy());
  }

  private Collection<Variable> createOutOfScopeVariables(
      Collection<CSimpleDeclaration> declarations, CFANode pCfaNode) {
    ImmutableSet.Builder<Variable> outOfScopeBuilder = new ImmutableSet.Builder<>();
    for (CSimpleDeclaration declaration : declarations) {
      Optional<NumericVariable> variable =
          NumericVariable.valueOf(declaration, pCfaNode, precision, state.getManager(), logger);
      variable.ifPresent(var -> outOfScopeBuilder.add(var));
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
  private Collection<NumericState> removeVariablesFromEach(
      Collection<Variable> pVariables, Collection<NumericState> pStates) {
    if (pVariables.isEmpty()) {
      return pStates;
    }

    ImmutableSet.Builder<NumericState> successorsBuilder = new ImmutableSet.Builder<>();

    for (NumericState pState : pStates) {
      successorsBuilder.add(pState.removeVariables(pVariables));
    }

    return successorsBuilder.build();
  }

  /** Removes all empty states. */
  public static Collection<NumericState> removeEmptyStates(Collection<NumericState> pStates) {
    ImmutableSet.Builder<NumericState> stateBuilder = new ImmutableSet.Builder<>();

    for (NumericState tempState : pStates) {
      if (!tempState.isBottom()) {
        stateBuilder.add(tempState);
      }
    }

    return stateBuilder.build();
  }

  Collection<NumericState> setLoopHead(Collection<NumericState> pStates) {
    ImmutableSet.Builder<NumericState> stateBuilder = new ImmutableSet.Builder<>();

    for (NumericState tempState : pStates) {
      stateBuilder.add(tempState.getAsLoopHead());
    }

    return stateBuilder.build();
  }

  @Override
  protected Collection<NumericState> postProcessing(
      Collection<NumericState> successors, CFAEdge edge) {
    if (successors == null) {
      return ImmutableSet.of();
    } else {
      // Set states as loop head if necessary
      Collection<NumericState> setLoopHeadStates;
      if (useLoopInformation && edge.getSuccessor().isLoopStart()) {
        setLoopHeadStates = setLoopHead(successors);
      } else {
        setLoopHeadStates = successors;
      }

      // Remove out of scope variables from each successor
      Collection<Variable> outOfScopeVariables =
          createOutOfScopeVariables(
              edge.getSuccessor().getOutOfScopeVariables(), edge.getSuccessor());
      Collection<NumericState> reducedSuccessors =
          removeVariablesFromEach(outOfScopeVariables, setLoopHeadStates);

      if (logger.wouldBeLogged(Level.FINEST)) {
        for (NumericState successor : reducedSuccessors) {
          logger.log(
              Level.FINEST,
              edge.getEdgeType(),
              edge.getCode(),
              "successor:",
              successor,
              "as Interval:",
              successor.getValue().toIntervalString(false));
        }
      }

      return reducedSuccessors;
    }
  }

  /** Sets which variable types should be considered. */
  public enum HandleNumericTypes {
    /** Consider only integer valued variables. */
    ONLY_INTEGERS,
    /** Consider only real valued variables. */
    ONLY_DOUBLES,
    /** Consider both integer and real valued variables. */
    INTEGERS_AND_DOUBLES;

    public boolean handleReals() {
      return this == ONLY_DOUBLES || this == INTEGERS_AND_DOUBLES;
    }

    public boolean handleIntegers() {
      return this == ONLY_INTEGERS || this == INTEGERS_AND_DOUBLES;
    }
  }
}
