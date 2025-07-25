// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.functionpointer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.FunctionPointerTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.InvalidTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NullTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

@Options(prefix = "cpa.functionpointer")
class FunctionPointerTransferRelation extends SingleEdgeTransferRelation {

  @Option(
      secure = true,
      description =
          "whether function pointers with invalid targets (e.g., 0) should be tracked in order to"
              + " find calls to such pointers")
  private boolean trackInvalidFunctionPointers = false;

  @Option(
      secure = true,
      description =
          "When an invalid function pointer is called, do not assume all functions as possible"
              + " targets and instead call no function.")
  private boolean ignoreInvalidFunctionPointerCalls = false;

  @Option(
      secure = true,
      description =
          "When an unknown function pointer is called, do not assume all functions as possible"
              + " targets and instead call no function (this is unsound).")
  private boolean ignoreUnknownFunctionPointerCalls = false;

  private final LogManagerWithoutDuplicates logger;

  FunctionPointerTransferRelation(LogManager pLogger, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);

    if (ignoreInvalidFunctionPointerCalls && !trackInvalidFunctionPointers) {
      throw new InvalidConfigurationException(
          "FunctionPointerCPA cannot ignore invalid function pointer calls "
              + "when such pointers are not tracked, "
              + "please set cpa.functionpointer.trackInvalidFunctionPointers=true");
    }
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException {

    final FunctionPointerState oldState = (FunctionPointerState) pElement;

    // check assumptions about function pointers, like p == &h, where p is a function pointer, h  is
    // a function
    if (!shouldGoByEdge(oldState, pCfaEdge)) {
      // should not go by the edge
      return ImmutableSet.of();
    }

    // print warning if we go by the default edge of a function pointer call
    // (i.e., the edge for the case where we don't have information about the target).
    String functionCallVariable = getFunctionPointerCall(pCfaEdge);
    if (functionCallVariable != null) {
      FunctionPointerTarget target = oldState.getTarget(functionCallVariable);
      if (target instanceof NamedFunctionTarget namedFunctionTarget) {
        String functionName = namedFunctionTarget.getFunctionName();
        logger.logfOnce(
            Level.WARNING,
            "%s: Function pointer %s points to %s,"
                + " but no corresponding call edge was created during preprocessing."
                + " Ignoring function pointer call: %s",
            pCfaEdge.getFileLocation(),
            functionCallVariable,
            functionName,
            pCfaEdge.getDescription());
      } else {
        logger.logfOnce(
            Level.WARNING,
            "%s: Ignoring call via function pointer %s"
                + " for which no suitable target was found in line: %s",
            pCfaEdge.getFileLocation(),
            functionCallVariable,
            pCfaEdge.getDescription());
      }
    }

    // now handle the edge
    FunctionPointerState.Builder newState = oldState.createBuilder();
    handleEdge(newState, pCfaEdge);

    return ImmutableSet.of(newState.build());
  }

