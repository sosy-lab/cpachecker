// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IfElement;

public class TaintAnalysisTransferRelation extends SingleEdgeTransferRelation {

  private static final List<String> SOURCES =
      Lists.newArrayList(
          "__VERIFIER_nondet_int",
          "__VERIFIER_nondet_float",
          "__VERIFIER_nondet_double",
          "__VERIFIER_nondet_char");
  //  private static final List<String> SINKS = Lists.newArrayList("printf");

  private final LogManager logger;
  private final @Nullable LoopStructure loopStructure;
  private final @Nullable AstCfaRelation astCfaRelation;
  private @Nullable ImmutableSet<IfElement> ifElements = ImmutableSet.of();

  public TaintAnalysisTransferRelation(
      LogManager pLogger,
      @Nullable LoopStructure pLoopStructure,
      @Nullable AstCfaRelation pAstCfaRelation) {
    logger = pLogger;
    loopStructure = pLoopStructure;
    astCfaRelation = pAstCfaRelation;
  }

  private TaintAnalysisState generateNewState(
      TaintAnalysisState pState,
      Set<CIdExpression> pKilledVars,
      Set<CIdExpression> pGeneratedVars,
      Map<CIdExpression, CExpression> pValues) {
    logger.logf(Level.FINEST, "Killed %s, generated %s", pKilledVars, pGeneratedVars);

    Map<CIdExpression, CExpression> newTaintedVars = new HashMap<>(pState.getTaintedVariables());
    Map<CIdExpression, CExpression> newUntaintedVars =
        new HashMap<>(pState.getUntaintedVariables());

    Map<CIdExpression, CExpression> evaluatedValues = new HashMap<>();

    for (Entry<CIdExpression, CExpression> entry : pValues.entrySet()) {
      CIdExpression var = entry.getKey();
      CExpression expr = entry.getValue();

      CExpression evaluatedExpr =
          TaintAnalysisUtils.evaluateExpression(
              expr, pState.getTaintedVariables(), pState.getUntaintedVariables());

      evaluatedValues.put(var, evaluatedExpr);
    }

    for (CIdExpression killedVar : pKilledVars) {
      newTaintedVars.remove(killedVar);

      CExpression value = evaluatedValues.getOrDefault(killedVar, null);
      newUntaintedVars.put(killedVar, value);

      // If there are pointers to the new untainted variable, untaint them as well
      for (Map.Entry<CIdExpression, CExpression> entry : pState.getTaintedVariables().entrySet()) {
        CIdExpression pointer = entry.getKey();
        CExpression mappedValue = entry.getValue();

        if (mappedValue instanceof CIdExpression idExpr && idExpr.equals(killedVar)) {
          newTaintedVars.remove(pointer);
          newUntaintedVars.put(pointer, mappedValue);
        }
      }
    }

    for (CIdExpression generatedVar : pGeneratedVars) {
      newUntaintedVars.remove(generatedVar);

      CExpression value = evaluatedValues.getOrDefault(generatedVar, null);
      newTaintedVars.put(generatedVar, value);

      // If there are pointers to the new tainted variable, taint them as well
      for (Map.Entry<CIdExpression, CExpression> entry :
          pState.getUntaintedVariables().entrySet()) {
        CIdExpression pointer = entry.getKey();
        CExpression mappedValue = entry.getValue();

        if (mappedValue instanceof CIdExpression idExpr && idExpr.equals(generatedVar)) {
          newUntaintedVars.remove(pointer);
          newTaintedVars.put(pointer, mappedValue);
        }
      }
    }

    return new TaintAnalysisState(newTaintedVars, newUntaintedVars, ImmutableSet.of(pState));
  }

