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
package org.sosy_lab.cpachecker.cpa.livevar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectingVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.base.Optional;

// TODO testing
/*
 * Note that alias information is currently not used, analysis may be imprecise
 * e.g. if a pointer pointing to a variable is dereferenced and assigned a new value
 */
public class LiveVariablesTransferRelation extends SingleEdgeTransferRelation {

  private static final String UNSUPPORT = "Only C code is supported.";
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  public LiveVariablesTransferRelation(final LogManager pLogger, final ShutdownNotifier pShutdownNotifier) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  /**
   * Returns a collection of all variable names which occur in expression
   */
  private Collection<? extends String> handleExpression(CExpression expression, String inFunction) {
    Set<CIdExpression> result = expression.accept(new CIdExpressionCollectingVisitor());
    Collection<String> liveVars = new ArrayList<>(result.size());
    for (CIdExpression exp : result) {
      liveVars.add(buildVarName(inFunction, exp));
    }
    return liveVars;
  }

  /**
   * Returns name of variable which is assigned a new value, only if its like a=...,
   * be conservative for a->x=, a.x=... or *p=... and return null
   */
  private String getAssignedVar(CLeftHandSide leftHandSideAssignment, String inFunction) {
    if (leftHandSideAssignment instanceof CComplexCastExpression) {
      // TODO currently conservatively handled
      return null;
    }
    if (leftHandSideAssignment instanceof CArraySubscriptExpression
        || leftHandSideAssignment instanceof CFieldReference
        || leftHandSideAssignment instanceof CPointerExpression) { return null; }
    assert (leftHandSideAssignment instanceof CIdExpression);
    Set<CIdExpression> result = leftHandSideAssignment.accept(new CIdExpressionCollectingVisitor());
    assert (result.size() == 1);
    return buildVarName(inFunction, result.iterator().next());
  }