  private boolean shouldGoByEdge(FunctionPointerState oldState, CFAEdge cfaEdge)
      throws UnrecognizedCodeException {
    if (cfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      CAssumeEdge a = (CAssumeEdge) cfaEdge;
      CExpression exp = a.getExpression();
      if (exp instanceof CBinaryExpression e) {
        BinaryOperator op = e.getOperator();
        // Special handling for == and != comparisons between function pointers
        if (e.getCalculationType() instanceof CPointerType calculationType
            && calculationType.getType() instanceof CFunctionType
            && (op == BinaryOperator.EQUALS || op == BinaryOperator.NOT_EQUALS)) {
          FunctionPointerState.Builder newState = oldState.createBuilder();
          FunctionPointerTarget v1 = getValue(e.getOperand1(), newState);
          FunctionPointerTarget v2 = getValue(e.getOperand2(), newState);
          logger.log(Level.ALL, "Operand1 value is", v1);
          logger.log(Level.ALL, "Operand2 value is", v2);
          if ((v1 instanceof NamedFunctionTarget && v2 instanceof NamedFunctionTarget)
              || v1 instanceof NullTarget
              || v2 instanceof NullTarget) {
            boolean result = (op == BinaryOperator.EQUALS) ? v1.equals(v2) : !v1.equals(v2);
            if (result != a.getTruthAssumption()) {
              logger.log(Level.FINE, "Should not go by the edge", a);
              return false; // should not go by this edge
            } else {
              logger.log(Level.FINE, "Should go by the edge", a);
              return true;
            }
          }
          if (a.getTruthAssumption()
              && ((cfaEdge.getSuccessor().getNumLeavingEdges() > 0
                      && cfaEdge.getSuccessor().getLeavingEdge(0).getEdgeType()
                          == CFAEdgeType.FunctionCallEdge)
                  || (cfaEdge.getSuccessor().getNumLeavingEdges() > 1
                      && cfaEdge.getSuccessor().getLeavingEdge(1).getEdgeType()
                          == CFAEdgeType.FunctionCallEdge))) {

            // This AssumedEdge has probably been created by converting a
            // function pointer call into a series of if-else-if-else edges,
            // where there is a single static function call in each branch.
            // If the user wishes, we skip these function calls by not going entering the branches.
            // Of course, we have to go into the else branches.

            if (ignoreInvalidFunctionPointerCalls) {
              if (v1 instanceof InvalidTarget && v2 instanceof NamedFunctionTarget) {
                logger.logfOnce(
                    Level.WARNING,
                    "%s: Assuming function pointer %s"
                        + " with invalid target does not point to %s.",
                    cfaEdge.getFileLocation(),
                    e.getOperand1(),
                    v2);
                return false;
              }
              if (v2 instanceof InvalidTarget && v1 instanceof NamedFunctionTarget) {
                logger.logfOnce(
                    Level.WARNING,
                    "%s: Assuming function pointer %s"
                        + " with invalid target does not point to %s.",
                    cfaEdge.getFileLocation(),
                    e.getOperand2(),
                    v1);
                return false;
              }
            }
            if (ignoreUnknownFunctionPointerCalls) {
              if (v1 instanceof UnknownTarget && v2 instanceof NamedFunctionTarget) {
                logger.logfOnce(
                    Level.WARNING,
                    "%s: Assuming function pointer %s"
                        + " with unknown target does not point to %s.",
                    cfaEdge.getFileLocation(),
                    e.getOperand1(),
                    v2);
                return false;
              }
              if (v2 instanceof UnknownTarget && v1 instanceof NamedFunctionTarget) {
                logger.logfOnce(
                    Level.WARNING,
                    "%s: Assuming function pointer %s"
                        + " with unknown target does not point to %s.",
                    cfaEdge.getFileLocation(),
                    e.getOperand2(),
                    v1);
                return false;
              }
            }
          }
        }
      }
    }
    return true;
  }

  private static String getFunctionPointerCall(CFAEdge pCfaEdge) throws UnrecognizedCodeException {
    if (pCfaEdge.getEdgeType() != CFAEdgeType.StatementEdge) {
      return null;
    }

    CStatement statement = ((CStatementEdge) pCfaEdge).getStatement();
    if (!(statement instanceof CFunctionCall cFunctionCall)) {
      return null;
    }

    CFunctionCallExpression funcCall = cFunctionCall.getFunctionCallExpression();
    CExpression nameExp = funcCall.getFunctionNameExpression();

    if ((nameExp instanceof CIdExpression idExp)
        && (idExp.getExpressionType() instanceof CFunctionType)) {
      // this is a regular function
      return null;
    }

    // functions may be called either as f() or as (*f)(),
    // so remove the star operator if it's there
    if (nameExp instanceof CPointerExpression) {
      nameExp = ((CPointerExpression) nameExp).getOperand();
    }

    if (nameExp instanceof CCastExpression) {
      nameExp = ((CCastExpression) nameExp).getOperand();
    }

    if (nameExp instanceof CIdExpression) {
      // a = f(b) or a = (*f)(b)
      return ((CIdExpression) nameExp).getDeclaration().getQualifiedName();
    } else if (nameExp instanceof CFieldReference) {
      // TODO This is a function pointer call "(s->f)()" or "(s.f)()"
      return null;
    } else if (nameExp instanceof CArraySubscriptExpression) {
      // TODO This is a function pointer call (*a[i])()
      return null;
    } else if (nameExp instanceof CPointerExpression) {
      // TODO double dereference (**f)()
      return null;
    } else {
      throw new UnrecognizedCodeException(
          "unknown function call expression of type " + nameExp.getClass().getSimpleName(),
          pCfaEdge,
          nameExp);
    }
  }