  /**
   * This is the main method that delegates the control-flow to the corresponding edge-type-specific
   * methods.
   */
  @Override
  public Collection<TaintAnalysisState> getAbstractSuccessorsForEdge(
      final AbstractState abstractState, final Precision abstractPrecision, final CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    TaintAnalysisState state =
        AbstractStates.extractStateByType(abstractState, TaintAnalysisState.class);
    if (Objects.isNull(state)) {
      throw new CPATransferException("state has the wrong format");
    }

    switch (cfaEdge.getEdgeType()) {
      case AssumeEdge -> {
        if (cfaEdge instanceof CAssumeEdge assumption) {
          return Collections.checkedList(
              handleAssumption(state, assumption, assumption.getExpression()),
              TaintAnalysisState.class);
        } else {
          throw new AssertionError("unknown edge");
        }
      }
      case FunctionCallEdge -> {
        if (cfaEdge instanceof CFunctionCallEdge fnkCall) {
          final CFunctionEntryNode succ = fnkCall.getSuccessor();
          final String calledFunctionName = succ.getFunctionName();

          return Collections.singleton(
              handleFunctionCallEdge(
                  state,
                  fnkCall,
                  fnkCall.getArguments(),
                  succ.getFunctionParameters(),
                  calledFunctionName));
        } else {
          throw new AssertionError("unknown edge");
        }
      }
      case FunctionReturnEdge -> {
        if (cfaEdge instanceof CFunctionReturnEdge fnkReturnEdge) {
          final String callerFunctionName = cfaEdge.getSuccessor().getFunctionName();
          final CFunctionSummaryEdge summaryEdge = fnkReturnEdge.getSummaryEdge();

          return Collections.singleton(
              handleFunctionReturnEdge(
                  state,
                  fnkReturnEdge,
                  summaryEdge,
                  summaryEdge.getExpression(),
                  callerFunctionName));
        } else {
          throw new AssertionError("unknown edge");
        }
      }
      case DeclarationEdge -> {
        if (cfaEdge instanceof CDeclarationEdge declarationEdge) {
          return Collections.singleton(handleDeclarationEdge(state, declarationEdge));
        } else {
          throw new AssertionError("unknown edge");
        }
      }
      case StatementEdge -> {
        if (cfaEdge instanceof CStatementEdge statementEdge) {

          return Collections.checkedList(
              handleStatementEdge(state, statementEdge), TaintAnalysisState.class);
        } else {
          throw new AssertionError("unknown edge");
        }
      }
      case ReturnStatementEdge -> {
        // this statement is a function return, e.g. return (a);
        // note that this is different from return edge,
        // this is a statement edge, which leads the function to the
        // last node of its CFA, where return edge is from that last node
        // to the return site of the caller function
        if (cfaEdge instanceof CReturnStatementEdge returnEdge) {
          return Collections.singleton(handleReturnStatementEdge(state, returnEdge));
        } else {
          throw new AssertionError("unknown edge");
        }
      }
      case BlankEdge -> {
        return Collections.singleton(handleBlankEdge(state, (BlankEdge) cfaEdge));
      }
      case CallToReturnEdge -> {
        if (cfaEdge instanceof CFunctionSummaryEdge cFunctionSummaryEdge) {
          return Collections.singleton(handleFunctionSummaryEdge(state, cFunctionSummaryEdge));
        } else {
          throw new AssertionError("unknown edge");
        }
      }
      default -> throw new UnrecognizedCFAEdgeException(cfaEdge);
    }
  }

  @SuppressWarnings("unused")
  private List<TaintAnalysisState> handleAssumption(
      TaintAnalysisState pState, CAssumeEdge pCfaEdge, CExpression pExpression) {

    populateIfElements(pCfaEdge);

    List<TaintAnalysisState> states = new ArrayList<>();

    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
    Map<CIdExpression, CExpression> values = new HashMap<>();

    // Evaluate the assumption expression if it's a binary expression
    if (pExpression instanceof CBinaryExpression binaryExpression) {
      CIntegerLiteralExpression evaluatedCondition =
          (CIntegerLiteralExpression)
              TaintAnalysisUtils.evaluateExpression(
                  binaryExpression, pState.getTaintedVariables(), pState.getUntaintedVariables());

      if (evaluatedCondition != null) {
        boolean conditionHolds = evaluatedCondition.getValue().equals(BigInteger.ONE);

        // flip the truth value of the evaluated condition when we are analyzing the negation
        conditionHolds = pCfaEdge.getTruthAssumption() == conditionHolds;

        if (conditionHolds) {
          // generate a new state when the condition holds
          TaintAnalysisState newState = generateNewState(pState, killedVars, generatedVars, values);
          states.add(newState);
        }
      } else {
        // generate a new state when cannot determine if the condition holds
        TaintAnalysisState newState = generateNewState(pState, killedVars, generatedVars, values);
        states.add(newState);
      }
    }

    return states;
  }

