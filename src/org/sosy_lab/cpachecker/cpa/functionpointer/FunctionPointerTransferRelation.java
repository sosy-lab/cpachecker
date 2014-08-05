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
package org.sosy_lab.cpachecker.cpa.functionpointer;

import java.util.Collection;
import java.util.List;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.FunctionPointerTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.InvalidTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

@Options(prefix="cpa.functionpointer")
class FunctionPointerTransferRelation implements TransferRelation {

  private static final String FUNCTION_RETURN_VARIABLE = "__cpachecker_return_var";

  @Option(description="whether function pointers with invalid targets (e.g., 0) should be tracked in order to find calls to such pointers")
  private boolean trackInvalidFunctionPointers = false;
  private final FunctionPointerTarget invalidFunctionPointerTarget;

  @Option(description="When an invalid function pointer is called, do not assume all functions as possible targets and instead call no function.")
  private boolean ignoreInvalidFunctionPointerCalls = false;

  @Option(description="When an unknown function pointer is called, do not assume all functions as possible targets and instead call no function (this is unsound).")
  private boolean ignoreUnknownFunctionPointerCalls = false;

  private final LogManagerWithoutDuplicates logger;

  FunctionPointerTransferRelation(LogManager pLogger, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);

    invalidFunctionPointerTarget = trackInvalidFunctionPointers
                                   ? InvalidTarget.getInstance()
                                   : UnknownTarget.getInstance();

    if (ignoreInvalidFunctionPointerCalls && !trackInvalidFunctionPointers) {
      throw new InvalidConfigurationException(
          "FunctionPointerCPA cannot ignore invalid function pointer calls " +
          "when such pointers are not tracked, " +
          "please set cpa.functionpointer.trackInvalidFunctionPointers=true");
    }
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    final FunctionPointerState oldState = (FunctionPointerState)pElement;

    //check assumptions about function pointers, like p == &h, where p is a function pointer, h  is a function
    if (!shouldGoByEdge(oldState, pCfaEdge)) {
      //should not go by the edge
      return ImmutableSet.of();//results is a empty set
    }

    // print warning if we go by the default edge of a function pointer call
    // (i.e., the edge for the case where we don't have information about the target).
    String functionCallVariable = getFunctionPointerCall(pCfaEdge);
    if (functionCallVariable != null) {
      FunctionPointerTarget target = oldState.getTarget(functionCallVariable);
      if (target instanceof NamedFunctionTarget) {
        String functionName = ((NamedFunctionTarget)target).getFunctionName();
        logger.logfOnce(Level.WARNING, "%s: Function pointer %s points to %s,"
            + " but no corresponding call edge was created during preprocessing."
            + " Ignoring function pointer call: %s",
            pCfaEdge.getFileLocation(), functionCallVariable, functionName, pCfaEdge.getDescription());
      } else {
        logger.logfOnce(Level.WARNING, "%s: Ignoring call via function pointer %s"
            + " for which no suitable target was found in line: %s",
            pCfaEdge.getFileLocation(), functionCallVariable, pCfaEdge.getDescription());
      }
    }

    // now handle the edge
    FunctionPointerState.Builder newState = oldState.createBuilder();
    handleEdge(newState, pCfaEdge);

