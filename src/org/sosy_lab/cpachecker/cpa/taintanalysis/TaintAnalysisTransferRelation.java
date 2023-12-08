// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.*;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
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

  public static final List<String> SOURCES = Lists.newArrayList("getRSAKey");
  public static final List<String> SINKS = Lists.newArrayList("printf");

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
          return Collections.singleton(
              handleDeclarationEdge(state, declarationEdge, declarationEdge.getDeclaration()));
        } else {
          throw new AssertionError("unknown edge");
        }
      }
      case StatementEdge -> {
        if (cfaEdge instanceof CStatementEdge statementEdge) {

          return Collections.singleton(
              handleStatementEdge(state, statementEdge, statementEdge.getStatement()));
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
    Set<CIdExpression> killedVars = Sets.newHashSet();
    Set<CIdExpression> generatedVars = Sets.newHashSet();

    return generateNewState(pState, killedVars, generatedVars);
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleBlankEdge(TaintAnalysisState pState, BlankEdge pCfaEdge) {
    Set<CIdExpression> killedVars = Sets.newHashSet();
    Set<CIdExpression> generatedVars = Sets.newHashSet();

    return generateNewState(pState, killedVars, generatedVars);
  }

  private TaintAnalysisState handleDeclarationEdge(
      TaintAnalysisState pState, CDeclarationEdge pCfaEdge, CDeclaration pDeclaration) {
    Set<CIdExpression> killedVars = Sets.newHashSet();
    Set<CIdExpression> generatedVars = Sets.newHashSet();


    if (pDeclaration instanceof CVariableDeclaration) {
      CVariableDeclaration dec = (CVariableDeclaration) pCfaEdge.getDeclaration();
      CInitializer initializer = dec.getInitializer();
      CIdExpression variableLHS = TaintAnalysisUtils.getCidExpressionForCVarDec(dec);
      // If a RHS contains an expression with a tainted variable, also mark the variable on the
      // LHS as tainted. If no variable or not expression is present, kill is
      if (Objects.nonNull(initializer) && initializer instanceof CInitializerExpression) {
        CExpression expr = ((CInitializerExpression) initializer).getExpression();
        boolean taintedVarsRHS = TaintAnalysisUtils.getAllVarsAsCExpr(expr).stream()
            .anyMatch(var -> pState.getTaintedVariables().contains(var));
        if (taintedVarsRHS) {
          generatedVars.add(variableLHS);
        } else {
          killedVars.add(variableLHS);
        }
      } else {
        killedVars.add(variableLHS);
      }
    }


    return generateNewState(pState, killedVars, generatedVars);
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleFunctionCallEdge(
      TaintAnalysisState pState,
      CFunctionCallEdge pCfaEdge,
      List<? extends CExpression> pArguments,
      List<? extends AParameterDeclaration> pFunctionParameters,
      String pCalledFunctionName) {
    // This is only needed for intra-procedural analayzes
    return new TaintAnalysisState(Sets.newHashSet(pState.getTaintedVariables()));
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleFunctionReturnEdge(
      TaintAnalysisState pState,
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pSummaryEdge,
      CFunctionCall pExpression,
      String pCallerFunctionName) {
    //This is only needed for intra-procedural analayzes
    return new TaintAnalysisState(Sets.newHashSet(pState.getTaintedVariables()));
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleFunctionSummaryEdge(
      TaintAnalysisState pState, CFunctionSummaryEdge pCfaEdge) {
    //This is only needed for intra-procedural analayzes
    return new TaintAnalysisState(Sets.newHashSet(pState.getTaintedVariables()));
  }

  @SuppressWarnings("unused")
  private TaintAnalysisState handleReturnStatementEdge(
      TaintAnalysisState pState, CReturnStatementEdge pCfaEdge) {
    Set<CIdExpression> killedVars = Sets.newHashSet();
    Set<CIdExpression> generatedVars = Sets.newHashSet();

    return generateNewState(pState, killedVars, generatedVars);
  }

  private TaintAnalysisState handleStatementEdge(
      TaintAnalysisState pState, CStatementEdge pCfaEdge, CStatement pStatement) {
    Set<CIdExpression> killedVars = Sets.newHashSet();
    Set<CIdExpression> generatedVars = Sets.newHashSet();


    if (pCfaEdge.getStatement() instanceof CExpressionAssignmentStatement) {
      CLeftHandSide lhs =
          ((CExpressionAssignmentStatement) pCfaEdge.getStatement()).getLeftHandSide();
      if (lhs instanceof CIdExpression variableLHS) {
        // If a LHS is a variable and the RHS contains an expression with a tainted variable, also
        // mark the variable on the LHS as tainted. If no variable is tainted, kill the variable on
        // LHS
        CExpression rhs =
            ((CExpressionAssignmentStatement) pCfaEdge.getStatement()).getRightHandSide();
        boolean taintedVarsRHS =
            TaintAnalysisUtils.getAllVarsAsCExpr(rhs)
                .stream()
                .anyMatch(var -> pState.getTaintedVariables().contains(var));
        if (taintedVarsRHS) {
          generatedVars.add(variableLHS);
        } else {
          killedVars.add(variableLHS);
        }
      }
    } else if (pStatement instanceof CFunctionCallAssignmentStatement) {
      CLeftHandSide lhs =
          ((CFunctionCallAssignmentStatement) pCfaEdge.getStatement()).getLeftHandSide();
      if (lhs instanceof CIdExpression variableLHS) {
        // If a LHS is a variable and the RHS is a function call to a source, generate the lhs
        // variable.
        // Otherwise, kill the variable
        if (isSource((CFunctionCallAssignmentStatement) pStatement)) {
          generatedVars.add(variableLHS);
        } else {
          killedVars.add(variableLHS);
        }
      }
    } else if (pStatement instanceof CFunctionCallStatement) {

      // Finally, check if Statement is a call to a sink and if any tainted variables are given as
      // argument
      if (isSink((CFunctionCallStatement) pStatement)) {
        CFunctionCallExpression call =
            ((CFunctionCallStatement) pStatement).getFunctionCallExpression();
        boolean leaked =
            call.getParameterExpressions()
                .stream().filter(e -> e instanceof CIdExpression)
                .anyMatch(arg -> pState.getTaintedVariables().contains(arg));
        if (leaked) {
          logger.log(Level.WARNING, "Leaking information");
          TaintAnalysisState newState = generateNewState(pState, killedVars, generatedVars);
          newState.setViolatesProperty();
          return newState;
        }
      }
    }


    return generateNewState(pState, killedVars, generatedVars);
  }

  private boolean isSink(CFunctionCall pStatement) {

    return SINKS.contains(
        pStatement.getFunctionCallExpression().getFunctionNameExpression().toString());
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