  private void populateIfElements(CAssumeEdge pCfaEdge) {
    FileLocation fileLocation = pCfaEdge.getFileLocation();

    assert ifElements != null;
    Set<IfElement> ifElementsList = new HashSet<>(ifElements);
    int startColumnNumber = fileLocation.getStartColumnInLine();
    int startLineNumber = fileLocation.getStartingLineInOrigin();

    assert astCfaRelation != null;

    for (int i = 0; i < startColumnNumber; i++) {
      Optional<IfElement> ifElement =
          astCfaRelation.getIfStructureStartingAtColumn(i, startLineNumber);

      ifElement.ifPresent(ifElementsList::add);
    }

    ifElements = ImmutableSet.copyOf(ifElementsList);
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleBlankEdge(TaintAnalysisState pState, BlankEdge pCfaEdge) {
    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
    Map<CIdExpression, CExpression> values = new HashMap<>();

    return generateNewState(pState, killedVars, generatedVars, values);
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleFunctionCallEdge(
      TaintAnalysisState pState,
      CFunctionCallEdge pCfaEdge,
      List<? extends CExpression> pArguments,
      List<? extends AParameterDeclaration> pFunctionParameters,
      String pCalledFunctionName) {

    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
    Map<CIdExpression, CExpression> values = new HashMap<>();

    if (pArguments.size() != pFunctionParameters.size()) {
      throw new AssertionError("number of arguments does not match number of parameters");
    }

    if (!pArguments.isEmpty()) {
      // Before entering the called function, we associate the function arguments with the passed
      // parameters
      for (int i = 0; i < pArguments.size(); i++) {
        CIdExpression assignmentLHS;
        CExpression assignmentRHS = pArguments.get(i);

        if (pFunctionParameters.get(i) instanceof CParameterDeclaration parmDec) {
          assignmentLHS = TaintAnalysisUtils.getCidExpressionForCParDec(parmDec);

          boolean rhsIsTainted =
              TaintAnalysisUtils.getAllVarsAsCExpr(assignmentRHS).stream()
                  .anyMatch(var -> pState.getTaintedVariables().containsKey(var));

          if (rhsIsTainted) {
            generatedVars.add(assignmentLHS);
            values.put(assignmentLHS, assignmentRHS);
          } else {
            killedVars.add(assignmentLHS);
            values.put(assignmentLHS, assignmentRHS);
          }
        }
      }
    }

    return generateNewState(pState, killedVars, generatedVars, values);
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleFunctionReturnEdge(
      TaintAnalysisState pState,
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pSummaryEdge,
      CFunctionCall pExpression,
      String pCallerFunctionName) {
    // This is only needed for intra-procedural analyses
    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
    Map<CIdExpression, CExpression> values = new HashMap<>();

    Optional<CVariableDeclaration> returnVariableOpt =
        pSummaryEdge.getFunctionEntry().getReturnVariable();

    // If the function call has a return value, track it
    if (returnVariableOpt.isPresent()) {
      CVariableDeclaration returnVariable = returnVariableOpt.orElseThrow();
      CIdExpression returnVariableAsCIdExpr =
          TaintAnalysisUtils.getCidExpressionForCVarDec(returnVariable);

      CExpression returnValue;
      if (pState.getTaintedVariables().containsKey(returnVariableAsCIdExpr)) {
        returnValue = pState.getTaintedVariables().get(returnVariableAsCIdExpr);
      } else {
        returnValue = pState.getUntaintedVariables().get(returnVariableAsCIdExpr);
      }

      boolean returnVariableIsTainted =
          pState.getTaintedVariables().containsKey(returnVariableAsCIdExpr);

      if (pExpression instanceof CFunctionCallAssignmentStatement assignStmt) {

        CExpression assignStmtLHS = assignStmt.getLeftHandSide();

        if (assignStmtLHS instanceof CIdExpression lhs) {
          if (returnVariableIsTainted) {
            generatedVars.add(lhs);
            values.put(lhs, returnValue);
          } else {
            killedVars.add(lhs);
            values.put(lhs, returnValue);
          }
        } else if (assignStmtLHS instanceof CPointerExpression pointerLHS) {
          CExpression pointer = pointerLHS.getOperand();

          if (pointer instanceof CIdExpression idPointerExpr) {
            if (returnVariableIsTainted) {
              for (Map.Entry<CIdExpression, CExpression> entry :
                  pState.getUntaintedVariables().entrySet()) {
                CIdExpression savedPointer = entry.getKey();
                CExpression mappedValue = entry.getValue();

                if (mappedValue instanceof CIdExpression mappedValueAsIdExpr) {
                  if (savedPointer.equals(idPointerExpr)) {
                    generatedVars.add(mappedValueAsIdExpr);
                    values.put(mappedValueAsIdExpr, returnValue);
                    break;
                  }
                }
              }
            } else {
              for (Map.Entry<CIdExpression, CExpression> entry :
                  pState.getTaintedVariables().entrySet()) {
                CIdExpression savedPointer = entry.getKey();
                CExpression mappedValue = entry.getValue();

                if (mappedValue instanceof CIdExpression mappedValueAsIdExpr) {
                  if (savedPointer.equals(idPointerExpr)) {
                    killedVars.add(mappedValueAsIdExpr);
                    values.put(mappedValueAsIdExpr, returnValue);
                    break;
                  }
                }
              }
            }
          }
        }
      }
    }

    return generateNewState(pState, killedVars, generatedVars, values);
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleFunctionSummaryEdge(
      TaintAnalysisState pState, CFunctionSummaryEdge pCfaEdge) {
    // This is only needed for intra-procedural analyses
    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
    Map<CIdExpression, CExpression> values = new HashMap<>();

    return generateNewState(pState, killedVars, generatedVars, values);
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleReturnStatementEdge(
      TaintAnalysisState pState, CReturnStatementEdge pCfaEdge) {
    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
    Map<CIdExpression, CExpression> values = new HashMap<>();

    Optional<CAssignment> returnStatementAsAssignment =
        pCfaEdge.getReturnStatement().asAssignment();

    if (returnStatementAsAssignment.isPresent()) {

      CIdExpression lhs =
          (CIdExpression) returnStatementAsAssignment.orElseThrow().getLeftHandSide();
      CExpression returnExpression =
          (CExpression) returnStatementAsAssignment.orElseThrow().getRightHandSide();

      boolean returnExpressionIsTainted =
          TaintAnalysisUtils.getAllVarsAsCExpr(returnExpression).stream()
              .anyMatch(var -> pState.getTaintedVariables().containsKey(var));

      if (returnExpressionIsTainted) {
        if (pCfaEdge.getSuccessor().getFunction().getName().equals("main")) {
          logger.log(Level.FINE, "Main is returning a tainted value");
          pState.setViolatesProperty();
        }
        generatedVars.add(lhs);
        values.put(lhs, returnExpression);
      } else {
        killedVars.add(lhs);
        values.put(lhs, returnExpression);
      }
    }

    return generateNewState(pState, killedVars, generatedVars, values);
  }

  private TaintAnalysisState handleDeclarationEdge(
      TaintAnalysisState pState, CDeclarationEdge pCfaEdge) {

    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
    Map<CIdExpression, CExpression> values = new HashMap<>();
    CDeclaration pDeclaration = pCfaEdge.getDeclaration();

    if (pDeclaration instanceof CVariableDeclaration) {

      CVariableDeclaration dec = (CVariableDeclaration) pCfaEdge.getDeclaration();
      CInitializer initializer = dec.getInitializer();
      CIdExpression variableLHS = TaintAnalysisUtils.getCidExpressionForCVarDec(dec);

      // If a RHS contains an expression with a tainted variable, also mark the variable on the
      // LHS as tainted. If no variable or no expression is present, kill it.
      if (Objects.nonNull(initializer)) {

        if (initializer instanceof CInitializerExpression initExpr) {

          checkIfInitializerExpressionIsTainted(
              pState, initExpr, generatedVars, variableLHS, killedVars, values);
        }

        if (initializer instanceof CInitializerList initList) {
          // initialize arrays pointing to the RegularImmutableList containing its
          // elements in the taitned/untainted variables map
          List<CInitializer> initializerList = initList.getInitializers();

          for (CInitializer init : initializerList) {
            if (init instanceof CInitializerExpression initExpr) {

              checkIfInitializerExpressionIsTainted(
                  pState, initExpr, generatedVars, variableLHS, killedVars, values);
            }
          }
        }

        if (initializer instanceof CDesignatedInitializer) {
          logger.log(Level.FINE, "initializer is a designated initializer");
        }
      } else {
        killedVars.add(variableLHS);
      }
    }

    if (pDeclaration
        instanceof CFunctionDeclaration pCFunctionDeclaration) { // extern function defs land here
      if (pCFunctionDeclaration.getName().equals("main")) {
        for (CParameterDeclaration paramDec : pCFunctionDeclaration.getParameters()) {
          CIdExpression paramIdExpr = TaintAnalysisUtils.getCidExpressionForCParDec(paramDec);
          // taint arguments passed to the main function
          generatedVars.add(paramIdExpr);
        }
      }
    }

    if (pDeclaration instanceof CComplexTypeDeclaration) { // E.g., struct Triple{int a, b, c}
      logger.log(Level.FINE, "declaration is instance of CComplexTypeDeclaration");
    }

    if (pDeclaration instanceof CTypeDefDeclaration) {
      logger.log(Level.FINE, "declaration is instance of CTypeDefDeclaration");
    }

    return generateNewState(pState, killedVars, generatedVars, values);
  }

  private static void checkIfInitializerExpressionIsTainted(
      TaintAnalysisState pState,
      CInitializerExpression initializer,
      Set<CIdExpression> generatedVars,
      CIdExpression variableLHS,
      Set<CIdExpression> killedVars,
      Map<CIdExpression, CExpression> values) {

    CExpression expr = initializer.getExpression();

    boolean taintedVarsRHS =
        TaintAnalysisUtils.getAllVarsAsCExpr(expr).stream()
            .anyMatch(var -> pState.getTaintedVariables().containsKey(var));

    if (taintedVarsRHS) {
      generatedVars.add(variableLHS);
      values.put(variableLHS, expr);
    } else {
      killedVars.add(variableLHS);
      values.put(variableLHS, expr);
    }
  }

  private ImmutableList<TaintAnalysisState> handleStatementEdge(
      TaintAnalysisState pState, CStatementEdge pCfaEdge) throws CPATransferException {

    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
    CStatement pStatement = pCfaEdge.getStatement();
    Set<CIdExpression> taintedVariables = pState.getTaintedVariables().keySet();
    List<TaintAnalysisState> newStates = new ArrayList<>();
    Map<CIdExpression, CExpression> values = new HashMap<>();

    boolean statementIsInLoop = false;
    boolean loopIsControlledByTaintedVars = false;

    Optional<Loop> OptionalLoopOfCurrentStatement = getLoopForStatement(pCfaEdge.getPredecessor());

    if (OptionalLoopOfCurrentStatement.isPresent()) {
      statementIsInLoop = true;

      Loop loopOfCurrentStatement = OptionalLoopOfCurrentStatement.orElseThrow();

      loopIsControlledByTaintedVars =
          loopIsControlledByTaintedVars(loopOfCurrentStatement, taintedVariables);
    }

    // Get the if structure for the condition edge if available
    Optional<IfElement> optionalIfStructureOfCurrentStatement =
        getIfStructureOfCurrentStatement(pStatement);

    boolean statementIsInIfElement = false;
    boolean conditionIsControledByTaintedVariables = false;

    if (optionalIfStructureOfCurrentStatement.isPresent()) {
      statementIsInIfElement = true;

      IfElement ifElement = optionalIfStructureOfCurrentStatement.orElseThrow();

      conditionIsControledByTaintedVariables =
          conditionIsControlledByTaintedVariables(ifElement, taintedVariables);
    }

    if (pStatement instanceof CExpressionStatement) {
      newStates.add(generateNewState(pState, killedVars, generatedVars, values));

    } else if (pStatement instanceof CExpressionAssignmentStatement exprAssignStmt) {
      // E.g.: z = x * x + y;
      CLeftHandSide lhs = exprAssignStmt.getLeftHandSide();
      CExpression rhs = exprAssignStmt.getRightHandSide();
      Set<CIdExpression> rhsVarsAsCExpr = TaintAnalysisUtils.getAllVarsAsCExpr(rhs);
      Set<CIdExpression> lhsVarsAsCExpr = TaintAnalysisUtils.getAllVarsAsCExpr(lhs);

      boolean taintedVarsRHS =
          rhsVarsAsCExpr.stream().anyMatch(var -> taintedVariables.contains(var));

      boolean taintedVarsLHS =
          lhsVarsAsCExpr.stream().anyMatch(var -> taintedVariables.contains(var));

      if (lhs instanceof CIdExpression variableLHS) {
        // If a LHS is a variable and the RHS contains an expression with a tainted variable, also
        // mark the variable on the LHS as tainted. If no variable is tainted, kill the variable on
        // LHS
        if (statementIsInLoop || statementIsInIfElement) {
          if (!loopIsControlledByTaintedVars && !conditionIsControledByTaintedVariables) {
            if (taintedVarsRHS) {
              if (!taintedVarsLHS) {
                generatedVars.add(variableLHS);
              }
            } else {
              killedVars.add(variableLHS);
            }
          } else {
            if (!taintedVarsLHS) {
              generatedVars.add(variableLHS);
            }
          }
        } else {
          if (taintedVarsRHS) {
            if (!taintedVarsLHS) {
              generatedVars.add(variableLHS);
            }
          } else {
            killedVars.add(variableLHS);
          }
        }

        values.put(variableLHS, rhs);

      } else if (lhs instanceof CArraySubscriptExpression arraySubscriptLHS) {
        // If the LHS is an array element and the RHS contains a tainted variable,
        // mark the array variable as tainted
        CExpression arrayExpr = arraySubscriptLHS.getArrayExpression();
        if (arrayExpr instanceof CIdExpression arrayVariable) {
          if (taintedVarsRHS) {
            generatedVars.add(arrayVariable);
          }

          values.put(arrayVariable, rhs);
          // here we don't sanitize the lhs when the rhs is clean,
          // because the array could have more tainted elements.
          // TODO: Sanitize array when all contained elements are untainted?
        }

      } else if (lhs instanceof CPointerExpression pointerLHS) {

        CExpression pointer = pointerLHS.getOperand();

        if (pointer instanceof CIdExpression idPointerExpr) {
          if (taintedVarsRHS) {
            for (Map.Entry<CIdExpression, CExpression> entry :
                pState.getUntaintedVariables().entrySet()) {
              CIdExpression savedPointer = entry.getKey();
              CExpression mappedValue = entry.getValue();

              if (mappedValue instanceof CIdExpression mappedValueAsIdExpr) {
                if (savedPointer.equals(idPointerExpr)) {
                  generatedVars.add(mappedValueAsIdExpr);
                  values.put(mappedValueAsIdExpr, rhs);
                  break;
                }
              }
            }
          } else {
            for (Map.Entry<CIdExpression, CExpression> entry :
                pState.getTaintedVariables().entrySet()) {
              CIdExpression savedPointer = entry.getKey();
              CExpression mappedValue = entry.getValue();

              if (mappedValue instanceof CIdExpression mappedValueAsIdExpr) {
                if (savedPointer.equals(idPointerExpr)) {
                  killedVars.add(mappedValueAsIdExpr);
                  values.put(mappedValueAsIdExpr, rhs);
                  break;
                }
              }
            }
          }
        }
      } else {
        logger.log(
            Level.INFO,
            "lhs is not an instance of CIdExpression or CArraySubscriptExpression or"
                + " CPointerExpression");
      }

      newStates.add(generateNewState(pState, killedVars, generatedVars, values));

    } else if (pStatement instanceof CFunctionCallStatement functionCallStmt) {
      CFunctionCallExpression callExpr = functionCallStmt.getFunctionCallExpression();
      String functionName = callExpr.getFunctionNameExpression().toString();

      if ("__VERIFIER_set_public".equals(functionName)) {
        List<CExpression> params = callExpr.getParameterExpressions();

        if (params.size() == 2) {

          CExpression exprToCheck = params.get(0);
          CExpression newPublicState = params.get(1);

          if (exprToCheck instanceof CIdExpression expr) {
            try {
              int varMustBePublic = TaintAnalysisUtils.evaluateExpressionToInteger(newPublicState);
              int varIsCurrentlyTainted = taintedVariables.contains(expr) ? 1 : 0;

              if (varIsCurrentlyTainted == 1 && varMustBePublic == 1) {
                killedVars.add(expr);
              } else if (varIsCurrentlyTainted == 0 && varMustBePublic == 0) {
                generatedVars.add(expr);
              }

            } catch (CPATransferException e) {
              throw new CPATransferException("Error processing setPublic call", e);
            }
          } else if (exprToCheck instanceof CPointerExpression) {
            logger.logf(Level.INFO, "exprToCheck is a pointer expression");
          } // TODO: else if (exprToCheck instanceof CArraySubscriptExpression arraySubscript) {}
        }
        newStates.add(generateNewState(pState, killedVars, generatedVars, values));

      } else if ("__VERIFIER_is_public".equals(functionName)) {
        List<CExpression> params = callExpr.getParameterExpressions();

        if (params.size() == 2) {

          CExpression exprToCheck = params.get(0);
          CExpression taintCheck = params.get(1);
          int expectedPublicity = TaintAnalysisUtils.evaluateExpressionToInteger(taintCheck);
          boolean expressionIsTainted;

          Set<CIdExpression> exprToCheckParams = TaintAnalysisUtils.getAllVarsAsCExpr(exprToCheck);
          expressionIsTainted = taintedVariables.stream().anyMatch(exprToCheckParams::contains);

          checkInformationFlowViolation(
              pState, pCfaEdge, expectedPublicity, expressionIsTainted, exprToCheck);

          newStates.add(generateNewState(pState, killedVars, generatedVars, values));
        }
      } else {
        // E.g., calls to any function like f(param1, ..., paramN);, where the parameters
        // can also be functions or expressions.
        List<CExpression> params =
            functionCallStmt.getFunctionCallExpression().getParameterExpressions();

        Set<CIdExpression> allVarsAsCExpr = new HashSet<>();

        // loop for extracting the parameters as CIdExpressions
        for (CExpression param : params) {
          if (param instanceof CBinaryExpression) {
            Set<CIdExpression> allVars = TaintAnalysisUtils.getAllVarsAsCExpr(param);
            allVarsAsCExpr.addAll(allVars);
          }

          if (param instanceof CIdExpression cIdExpr) {
            allVarsAsCExpr.add(cIdExpr);
          }
        }

        // if one of the parameters is tainted, mark the other parameters as tainted as well
        for (CIdExpression cIdExpression : allVarsAsCExpr) {
          if (taintedVariables.contains(cIdExpression)) {
            generatedVars.addAll(allVarsAsCExpr);
            // TODO: check what happens with the values-map here
            break;
          }
        }
        newStates.add(generateNewState(pState, killedVars, generatedVars, values));
      }

    } else if (pStatement instanceof CFunctionCallAssignmentStatement functionCallAssignStmt) {
      // e.g., x = __VERIFIER_nondet_int();
      CLeftHandSide lhs = functionCallAssignStmt.getLeftHandSide();
      CFunctionCallExpression rhs = functionCallAssignStmt.getRightHandSide();
      List<CExpression> parameters = rhs.getParameterExpressions();

      boolean rhsIsTainted = false;

      for (CExpression parameter : parameters) {
        Set<CIdExpression> allVarsAsCExpr = TaintAnalysisUtils.getAllVarsAsCExpr(parameter);

        for (CIdExpression var : allVarsAsCExpr) {
          if (taintedVariables.contains(var)) {
            rhsIsTainted = true;
            break;
          }
        }
      }

      if (lhs instanceof CIdExpression variableLHS) {
        // If a LHS is a variable and the RHS is a function call to a source or a function that
        // takes a tainted variable as parameter, mark the variable on the LHS as tainted and
        // generate the lhs variable. Otherwise, kill the variable.
        if (isSource(functionCallAssignStmt) || rhsIsTainted) {
          generatedVars.add(variableLHS);
        } else {
          killedVars.add(variableLHS);
        }
      } else if (lhs instanceof CPointerExpression pointerLHS) {

        CExpression pointer = pointerLHS.getOperand();

        if (pointer instanceof CIdExpression idPointerExpr) {
          if (isSource(functionCallAssignStmt) || rhsIsTainted) {
            for (Map.Entry<CIdExpression, CExpression> entry :
                pState.getUntaintedVariables().entrySet()) {
              CIdExpression savedPointer = entry.getKey();
              CExpression mappedValue = entry.getValue();

              if (mappedValue instanceof CIdExpression mappedValueAsIdExpr) {
                if (savedPointer.equals(idPointerExpr)) {
                  generatedVars.add(mappedValueAsIdExpr);
                  break;
                }
              }
            }
          } else {
            for (Map.Entry<CIdExpression, CExpression> entry :
                pState.getTaintedVariables().entrySet()) {
              CIdExpression savedPointer = entry.getKey();
              CExpression mappedValue = entry.getValue();

              if (mappedValue instanceof CIdExpression mappedValueAsIdExpr) {
                if (savedPointer.equals(idPointerExpr)) {
                  killedVars.add(mappedValueAsIdExpr);
                  break;
                }
              }
            }
          }
        }
      } else if (lhs instanceof CArraySubscriptExpression arraySubscriptLHS) {
        CExpression arrayExpr = arraySubscriptLHS.getArrayExpression();
        if (isSource(functionCallAssignStmt) || rhsIsTainted) {
          generatedVars.add((CIdExpression) arrayExpr);
        } else {
          killedVars.add((CIdExpression) arrayExpr);
        }
      }
      newStates.add(generateNewState(pState, killedVars, generatedVars, values));

    } else {
      newStates.add(generateNewState(pState, killedVars, generatedVars, values));
    }

    return ImmutableList.copyOf(newStates);
  }

  private boolean conditionIsControlledByTaintedVariables(
      IfElement pIfElement, Set<CIdExpression> pTaintedVariables) {

    assert astCfaRelation != null;
    ASTElement conditionElement = pIfElement.getConditionElement();

    int startingLineNumber = conditionElement.location().getStartingLineNumber();
    int startColumnInLine = conditionElement.location().getStartColumnInLine();

    // (lazy) initialize the tightest statement for starting
    astCfaRelation.getTightestStatementForStarting(startingLineNumber, startColumnInLine);

    Optional<CFANode> optionalConditionNode = Optional.empty();
    for (int i = 0; i < startColumnInLine; i++) {
      optionalConditionNode = astCfaRelation.getNodeForStatementLocation(startingLineNumber, i);
      if (optionalConditionNode.isPresent()) {
        break;
      }
    }

    if (optionalConditionNode.isPresent()) {
      CFANode conditionNode = optionalConditionNode.orElseThrow();
      CFAEdge statementInConditionEdge = conditionNode.getLeavingEdge(0);
      if (statementInConditionEdge instanceof CAssumeEdge assumeEdge) {
        CExpression expr = assumeEdge.getExpression();
        Set<CIdExpression> varsAsCidExpr = TaintAnalysisUtils.getAllVarsAsCExpr(expr);
        return varsAsCidExpr.stream().anyMatch(pTaintedVariables::contains);
      }
    }

    return false;
  }

  private Optional<IfElement> getIfStructureOfCurrentStatement(CStatement pPStatement) {

    assert ifElements != null;
    for (IfElement ifElement : ifElements) {
      if (ifElement.getCompleteElement().location().getStartingLineNumber()
              < pPStatement.getFileLocation().getStartingLineNumber()
          && ifElement.getCompleteElement().location().getEndingLineNumber()
              > pPStatement.getFileLocation().getEndingLineNumber()) {
        return Optional.of(ifElement);
      }
    }
    return Optional.empty();
  }

  private boolean loopIsControlledByTaintedVars(
      Loop loopOfCurrentStatement, Set<CIdExpression> taintedVariables) {

    boolean isNestedLoop = isNestedLoop(loopOfCurrentStatement);

    boolean loopConditionIsControlledByTaintedVars =
        loopConditionIsControlledByTaintedVars(loopOfCurrentStatement, taintedVariables);
    boolean loopIterationIsControlledByTaintedVars =
        loopIterationIndexIsControlledByTaintedVars(loopOfCurrentStatement, taintedVariables);

    if (loopConditionIsControlledByTaintedVars || loopIterationIsControlledByTaintedVars) {
      return true;
    }

    if (!isNestedLoop) {
      return false;
    }

    assert loopStructure != null;

    Loop outterLoop =
        getLoopForStatement(
                loopOfCurrentStatement.getIncomingEdges().iterator().next().getPredecessor())
            .orElseThrow();

    return loopIsControlledByTaintedVars(outterLoop, taintedVariables);
  }

  private boolean isNestedLoop(Loop loopOfCurrentStatement) {
    return getLoopForStatement(
            loopOfCurrentStatement.getIncomingEdges().iterator().next().getPredecessor())
        .isPresent();
  }

  private boolean loopIterationIndexIsControlledByTaintedVars(
      Loop pLoopOfCurrentStatement, Set<CIdExpression> pTaintedVariables) {
    boolean result = false;
    for (CFANode node : pLoopOfCurrentStatement.getLoopHeads()) {
      // the entering edges of a loop head are the declaration/definition and modification of the
      // iteration index
      for (int i = 0; i < node.getNumEnteringEdges(); i++) {
        CFAEdge edge = node.getEnteringEdge(i);
        if (edge instanceof CStatementEdge pCStatementEdge) {
          if (pCStatementEdge.getStatement()
              instanceof CExpressionAssignmentStatement pCExpressionAssignmentStatement) {

            Set<CIdExpression> lhsVarsAsCExpr =
                TaintAnalysisUtils.getAllVarsAsCExpr(
                    pCExpressionAssignmentStatement.getLeftHandSide());

            Set<CIdExpression> rhsVarsAsCExpr =
                TaintAnalysisUtils.getAllVarsAsCExpr(
                    pCExpressionAssignmentStatement.getRightHandSide());

            result =
                lhsVarsAsCExpr.stream().anyMatch(var -> pTaintedVariables.contains(var))
                    || rhsVarsAsCExpr.stream().anyMatch(var -> pTaintedVariables.contains(var));
          }
        }
      }
    }
    return result;
  }

  private boolean loopConditionIsControlledByTaintedVars(
      Loop pLoopOfCurrentStatement, Set<CIdExpression> pTaintedVariables) {
    boolean result = false;
    for (CFANode node : pLoopOfCurrentStatement.getLoopHeads()) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        // the leaving edges of the loop head are the loop conditions
        CFAEdge edge = node.getLeavingEdge(i);

        if (edge instanceof CAssumeEdge assumeEdge) {
          Set<CIdExpression> varsAsCExpr =
              TaintAnalysisUtils.getAllVarsAsCExpr(assumeEdge.getExpression());
          result = varsAsCExpr.stream().anyMatch(var -> pTaintedVariables.contains(var));
        }
      }
    }
    return result;
  }

  private void checkInformationFlowViolation(
      TaintAnalysisState pState,
      CStatementEdge pCfaEdge,
      int expectedPublicity,
      boolean isCurrentlyTainted,
      CExpression firstArg) {

    boolean varShouldBePublic = expectedPublicity == 1;

    if (varShouldBePublic == isCurrentlyTainted) {
      logger.logf(
          Level.WARNING,
          "Information flow violation at %s: Variable '%s' was expected to be %s, but is %s",
          pCfaEdge.getFileLocation(),
          firstArg.toASTString(),
          varShouldBePublic ? "public" : "tainted",
          isCurrentlyTainted ? "tainted" : "public");

      pState.setViolatesProperty();
    }
  }

  private boolean isSource(CFunctionCall pStatement) {

    return SOURCES.contains(
        pStatement.getFunctionCallExpression().getFunctionNameExpression().toString());
  }

  private Optional<Loop> getLoopForStatement(final CFANode node) {
    assert loopStructure != null;
    for (Loop loop : loopStructure.getAllLoops()) {
      if (loop.getLoopNodes().contains(node)) {
        return Optional.of(loop);
      }
    }
    return Optional.empty();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    logger.logf(
        Level.FINEST,
        "Current abstract state at location %s is  '%s'",
        AbstractStates.extractLocations(pOtherStates).first().get(),
        AbstractStates.extractStateByType(pState, TaintAnalysisState.class));
    return super.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }
}
