/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.harness;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.harness.PointerFunctionExtractor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class HarnessTransferRelation implements TransferRelation {

  private final LogManager logger;
  private final Set<AFunctionDeclaration> unimplementedPointerReturnTypeFunctions;
  private final Set<AFunctionDeclaration> unimplementedPointerTypeParameterFunctions;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final ConfigurableProgramAnalysis wrappedCpa;
  protected final TransferRelation wrappedTransfer;

  public HarnessTransferRelation(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      CFA pCFA,
      ShutdownNotifier pShutdownNotifier) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    unimplementedPointerReturnTypeFunctions =
        PointerFunctionExtractor.getExternUnimplementedPointerReturnTypeFunctions(pCFA);
    unimplementedPointerTypeParameterFunctions =
        PointerFunctionExtractor.getExternUnimplementedPointerTypeParameterFunctions(pCFA);
    cfa = pCFA;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;
    wrappedCpa = pCpa;
    wrappedTransfer = pCpa.getTransferRelation();
  }

  private boolean containsTargetState(Collection<? extends AbstractState> pWrappedSuccessors) {
    for (AbstractState wrappedSuccessor : pWrappedSuccessors) {
      assert wrappedSuccessor instanceof CompositeState : "HarnessCPA should wrap a CompositeCPA.";
      CompositeState compositeWrappedSuccessor = (CompositeState) wrappedSuccessor;
      ImmutableList<AbstractState> compositeWrappedSuccessorStates =
          compositeWrappedSuccessor.getWrappedStates();
      for (AbstractState compositeWrappedState : compositeWrappedSuccessorStates) {
        if (AbstractStates.isTargetState(compositeWrappedState)) {
          return true;
      }
    }
    }
    return false;
  }

  private PredicateAbstractState
      retrievePredicateState(Collection<? extends AbstractState> pWrappedSuccessors)
          throws InvalidConfigurationException {
    for (AbstractState wrappedSuccessor : pWrappedSuccessors) {
      assert wrappedSuccessor instanceof CompositeState : "HarnessCPA should wrap a CompositeCPA.";
      CompositeState compositeWrappedSuccessor = (CompositeState) wrappedSuccessor;
      ImmutableList<AbstractState> compositeWrappedSuccessorStates =
          compositeWrappedSuccessor.getWrappedStates();
      for (AbstractState compositeWrappedState : compositeWrappedSuccessorStates) {
        if (compositeWrappedState instanceof PredicateAbstractState) {
          return (PredicateAbstractState) compositeWrappedState;
      }
    }
    }
    throw new InvalidConfigurationException(
        "Could not find PredicateAbstractState among AbstractStates.");
  }

  @Override
  public Collection<HarnessState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pEdge)
          throws CPATransferException, InterruptedException {
    HarnessState harnessState = (HarnessState) pState;
    AbstractSingleWrapperState wrapperState = (AbstractSingleWrapperState) pState;
    Collection<? extends AbstractState> wrappedSuccessors = Collections.emptyList();
    Collection<? extends AbstractState> harnessSuccessors = Collections.emptyList();

    Set<HarnessState> result = new HashSet<>();

    try {
      wrappedSuccessors = wrappedTransfer.getAbstractSuccessorsForEdge(wrapperState.getWrappedState(), pPrecision, pEdge);
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "Could not get successors for wrapped CPA.");
    }

    if (wrappedSuccessors.isEmpty()) {
      return result;
    }

    boolean containsTargetState = containsTargetState(wrappedSuccessors);

    PredicateAbstractState predicateState = null;
    try {
      predicateState = retrievePredicateState(wrappedSuccessors);
      if (containsTargetState) {

        PredicateCPA predicateCPA =
            CPAs.retrieveCPAOrFail(wrappedCpa, PredicateCPA.class, HarnessTransferRelation.class);
        Solver solver = predicateCPA.getSolver();
        try (ProverEnvironment proverEnvironment =
            solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);) {
          PathFormulaManager pathFormulaManager = predicateCPA.getPathFormulaManager();
          FormulaManagerView formulaManagerView = solver.getFormulaManager();
          FunctionFormulaManagerView functionFormulaManager =
              formulaManagerView.getFunctionFormulaManager();

          CtoFormulaTypeHandler typeHandler =
              new CtoFormulaTypeHandler(logger, cfa.getMachineModel());

          Map<AFunctionDeclaration, List<Formula>> newNamesForFunctions = new HashMap<>();

          FunctionIndexer functionIndexer =
              new FunctionIndexer(solver, typeHandler, pathFormulaManager);
          ExportedLocationMapper exportedLocationMapper =
              new ExportedLocationMapper(formulaManagerView);
          FunctionToIndicesResolver functionToIndicesResolver =
              new FunctionToIndicesResolver(proverEnvironment, functionFormulaManager);

          FunctionDeclaration<?> indexFunctionDeclaration =
              functionFormulaManager.declareUF(
                  "__harness__location_index",
                  FormulaType.IntegerType,
                  typeHandler.getPointerType());

          BooleanFormula renamedExternFunctionCallsFormula =
              functionIndexer.renameExternFunctionCalls(
                  predicateState,
                  newNamesForFunctions,
                  unimplementedPointerReturnTypeFunctions);
          proverEnvironment.push(renamedExternFunctionCallsFormula);

          BooleanFormula exportedLocationsToIndexFormula =
              exportedLocationMapper.getExportedLocationsToIndexFormula(
                  harnessState.getExternallyKnownPointers(),
                  predicateCPA.getPathFormulaManager(),
                  indexFunctionDeclaration);

          proverEnvironment.push(exportedLocationsToIndexFormula);

          Map<AFunctionDeclaration, List<Integer>> functionsToIndicesMap =
              functionToIndicesResolver.resolveFunctions(
                  unimplementedPointerReturnTypeFunctions,
                  newNamesForFunctions,
                  indexFunctionDeclaration);

          harnessState.setIndices(functionsToIndicesMap);
          harnessSuccessors = Collections.singleton(harnessState);

        } catch (SolverException e) {
          logger.log(Level.WARNING, "Solver could not generate new ProverEnvironment.");
          logger.logDebugException(e);
          return result;
        }

      } else {
        FormulaFromExpressionBuilder formulaFromExpressionBuilder =
            new FormulaFromExpressionBuilder(
                cfa,
                logger,
                wrappedCpa,
                predicateState,
                config,
                shutdownNotifier);

        harnessSuccessors =
            getAbstractSuccessorsForEdgeWithoutWrapping(
                pState,
                pEdge,
                formulaFromExpressionBuilder);
      }
    } catch (InvalidConfigurationException e1) {
      logger.log(Level.SEVERE, "Could not extract PredicateCPA from CPAs wrapped by HarnessCPA.");
    }

    for (AbstractState abstractSuccessor : harnessSuccessors) {
      HarnessState harnessSuccessor = (HarnessState) abstractSuccessor;
      for (AbstractState wrappingSuccessor : wrappedSuccessors) {
        HarnessState wrappedSuccessor = harnessSuccessor.setWrappedState(wrappingSuccessor);
        result.add(wrappedSuccessor);
      }
    }
    return result;
  }

  public Collection<? extends AbstractState> getAbstractSuccessorsForEdgeWithoutWrapping(
      AbstractState pState,
      CFAEdge pEdge,
      FormulaFromExpressionBuilder pFormulaFromExpressionBuilder)
      throws CPATransferException, InvalidConfigurationException {

    HarnessState state = (HarnessState) pState;
    HarnessState result = (HarnessState) pState;

    if (!(pEdge.getFileLocation().getNiceFileName() == "")) {
      return Collections.singleton(result);
    }
    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
      case BlankEdge:
      case CallToReturnEdge:
      case DeclarationEdge:
      case FunctionCallEdge:
      case FunctionReturnEdge:
      case ReturnStatementEdge:
        break;
      case StatementEdge:
        result = handleStatementEdge(state, (CStatementEdge) pEdge, pFormulaFromExpressionBuilder);
        break;
      default: {
        throw new UnrecognizedCodeException("Unrecognized CFA edge.", pEdge);
      }
    }
    return Collections.singleton(result);
  }

  private HarnessState
      handleStatementEdge(
          HarnessState pState,
          CStatementEdge pEdge,
          FormulaFromExpressionBuilder pFormulaFromExpressionBuilder)
          throws InvalidConfigurationException {
    CStatement statement = pEdge.getStatement();
    if (statement instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement functionCallAssignmentStatement =
          (AFunctionCallAssignmentStatement) statement;
      ALeftHandSide leftHandSide = functionCallAssignmentStatement.getLeftHandSide();
      CompositeCPA compositeCPA = (CompositeCPA) wrappedCpa;
      for (ConfigurableProgramAnalysis innerCPA : compositeCPA.getWrappedCPAs()) {
        if (innerCPA instanceof ControlAutomatonCPA) {
          // the one i want can be filtered from here
        }
      }
    }
    if (statement instanceof CFunctionCall) {
      CFunctionCall functionCall = (CFunctionCall) statement;
      CFunctionCallExpression functionCallExpression =
          functionCall.getFunctionCallExpression();
      if (isExternFunction(functionCallExpression)) {
        List<CExpression> functionParameters = functionCallExpression.getParameterExpressions();
        List<CExpression> functionParametersOfPointerType =
            functionParameters
                .stream()
                .filter(
                    cExpression ->
                        (cExpression.getExpressionType() instanceof CPointerType)
                            || cExpression.getExpressionType() instanceof CArrayType)
                .map(
                    cExpression -> {
                      if (cExpression.getExpressionType() instanceof CArrayType
                          && cExpression instanceof CIdExpression) {
                        CIdExpression cIdExpression = (CIdExpression) cExpression;
                        CPointerType asPointerType = CPointerType.POINTER_TO_VOID;
                        CExpression newExpression =
                            new CIdExpression(
                                cIdExpression.getFileLocation(),
                                asPointerType,
                                cIdExpression.toASTString(),
                                cIdExpression.getDeclaration());
                        return newExpression;

                      } else {
                        return cExpression;
                      }
                    })
                .collect(Collectors.toList());

        List<Formula> formulas =
            pFormulaFromExpressionBuilder
                .buildFormulasFromExpressions(pEdge, functionParametersOfPointerType);
        HarnessState newState =
            pState.addExternallyKnownLocations(formulas);
        return newState;
      }
    }
    return pState;
  }

  private boolean isExternFunction(CFunctionCallExpression pFunctionCallExpression) {
    AFunctionDeclaration functionDeclaration = pFunctionCallExpression.getDeclaration();
    return unimplementedPointerReturnTypeFunctions.contains(functionDeclaration)
        || unimplementedPointerTypeParameterFunctions.contains(functionDeclaration);
  }

  @Override
  public Collection<HarnessState>
      getAbstractSuccessors(AbstractState pState, Precision pPrecision)
          throws CPATransferException, InterruptedException {
    Collection<HarnessState> result = new HashSet<>();
    HarnessState wrapperState = (HarnessState) pState;
    Collection<? extends AbstractState> abstractSuccessors =
        wrappedTransfer.getAbstractSuccessors(wrapperState.getWrappedState(), pPrecision);

    HarnessState harnessState = (HarnessState) pState;

    AbstractStateWithLocations locState =
        extractStateByType(harnessState, AbstractStateWithLocations.class);
    Set<HarnessState> harnessStateSuccessors = new HashSet<>();
    if (locState != null) {
      Iterable<CFAEdge> outGoingEdges = locState.getOutgoingEdges();
      for (CFAEdge edge : outGoingEdges) {
        Collection<HarnessState> edgeSuccessors =
            getAbstractSuccessorsForEdge(harnessState, pPrecision, edge);
        if (!edgeSuccessors.isEmpty()) {
          harnessStateSuccessors.add(edgeSuccessors.iterator().next());
        }
      }
    }

    if (!harnessStateSuccessors.isEmpty()) {
      harnessState = harnessStateSuccessors.iterator().next();
    }

    for (AbstractState abstractSuccessor : abstractSuccessors) {
      HarnessState newWrapperState = harnessState.setWrappedState(abstractSuccessor);
      result.add(newWrapperState);
    }

    return result;
  }

}