    return ImmutableSet.of(newState.build());
  }

  private boolean shouldGoByEdge(FunctionPointerState oldState, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    if (cfaEdge.getEdgeType()==CFAEdgeType.AssumeEdge) {
      CAssumeEdge a = (CAssumeEdge)cfaEdge;
      CExpression exp = a.getExpression();
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      if (exp instanceof CBinaryExpression) {
        CBinaryExpression e = (CBinaryExpression)exp;
        BinaryOperator op = e.getOperator();
        if (op == BinaryOperator.EQUALS) {
          FunctionPointerState.Builder newState = oldState.createBuilder();
          FunctionPointerTarget v1 = getValue(e.getOperand1(), newState, functionName);
          FunctionPointerTarget v2 = getValue(e.getOperand2(), newState, functionName);
          logger.log(Level.ALL, "Operand1 value is", v1);
          logger.log(Level.ALL, "Operand2 value is", v2);
          if (v1 instanceof NamedFunctionTarget
              && v2 instanceof NamedFunctionTarget) {
            boolean eq = v1.equals(v2);
            if (eq != a.getTruthAssumption()) {
              logger.log(Level.FINE, "Should not go by the edge", a);
              return false;//should not go by this edge
            } else {
              logger.log(Level.FINE, "Should go by the edge", a);
              return true;
            }
          }
          if (a.getTruthAssumption()
              && (cfaEdge.getSuccessor().getNumLeavingEdges() > 0
                  && cfaEdge.getSuccessor().getLeavingEdge(0).getEdgeType() == CFAEdgeType.FunctionCallEdge
                  || cfaEdge.getSuccessor().getNumLeavingEdges() > 1
                  && cfaEdge.getSuccessor().getLeavingEdge(1).getEdgeType() == CFAEdgeType.FunctionCallEdge)) {

            // This AssumedEdge has probably been created by converting a
            // function pointer call into a series of if-else-if-else edges,
            // where there is a single static function call in each branch.
            // If the user wishes, we skip these function calls by not going entering the branches.
            // Of course we have to go into the else branches.

            if (ignoreInvalidFunctionPointerCalls) {
              if (v1 instanceof InvalidTarget && v2 instanceof NamedFunctionTarget) {
                logger.logfOnce(Level.WARNING, "%s: Assuming function pointer %s"
                    + " with invalid target does not point to %s.",
                    cfaEdge.getFileLocation(), e.getOperand1(), v2);
                return false;
              }
              if (v2 instanceof InvalidTarget && v1 instanceof NamedFunctionTarget) {
                logger.logfOnce(Level.WARNING, "%s: Assuming function pointer %s"
                    + " with invalid target does not point to %s.",
                    cfaEdge.getFileLocation(), e.getOperand2(), v1);
                return false;
              }
            }
            if (ignoreUnknownFunctionPointerCalls) {
              if (v1 instanceof UnknownTarget && v2 instanceof NamedFunctionTarget) {
                logger.logfOnce(Level.WARNING, "%s: Assuming function pointer %s"
                    + " with unknown target does not point to %s.",
                    cfaEdge.getFileLocation(), e.getOperand1(), v2);
                return false;
              }
              if (v2 instanceof UnknownTarget && v1 instanceof NamedFunctionTarget) {
                logger.logfOnce(Level.WARNING, "%s: Assuming function pointer %s"
                    + " with unknown target does not point to %s.",
                    cfaEdge.getFileLocation(), e.getOperand2(), v1);
                return false;
              }
            }
          }
        }
      }
    }
    return true;
  }

  private String getFunctionPointerCall(CFAEdge pCfaEdge) throws UnrecognizedCCodeException {
    if (pCfaEdge.getEdgeType() != CFAEdgeType.StatementEdge) {
      return null;
    }

    CStatement statement = ((CStatementEdge)pCfaEdge).getStatement();
    if (!(statement instanceof CFunctionCall)) {
      return null;
    }

    CFunctionCallExpression funcCall = ((CFunctionCall)statement).getFunctionCallExpression();
    CExpression nameExp = funcCall.getFunctionNameExpression();
    String currentFunction = pCfaEdge.getPredecessor().getFunctionName();

    if (nameExp instanceof CIdExpression) {
      CIdExpression idExp = (CIdExpression)nameExp;
      if (idExp.getExpressionType() instanceof CFunctionType) {
        // this is a regular function
        return null;
      }
    }

    // functions may be called either as f() or as (*f)(),
    // so remove the star operator if its there
    if (nameExp instanceof CPointerExpression) {
      nameExp = ((CPointerExpression)nameExp).getOperand();
    }

    if (nameExp instanceof CCastExpression) {
      nameExp = ((CCastExpression) nameExp).getOperand();
    }

    if (nameExp instanceof CIdExpression) {
      // a = f(b) or a = (*f)(b)
      return scopedIfNecessary((CIdExpression)nameExp, currentFunction);
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
      throw new UnrecognizedCCodeException("unknown function call expression of type " + nameExp.getClass().getSimpleName(), pCfaEdge, nameExp);
    }
  }

  private void handleEdge(final FunctionPointerState.Builder newState, CFAEdge pCfaEdge) throws CPATransferException {

    switch (pCfaEdge.getEdgeType()) {

      // declaration of a function pointer.
      case DeclarationEdge: {
        CDeclarationEdge declEdge = (CDeclarationEdge) pCfaEdge;
        handleDeclaration(newState, declEdge);
        break;
      }

      // if edge is a statement edge, e.g. a = b + c
      case StatementEdge: {
        CStatementEdge statementEdge = (CStatementEdge) pCfaEdge;
        handleStatement(newState, statementEdge.getStatement(), pCfaEdge);
        break;
      }

      case FunctionCallEdge: {
        CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pCfaEdge;
        handleFunctionCall(newState, functionCallEdge);
        break;
      }

      case ReturnStatementEdge: {
        CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge)pCfaEdge;
        handleReturnStatement(newState, returnStatementEdge.getExpression(), pCfaEdge);
        break;
      }

      case FunctionReturnEdge: {
        CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) pCfaEdge;
        handleFunctionReturn(newState, functionReturnEdge);
        break;
      }

      // maybe two function pointers are compared.
      case AssumeEdge: {
        break;
      }

      // nothing to do.
      case BlankEdge:
      case CallToReturnEdge: {
        break;
      }

      case MultiEdge: {
        for (CFAEdge currentEdge : ((MultiEdge)pCfaEdge).getEdges()) {
          handleEdge(newState, currentEdge);
        }
        break;
      }

      default:
        throw new UnrecognizedCFAEdgeException(pCfaEdge);
    }
  }

  private void handleDeclaration(FunctionPointerState.Builder pNewState, CDeclarationEdge declEdge) throws UnrecognizedCCodeException {

    if (!(declEdge.getDeclaration() instanceof CVariableDeclaration)) {
      // not a variable declaration
      return;
    }
    CVariableDeclaration decl = (CVariableDeclaration)declEdge.getDeclaration();

    String functionName = declEdge.getPredecessor().getFunctionName();

    // get name of declaration
    String name = decl.getQualifiedName();

    // get initial value
    FunctionPointerTarget initialValue = invalidFunctionPointerTarget;

    if (decl.getInitializer() != null) {
      CInitializer init = decl.getInitializer();
      if (init instanceof CInitializerExpression) {
        initialValue = getValue(((CInitializerExpression) init).getExpression(), pNewState, functionName);
      }
    }

    // store declaration in abstract state
    pNewState.setTarget(name, initialValue);
  }

  private void handleStatement(FunctionPointerState.Builder pNewState, CStatement pStatement,
        CFAEdge pCfaEdge) throws UnrecognizedCCodeException {

    if (pStatement instanceof CAssignment) {
      // assignment like "a = b" or "a = foo()"
      String functionName = pCfaEdge.getPredecessor().getFunctionName();

      CAssignment assignment = (CAssignment)pStatement;
      String varName = getLeftHandSide(assignment.getLeftHandSide(), pCfaEdge, functionName);

      if (varName != null) {
        FunctionPointerTarget target = getValue(assignment.getRightHandSide(), pNewState, functionName);
        pNewState.setTarget(varName, target);
      }

    } else if (pStatement instanceof CFunctionCallStatement) {
      // external function call without return value

    } else if (pStatement instanceof CExpressionStatement) {
      // side-effect free statement

    } else {
      throw new UnrecognizedCCodeException("unknown statement", pCfaEdge, pStatement);
    }
  }

  private void handleFunctionCall(FunctionPointerState.Builder pNewState, CFunctionCallEdge callEdge) throws UnrecognizedCCodeException {

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<CParameterDeclaration> formalParams = functionEntryNode.getFunctionParameters();
    List<CExpression> arguments = callEdge.getArguments();

    if (functionEntryNode.getFunctionDefinition().getType().takesVarArgs()) {
      if (formalParams.size() > arguments.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", callEdge);
      }

    } else {
      if (formalParams.size() != arguments.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", callEdge);
      }
    }

    // used to get value in caller context
    ExpressionValueVisitor v = new ExpressionValueVisitor(pNewState, callerFunctionName, invalidFunctionPointerTarget);

    for (int i=0; i < formalParams.size(); i++) {
      String paramName = formalParams.get(i).getQualifiedName();
      CExpression actualArgument = arguments.get(i);

      FunctionPointerTarget target = actualArgument.accept(v);
      pNewState.setTarget(paramName, target);

      // TODO only do this if declared type is function pointer?
    }
  }

  private void handleReturnStatement(FunctionPointerState.Builder pNewState,
      Optional<CExpression> returnValue,
      CFAEdge pCfaEdge) throws UnrecognizedCCodeException {

    if (returnValue.isPresent()) {
      String functionName = pCfaEdge.getPredecessor().getFunctionName();
      FunctionPointerTarget target = getValue(returnValue.get(), pNewState, functionName);

      pNewState.setTarget(FUNCTION_RETURN_VARIABLE, target);
    }
  }


  private void handleFunctionReturn(FunctionPointerState.Builder pNewState, CFunctionReturnEdge pFunctionReturnEdge) throws UnrecognizedCCodeException {
    CFunctionSummaryEdge summaryEdge = pFunctionReturnEdge.getSummaryEdge();
    assert summaryEdge != null;

    CFunctionCall funcCall = summaryEdge.getExpression();
    if (funcCall instanceof CFunctionCallAssignmentStatement) {

      CExpression left = ((CFunctionCallAssignmentStatement)funcCall).getLeftHandSide();

      String callerFunction = summaryEdge.getSuccessor().getFunctionName();
      String varName = getLeftHandSide(left, summaryEdge, callerFunction);

      if (varName != null) {

        FunctionPointerTarget target = pNewState.getTarget(FUNCTION_RETURN_VARIABLE);
        pNewState.setTarget(varName, target);
      }
    }

    // clear special variable
    pNewState.setTarget(FUNCTION_RETURN_VARIABLE, UnknownTarget.getInstance());

    // clear all local variables of inner function
    String calledFunction = pFunctionReturnEdge.getPredecessor().getFunctionName();
    pNewState.clearVariablesWithPrefix(calledFunction + "::");
  }

  private String getLeftHandSide(CExpression lhsExpression, CFAEdge edge, String functionName) throws UnrecognizedCCodeException {

    if (lhsExpression instanceof CIdExpression) {
      // a = ...
      return scopedIfNecessary((CIdExpression)lhsExpression, functionName);

    } else if (lhsExpression instanceof CPointerExpression) {
      // *a = ...
      // TODO: Support this statement.

    } else if (lhsExpression instanceof CFieldReference) {

      //String functionName = pCfaEdge.getPredecessor().getFunctionName();
      //handleAssignmentToVariable(op1.getRawSignature(), op2, v);

      // TODO: Support this statement.

    } else if (lhsExpression instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arrayExp = (CArraySubscriptExpression)lhsExpression;
      if (arrayExp.getArrayExpression() instanceof CIdExpression
          && arrayExp.getSubscriptExpression() instanceof CIntegerLiteralExpression) {
        return arrayElementVariable(arrayExp, functionName);
      }

    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", edge, lhsExpression);
    }
    return null;
  }

  private FunctionPointerTarget getValue(CRightHandSide exp, FunctionPointerState.Builder element, String function) throws UnrecognizedCCodeException {
    return exp.accept(new ExpressionValueVisitor(element, function, invalidFunctionPointerTarget));
  }

  private static class ExpressionValueVisitor extends DefaultCExpressionVisitor<FunctionPointerTarget, UnrecognizedCCodeException>
                                              implements CRightHandSideVisitor<FunctionPointerTarget, UnrecognizedCCodeException> {

    private final FunctionPointerState.Builder state;
    private final String function;
    private final FunctionPointerTarget targetForInvalidPointers;

    private ExpressionValueVisitor(FunctionPointerState.Builder pElement, String pFunction,
                                   FunctionPointerTarget pTargetForInvalidPointers) {
      state = pElement;
      function = pFunction;
      targetForInvalidPointers = pTargetForInvalidPointers;
    }

    @Override
    public FunctionPointerTarget visit(CArraySubscriptExpression pE) throws UnrecognizedCCodeException {
      if (pE.getSubscriptExpression() instanceof CIntegerLiteralExpression
          && pE.getArrayExpression() instanceof CIdExpression) {

        return state.getTarget(arrayElementVariable(pE, function));
      }
      return super.visit(pE);
    }

    @Override
    public FunctionPointerTarget visit(CUnaryExpression pE) {
      if ((pE.getOperator() == UnaryOperator.AMPER) && (pE.getOperand() instanceof CIdExpression)) {
        return extractFunctionId((CIdExpression)pE.getOperand());
      }
      return visitDefault(pE);
    }

    @Override
    public FunctionPointerTarget visit(CPointerExpression pE) {
      if (pE.getOperand() instanceof CIdExpression) {
        return extractFunctionId((CIdExpression)pE.getOperand());
      }
      return visitDefault(pE);
    }

    private FunctionPointerTarget extractFunctionId(CIdExpression operand) {
      if ( (operand.getDeclaration()!=null && operand.getDeclaration().getType() instanceof CFunctionType)
        || (operand.getExpressionType() instanceof CFunctionType)) {
        return new NamedFunctionTarget(operand.getName());
      }
      if (operand.getExpressionType() instanceof CPointerType) {
        CPointerType t = (CPointerType)operand.getExpressionType();
        if (t.getType() instanceof CFunctionType) {
          return state.getTarget(scopedIfNecessary(operand, function));
        }
      }
      return visitDefault(operand);
    }

    @Override
    public FunctionPointerTarget visit(CIdExpression pE) {
      if (pE.getDeclaration() instanceof CFunctionDeclaration
          || pE.getExpressionType() instanceof CFunctionType) {
        return new NamedFunctionTarget(pE.getName());
      }

      return state.getTarget(scopedIfNecessary(pE, function));
    }

    @Override
    public FunctionPointerTarget visit(CCastExpression pE) throws UnrecognizedCCodeException {
      return pE.getOperand().accept(this);
    }

    @Override
    public FunctionPointerTarget visit(CComplexCastExpression pE) throws UnrecognizedCCodeException {
      // evaluation of complex numbers is not supported by now
      return UnknownTarget.getInstance();
    }

    @Override
    protected FunctionPointerTarget visitDefault(CExpression pExp) {
      return UnknownTarget.getInstance();
    }

    @Override
    public FunctionPointerTarget visit(CFunctionCallExpression pIastFunctionCallExpression) {
      return UnknownTarget.getInstance();
    }

    @Override
    public FunctionPointerTarget visit(CCharLiteralExpression pE) {
      return targetForInvalidPointers;
    }

    @Override
    public FunctionPointerTarget visit(CFloatLiteralExpression pE) {
      return targetForInvalidPointers;
    }

    @Override
    public FunctionPointerTarget visit(CIntegerLiteralExpression pE) {
      return targetForInvalidPointers;
    }

    @Override
    public FunctionPointerTarget visit(CStringLiteralExpression pE) {
      return targetForInvalidPointers;
    }

    @Override
    public FunctionPointerTarget visit(CImaginaryLiteralExpression pE) {
      return targetForInvalidPointers;
    }
  }

  // looks up the variable in the current namespace
  private static String scopedIfNecessary(CIdExpression var, String function) {
    return var.getDeclaration().getQualifiedName();
  }

  private static String arrayElementVariable(CArraySubscriptExpression exp, String function) {
    assert exp.getSubscriptExpression() instanceof CIntegerLiteralExpression;
    String name = scopedIfNecessary((CIdExpression)exp.getArrayExpression(), function);
    name += "[" + exp.getSubscriptExpression().toASTString() + "]";
    return name;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {
    // in this method we could access the abstract domains of other CPAs
    // if required.
    return null;
  }
}
