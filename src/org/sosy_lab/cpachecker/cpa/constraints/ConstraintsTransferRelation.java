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
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintFactory;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Optional;

/**
 * Transfer relation for Symbolic Execution Analysis.
 */
@Options(prefix="cpa.constraints")
public class ConstraintsTransferRelation
    extends ForwardingTransferRelation<ConstraintsState, ConstraintsState, SingletonPrecision> {

  private final LogManager logger;

  private boolean missingInformation = false;
  private AExpression missingInformationExpression = null;
  private boolean missingInformationTruth;

  private Solver solver;
  private FormulaManagerView formulaManager;

  public ConstraintsTransferRelation(LogManager pLogger, Configuration pConfig, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    logger = pLogger;

    initializeSolver(pLogger, pConfig, pShutdownNotifier);
  }

  private void initializeSolver(LogManager pLogger, Configuration pConfig, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    final FormulaManagerFactory factory = new FormulaManagerFactory(pConfig, pLogger, pShutdownNotifier);

    final FormulaManager baseManager = factory.getFormulaManager();
    formulaManager = new FormulaManagerView(baseManager, pConfig, pLogger);

    solver = new Solver(formulaManager, factory);
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

    final ConstraintFactory factory = ConstraintFactory.getInstance(functionName);
    final FileLocation fileLocation = pCfaEdge.getFileLocation();

    ConstraintsState newState = null;
    try {
      newState = getNewState(state, pExpression, factory, pTruthAssumption, fileLocation);

    } catch (SolverException | InterruptedException e) {
      logger.logUserException(Level.WARNING, e, fileLocation.toString());
    }

    return newState;
  }

  private ConstraintsState getNewState(ConstraintsState pOldState, AExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption, FileLocation pFileLocation) throws SolverException, InterruptedException {

    ConstraintsState newState = ConstraintsState.copyOf(pOldState);

    try {
      Optional<Constraint> newConstraint = createConstraint(pExpression, pFactory, pTruthAssumption);

      if (pFactory.hasMissingInformation()) {
        assert !missingInformation && missingInformationExpression == null && !missingInformationTruth;
        missingInformation = true;
        missingInformationExpression = pExpression;
        missingInformationTruth = pTruthAssumption;

        assert !newConstraint.isPresent();
      }

      if (newConstraint.isPresent()) {
        newState.addConstraint(newConstraint.get());
      }

    } catch (UnrecognizedCodeException e) {
      logger.logUserException(Level.WARNING, e, pFileLocation.toString());
    }

    if (!isSolvable(newState)) {
      return null;

    } else {
      return newState;
    }
  }

  private boolean isSolvable(ConstraintsState pState) throws SolverException, InterruptedException {
    BooleanFormula constraintConjunction = getConjunction(pState);

    return !solver.isUnsat(constraintConjunction);
  }

  private BooleanFormula getConjunction(ConstraintsState pState) {
    final Set<Constraint> constraints = pState.getConstraints();
    final ConstraintVisitor<Formula> formulaCreator = new BooleanFormulaCreator(formulaManager);
    Formula completeFormula = null;
    Formula currFormula;

    for (Constraint currConstraint : constraints) {

      currFormula = currConstraint.accept(formulaCreator);

      if (completeFormula == null) {
        completeFormula = currFormula;

      } else {
        completeFormula = formulaManager.makeAnd(completeFormula, currFormula);
      }
    }

    if (completeFormula == null) {
      final BooleanFormulaManager manager = formulaManager.getBooleanFormulaManager();

      completeFormula = manager.makeBoolean(true);
    }

    return (BooleanFormula) completeFormula;
  }

  private Optional<Constraint> createConstraint(AExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption) throws UnrecognizedCodeException {

    if (pExpression instanceof JBinaryExpression) {
      return createConstraint((JBinaryExpression) pExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof CBinaryExpression) {
      return createConstraint((CBinaryExpression) pExpression, pFactory, pTruthAssumption);

    } else {
      throw new AssertionError("Unhandled expression type " + pExpression.getClass());
    }
  }

  private Optional<Constraint> createConstraint(JBinaryExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption) throws UnrecognizedCodeException {

    JExpression leftOperand = pExpression.getOperand1();
    JExpression rightOperand = pExpression.getOperand2();

    Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(leftOperand, pExpression.getOperator(), rightOperand);
    } else {
      constraint = pFactory.createNegativeConstraint(leftOperand, pExpression.getOperator(), rightOperand);
    }

    return Optional.fromNullable(constraint);
  }

  private Optional<Constraint> createConstraint(CBinaryExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption) throws UnrecognizedCodeException {

    CExpression leftOperand = pExpression.getOperand1();
    CExpression rightOperand = pExpression.getOperand2();

    Constraint constraint;

    if (pTruthAssumption) {
      constraint = pFactory.createPositiveConstraint(leftOperand, pExpression.getOperator(), rightOperand);
    } else {
      constraint = pFactory.createNegativeConstraint(leftOperand, pExpression.getOperator(), rightOperand);
    }

    return Optional.fromNullable(constraint);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pStateToStrengthen,
      List<AbstractState> pStrengtheningStates, CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    assert pStateToStrengthen instanceof ConstraintsState;

    Collection<ConstraintsState> newStates = new ArrayList<>();

    if (!missingInformation) {
      return null;
    }

    for (AbstractState currState : pStrengtheningStates) {
      if (currState instanceof ValueAnalysisState) {
        Collection<ConstraintsState> newValueStrengthenedStates =
            strengthen((ConstraintsState) pStateToStrengthen, (ValueAnalysisState) currState, pCfaEdge);

        newStates.addAll(newValueStrengthenedStates);
      }
    }

    return newStates;
  }

  private Collection<ConstraintsState> strengthen(ConstraintsState pStateToStrengthen,
      ValueAnalysisState pStrengtheningState, CFAEdge pCfaEdge) {

    Collection<ConstraintsState> newStates = new ArrayList<>();
    final String functionName = pCfaEdge.getPredecessor().getFunctionName();
    final ConstraintFactory factory = ConstraintFactory.getInstance(functionName, pStrengtheningState);
    final FileLocation fileLocation = pCfaEdge.getFileLocation();

    ConstraintsState newState = null;
    try {
      newState =
          getNewState(pStateToStrengthen, missingInformationExpression, factory, missingInformationTruth, fileLocation);

    } catch (SolverException | InterruptedException e) {
      logger.logUserException(Level.WARNING, e, fileLocation.toString());
    }

    if (newState != null) {
      newStates.add(newState);
    }
    resetMissingInformationStatus();

    return newStates;
  }


  private void resetMissingInformationStatus() {
    missingInformation = false;
    missingInformationExpression = null;
    missingInformationTruth = false;
  }
}
