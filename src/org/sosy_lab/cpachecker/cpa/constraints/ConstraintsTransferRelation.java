// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintFactory;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver.SolverResult;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver.SolverResult.Satisfiability;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.util.StateSimplifier;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.java_smt.api.SolverException;

/** Transfer relation for Symbolic Execution Analysis. */
@Options(prefix = "cpa.constraints")
public class ConstraintsTransferRelation
    extends ForwardingTransferRelation<ConstraintsState, ConstraintsState, SingletonPrecision> {

  private enum CheckStrategy {
    AT_ASSUME,
    AT_TARGET
  }

  @Option(
      name = "satCheckStrategy",
      description = "When to check the satisfiability of constraints")
  private CheckStrategy checkStrategy = CheckStrategy.AT_ASSUME;

  private final LogManagerWithoutDuplicates logger;

  private MachineModel machineModel;

  private ConstraintsSolver solver;
  private StateSimplifier simplifier;

  public ConstraintsTransferRelation(
      final ConstraintsSolver pSolver,
      final ConstraintsStatistics pStats,
      final MachineModel pMachineModel,
      final LogManager pLogger,
      final Configuration pConfig)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = new LogManagerWithoutDuplicates(pLogger);
    machineModel = pMachineModel;
    simplifier = new StateSimplifier(pConfig, pStats);
    solver = pSolver;
  }

  @Override
  protected ConstraintsState handleFunctionCallEdge(
      FunctionCallEdge pCfaEdge,
      List<? extends AExpression> pArguments,
      List<? extends AParameterDeclaration> pParameters,
      String pCalledFunctionName) {
    return state;
  }

  @Override
  protected ConstraintsState handleFunctionReturnEdge(
      FunctionReturnEdge pCfaEdge, AFunctionCall pSummaryExpression, String pCallerFunctionName) {
    return state;
  }

  @Override
  protected ConstraintsState handleStatementEdge(AStatementEdge pCfaEdge, AStatement pStatement) {
    return state;
  }

  @Override
  protected ConstraintsState handleReturnStatementEdge(AReturnStatementEdge pCfaEdge) {
    return state;
  }

  @Override
  protected ConstraintsState handleFunctionSummaryEdge(FunctionSummaryEdge pCfaEdge) {
    return state;
  }

  @Override
  protected ConstraintsState handleDeclarationEdge(
      ADeclarationEdge pCfaEdge, ADeclaration pDeclaration) throws CPATransferException {
    return state;
  }

  @Override
  protected ConstraintsState handleBlankEdge(BlankEdge cfaEdge) {
    // FIXME: Find a better way to have consistent symbolic identifier names
    if (cfaEdge.getDescription().equals("INIT GLOBAL VARS")) {
      SymbolicValueFactory.reset();
    }
    return state;
  }

  @Override
  protected ConstraintsState handleAssumption(
      AssumeEdge pCfaEdge, AExpression pExpression, boolean pTruthAssumption) {
    return state;
  }

  private ConstraintsState getNewState(
      ConstraintsState pOldState,
      AExpression pExpression,
      ConstraintFactory pFactory,
      boolean pTruthAssumption)
      throws UnrecognizedCodeException, SolverException, InterruptedException {

    return computeNewStateByCreatingConstraint(pOldState, pExpression, pFactory, pTruthAssumption);
  }

  private ConstraintsState computeNewStateByCreatingConstraint(
      final ConstraintsState pOldState,
      final AExpression pExpression,
      final ConstraintFactory pFactory,
      final boolean pTruthAssumption)
      throws UnrecognizedCodeException, SolverException, InterruptedException {

    Optional<Constraint> oNewConstraint = createConstraint(pExpression, pFactory, pTruthAssumption);

    if (oNewConstraint.isPresent()) {
      final Constraint newConstraint = oNewConstraint.orElseThrow();

      // If a constraint is trivial, its satisfiability is not influenced by other constraints.
      // So to evade more expensive SAT checks, we just check the constraint on its own.
      if (newConstraint.isTrivial()) {
        if (solver.checkUnsat(newConstraint, functionName) == Satisfiability.UNSAT) {
          return null;
        }
      } else {
        return pOldState.copyWithNew(newConstraint);
      }
    }

    return pOldState;
  }

  private Optional<Constraint> createConstraint(
      AExpression pExpression, ConstraintFactory pFactory, boolean pTruthAssumption)
      throws UnrecognizedCodeException {

    if (pExpression instanceof JBinaryExpression jBinaryExpression) {
      return createConstraint(jBinaryExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof JUnaryExpression jUnaryExpression) {
      return createConstraint(jUnaryExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof CBinaryExpression cBinaryExpression) {
      return createConstraint(cBinaryExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof AIdExpression aIdExpression) {
      // id expressions in assume edges are created by a call of __VERIFIER_assume(x), for example
      return createConstraint(aIdExpression, pFactory, pTruthAssumption);

    } else {
      throw new AssertionError("Unhandled expression type " + pExpression.getClass());
    }
  }

  private Optional<Constraint> createConstraint(
      JBinaryExpression pExpression, ConstraintFactory pFactory, boolean pTruthAssumption)
      throws UnrecognizedCodeException {

    Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(pExpression);
    } else {
      constraint = pFactory.createNegativeConstraint(pExpression);
    }

    return Optional.ofNullable(constraint);
  }

  private Optional<Constraint> createConstraint(
      JUnaryExpression pExpression, ConstraintFactory pFactory, boolean pTruthAssumption)
      throws UnrecognizedCodeException {
    Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(pExpression);
    } else {
      constraint = pFactory.createNegativeConstraint(pExpression);
    }

    return Optional.ofNullable(constraint);
  }

  private Optional<Constraint> createConstraint(
      CBinaryExpression pExpression, ConstraintFactory pFactory, boolean pTruthAssumption)
      throws UnrecognizedCodeException {

    Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(pExpression);
    } else {
      constraint = pFactory.createNegativeConstraint(pExpression);
    }

    return Optional.ofNullable(constraint);
  }

  private Optional<Constraint> createConstraint(
      AIdExpression pExpression, ConstraintFactory pFactory, boolean pTruthAssumption) {
    Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(pExpression);
    } else {
      constraint = pFactory.createNegativeConstraint(pExpression);
    }

    return Optional.ofNullable(constraint);
  }

  private ConstraintsState simplify(
      final ConstraintsState pState, final ValueAnalysisState pValueState) {
    return simplifier.simplify(pState, pValueState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      final AbstractState pStateToStrengthen,
      final Iterable<AbstractState> pStrengtheningStates,
      final CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    assert pStateToStrengthen instanceof ConstraintsState;

    super.setInfo(pStateToStrengthen, pPrecision, pCfaEdge);

    final ConstraintsState initialStateToStrengthen = (ConstraintsState) pStateToStrengthen;

    final String currentFunctionName = pCfaEdge.getPredecessor().getFunctionName();

    List<ConstraintsState> newStates = new ArrayList<>();
    newStates.add(initialStateToStrengthen);

    boolean nothingChanged = true;

    for (AbstractState currStrengtheningState : pStrengtheningStates) {
      ConstraintsState currStateToStrengthen = newStates.get(0);
      StrengthenOperator strengthenOperator = null;

      if (currStrengtheningState instanceof ValueAnalysisState) {
        strengthenOperator = new ValueAnalysisStrengthenOperator();

      } else if (currStrengtheningState instanceof AutomatonState) {
        strengthenOperator = new AutomatonStrengthenOperator();
      }

      if (strengthenOperator != null) {
        Optional<Collection<ConstraintsState>> oNewStrengthenedStates =
            strengthenOperator.strengthen(
                currStateToStrengthen, currStrengtheningState, currentFunctionName, pCfaEdge);

        if (oNewStrengthenedStates.isPresent()) {
          newStates.clear(); // remove the old state to replace it with the new, strengthened result
          nothingChanged = false;
          Collection<ConstraintsState> strengthenedStates = oNewStrengthenedStates.orElseThrow();

          if (!strengthenedStates.isEmpty()) {
            ConstraintsState newState = Iterables.getOnlyElement(strengthenedStates);
            newStates.add(newState);
          }
        }
      }

      // if a strengthening resulted in bottom, we can return bottom without performing other
      // strengthen operations
      if (newStates.isEmpty()) {
        return ImmutableList.of();
      }
    }

    if (nothingChanged) {
      return ImmutableList.of(pStateToStrengthen);
    } else {
      return ImmutableList.copyOf(newStates);
    }
  }

  private static ConstraintsState getIfSatisfiable(
      ConstraintsState pStateToCheck, String functionName, ConstraintsSolver solver)
      throws UnrecognizedCodeException, SolverException, InterruptedException {
    SolverResult solverResult = solver.checkUnsat(pStateToCheck, functionName);
    if (solverResult.satisfiability() == Satisfiability.SAT) {
      ConstraintsState newState = pStateToCheck;
      if (solverResult.model().isPresent()) {
        newState = pStateToCheck.copyWithSatisfyingModel(solverResult.model().orElseThrow());
      }
      if (solverResult.definiteAssignments().isPresent()) {
        newState =
            pStateToCheck.copyWithDefiniteAssignment(
                solverResult.definiteAssignments().orElseThrow());
      }
      return newState;
    }
    return null;
  }

  private final class ValueAnalysisStrengthenOperator implements StrengthenOperator {

    @Override
    public Optional<Collection<ConstraintsState>> strengthen(
        final ConstraintsState pStateToStrengthen,
        final AbstractState pValueState,
        final String pFunctionName,
        final CFAEdge pCfaEdge)
        throws CPATransferException, InterruptedException {

      assert pValueState instanceof ValueAnalysisState;

      if (!(pCfaEdge instanceof AssumeEdge assume)) {
        return Optional.empty();
      }

      final ValueAnalysisState valueState = (ValueAnalysisState) pValueState;

      final boolean truthAssumption = assume.getTruthAssumption();
      final AExpression edgeExpression = assume.getExpression();

      final ConstraintFactory factory =
          ConstraintFactory.getInstance(pFunctionName, valueState, machineModel, logger);

      ConstraintsState newState;
      try {

        newState = getNewState(pStateToStrengthen, edgeExpression, factory, truthAssumption);

        // newState == null represents the bottom element, so we return an empty collection
        // (which represents the bottom element for strengthen methods)
        if (newState != null) {
          newState = simplify(newState, valueState);
          if (checkStrategy == CheckStrategy.AT_ASSUME) {
            newState = getIfSatisfiable(newState, functionName, solver);
          }
          if (newState != null) {
            if (newState.equals(pStateToStrengthen)) {
              // return of empty optional means state is unchanged
              return Optional.empty();
            } else {
              return Optional.of(ImmutableList.of(newState));
            }
          }
        }
        assert newState == null : "newState must be null if empty list is returned";
        // return of an empty list means state is unreachable
        return Optional.of(ImmutableList.of());

      } catch (SolverException e) {
        throw new CPATransferException(
            "Error while strengthening ConstraintsState with ValueAnalysisState", e);
      }
    }
  }

  private final class AutomatonStrengthenOperator implements StrengthenOperator {

    @Override
    public Optional<Collection<ConstraintsState>> strengthen(
        final ConstraintsState pStateToStrengthen,
        final AbstractState pStrengtheningState,
        final String pFunctionName,
        final CFAEdge pEdge)
        throws CPATransferException, InterruptedException {
      assert pStrengtheningState instanceof AutomatonState;

      if (checkStrategy != CheckStrategy.AT_TARGET) {
        return Optional.empty();
      }

      final AutomatonState automatonState = (AutomatonState) pStrengtheningState;

      try {
        if (automatonState.isTarget()
            && getIfSatisfiable(pStateToStrengthen, functionName, solver) == null) {

          return Optional.of(ImmutableSet.of());

        } else {
          return Optional.empty();
        }
      } catch (SolverException e) {
        throw new CPATransferException("Error while strengthening.", e);
      }
    }
  }

  private interface StrengthenOperator {

    /**
     * Strengthen the given {@link ConstraintsState} with the provided {@link AbstractState}.
     *
     * <p>If the returned {@link Optional} instance is empty, strengthening of the given <code>
     * ConstraintsState</code> changed nothing.
     *
     * <p>Otherwise, the returned <code>Collection</code> contained in the <code>Optional</code> has
     * the same meaning as the <code>Collection</code> returned by {@link #strengthen(AbstractState,
     * Iterable, CFAEdge, Precision)}.
     *
     * @param stateToStrengthen the state to strengthen
     * @param strengtheningState the strengthening state
     * @param functionName the name of the current location's function
     * @param edge the current {@link CFAEdge} we treat
     * @return an empty <code>Optional</code> instance, if <code>pStateToStrengthen</code> was not
     *     changed after strengthening. A <code>Optional</code> instance containing a <code>
     *     Collection</code> with the strengthened state, otherwise
     */
    Optional<Collection<ConstraintsState>> strengthen(
        ConstraintsState stateToStrengthen,
        AbstractState strengtheningState,
        String functionName,
        CFAEdge edge)
        throws CPATransferException, InterruptedException;
  }
}