  /**
   * Replaces {@link org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.InvalidTarget}
   * with {@link org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget}.
   */
  private static FunctionPointerTarget abstractInvalidTarget(FunctionPointerTarget pTarget) {
    return (pTarget instanceof InvalidTarget || pTarget instanceof NullTarget)
        ? UnknownTarget.getInstance()
        : pTarget;
  }

  /**
   * Abstract{@link org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.InvalidTarget}
   * to {@link org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget}, but
   * only if {@link #trackInvalidFunctionPointers} is set to <code>true</code>.
   *
   * <p>Abstracting invalid targets to unknown targets allows us to skip these values later in the
   * {@link FunctionPointerState}.
   */
  private FunctionPointerTarget maybeAbstractInvalidTarget(FunctionPointerTarget pTarget) {
    return !trackInvalidFunctionPointers ? abstractInvalidTarget(pTarget) : pTarget;
  }

  private void handleEdge(final FunctionPointerState.Builder newState, CFAEdge pCfaEdge)
      throws CPATransferException {

    switch (pCfaEdge.getEdgeType()) {
      case DeclarationEdge -> {
        // declaration of a function pointer.
        CDeclarationEdge declEdge = (CDeclarationEdge) pCfaEdge;
        handleDeclaration(newState, declEdge);
        // if edge is a statement edge, e.g. a = b + c
      }
      case StatementEdge -> {
        CStatementEdge statementEdge = (CStatementEdge) pCfaEdge;
        handleStatement(newState, statementEdge.getStatement(), pCfaEdge);
      }
      case FunctionCallEdge -> {
        CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pCfaEdge;
        handleFunctionCall(newState, functionCallEdge);
      }
      case ReturnStatementEdge -> {
        CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge) pCfaEdge;
        handleReturnStatement(newState, returnStatementEdge.asAssignment(), pCfaEdge);
      }
      case FunctionReturnEdge -> {
        CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) pCfaEdge;
        handleFunctionReturn(newState, functionReturnEdge);
        // maybe two function pointers are compared.
      }
      case AssumeEdge -> {
        // nothing to do.
      }
      case BlankEdge, CallToReturnEdge -> {}
    }
  }

  private void handleDeclaration(FunctionPointerState.Builder pNewState, CDeclarationEdge declEdge)
      throws UnrecognizedCodeException {

    if (!(declEdge.getDeclaration() instanceof CVariableDeclaration decl)) {
      // not a variable declaration
      return;
    }

    // get name of declaration
    String name = decl.getQualifiedName();

    // get initial value
    FunctionPointerTarget initialValue = InvalidTarget.getInstance();

    if (decl.getInitializer() != null) {
      CInitializer init = decl.getInitializer();
      if (init instanceof CInitializerExpression cInitializerExpression) {
        initialValue = getValue(cInitializerExpression.getExpression(), pNewState);
      }
    }

    // store declaration in abstract state
    pNewState.setTarget(name, maybeAbstractInvalidTarget(initialValue));
  }

  private void handleStatement(
      FunctionPointerState.Builder pNewState, CStatement pStatement, CFAEdge pCfaEdge)
      throws UnrecognizedCodeException {

    // TODO: Handle calls like "int r = atexit(argExpr)" that don't ignore the return value
    if (pStatement instanceof CFunctionCallAssignmentStatement callAssignStmt
        && callAssignStmt.getLeftHandSide() instanceof CIdExpression leftSide
        && callAssignStmt.getRightHandSide().getFunctionNameExpression()
            instanceof CIdExpression fnExpr
        && fnExpr.getName().equals("__CPACHECKER_atexit_next")) {
      // We found a call "x = __CPA_CHECKER_atexit_next()":
      // Get the target for the function pointer returned by __CPACHECKER_atexit_next() from the
      // atexit stack and store it in "x".
      // Then remove the last element from the stack.
      String varName = getLeftHandSide(leftSide, pCfaEdge);
      FunctionPointerTarget target = pNewState.popTarget();
      pNewState.setTarget(varName, target);

    } else if (pStatement instanceof CAssignment cAssignment) {
      // assignment like "a = b" or "a = foo()"
      handleAssignment(pNewState, cAssignment, pCfaEdge);

    } else if (pStatement instanceof CFunctionCallStatement callStmt
        && callStmt.getFunctionCallExpression().getFunctionNameExpression()
            instanceof CIdExpression fnExpr
        && fnExpr.getName().equals("atexit")) {
      // We've found a statement "atexit(<argExpr>)":
      // Evaluate <argExpr> to get a target for the function pointer and store it on the stack
      List<CExpression> params = callStmt.getFunctionCallExpression().getParameterExpressions();
      Preconditions.checkArgument(
          params.size() == 1,
          "atexit() takes one argument, but it was called with %s",
          params.size());
      CExpression argExpr = params.get(0);
      ExpressionValueVisitor evaluator = new ExpressionValueVisitor(pNewState);
      FunctionPointerTarget target = argExpr.accept(evaluator);
      // Note: We want AtExitState.peek() to only return NullTarget when the stack is actually
      // empty. Because of this we have to use abstractInvalidTarget() here to make sure no
      // NullTarget can be pushed onto the stack by calling atexit(0). The call to
      // abstractInvalidTarget() makes sure that in such cases the target is always replaced by
      // UnknowTarget before being pushed onto the stack.
      pNewState.pushTarget(abstractInvalidTarget(target));

    } else if (pStatement instanceof CFunctionCallStatement) {
      // external function call without return value

    } else if (pStatement instanceof CExpressionStatement) {
      // side-effect free statement

    } else {
      throw new UnrecognizedCodeException("unknown statement", pCfaEdge, pStatement);
    }
  }

  private void handleAssignment(
      FunctionPointerState.Builder pNewState, CAssignment assignment, CFAEdge pCfaEdge)
      throws UnrecognizedCodeException {
    String varName = getLeftHandSide(assignment.getLeftHandSide(), pCfaEdge);

    if (varName != null) {
      FunctionPointerTarget target = getValue(assignment.getRightHandSide(), pNewState);
      pNewState.setTarget(varName, maybeAbstractInvalidTarget(target));
    }
  }

  private void handleFunctionCall(
      FunctionPointerState.Builder pNewState, CFunctionCallEdge callEdge)
      throws UnrecognizedCodeException {

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();

    List<CParameterDeclaration> formalParams = functionEntryNode.getFunctionParameters();
    List<CExpression> arguments = callEdge.getArguments();

    if (functionEntryNode.getFunctionDefinition().getType().takesVarArgs()) {
      if (formalParams.size() > arguments.size()) {
        throw new UnrecognizedCodeException(
            "Number of parameters on function call does not match function definition", callEdge);
      }

    } else {
      if (formalParams.size() != arguments.size()) {
        throw new UnrecognizedCodeException(
            "Number of parameters on function call does not match function definition", callEdge);
      }
    }

    // used to get value in caller context
    ExpressionValueVisitor v = new ExpressionValueVisitor(pNewState);

    for (int i = 0; i < formalParams.size(); i++) {
      String paramName = formalParams.get(i).getQualifiedName();
      CExpression actualArgument = arguments.get(i);

      FunctionPointerTarget target = actualArgument.accept(v);
      pNewState.setTarget(paramName, maybeAbstractInvalidTarget(target));

      // TODO only do this if declared type is function pointer?
    }
  }

  private void handleReturnStatement(
      FunctionPointerState.Builder pNewState,
      Optional<CAssignment> returnStatement,
      CFAEdge pCfaEdge)
      throws UnrecognizedCodeException {

    if (returnStatement.isPresent()) {
      handleAssignment(pNewState, returnStatement.orElseThrow(), pCfaEdge);
    }
  }

  private void handleFunctionReturn(
      FunctionPointerState.Builder pNewState, CFunctionReturnEdge pFunctionReturnEdge)
      throws UnrecognizedCodeException {
    CFunctionSummaryEdge summaryEdge = pFunctionReturnEdge.getSummaryEdge();
    assert summaryEdge != null;

    CFunctionCall funcCall = pFunctionReturnEdge.getFunctionCall();
    if (funcCall instanceof CFunctionCallAssignmentStatement cFunctionCallAssignmentStatement) {
      CExpression left = cFunctionCallAssignmentStatement.getLeftHandSide();
      String varName = getLeftHandSide(left, summaryEdge);
      if (varName != null) {
        Optional<CVariableDeclaration> returnValue =
            pFunctionReturnEdge.getFunctionEntry().getReturnVariable();
        if (returnValue.isPresent()) {
          FunctionPointerTarget target =
              pNewState.getTarget(returnValue.orElseThrow().getQualifiedName());
          pNewState.setTarget(varName, target);
        } else {
          pNewState.setTarget(varName, UnknownTarget.getInstance());
        }
      }
    }

    // clear all local variables of inner function
    String calledFunction = pFunctionReturnEdge.getPredecessor().getFunctionName();
    pNewState.clearVariablesForFunction(calledFunction);
  }

  private String getLeftHandSide(CExpression lhsExpression, CFAEdge edge)
      throws UnrecognizedCodeException {

    if (lhsExpression instanceof CIdExpression cIdExpression) {
      // a = ...
      return cIdExpression.getDeclaration().getQualifiedName();

    } else if (lhsExpression instanceof CPointerExpression) {
      // *a = ...
      // TODO: Support this statement.

    } else if (lhsExpression instanceof CFieldReference) {

      // String functionName = pCfaEdge.getPredecessor().getFunctionName();
      // handleAssignmentToVariable(op1.getRawSignature(), op2, v);

      // TODO: Support this statement.

    } else if (lhsExpression instanceof CArraySubscriptExpression arrayExp) {
      if (arrayExp.getArrayExpression() instanceof CIdExpression
          && arrayExp.getSubscriptExpression() instanceof CIntegerLiteralExpression) {
        return arrayElementVariable(arrayExp);
      }

    } else {
      throw new UnrecognizedCodeException(
          "left operand of assignment has to be a variable", edge, lhsExpression);
    }
    return null;
  }

  private FunctionPointerTarget getValue(CRightHandSide exp, FunctionPointerState.Builder element)
      throws UnrecognizedCodeException {
    return exp.accept(new ExpressionValueVisitor(element));
  }

  static String arrayElementVariable(CArraySubscriptExpression exp) {
    Preconditions.checkArgument(exp.getSubscriptExpression() instanceof CIntegerLiteralExpression);
    return ((CIdExpression) exp.getArrayExpression()).getDeclaration().getQualifiedName()
        + "["
        + exp.getSubscriptExpression().toASTString()
        + "]";
  }
}