  /**
   * This function handles assumptions like "if(a==b)" and "if(a!=0)".
   */
  private LiveVariablesState handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption,
      String inFunction, LiveVariablesState state)
      throws CPATransferException {
    logger.logf(Level.FINER, "Handle assumption, all variables occurring in assumption %s become live.", expression);
    return state.addLiveVariables(handleExpression(expression, inFunction));
  }

  /**
   * This function handles assignments like x=a+b.
   */
  private LiveVariablesState handleAssignment(CExpressionAssignmentStatement statement, String inFunction,
      LiveVariablesState state) {
    logger.logf(Level.FINER, "Handle assignment. Assigned variable (left hand side %s) is no longer live. All variables occurring in assignment expression (right hand side %s) become live.", statement.getLeftHandSide(), statement.getRightHandSide());
    logger.logf(Level.FINEST, "If an array entry, a struct entry or a dereferenced pointer is assigned. Variable on left hand side is kept alive.");
    return state.removeAndAddLiveVariables(getAssignedVar(statement.getLeftHandSide(), inFunction),
        handleExpression(statement.getRightHandSide(), inFunction));
  }

  private Collection<? extends String> getVariablesUsedForInitialization(CInitializer init, String inFunction) {
    if (init instanceof CDesignatedInitializer) {
      // e.g. .x=b or .p.x.=1  as part of struct initialization
      return getVariablesUsedForInitialization(((CDesignatedInitializer) init).getRightHandSide(), inFunction);
    }
    if (init instanceof CInitializerList)
    {
      Collection<? extends String> result;
      Collection<String> returnResult = new ArrayList<>();
      // e.g. {a, b, s->x} (array) , {.x=1, .y=0} (initialization of struct)
      for (CInitializer inList : ((CInitializerList) init).getInitializers()) {
        result = getVariablesUsedForInitialization(inList, inFunction);
        if (result == null) { return null; }
        returnResult.addAll(result);
      }
      return returnResult;
    }
    if (init instanceof CInitializerExpression) { return handleExpression(
        ((CInitializerExpression) init).getExpression(), inFunction); }
    return null;
  }

  /** This function handles declarations like "int a;", "int b=a;", "int x[] = {a, b};", "struct str s;", "struct str s={.x=1}", "ownType t;" or "int *p = &a;".
    */
  private LiveVariablesState handleVariableDeclaration(CDeclarationEdge cfaEdge, CVariableDeclaration decl,
      String inFunction, LiveVariablesState state) throws UnsupportedCCodeException {
    logger.logf(Level.FINER, "After declaration variable %s is no longer alive.", decl.getName());
    CInitializer init = decl.getInitializer();
    if (init != null) {
      logger.logf(Level.FINER, "Declared variable is initialized. All variables used in initialization expression %s become alive.", init);
      Collection<? extends String> result = getVariablesUsedForInitialization(init, inFunction);
      if (result == null) { throw new UnsupportedCCodeException("Unknown initializer used in declaration", cfaEdge); }
      return state.removeAndAddLiveVariables(buildVarName(inFunction, decl), result);
    }
    return state.removeLiveVariable(buildVarName(inFunction, decl));
  }

  private LiveVariablesState handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl, String inFunction,
      LiveVariablesState state) throws CPATransferException {
    logger.logf(Level.FINER,"Handle declaration %s. Only variable declaration change the state.", decl);
    // combined declaration of typedef and struct declaration are separated into two statements
    // similar for struct declaration and variable declaration
    if (decl instanceof CComplexTypeDeclaration) {
      // handles structs
      return state;
    }
    if (decl instanceof CTypeDefDeclaration) {
      // handles typedefs
      return state;
    }
    if (decl instanceof CFunctionDeclaration) { return state; }
    if (decl instanceof CVariableDeclaration) {
      // handles variable declarations, simple type like int, arrays, structs, pointers, types introduced by typedef, etc.
      return handleVariableDeclaration(cfaEdge, (CVariableDeclaration) decl,
          inFunction, state);
    }
    throw new UnsupportedCCodeException("Unknown declaration", cfaEdge);
  }

  /** This function handles function calls like "f(x)", that calls "f(int a)".  */
  private LiveVariablesState handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName, String callerFunction, LiveVariablesState state) throws CPATransferException {
    logger.logf(Level.FINER, "Handles the change from caller to callee. All declared parameter variables are no longer alive. Variables used by the function caller to initialize the parameters become alive.");
    Collection<String> variablesInArguments = new ArrayList<>();
    for (CExpression exp : arguments) {
      variablesInArguments.addAll(handleExpression(exp, callerFunction));
    }
    Collection<String> parameterVars = new ArrayList<>(parameters.size());
    for (CParameterDeclaration decl : parameters) {
      parameterVars.add(buildFunctionVarName(calledFunctionName, decl.getName()));
    }
    return state.removeAndAddLiveVariables(parameterVars, variablesInArguments);
  }

  /** This function handles functionReturns like "y=f(x)". */
  private LiveVariablesState handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName, LiveVariablesState state)
      throws CPATransferException {
    logger.logf(Level.FINER, "Handles the change from calle to caller.");
    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      logger.logf(Level.FINER,"Function result is assigned to variable %s which is no longer alive.", ((CFunctionCallAssignmentStatement) summaryExpr).getLeftHandSide());
      return state.removeLiveVariable(
        getAssignedVar(((CFunctionCallAssignmentStatement) summaryExpr).getLeftHandSide(), callerFunctionName)); }
    return state;
  }

  /** This function handles functionStatements like "return (x)". */
  private LiveVariablesState handleReturnStatementEdge(CReturnStatementEdge cfaEdge, Optional<CExpression> pOptionalExpr,
      String inFunction, LiveVariablesState state)
      throws CPATransferException {
    if (pOptionalExpr.isPresent()) {
      logger.logf(Level.FINER, "Handles return statement. All variables occurring in return expression %s becom live.",
          pOptionalExpr);
      return state.addLiveVariables(handleExpression(pOptionalExpr.get(), inFunction));
    }
    logger.logf(Level.FINER, "Handle statement return; (no return expression). State remains unchanged.");
    return state;
  }

  private LiveVariablesState handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge, LiveVariablesState state)
      throws CPATransferException {
    logger.logf(Level.FINER, "Handle function call like an external function call. If function result is assigned to variable, this variable is no longer live. Parameters become live.");
    String assignedVar = null;
    assert (cfaEdge.getPredecessor().getFunctionName().equals(cfaEdge.getSuccessor().getFunctionName()));
    String callerFunction = cfaEdge.getPredecessor().getFunctionName();
    if (cfaEdge.getExpression() instanceof CFunctionCallAssignmentStatement) {
      assignedVar =
          getAssignedVar(((CFunctionCallAssignmentStatement) cfaEdge.getExpression()).getLeftHandSide(), callerFunction);
    }
    Collection<String> variablesInArguments = new ArrayList<>();
    for (CExpression exp : cfaEdge.getExpression().getFunctionCallExpression().getParameterExpressions()) {
      variablesInArguments.addAll(handleExpression(exp, callerFunction));
    }
    return state.removeAndAddLiveVariables(assignedVar, variablesInArguments);
  }

  /** This function handles statements like "a = 0;" and "b = !a;"
   * and calls of external functions. */
  private LiveVariablesState handleStatementEdge(CStatementEdge cfaEdge, CStatement statement, String inFunction,
      LiveVariablesState state)
      throws CPATransferException {
    if (statement instanceof CExpressionAssignmentStatement) {
      logger.logf(Level.FINER, "Handle assignment. Right hand side is an expression.");
      return handleAssignment(
        (CExpressionAssignmentStatement) statement, inFunction, state); }
    if (statement instanceof CExpressionStatement) {
      logger.logf(Level.FINER, "Handle expression statement. Any variable in expression becomes alive.");
      return state.addLiveVariables(handleExpression(
        ((CExpressionStatement) statement).getExpression(), inFunction)); }
    if (statement instanceof CFunctionCallAssignmentStatement) {
      logger.logf(Level.FINER, "Handle assignment. Right hand side is an external function call. Parameters become live. Assigned variable is no longer live.");
      Collection<String> newLiveVars = new ArrayList<>();
      for (CExpression expression : ((CFunctionCallAssignmentStatement) statement).getFunctionCallExpression()
          .getParameterExpressions()) {
        newLiveVars.addAll(handleExpression(expression, inFunction));
      }
      return state.removeAndAddLiveVariables(
          getAssignedVar(((CFunctionCallAssignmentStatement) statement).getLeftHandSide(), inFunction), newLiveVars);
    }
    if (statement instanceof CFunctionCallStatement) {
      logger.logf(Level.FINER, "Handle external function call. Return result if available is not used. Parameters become live.");
      Collection<String> newLiveVars = new ArrayList<>();
      for (CExpression expression : ((CFunctionCallStatement) statement).getFunctionCallExpression()
          .getParameterExpressions()) {
        newLiveVars.addAll(handleExpression(expression, inFunction));
      }
      return state.addLiveVariables(newLiveVars);
    }
    throw new CPATransferException("Unknown statement.");
  }

  protected LiveVariablesState handleSimpleEdge(final CFAEdge cfaEdge, final LiveVariablesState currentState)
      throws CPATransferException {

    switch (cfaEdge.getEdgeType()) {
    case DeclarationEdge:
      final ADeclarationEdge declarationEdge = (ADeclarationEdge) cfaEdge;
      assert (cfaEdge.getPredecessor().getFunctionName().equals(cfaEdge.getSuccessor().getFunctionName()));
      if (declarationEdge instanceof CDeclarationEdge) { return handleDeclarationEdge(
          (CDeclarationEdge) declarationEdge,
          (CDeclaration) declarationEdge.getDeclaration(), cfaEdge.getSuccessor()
              .getFunctionName(), currentState); }
      throw new UnsupportedCCodeException(UNSUPPORT, cfaEdge);
    case StatementEdge:
      final AStatementEdge statementEdge = (AStatementEdge) cfaEdge;
      assert (cfaEdge.getPredecessor().getFunctionName().equals(cfaEdge.getSuccessor().getFunctionName()));
      if (statementEdge instanceof CStatementEdge) { return handleStatementEdge((CStatementEdge) statementEdge,
          ((CStatementEdge) statementEdge).getStatement(), cfaEdge.getSuccessor().getFunctionName(), currentState); }
      throw new UnsupportedCCodeException(UNSUPPORT, cfaEdge);

    case ReturnStatementEdge:
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge,
      // this is a statement edge, which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      final AReturnStatementEdge returnEdge = (AReturnStatementEdge) cfaEdge;
      if (returnEdge instanceof CReturnStatementEdge) {
        assert (cfaEdge.getPredecessor().getFunctionName().equals(cfaEdge.getSuccessor().getFunctionName()));
        return handleReturnStatementEdge((CReturnStatementEdge) returnEdge,
            ((CReturnStatementEdge) returnEdge).getExpression(), cfaEdge.getSuccessor().getFunctionName(), currentState);
      } else {
        throw new UnsupportedCCodeException(UNSUPPORT, cfaEdge);
      }

    case BlankEdge:
      logger.logf(Level.FINER, "Handle edge without any code information only used to model control flow. Live variable state remains.");
      return currentState;

    case CallToReturnEdge:
      if (cfaEdge instanceof CFunctionSummaryEdge) {
        return handleFunctionSummaryEdge((CFunctionSummaryEdge) cfaEdge, currentState);
      } else {
        throw new UnsupportedCCodeException(UNSUPPORT, cfaEdge);
      }

    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }
  }

  /** This method just forwards the handling to every inner edge.
   * @throws InterruptedException */
  protected LiveVariablesState handleMultiEdge(MultiEdge cfaEdge, LiveVariablesState current)
      throws CPATransferException, InterruptedException {
    logger.logf(Level.FINEST, "Start handling of multi edge. Edges summarized by multi edge are handled sequentially");
    for (final CFAEdge innerEdge : cfaEdge) {
      shutdownNotifier.shutdownIfNecessary();
      logger.logf(Level.FINEST, "Next edge handled is %s.", innerEdge);
      current = handleSimpleEdge(innerEdge, current);
    }
    return current;
  }


  @SuppressWarnings("unchecked")
  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {
    if (!(pState instanceof LiveVariablesState)) { throw new CPATransferException(
        "Expected abstract state of type LiveVariablesState"); }
    if (cfaEdge == null) { throw new CPATransferException("Require CFA edge for which successors must be computed."); }
    LiveVariablesState current = (LiveVariablesState) pState;
    logger.logf(Level.FINE, "Compute successor of live variable %s state along cfa edge %s.", current, cfaEdge);
    final LiveVariablesState successor;

    switch (cfaEdge.getEdgeType()) {

    case AssumeEdge:
      if (cfaEdge instanceof CAssumeEdge) {
        final CAssumeEdge assumption = (CAssumeEdge) cfaEdge;
        assert (cfaEdge.getPredecessor().getFunctionName().equals(cfaEdge.getSuccessor().getFunctionName()));
        successor =
            handleAssumption(assumption, assumption.getExpression(), assumption.getTruthAssumption(), cfaEdge
                .getSuccessor().getFunctionName(), current);
      } else {
        throw new UnsupportedCCodeException(UNSUPPORT, cfaEdge);
      }
      break;

    case FunctionCallEdge:
      final FunctionCallEdge fnkCall = (FunctionCallEdge) cfaEdge;
      final FunctionEntryNode succ = fnkCall.getSuccessor();
      final String calledFunctionName = succ.getFunctionName();
      final String caller = fnkCall.getPredecessor().getFunctionName();
      if (fnkCall instanceof CFunctionCallEdge) {
        successor = handleFunctionCallEdge((CFunctionCallEdge) fnkCall, ((CFunctionCallEdge) fnkCall).getArguments(),
            (List<CParameterDeclaration>) succ.getFunctionParameters(), calledFunctionName, caller, current);
      } else {
        throw new UnsupportedCCodeException(UNSUPPORT, cfaEdge);
      }
      break;

    case FunctionReturnEdge:
      final String callerFunctionName = cfaEdge.getSuccessor().getFunctionName();
      if (cfaEdge instanceof CFunctionReturnEdge) {
        final CFunctionReturnEdge fnkReturnEdge = (CFunctionReturnEdge) cfaEdge;
        final CFunctionSummaryEdge summaryEdge = fnkReturnEdge.getSummaryEdge();
        successor = handleFunctionReturnEdge(fnkReturnEdge,
            summaryEdge, summaryEdge.getExpression(), callerFunctionName, current);
      } else {
        throw new UnsupportedCCodeException(UNSUPPORT, cfaEdge);
      }

      break;

    case MultiEdge:
      successor = handleMultiEdge((MultiEdge) cfaEdge, current);
      break;

    default:
      successor = handleSimpleEdge(cfaEdge, current);
    }

    if (successor == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(successor);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    //do nothing and return null as result
    return null;
  }


  private static boolean isGlobal(final CIdExpression exp) {
    CSimpleDeclaration decl = exp.getDeclaration();
    if (decl instanceof CDeclaration) { return ((CDeclaration) decl).isGlobal(); }
    return false;
  }

  private static String buildFunctionVarName(final String function, final String var) {
    return buildVarName(function, var);
  }

  private static String buildGlobalVarName(final String var) {
    return buildVarName(null, var);
  }

  private static String buildVarName(@Nullable final String function, final String var) {
    return (function == null) ? var : function + "::" + var;
  }

  private static String buildVarName(final String function, CIdExpression var) {
    if (isGlobal(var)) { return buildGlobalVarName(var.getName()); }
    return buildFunctionVarName(function, var.getName());
  }

  private String buildVarName(String pInFunction, CVariableDeclaration pDecl) {
    return pDecl.isGlobal() ? buildGlobalVarName(pDecl.getName()) : buildFunctionVarName(pInFunction, pDecl.getName());
  }
}
