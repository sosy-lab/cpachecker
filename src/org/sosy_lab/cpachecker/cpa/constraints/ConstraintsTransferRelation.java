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
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
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
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;

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

  @Option(secure=true, description="The way numbers are represented in boolean formulas.")
  private FormulaNumberHandlingType formulaNumberHandling = FormulaNumberHandlingType.INTEGER;


  private final LogManagerWithoutDuplicates logger;

  private MachineModel machineModel;

  private Solver solver;
  private FormulaManagerView formulaManager;
  private CtoFormulaConverter converter;

  public ConstraintsTransferRelation(MachineModel pMachineModel, LogManager pLogger,
      Configuration pConfig, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = new LogManagerWithoutDuplicates(pLogger);
    machineModel = pMachineModel;
    initializeSolver(pLogger, pConfig, pShutdownNotifier);
    initializeCToFormulaConverter(pLogger, pConfig, pShutdownNotifier);
  }

  private void initializeSolver(LogManager pLogger, Configuration pConfig, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    formulaManager = solver.getFormulaManager();
  }

  // Can only be called after machineModel and formulaManager are set
  private void initializeCToFormulaConverter(LogManager pLogger, Configuration pConfig,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {

    FormulaEncodingOptions options = new FormulaEncodingOptions(pConfig);
    CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(pLogger, options, machineModel, formulaManager);

    converter = new CtoFormulaConverter(options,
                                        formulaManager,
                                        machineModel,
                                        Optional.<VariableClassification>absent(),
                                        pLogger,
                                        pShutdownNotifier,
                                        typeHandler,
                                        AnalysisDirection.FORWARD);
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
    return state;
  }

  private ConstraintsState getNewState(ConstraintsState pOldState, ValueAnalysisState pValueState,
      AExpression pExpression, ConstraintFactory pFactory, boolean pTruthAssumption, String pFunctionName,
      FileLocation pFileLocation)
        throws SolverException, InterruptedException {

    ConstraintsState newState = pOldState.copyOf();

    newState.initialize(solver, formulaManager, getFormulaCreator(pValueState, pFunctionName));

    try {
      Optional<Constraint> newConstraint = createConstraint(pExpression, pFactory, pTruthAssumption);

      if (newConstraint.isPresent()) {
        newState.add(newConstraint.get());

        if (newState.isUnsat()) {
          return null;

        } else {
          newState = simplify(newState, pValueState);

          return newState;
        }

      } else {

        return newState;
      }

    } catch (UnrecognizedCodeException e) {
      logger.logUserException(Level.WARNING, e, pFileLocation.toString());
    }

    return newState;
  }

  private FormulaCreator getFormulaCreator(ValueAnalysisState pValueState, String pFunctionName) {
    switch (formulaNumberHandling) {
      case INTEGER:
        return new IntegerFormulaCreator(formulaManager, pValueState, machineModel);
      case BITVECTOR:
        return new BitvectorFormulaCreator(formulaManager, getConverter(), pValueState, pFunctionName, machineModel);
      default:
        throw new AssertionError("Unhandled handling type " + formulaNumberHandling);
    }
  }

  private CtoFormulaConverter getConverter() {
    return converter;
  }

  private Optional<Constraint> createConstraint(AExpression pExpression, ConstraintFactory pFactory,
      boolean pTruthAssumption) throws UnrecognizedCodeException {

    if (pExpression instanceof JBinaryExpression) {
      return createConstraint((JBinaryExpression) pExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof JUnaryExpression) {
      return createConstraint((JUnaryExpression) pExpression, pFactory, pTruthAssumption);

    } else if (pExpression instanceof CBinaryExpression) {
      return createConstraint((CBinaryExpression)pExpression, pFactory, pTruthAssumption);

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

  private ConstraintsState simplify(ConstraintsState pState, ValueAnalysisState pValueState) {
    StateSimplifier simplifier = new StateSimplifier(machineModel, logger);

    return simplifier.simplify(pState, pValueState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pStateToStrengthen,
      List<AbstractState> pStrengtheningStates, CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException {
    assert pStateToStrengthen instanceof ConstraintsState;

    List<ConstraintsState> newStates = new ArrayList<>();
    boolean nothingChanged = true;

    if (pCfaEdge.getEdgeType() != CFAEdgeType.AssumeEdge) {
      return null;
    }

    for (AbstractState currState : pStrengtheningStates) {
      if (currState instanceof ValueAnalysisState) {
        Optional<Collection<ConstraintsState>> newValueStrengthenedStates =
            strengthen((ConstraintsState) pStateToStrengthen, (ValueAnalysisState) currState, (AssumeEdge) pCfaEdge);

        if (newValueStrengthenedStates.isPresent()) {
          nothingChanged = false;
          newStates.addAll(newValueStrengthenedStates.get());
        }
      }
    }

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
      ValueAnalysisState pStrengtheningState, AssumeEdge pCfaEdge) {

    Collection<ConstraintsState> newStates = new ArrayList<>();
    final String functionName = pCfaEdge.getPredecessor().getFunctionName();
    final ConstraintFactory factory =
        ConstraintFactory.getInstance(functionName, Optional.of(pStrengtheningState), machineModel, logger);
    final FileLocation fileLocation = pCfaEdge.getFileLocation();

    ConstraintsState newState = null;
    try {
      newState = getNewState(pStateToStrengthen,
                             pStrengtheningState,
                             pCfaEdge.getExpression(),
                             factory,
                             pCfaEdge.getTruthAssumption(),
                             functionName,
                             fileLocation);

    } catch (SolverException | InterruptedException e) {
      logger.logUserException(Level.WARNING, e, fileLocation.toString());
    }

    // newState == null represents the bottom element, so we return an empty collection
    if (newState != null) {
      newStates.add(newState);

      if (newState.size() == pStateToStrengthen.size()) {
        return Optional.absent();
      }
    }
    return Optional.of(newStates);
  }
}
