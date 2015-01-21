/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintFactory;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicIdentifier;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Optional;

/**
 * Transfer relation for Symbolic Execution Analysis.
 */
@Options(prefix="cpa.constraints")
public class ConstraintsTransferRelation
    extends ForwardingTransferRelation<ConstraintsState, ConstraintsState, SingletonPrecision> {

  /**
   * Enum for possible types of number handling in formulas for SAT checks.
   */
  public static enum FormulaNumberHandlingType {
    INTEGER, BITVECTOR
  }

  @Option(secure=true, description="")
  private FormulaNumberHandlingType formulaNumberHandling = FormulaNumberHandlingType.INTEGER;


  private final LogManagerWithoutDuplicates logger;

  private boolean missingInformation = false;
  private AExpression missingInformationExpression = null;
  private boolean missingInformationTruth;

  private MachineModel machineModel;

  private Solver solver;
  private FormulaManagerView formulaManager;

  public ConstraintsTransferRelation(MachineModel pMachineModel, LogManager pLogger,
      Configuration pConfig, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = new LogManagerWithoutDuplicates(pLogger);
    machineModel = pMachineModel;
    initializeSolver(pLogger, pConfig, pShutdownNotifier);
  }

  private void initializeSolver(LogManager pLogger, Configuration pConfig, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    formulaManager = solver.getFormulaManager();
  }

  @Override
  protected ConstraintsState handleFunctionCallEdge(FunctionCallEdge pCfaEdge, List<? extends AExpression> pArguments,
      List<? extends AParameterDeclaration> pParameters, String pCalledFunctionName) {
    return state;
  }

  @Override
  protected ConstraintsState handleFunctionReturnEdge(FunctionReturnEdge pCfaEdge,
      FunctionSummaryEdge pFunctionCallEdge, AFunctionCall pSummaryExpression, String pCallerFunctionName) {
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
  protected ConstraintsState handleDeclarationEdge(ADeclarationEdge pCfaEdge, ADeclaration pDeclaration)
      throws CPATransferException {
    return state;
  }

  @Override
  protected ConstraintsState handleAssumption(AssumeEdge pCfaEdge, AExpression pExpression, boolean pTruthAssumption) {
    missingInformationExpression = pExpression;
    missingInformationTruth = pTruthAssumption;
    missingInformation = true;

    return state;
  }

  private ConstraintsState getNewState(ConstraintsState pOldState, AExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption, FileLocation pFileLocation) throws SolverException, InterruptedException {

    ConstraintsState newState = pOldState.copyOf();

    if (!newState.isInitialized()) {
      newState.initialize(solver, formulaManager, getFormulaCreator());
    }

    try {
      Optional<Constraint> newConstraint = createConstraint(pExpression, pFactory, pTruthAssumption);

      assert !pFactory.hasMissingInformation();
      assert newConstraint.isPresent();

      newState.add(newConstraint.get());

      if (newState.isUnsat()) {
        return null;

      } else {
        newState = simplify(newState);

        return newState;
      }

    } catch (UnrecognizedCodeException e) {
      logger.logUserException(Level.WARNING, e, pFileLocation.toString());
    }

    return newState;
  }

  private FormulaCreator<? extends Formula> getFormulaCreator() {
    switch (formulaNumberHandling) {
      case INTEGER:
        return new IntegerFormulaCreator(formulaManager);
      case BITVECTOR:
        return new BitvectorFormulaCreator(formulaManager, machineModel);
      default:
        throw new AssertionError("Unhandled handling type " + formulaNumberHandling);
    }
  }

  private Optional<Constraint> createConstraint(AExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption) throws UnrecognizedCodeException {

    if (pExpression instanceof JBinaryExpression) {
      return createConstraint((JBinaryExpression) pExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof JUnaryExpression) {
      return createConstraint((JUnaryExpression) pExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof CBinaryExpression) {
      return createConstraint((CBinaryExpression)pExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof CUnaryExpression) {
      return createConstraint((CUnaryExpression) pExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof AIdExpression) {
      return createConstraint((AIdExpression) pExpression, pFactory, pTruthAssumption);
    } else {
      throw new AssertionError("Unhandled expression type " + pExpression.getClass());
    }
  }

  private Optional<Constraint> createConstraint(JBinaryExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption) throws UnrecognizedCodeException {

    Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(pExpression);
    } else {
      constraint = pFactory.createNegativeConstraint(pExpression);
    }

    return Optional.fromNullable(constraint);
  }

  private Optional<Constraint> createConstraint(JUnaryExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption) throws UnrecognizedCodeException {
    Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(pExpression);
    } else {
      constraint = pFactory.createNegativeConstraint(pExpression);
    }

    return Optional.fromNullable(constraint);
  }

  private Optional<Constraint> createConstraint(CBinaryExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption) throws UnrecognizedCodeException {

    Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(pExpression);
    } else {
      constraint = pFactory.createNegativeConstraint(pExpression);
    }

    return Optional.fromNullable(constraint);
  }

  private Optional<Constraint> createConstraint(CUnaryExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption) throws UnrecognizedCodeException {
    Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(pExpression);
    } else {
      constraint = pFactory.createNegativeConstraint(pExpression);
    }

    return Optional.fromNullable(constraint);
  }

  private Optional<Constraint> createConstraint(AIdExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption) throws UnrecognizedCodeException {
   Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(pExpression);
    } else {
      constraint = pFactory.createNegativeConstraint(pExpression);
    }

    return Optional.fromNullable(constraint);
  }

  private ConstraintsState simplify(ConstraintsState pState) {
    ConstraintsState newState;

    newState = replaceSymbolicIdentifiersWithConcreteValues(pState);
    newState = removeTrivialConstraints(newState);

    return newState;
  }

  private ConstraintsState replaceSymbolicIdentifiersWithConcreteValues(ConstraintsState pState) {
    final IdentifierAssignment definiteAssignment = pState.getDefiniteAssignment();

    if (definiteAssignment.isEmpty()) {
      return pState;
    }

    ConstraintsState newState = pState.copyOf();
    newState.clear();

    for (Map.Entry<SymbolicIdentifier, Value> entry : definiteAssignment.entrySet()) {
      final SymbolicIdentifier id = entry.getKey();
      final Value newValue = entry.getValue();

      ConstraintIdentifierReplacer replacer = new ConstraintIdentifierReplacer(id, newValue, machineModel, logger);
      for (Constraint c : pState) {
        newState.add(c.accept(replacer));
      }
    }

    return newState;
  }

  private ConstraintsState removeTrivialConstraints(ConstraintsState pState) {
    ConstraintsState newState = pState.copyOf();

    Iterator<Constraint> it = newState.iterator();

    while (it.hasNext()) {
      Constraint currConstraint = it.next();

      if (currConstraint.isTrivial()) {
        it.remove();
      }
    }

    return newState;
  }


  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pStateToStrengthen,
      List<AbstractState> pStrengtheningStates, CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    assert pStateToStrengthen instanceof ConstraintsState;

    List<ConstraintsState> newStates = new ArrayList<>();
    boolean nothingChanged = true;

    if (!missingInformation) {
      assert !missingInformationTruth && missingInformationExpression == null;
      return null;
    }

    for (AbstractState currState : pStrengtheningStates) {
      if (currState instanceof ValueAnalysisState) {
        Optional<Collection<ConstraintsState>> newValueStrengthenedStates =
            strengthen((ConstraintsState)pStateToStrengthen, (ValueAnalysisState)currState, pCfaEdge);

        if (newValueStrengthenedStates.isPresent()) {
          nothingChanged = false;
          newStates.addAll(newValueStrengthenedStates.get());
        }
      }
    }

    resetMissingInformationStatus();

    if (nothingChanged) {
      return null;
    } else {
      return newStates;
    }
  }

  /**
   * Strengthen the given {@link ConstraintsState} with the provided {@link ValueAnalysisState}.
   *
   * If the returned {@link Optional} instance is empty, strengthening of the given <code>ConstraintsState</code>
   * changed nothing.
   *
   * Otherwise, the returned <code>Collection</code> contained in the <code>Optional</code> has the same
   * meaning as the <code>Collection</code> returned by {@link #strengthen(AbstractState, List, CFAEdge, Precision)}.
   *
   * @param pStateToStrengthen the state to strengthen
   * @param pStrengtheningState the state used for strengthening the given <code>ConstraintsState</code>
   * @param pCfaEdge the current {@link CFAEdge} we are at
   *
   * @return an empty <code>Optional</code> instance, if <code>pStateToStrengthen</code> was not changed after
   *    strengthening. A <code>Optional</code> instance containing a <code>Collection</code> with the strengthened state,
   *    otherwise
   */
  private Optional<Collection<ConstraintsState>> strengthen(ConstraintsState pStateToStrengthen,
      ValueAnalysisState pStrengtheningState, CFAEdge pCfaEdge) {

    Collection<ConstraintsState> newStates = new ArrayList<>();
    final String functionName = pCfaEdge.getPredecessor().getFunctionName();
    final ConstraintFactory factory = ConstraintFactory.getInstance(functionName, Optional.of(pStrengtheningState));
    final FileLocation fileLocation = pCfaEdge.getFileLocation();

    ConstraintsState newState = null;
    try {
      newState =
          getNewState(pStateToStrengthen, missingInformationExpression, factory, missingInformationTruth, fileLocation);

    } catch (SolverException | InterruptedException e) {
      logger.logUserException(Level.WARNING, e, fileLocation.toString());
    }

    // newState == null represents the bottom element, so we return an empty collection
    if (newState != null) {
      newStates.add(newState);

      if (newState.size() == pStateToStrengthen.size()) {
        resetMissingInformationStatus();
        return Optional.absent();
      }
    }
    return Optional.of(newStates);
  }


  private void resetMissingInformationStatus() {
    missingInformation = false;
    missingInformationExpression = null;
    missingInformationTruth = false;
  }
}
