// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
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

public class TaintAnalysisTransferRelation extends SingleEdgeTransferRelation {

  private static final List<String> SOURCES =
      Lists.newArrayList(
          "__VERIFIER_nondet_int",
          "__VERIFIER_nondet_float",
          "__VERIFIER_nondet_double",
          "__VERIFIER_nondet_char");
  //  private static final List<String> SINKS = Lists.newArrayList("printf");

  private final LogManager logger;

  public TaintAnalysisTransferRelation(LogManager pLogger) {

    logger = pLogger;
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

    Map<CIdExpression, CLiteralExpression> evaluatedValues = new HashMap<>();

    for (Entry<CIdExpression, CExpression> entry : pValues.entrySet()) {
      CIdExpression var = entry.getKey();
      CExpression expr = entry.getValue();

      CLiteralExpression evaluatedExpr =
          TaintAnalysisUtils.evaluateExpression(
              expr, pState.getTaintedVariables(), pState.getUntaintedVariables());

      evaluatedValues.put(var, evaluatedExpr);
    }

    for (CIdExpression killedVar : pKilledVars) {
      newTaintedVars.remove(killedVar);

      CExpression value = evaluatedValues.getOrDefault(killedVar, null);
      newUntaintedVars.put(killedVar, value);
    }

    for (CIdExpression generatedVar : pGeneratedVars) {
      newUntaintedVars.remove(generatedVar);

      CExpression value = evaluatedValues.getOrDefault(generatedVar, null);
      newTaintedVars.put(generatedVar, value);
    }

    return new TaintAnalysisState(newTaintedVars, newUntaintedVars);
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

          return Collections.singleton(handleStatementEdge(state, statementEdge));
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
        if (cfaEdge instanceof CFunctionSummaryEdge) {
          return Collections.singleton(
              handleFunctionSummaryEdge(state, (CFunctionSummaryEdge) cfaEdge));
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
        boolean conditionHolds =
            evaluatedCondition.getValue().equals(BigInteger.ONE);

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
    } // TODO: more cases for instances of the pExpression might be needed (?)

    return states;
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
      for (int i = 0; i < pArguments.size(); i++) {
        CIdExpression assignmentLHS;
        CExpression assignmentRHS = pArguments.get(i);

        if (pFunctionParameters.get(i) instanceof CParameterDeclaration parmDec) {
          assignmentLHS = TaintAnalysisUtils.getCidExpressionForCParDec(parmDec);

          CExpressionAssignmentStatement exprAsmtStmt =
              new CExpressionAssignmentStatement(
                  pCfaEdge.getFileLocation(), assignmentLHS, assignmentRHS);

          boolean rhsIsTainted =
              TaintAnalysisUtils.getAllVarsAsCExpr(assignmentRHS).stream()
                  .anyMatch(var -> pState.getTaintedVariables().containsKey(var));

          CLiteralExpression evaluatedValue =
              TaintAnalysisUtils.evaluateExpression(
                  assignmentRHS, pState.getTaintedVariables(), pState.getUntaintedVariables());

          if (rhsIsTainted) {
            generatedVars.add(assignmentLHS);
            values.put(assignmentLHS, evaluatedValue);
          } else {
            killedVars.add(assignmentLHS);
            values.put(assignmentLHS, evaluatedValue);
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
      CVariableDeclaration returnVariable = returnVariableOpt.get();
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
        if (assignStmt.getLeftHandSide() instanceof CIdExpression lhs) {
          if (returnVariableIsTainted) {
            generatedVars.add(lhs);
            values.put(lhs, returnValue);
          } else {
            killedVars.add(lhs);
            values.put(lhs, returnValue);
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
    CIdExpression lhs = (CIdExpression) returnStatementAsAssignment.get().getLeftHandSide();
    CExpression returnExpression =
        (CExpression) returnStatementAsAssignment.get().getRightHandSide();

    boolean returnExpressionIsTainted =
        TaintAnalysisUtils.getAllVarsAsCExpr(returnExpression).stream()
            .anyMatch(var -> pState.getTaintedVariables().containsKey(var));

    if (returnExpressionIsTainted) {
      generatedVars.add(lhs);
      values.put(lhs, returnExpression);
    } else {
      killedVars.add(lhs);
      values.put(lhs, returnExpression);
    }

    CFANode predecessor = pCfaEdge.getPredecessor();
    AFunctionDeclaration functionDeclaration = predecessor.getFunction();

    for (AParameterDeclaration parameterDeclaration : functionDeclaration.getParameters()) {
      if (parameterDeclaration instanceof CParameterDeclaration parmDec) {
        CIdExpression functionParameter = (TaintAnalysisUtils.getCidExpressionForCParDec(parmDec));
        killedVars.add(functionParameter);
        values.put(functionParameter, null);
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

    if (pDeclaration instanceof CFunctionDeclaration) { // extern function defs land here
      logger.log(Level.FINE, "declaration is instance of CFunctionDeclaration");
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

  private TaintAnalysisState handleStatementEdge(TaintAnalysisState pState, CStatementEdge pCfaEdge)
      throws CPATransferException {
    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
    CStatement pStatement = pCfaEdge.getStatement();
    Set<CIdExpression> taintedVariables = pState.getTaintedVariables().keySet();

    if (pStatement instanceof CExpressionStatement) {
      // TODO: implement, see taintByCommaOperator, line 19
      logger.log(Level.INFO, "Statement is an expression statement");
    }

    if (pStatement instanceof CExpressionAssignmentStatement exprAssignStmt) {
      // E.g.: z = x * x + y;
      CLeftHandSide lhs = exprAssignStmt.getLeftHandSide();
      CExpression rhs = exprAssignStmt.getRightHandSide();
      Set<CIdExpression> allVarsAsCExpr = TaintAnalysisUtils.getAllVarsAsCExpr(rhs);

      boolean taintedVarsRHS =
          allVarsAsCExpr.stream().anyMatch(var -> taintedVariables.contains(var));

      Map<CIdExpression, CExpression> values = new HashMap<>();

      if (lhs instanceof CIdExpression variableLHS) {
        // If a LHS is a variable and the RHS contains an expression with a tainted variable, also
        // mark the variable on the LHS as tainted. If no variable is tainted, kill the variable on
        // LHS
        if (taintedVarsRHS) {
          generatedVars.add(variableLHS);
          values.put(variableLHS, rhs);
        } else {
          killedVars.add(variableLHS);
          values.put(variableLHS, rhs);
        }
      } else if (lhs instanceof CArraySubscriptExpression arraySubscriptLHS) {
        // If the LHS is an array element and the RHS contains a tainted variable,
        // mark the array variable as tainted
        CExpression arrayExpr = arraySubscriptLHS.getArrayExpression();
        if (arrayExpr instanceof CIdExpression arrayVariable) {
          if (taintedVarsRHS) {
            generatedVars.add(arrayVariable);
            values.put(arrayVariable, rhs);
          }
        }

      } else {
        logger.log(
            Level.INFO, "lhs is not an instance of CIdExpression or CArraySubscriptExpression");
      }

      return generateNewState(pState, killedVars, generatedVars, values);
    }

    if (pStatement instanceof CFunctionCallStatement functionCallStmt) {
      CFunctionCallExpression callExpr = functionCallStmt.getFunctionCallExpression();
      String functionName = callExpr.getFunctionNameExpression().toString();

      if ("__VERIFIER_set_public".equals(functionName)) {
        List<CExpression> params = callExpr.getParameterExpressions();

        if (params.size() == 2 && params.get(0) instanceof CIdExpression expr) {
          // TODO: this already handles the tainting of arrays, but still check whether further
          // cases are needed
          try {
            CExpression sanitizationFlag = params.get(1);
            int varMustBePublic = TaintAnalysisUtils.evaluateExpressionToInteger(sanitizationFlag);
            int varIsCurrentlyTainted = taintedVariables.contains(expr) ? 1 : 0;

            if (varIsCurrentlyTainted == 1 && varMustBePublic == 1) {
              killedVars.add(expr);
            } else if (varIsCurrentlyTainted == 0 && varMustBePublic == 0) {
              generatedVars.add(expr);
            }

          } catch (CPATransferException e) {
            throw new CPATransferException("Error processing setPublic call", e);
          }
        }
      } else if ("__VERIFIER_is_public".equals(functionName)) {
        List<CExpression> params = callExpr.getParameterExpressions();

        if (params.size() == 2) {

          CExpression exprToCheck = params.get(0);
          CExpression taintCheck = params.get(1);
          int expectedPublicity = TaintAnalysisUtils.evaluateExpressionToInteger(taintCheck);
          boolean expressionIsTainted;

          Set<CIdExpression> exprToCheckParams = TaintAnalysisUtils.getAllVarsAsCExpr(exprToCheck);
          expressionIsTainted = taintedVariables.stream().anyMatch(exprToCheckParams::contains);

          if (exprToCheck instanceof CIdExpression) {
            // E.g., __VERIFIER_is_public(x, 1);
            logger.log(Level.INFO, "first parameter is a CIdExpression");
          }

          if (exprToCheck instanceof CBinaryExpression) {
            // E.g., __VERIFIER_is_public(x + y * z, 1);
            logger.log(Level.INFO, "first parameter is a CBinaryExpression");
          }

          if (exprToCheck instanceof CArraySubscriptExpression) {
            // E.g., __VERIFIER_is_public(d[i], 1);
            // (Passing only d as the first arg --no index-- will be handled as a CIdExpression)

            // When an array d is tainted, we taint all its components as well, and
            // when one part of an array is tainted, we taint the whole array.
            // I.e., isTainted(d) <==> isTainted(d[i]), for all 0 <= i < d.length.
            logger.log(Level.INFO, "first parameter is a CArraySubscriptExpression");
          }

          if (exprToCheck instanceof CUnaryExpression) {
            // E.g., __VERIFIER_is_public(-x, 1);
            // E.g., __VERIFIER_is_public(&x, 1);
            logger.log(Level.INFO, "first parameter is a CUnaryExpression");
          }

          if (exprToCheck instanceof CPointerExpression) {
            // E.g., __VERIFIER_is_public(*x, 1);
            // TODO
            logger.log(Level.INFO, "first parameter is a CPointerExpression");
          }

          if (exprToCheck instanceof CFieldReference fieldRef) {
            // E.g., __VERIFIER_is_public(t.a, 1);
            expressionIsTainted = taintedVariables.contains(fieldRef.getFieldOwner());
          }

          if (exprToCheck instanceof CCastExpression castExpr) {
            // E.g., __VERIFIER_is_public((char) x, 1);
            CExpression operand = castExpr.getOperand();

            if (operand instanceof CIdExpression idExpr) {
              expressionIsTainted = taintedVariables.contains(idExpr);
            }

            if (operand instanceof CBinaryExpression binExpr) {

              Set<CIdExpression> allVarsAsCExpr = TaintAnalysisUtils.getAllVarsAsCExpr(binExpr);

              expressionIsTainted =
                  allVarsAsCExpr.stream().anyMatch(var -> taintedVariables.contains(var));
            }

            if (operand instanceof CTypeIdExpression) {
              logger.log(Level.INFO, "first parameter is a CTypeIdExpression");
            }

            if (operand instanceof CCharLiteralExpression) {
              logger.log(Level.INFO, "first parameter is a CCharLiteralExpression");
            }

            if (operand instanceof CStringLiteralExpression) {
              logger.log(Level.INFO, "first parameter is a CStringLiteralExpression");
            }

            if (operand instanceof CFloatLiteralExpression) {
              logger.log(Level.INFO, "first parameter is a CFloatLiteralExpression");
            }

            if (operand instanceof CIntegerLiteralExpression) {
              logger.log(Level.INFO, "first parameter is a CIntegerLiteralExpression");
            }

            if (operand instanceof CUnaryExpression) {
              logger.log(Level.INFO, "first parameter is a CUnaryExpression");
            }

            if (operand instanceof CPointerExpression) {
              logger.log(Level.INFO, "first parameter is a CPointerExpression");
            }

            if (operand instanceof CFieldReference) {
              logger.log(Level.INFO, "first parameter is a CFieldReference");
            }

            if (operand instanceof CArraySubscriptExpression) {
              logger.log(Level.INFO, "first parameter is a CArraySubscriptExpression");
            }

            if (operand instanceof CStringLiteralExpression) {
              logger.log(Level.INFO, "first parameter is a CStringLiteralExpression");
            }

            if (operand instanceof CFloatLiteralExpression) {
              logger.log(Level.INFO, "first parameter is a CFloatLiteralExpression");
            }
          }

          if (exprToCheck instanceof CTypeIdExpression) {
            // E.g., __VERIFIER_is_public(sizeof(int), 1);
            // --> not reachable, cause CPAchecker evaluates the expression before passing
            // it to the taint analysis
            logger.log(Level.INFO, "first parameter is a CTypeIdExpression");
          }

          if (exprToCheck instanceof CCharLiteralExpression) {
            // E.g., __VERIFIER_is_public('a', 1);
            // Will never be tainted
            logger.log(Level.INFO, "first parameter is a CCharLiteralExpression");
          }

          if (exprToCheck instanceof CStringLiteralExpression) {
            // E.g., __VERIFIER_is_public("hello", 1);
            // Will never be tainted
            logger.log(Level.INFO, "first parameter is a CStringLiteralExpression");
          }

          if (exprToCheck instanceof CFloatLiteralExpression) {
            // E.g., __VERIFIER_is_public(5.3, 1);
            // Will never be tainted
            logger.log(Level.INFO, "first parameter is a CFloatLiteralExpression");
          }

          if (exprToCheck instanceof CIntegerLiteralExpression) {
            // E.g., __VERIFIER_is_public(5, 1);
            // Will never be tainted
            logger.log(Level.INFO, "first parameter is a CIntegerLiteralExpression");
          }

          TaintAnalysisState newErrorState =
              checkInformationFlowViolation(
                  pState,
                  pCfaEdge,
                  expectedPublicity,
                  expressionIsTainted,
                  exprToCheck,
                  killedVars,
                  generatedVars);

          if (newErrorState != null) {
            return newErrorState;
          }
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
            break;
          }
        }
      }
    }

    if (pStatement instanceof CFunctionCallAssignmentStatement functionCallAssignStmt) {
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
      }
      // TODO: Add more instance-cases, if necessary
    }

    Map<CIdExpression, CExpression> values = new HashMap<>();
    return generateNewState(pState, killedVars, generatedVars, values);
  }

  @Nullable
  private TaintAnalysisState checkInformationFlowViolation(
      TaintAnalysisState pState,
      CStatementEdge pCfaEdge,
      int expectedPublicity,
      boolean isCurrentlyTainted,
      CExpression firstArg,
      Set<CIdExpression> killedVars,
      Set<CIdExpression> generatedVars) {

    if (expectedPublicity == 1 && isCurrentlyTainted) {
      logger.logf(
          Level.WARNING,
          "Assertion violation at %s: Array '%s' was expected to be public but is" + " tainted.",
          pCfaEdge.getFileLocation(),
          firstArg.toASTString());
      Map<CIdExpression, CExpression> values = new HashMap<>();
      TaintAnalysisState newState = generateNewState(pState, killedVars, generatedVars, values);
      newState.setViolatesProperty();
      return newState;
    }
    if (expectedPublicity == 0 && !isCurrentlyTainted) {
      logger.logf(
          Level.WARNING,
          "Assertion violation at %s: Array '%s' was expected to be tainted but is" + " public.",
          pCfaEdge.getFileLocation(),
          firstArg.toASTString());

      Map<CIdExpression, CExpression> values = new HashMap<>();
      TaintAnalysisState newState = generateNewState(pState, killedVars, generatedVars, values);
      newState.setViolatesProperty();
      return newState;
    }
    return null;
  }

  private boolean isSource(CFunctionCall pStatement) {

    return SOURCES.contains(
        pStatement.getFunctionCallExpression().getFunctionNameExpression().toString());
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
