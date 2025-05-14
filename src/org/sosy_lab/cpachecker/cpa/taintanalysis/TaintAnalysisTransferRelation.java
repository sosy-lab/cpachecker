// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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
      Lists.newArrayList("__VERIFIER_nondet_int", "__VERIFIER_nondet_char");
  //  private static final List<String> SINKS = Lists.newArrayList("printf");

  private final LogManager logger;

  public TaintAnalysisTransferRelation(LogManager pLogger) {

    logger = pLogger;
  }

  private TaintAnalysisState generateNewState(
      TaintAnalysisState pState,
      Set<CIdExpression> pKilledVars,
      Set<CIdExpression> pGeneratedVars) {
    logger.log(Level.INFO, String.format("Killed %s, generated %s", pKilledVars, pGeneratedVars));
    return new TaintAnalysisState(
        Sets.union(Sets.difference(pState.getTaintedVariables(), pKilledVars), pGeneratedVars));
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
          return Collections.singleton(
              handleAssumption(
                  state, assumption, assumption.getExpression(), assumption.getTruthAssumption()));
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
  private TaintAnalysisState handleAssumption(
      TaintAnalysisState pState,
      CAssumeEdge pCfaEdge,
      CExpression pExpression,
      boolean pTruthAssumption) {
    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();

    return generateNewState(pState, killedVars, generatedVars);
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleBlankEdge(TaintAnalysisState pState, BlankEdge pCfaEdge) {
    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();

    return generateNewState(pState, killedVars, generatedVars);
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleFunctionCallEdge(
      TaintAnalysisState pState,
      CFunctionCallEdge pCfaEdge,
      List<? extends CExpression> pArguments,
      List<? extends AParameterDeclaration> pFunctionParameters,
      String pCalledFunctionName) {
    // This is only needed for intra-procedural analyses
    return new TaintAnalysisState(new HashSet<>(pState.getTaintedVariables()));
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleFunctionReturnEdge(
      TaintAnalysisState pState,
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pSummaryEdge,
      CFunctionCall pExpression,
      String pCallerFunctionName) {
    // This is only needed for intra-procedural analyses
    return new TaintAnalysisState(new HashSet<>(pState.getTaintedVariables()));
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleFunctionSummaryEdge(
      TaintAnalysisState pState, CFunctionSummaryEdge pCfaEdge) {
    // This is only needed for intra-procedural analyses
    return new TaintAnalysisState(new HashSet<>(pState.getTaintedVariables()));
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleReturnStatementEdge(
      TaintAnalysisState pState, CReturnStatementEdge pCfaEdge) {
    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();

    return generateNewState(pState, killedVars, generatedVars);
  }

  private TaintAnalysisState handleDeclarationEdge(
      TaintAnalysisState pState, CDeclarationEdge pCfaEdge) {

    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
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
              pState, initExpr, generatedVars, variableLHS, killedVars);
        }

        if (initializer instanceof CInitializerList initList) {

          List<CInitializer> initializerList = initList.getInitializers();

          for (CInitializer init : initializerList) {
            if (init instanceof CInitializerExpression initExpr) {

              checkIfInitializerExpressionIsTainted(
                  pState, initExpr, generatedVars, variableLHS, killedVars);
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

    return generateNewState(pState, killedVars, generatedVars);
  }

  private static void checkIfInitializerExpressionIsTainted(
      TaintAnalysisState pState,
      CInitializerExpression initializer,
      Set<CIdExpression> generatedVars,
      CIdExpression variableLHS,
      Set<CIdExpression> killedVars) {

    CExpression expr = initializer.getExpression();

    boolean taintedVarsRHS =
        TaintAnalysisUtils.getAllVarsAsCExpr(expr).stream()
            .anyMatch(var -> pState.getTaintedVariables().contains(var));

    if (taintedVarsRHS) {
      generatedVars.add(variableLHS);
    } else {
      killedVars.add(variableLHS);
    }
  }

  private TaintAnalysisState handleStatementEdge(TaintAnalysisState pState, CStatementEdge pCfaEdge)
      throws CPATransferException {
    Set<CIdExpression> killedVars = new HashSet<>();
    Set<CIdExpression> generatedVars = new HashSet<>();
    CStatement pStatement = pCfaEdge.getStatement();
    Set<CIdExpression> taintedVariables = pState.getTaintedVariables();

    if (pStatement instanceof CExpressionStatement) {
      // TODO: implement, see taintByCommaOperator, line 19
      logger.log(Level.INFO, "Statement is an expression statement");
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

          CExpression firstArg = params.get(0);
          CExpression taintCheck = params.get(1);
          int expectedPublicity = TaintAnalysisUtils.evaluateExpressionToInteger(taintCheck);
          boolean expressionIsTainted = false;

          if (firstArg instanceof CIdExpression) {
            // E.g., __VERIFIER_is_public(x, 1);
            expressionIsTainted = taintedVariables.contains(firstArg);
          }

          if (firstArg instanceof CBinaryExpression) {
            // E.g., __VERIFIER_is_public(x + y * z, 1);
            Set<CIdExpression> firstArgParams = TaintAnalysisUtils.getAllVarsAsCExpr(firstArg);
            expressionIsTainted = firstArgParams.stream().anyMatch(taintedVariables::contains);
          }

          if (firstArg instanceof CArraySubscriptExpression arrayArg) {
            // E.g., __VERIFIER_is_public(d[i], 1);
            // (Passing only d as the first arg --no index-- will be handled as a CIdExpression)

            // When an array d is tainted, we taint all its components as well, and
            // when one part of an array is tainted, we taint the whole array.
            // I.e., isTainted(d) <==> isTainted(d[i]), for all 0 <= i < d.length.
            expressionIsTainted = taintedVariables.contains(arrayArg.getArrayExpression());
          }

          if (firstArg instanceof CUnaryExpression unaryExpr) {
            // E.g., __VERIFIER_is_public(-x, 1);
            // E.g., __VERIFIER_is_public(&x, 1);
            expressionIsTainted = taintedVariables.contains(unaryExpr.getOperand());
          }

          if (firstArg instanceof CPointerExpression) {
            // E.g., __VERIFIER_is_public(*x, 1);
            // TODO
            logger.log(Level.INFO, "first parameter is a CPointerExpression");
          }

          if (firstArg instanceof CFieldReference fieldRef) {
            // E.g., __VERIFIER_is_public(t.a, 1);

            expressionIsTainted = taintedVariables.contains(fieldRef.getFieldOwner());
          }

          if (firstArg instanceof CCastExpression castExpr) {
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

          if (firstArg instanceof CTypeIdExpression) {
            // E.g., __VERIFIER_is_public(sizeof(int), 1);
            // --> not reachable, cause CPAchecker evaluates the expression before passing
            // it to the taint analysis
            logger.log(Level.INFO, "first parameter is a CTypeIdExpression");
          }

          if (firstArg instanceof CCharLiteralExpression) {
            // E.g., __VERIFIER_is_public('a', 1);
            // Will never be tainted
            logger.log(Level.INFO, "first parameter is a CCharLiteralExpression");
          }

          if (firstArg instanceof CStringLiteralExpression) {
            // E.g., __VERIFIER_is_public("hello", 1);
            // Will never be tainted
            logger.log(Level.INFO, "first parameter is a CStringLiteralExpression");
          }

          if (firstArg instanceof CFloatLiteralExpression) {
            // E.g., __VERIFIER_is_public(5.3, 1);
            // Will never be tainted
            logger.log(Level.INFO, "first parameter is a CFloatLiteralExpression");
          }

          if (firstArg instanceof CIntegerLiteralExpression) {
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
                  firstArg,
                  killedVars,
                  generatedVars);

          if (newErrorState != null) return newErrorState;
        }
      }
    }

    if (pStatement instanceof CExpressionAssignmentStatement exprAssignStmt) {
      // E.g.: z = x * x + y;
      CLeftHandSide lhs = exprAssignStmt.getLeftHandSide();
      CExpression rhs = exprAssignStmt.getRightHandSide();
      Set<CIdExpression> allVarsAsCExpr = TaintAnalysisUtils.getAllVarsAsCExpr(rhs);

      boolean functionContainsTaintedVar = false;

      if (allVarsAsCExpr.isEmpty()) {
        // TODO: delete this string parsing. Handle every function from which we can't access the
        // parameters via CPAchecker's parsing, as a tainting source.
        functionContainsTaintedVar = rhsOfRawStatementContainsTaintedParameters(pCfaEdge, pState);
      }

      boolean taintedVarsRHS =
          allVarsAsCExpr.stream().anyMatch(var -> taintedVariables.contains(var))
              || functionContainsTaintedVar;

      if (lhs instanceof CIdExpression variableLHS) {
        // If a LHS is a variable and the RHS contains an expression with a tainted variable, also
        // mark the variable on the LHS as tainted. If no variable is tainted, kill the variable on
        // LHS
        if (taintedVarsRHS) {
          generatedVars.add(variableLHS);
        } else {
          killedVars.add(variableLHS);
        }
      } else if (lhs instanceof CArraySubscriptExpression arraySubscriptLHS) {
        // If the LHS is an array element and the RHS contains a tainted variable,
        // mark the array variable as tainted
        CExpression arrayExpr = arraySubscriptLHS.getArrayExpression();
        if (arrayExpr instanceof CIdExpression arrayVariable) {
          if (taintedVarsRHS) {
            generatedVars.add(arrayVariable);
          }
        }
      }
      // TODO: Add more instance-cases, if necessary
    }

    if (pStatement instanceof CFunctionCallAssignmentStatement functionCallAssignStmt) {
      // e.g., x = __VERIFIER_nondet_int();
      CLeftHandSide lhs = functionCallAssignStmt.getLeftHandSide();

      if (lhs instanceof CIdExpression variableLHS) {
        // If a LHS is a variable and the RHS is a function call to a source or a function that
        // takes a tainted variable as parameter, mark the variable on the LHS as tainted and
        // generate the lhs variable. Otherwise, kill the variable.
        if (isSource(functionCallAssignStmt)
            || hasTaintedParameters(functionCallAssignStmt, pState)) {
          generatedVars.add(variableLHS);
        } else {
          killedVars.add(variableLHS);
        }
      }
      // TODO: Add more instance-cases, if necessary
    }

    return generateNewState(pState, killedVars, generatedVars);
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
      logger.log(
          Level.WARNING,
          String.format(
              "Assertion violation at %s: Array '%s' was expected to be public but is"
                  + " tainted.",
              pCfaEdge.getFileLocation(), firstArg.toASTString()));
      TaintAnalysisState newState = generateNewState(pState, killedVars, generatedVars);
      newState.setViolatesProperty();
      return newState;
    }
    if (expectedPublicity == 0 && !isCurrentlyTainted) {
      logger.log(
          Level.WARNING,
          String.format(
              "Assertion violation at %s: Array '%s' was expected to be tainted but is"
                  + " public.",
              pCfaEdge.getFileLocation(), firstArg.toASTString()));

      TaintAnalysisState newState = generateNewState(pState, killedVars, generatedVars);
      newState.setViolatesProperty();
      return newState;
    }
    return null;
  }

  /**
   * Analyzes the right-hand side (RHS) of a raw statement in the form of a {@link CStatementEdge}
   * for potential tainted parameters. This is necessary for taint analysis in CPAchecker when
   * dealing with specific cases where the RHS contains function calls or logical operations.
   *
   * <p>CPAchecker processes certain code structures (e.g., function calls with parameters or
   * logical operations like `&&` and `||`) by evaluating the expressions and passing the results
   * instead of the original raw parameters to the taint analysis. This overrides the needed
   * variables, making raw statement parsing essential to ensure proper recognition and propagation
   * of taint.
   *
   * <p>Special cases handled:
   *
   * <ul>
   *   <li>Function calls with parameters, in the form:
   *       <pre><code>functionName(param1, param2, ..., paramN)</code></pre>
   *   <li>Logical (non-bitwise) operations, in the form:
   *       <pre><code>lhs && rhs</code></pre>
   *       or
   *       <pre><code>lhs || rhs</code></pre>
   * </ul>
   *
   * <p>For these specific cases, the method extracts individual parameters from the RHS, either
   * from function calls or logical operations, and checks whether any of them are tainted by
   * comparing them against the list of tainted variables in {@link TaintAnalysisState}.
   *
   * @param pCfaEdge the {@link CStatementEdge} representing the statement in the CFA being analyzed
   * @param pState the current {@link TaintAnalysisState}, which contains the collection of tainted
   *     variables
   * @return {@code true} if any variable in the RHS of the raw statement matches a tainted variable
   *     from the state, indicating potential taint propagation; {@code false} otherwise
   */
  private boolean rhsOfRawStatementContainsTaintedParameters(
      CStatementEdge pCfaEdge, TaintAnalysisState pState) {

    String rawStatement = pCfaEdge.getRawStatement();

    if (rawStatement == null) {
      return false;
    }

    String rhsString;

    if (rawStatement.contains("=")) {
      // case: function of the form <functionName><(params)>
      rhsString = rawStatement.substring(rawStatement.indexOf('=') + 1).trim();
    } else {
      // case: logical operations && or ||
      rhsString = rawStatement.trim();
    }

    // Check if the RHS contains a function call pattern: functionName(param1, param2, ...)
    if (rhsString.contains("(") && rhsString.contains(")")) {
      int openParenIndex = rhsString.indexOf('(');
      int closeParenIndex = rhsString.lastIndexOf(')');

      if (openParenIndex < closeParenIndex) {
        String paramsString = rhsString.substring(openParenIndex + 1, closeParenIndex).trim();

        Iterable<String> paramsArray =
            Splitter.on(Pattern.compile("\\s*,\\s*")).split(paramsString);

        Set<CIdExpression> taintedVariables = pState.getTaintedVariables();

        for (String param : paramsArray) {
          param = param.trim();

          if (taintedVariables.stream().map(CIdExpression::getName).anyMatch(param::equals)) {
            return true;
          }
        }
      }
    }

    if (rhsString.contains("&&") || rhsString.contains("||")) {

      List<String> logicalParams =
          Splitter.on(Pattern.compile("\\s*(&&|\\|\\|)\\s*")).splitToList(rhsString);

      if (logicalParams.size() == 2) {
        Set<CIdExpression> taintedVariables = pState.getTaintedVariables();

        for (String param : logicalParams) {
          param = param.trim();

          if (taintedVariables.stream().map(CIdExpression::getName).anyMatch(param::equals)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private boolean hasTaintedParameters(
      CFunctionCallAssignmentStatement pStatement, TaintAnalysisState pState) {
    CFunctionCallExpression call = pStatement.getRightHandSide();

    return call.getParameterExpressions().stream()
        .filter(e -> e instanceof CIdExpression)
        .anyMatch(arg -> pState.getTaintedVariables().contains(arg));
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
    logger.log(
        Level.INFO,
        String.format(
            "Current abstract state at location %s is  '%s'",
            AbstractStates.extractLocations(pOtherStates).first().get(),
            AbstractStates.extractStateByType(pState, TaintAnalysisState.class)));
    return super.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }
}
