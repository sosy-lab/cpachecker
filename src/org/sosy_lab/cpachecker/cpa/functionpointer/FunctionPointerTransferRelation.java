/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.DefaultExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.StorageClass;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerElement.FunctionPointerTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerElement.InvalidTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerElement.NamedFunctionTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerElement.UnknownTarget;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

@Options(prefix="cpa.functionpointer")
class FunctionPointerTransferRelation implements TransferRelation {

  private static final String FUNCTION_RETURN_VARIABLE = "__cpachecker_return_var";

  @Option(description="whether function pointers with invalid targets (e.g., 0) should be tracked in order to find calls to such pointers")
  private boolean trackInvalidFunctionPointers = false;
  private final FunctionPointerTarget invalidFunctionPointerTarget;

  private final TransferRelation wrappedTransfer;
  private final CFA functions;
  private final LogManager logger;

  FunctionPointerTransferRelation(TransferRelation pWrappedTransfer, CFA pCfa, LogManager pLogger, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    wrappedTransfer = pWrappedTransfer;
    functions = pCfa;
    logger = pLogger;

    invalidFunctionPointerTarget = trackInvalidFunctionPointers
                                   ? InvalidTarget.getInstance()
                                   : UnknownTarget.getInstance();
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    final FunctionPointerElement oldState = (FunctionPointerElement)pElement;
    Collection<FunctionPointerElement> results;

    if (pCfaEdge == null) {
      CFANode node = extractLocation(oldState);
      results = new ArrayList<FunctionPointerElement>(node.getNumLeavingEdges());

      for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges(); edgeIdx++) {
        CFAEdge edge = node.getLeavingEdge(edgeIdx);
        if (!(edge instanceof FunctionPointerCallEdge)) {
          // ignore FunctionPointerCallEdges, they are from previous passes
          getAbstractSuccessorForEdge(oldState, pPrecision, edge, results);
        }
      }

    } else {
      results = new ArrayList<FunctionPointerElement>(1);
      getAbstractSuccessorForEdge(oldState, pPrecision, pCfaEdge, results);

    }
    return results;
  }

  private void getAbstractSuccessorForEdge(
      FunctionPointerElement oldState, Precision pPrecision, CFAEdge pCfaEdge, Collection<FunctionPointerElement> results)
      throws CPATransferException, InterruptedException {
    CFAEdge cfaEdge;

    // first, check if this is a function pointer call
    String functionCallVariable = getFunctionPointerCall(pCfaEdge);
    if (functionCallVariable != null) {
      // this is indeed a function call via a function pointer

      FunctionPointerTarget target = oldState.getTarget(functionCallVariable);
      if (target instanceof NamedFunctionTarget) {
        String functionName = ((NamedFunctionTarget)target).getFunctionName();
        CFAFunctionDefinitionNode fDefNode = functions.getFunctionHead(functionName);
        if (fDefNode != null) {
          logger.log(Level.FINEST, "Function pointer", functionCallVariable, "points to", target, "while it is used.");

          StatementEdge edge = (StatementEdge)pCfaEdge;
          IASTFunctionCall functionCall = (IASTFunctionCall)edge.getStatement();
          CFANode predecessorNode = edge.getPredecessor();
          CFANode successorNode = edge.getSuccessor();
          IASTFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
          int lineNumber = edge.getLineNumber();

          CFAFunctionExitNode fExitNode = fDefNode.getExitNode();

          List<IASTExpression> parameters = functionCallExpression.getParameterExpressions();

          // Create new edges.
          CallToReturnEdge calltoReturnEdge = new CallToReturnEdge(functionCall.asStatement().toASTString(), lineNumber, predecessorNode, successorNode, functionCall);

          FunctionPointerCallEdge callEdge = new FunctionPointerCallEdge(functionCallExpression.toASTString(), edge.getStatement(), lineNumber, predecessorNode, (FunctionDefinitionNode)fDefNode, parameters, calltoReturnEdge);
          predecessorNode.addLeavingEdge(callEdge);
          fDefNode.addEnteringEdge(callEdge);

          if (fExitNode.getNumEnteringEdges() > 0) {
            FunctionPointerReturnEdge returnEdge = new FunctionPointerReturnEdge("Return Edge to " + successorNode.getNodeNumber(), lineNumber, fExitNode, successorNode, callEdge, calltoReturnEdge);
            fExitNode.addLeavingEdge(returnEdge);
            successorNode.addEnteringEdge(returnEdge);

          } else {
            // exit node of called functions is not reachable, i.e. this function never returns
            // no need to add return edges
          }

          // now substitute the real edge with the fake edge
          cfaEdge = callEdge;
        } else {
          throw new UnrecognizedCCodeException("function pointer points to unknown function " + functionName, pCfaEdge);
        }

      } else if (target instanceof UnknownTarget) {
        // we known nothing, so just keep the old edge
        cfaEdge = pCfaEdge;

      } else if (target instanceof InvalidTarget) {
        throw new UnrecognizedCCodeException("function pointer points to invalid memory address", pCfaEdge);
      } else {
        throw new AssertionError();
      }

    } else {
      // use the real edge
      cfaEdge = pCfaEdge;
    }

    // Some CPAs rely on the call-to-return edge when processing the return edge.
    // We add it here to the CFA and remove it before returning from this function.
    if (cfaEdge instanceof FunctionPointerReturnEdge) {
      CallToReturnEdge calltoReturnEdge = ((FunctionPointerReturnEdge) cfaEdge).getSummaryEdge();
      calltoReturnEdge.getPredecessor().addLeavingSummaryEdge(calltoReturnEdge);
      calltoReturnEdge.getSuccessor().addEnteringSummaryEdge(calltoReturnEdge);
    }

    // now handle the edge, whether it is real or not
    Collection<? extends AbstractElement> newWrappedStates = wrappedTransfer.getAbstractSuccessors(oldState.getWrappedElement(), pPrecision, cfaEdge);

    for (AbstractElement newWrappedState : newWrappedStates) {
      FunctionPointerElement newState = oldState.createDuplicateWithNewWrappedElement(newWrappedState);

      newState = handleEdge(newState, cfaEdge);

      if (newState != null) {
        results.add(newState);
      }
    }

    if (pCfaEdge instanceof FunctionPointerReturnEdge) {
      // We are returning from a function that was called via a function pointer
      // Remove all fake edges we have created.

      FunctionPointerReturnEdge returnEdge = (FunctionPointerReturnEdge)pCfaEdge;

      // The call edge and the return edge are never removed from the CFA,
      // because we might need them for refinement.
      // CallstackCPA should force taking the right return edge.

      CFACreationUtils.removeSummaryEdgeFromNodes(returnEdge.getSummaryEdge());
    }
  }

  private String getFunctionPointerCall(CFAEdge pCfaEdge) throws UnrecognizedCCodeException {
    if (pCfaEdge.getEdgeType() != CFAEdgeType.StatementEdge) {
      return null;
    }

    IASTStatement statement = ((StatementEdge)pCfaEdge).getStatement();
    if (!(statement instanceof IASTFunctionCall)) {
      return null;
    }

    IASTFunctionCallExpression funcCall = ((IASTFunctionCall)statement).getFunctionCallExpression();
    IASTExpression nameExp = funcCall.getFunctionNameExpression();
    String currentFunction = pCfaEdge.getPredecessor().getFunctionName();

    // functions may be called either as f() or as (*f)(),
    // so remove the star operator if its there
    if (nameExp instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExp = (IASTUnaryExpression)nameExp;
      if (unaryExp.getOperator() == UnaryOperator.STAR) {
        // a = (*f)(b)
        nameExp = unaryExp.getOperand();

      } else {
        throw new UnrecognizedCCodeException("unknown function call expression", pCfaEdge, nameExp);
      }
    }

    if (nameExp instanceof IASTIdExpression) {
      // a = f(b) or a = (*f)(b)
      return scopedIfNecessary((IASTIdExpression)nameExp, currentFunction);
    } else if (nameExp instanceof IASTFieldReference) {
      // TODO This is a function pointer call "(s->f)()" or "(s.f)()"
      return null;
    } else {
      throw new UnrecognizedCCodeException("unknown function call expression", pCfaEdge, nameExp);
    }
  }

  private FunctionPointerElement handleEdge(FunctionPointerElement newState, CFAEdge pCfaEdge) throws CPATransferException {

    switch(pCfaEdge.getEdgeType()) {

      // declaration of a function pointer.
      case DeclarationEdge: {
        DeclarationEdge declEdge = (DeclarationEdge) pCfaEdge;
        handleDeclaration(newState, declEdge);
        break;
      }

      // if edge is a statement edge, e.g. a = b + c
      case StatementEdge: {
        StatementEdge statementEdge = (StatementEdge) pCfaEdge;
        handleStatement(newState, statementEdge.getStatement(), pCfaEdge);
        break;
      }

      case FunctionCallEdge: {
        FunctionCallEdge functionCallEdge = (FunctionCallEdge) pCfaEdge;
        handleFunctionCall(newState, functionCallEdge);
        break;
      }

      case ReturnStatementEdge: {
        ReturnStatementEdge returnStatementEdge = (ReturnStatementEdge)pCfaEdge;
        handleReturnStatement(newState, returnStatementEdge.getExpression(), pCfaEdge);
        break;
      }

      case FunctionReturnEdge: {
        FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) pCfaEdge;
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

      default:
        throw new UnrecognizedCFAEdgeException(pCfaEdge);
    }

    return newState;
  }

  private void handleDeclaration(FunctionPointerElement pNewState, DeclarationEdge declEdge) throws UnrecognizedCCodeException {

    if (declEdge.getStorageClass() != StorageClass.AUTO) {
      // not a variable declaration
      return;
    }

    String functionName = declEdge.getPredecessor().getFunctionName();

    // get name of declaration
    String name = declEdge.getName();
    if (name == null) {
      // not a variable declaration
      return;
    }
    if (!declEdge.isGlobal()) {
      name = scoped(name, functionName);
    }

    // get initial value
    FunctionPointerTarget initialValue = invalidFunctionPointerTarget;

    if (declEdge.getInitializer() != null) {
      IASTInitializer init = declEdge.getInitializer();
      if (init instanceof IASTInitializerExpression) {
        initialValue = getValue(((IASTInitializerExpression) init).getExpression(), pNewState, functionName);
      }
    }

    // store declaration in abstract state
    pNewState.setTarget(name, initialValue);
  }

  private void handleStatement(FunctionPointerElement pNewState, IASTStatement pStatement,
        CFAEdge pCfaEdge) throws UnrecognizedCCodeException {

    if (pStatement instanceof IASTAssignment) {
      // assignment like "a = b" or "a = foo()"
      String functionName = pCfaEdge.getPredecessor().getFunctionName();

      IASTAssignment assignment = (IASTAssignment)pStatement;
      String varName = getLeftHandSide(assignment.getLeftHandSide(), functionName);

      if (varName != null) {
        FunctionPointerTarget target = getValue(assignment.getRightHandSide(), pNewState, functionName);
        pNewState.setTarget(varName, target);
      }

    } else if (pStatement instanceof IASTFunctionCallStatement) {
      // external function call without return value

    } else if (pStatement instanceof IASTExpressionStatement) {
      // side-effect free statement

    } else {
      throw new UnrecognizedCCodeException(pCfaEdge, pStatement);
    }
  }

  private void handleFunctionCall(FunctionPointerElement pNewState, FunctionCallEdge callEdge) throws UnrecognizedCCodeException {

    FunctionDefinitionNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<IASTExpression> arguments = callEdge.getArguments();

    if (functionEntryNode.getFunctionDefinition().getDeclSpecifier().takesVarArgs()) {
      if (paramNames.size() > arguments.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", callEdge);
      }

    } else {
      if (paramNames.size() != arguments.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", callEdge);
      }
    }

    // used to get value in caller context
    ExpressionValueVisitor v = new ExpressionValueVisitor(pNewState, callerFunctionName, invalidFunctionPointerTarget);

    for (int i=0; i < paramNames.size(); i++) {
      String paramName = scoped(paramNames.get(i), calledFunctionName);
      IASTExpression actualArgument = arguments.get(i);

      FunctionPointerTarget target = actualArgument.accept(v);
      pNewState.setTarget(paramName, target);

      // TODO only do this if declared type is function pointer?
    }
  }

  private void handleReturnStatement(FunctionPointerElement pNewState, IASTExpression returnValue,
      CFAEdge pCfaEdge) throws UnrecognizedCCodeException {

    if (returnValue != null) {
      String functionName = pCfaEdge.getPredecessor().getFunctionName();
      FunctionPointerTarget target = getValue(returnValue, pNewState, functionName);

      pNewState.setTarget(FUNCTION_RETURN_VARIABLE, target);
    }
  }


  private void handleFunctionReturn(FunctionPointerElement pNewState, FunctionReturnEdge pFunctionReturnEdge) throws UnrecognizedCCodeException {
    CallToReturnEdge summaryEdge = pFunctionReturnEdge.getSuccessor().getEnteringSummaryEdge();
    assert summaryEdge != null;

    IASTFunctionCall funcCall = summaryEdge.getExpression();
    if (funcCall instanceof IASTFunctionCallAssignmentStatement) {

      IASTExpression left = ((IASTFunctionCallAssignmentStatement)funcCall).getLeftHandSide();

      String callerFunction = summaryEdge.getSuccessor().getFunctionName();
      String varName = getLeftHandSide(left, callerFunction);

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

  private String getLeftHandSide(IASTExpression lhsExpression, String functionName) throws UnrecognizedCCodeException {

    if (lhsExpression instanceof IASTIdExpression) {
      // a = ...
      return scopedIfNecessary((IASTIdExpression)lhsExpression, functionName);

    } else if (lhsExpression instanceof IASTUnaryExpression
        && ((IASTUnaryExpression)lhsExpression).getOperator() == UnaryOperator.STAR) {
      // *a = ...
      // TODO: Support this statement.

    } else if (lhsExpression instanceof IASTFieldReference) {

      //String functionName = pCfaEdge.getPredecessor().getFunctionName();
      //handleAssignmentToVariable(op1.getRawSignature(), op2, v);

      // TODO: Support this statement.

    } else if (lhsExpression instanceof IASTArraySubscriptExpression) {
      // TODO assignment to array cell

    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", null, lhsExpression);
    }
    return null;
  }

  private FunctionPointerTarget getValue(IASTRightHandSide exp, FunctionPointerElement element, String function) throws UnrecognizedCCodeException {
    return exp.accept(new ExpressionValueVisitor(element, function, invalidFunctionPointerTarget));
  }

  private static class ExpressionValueVisitor extends DefaultExpressionVisitor<FunctionPointerTarget, UnrecognizedCCodeException>
                                              implements RightHandSideVisitor<FunctionPointerTarget, UnrecognizedCCodeException> {

    private final FunctionPointerElement element;
    private final String function;
    private final FunctionPointerTarget targetForInvalidPointers;

    private ExpressionValueVisitor(FunctionPointerElement pElement, String pFunction,
                                   FunctionPointerTarget pTargetForInvalidPointers) {
      element = pElement;
      function = pFunction;
      targetForInvalidPointers = pTargetForInvalidPointers;
    }

    @Override
    public FunctionPointerTarget visit(IASTUnaryExpression pE) {
      if ((pE.getOperator() == UnaryOperator.AMPER) && (pE.getOperand() instanceof IASTIdExpression)) {
        IASTIdExpression operand = (IASTIdExpression)pE.getOperand();
        return new NamedFunctionTarget(operand.getName());

      } else {
        return visitDefault(pE);
      }
    }

    @Override
    public FunctionPointerTarget visit(IASTIdExpression pE) {
      return element.getTarget(scopedIfNecessary(pE, function));
    }

    @Override
    public FunctionPointerTarget visit(IASTCastExpression pE) throws UnrecognizedCCodeException {
      return pE.getOperand().accept(this);
    }

    @Override
    protected FunctionPointerTarget visitDefault(IASTExpression pExp) {
      return UnknownTarget.getInstance();
    }

    @Override
    public FunctionPointerTarget visit(IASTFunctionCallExpression pIastFunctionCallExpression) {
      return UnknownTarget.getInstance();
    }

    @Override
    public FunctionPointerTarget visit(IASTCharLiteralExpression pE) {
      return targetForInvalidPointers;
    }

    @Override
    public FunctionPointerTarget visit(IASTFloatLiteralExpression pE) {
      return targetForInvalidPointers;
    }

    @Override
    public FunctionPointerTarget visit(IASTIntegerLiteralExpression pE) {
      return targetForInvalidPointers;
    }

    @Override
    public FunctionPointerTarget visit(IASTStringLiteralExpression pE) {
      return targetForInvalidPointers;
    }
  }

  // looks up the variable in the current namespace
  private static String scopedIfNecessary(IASTIdExpression var, String function) {
    IASTSimpleDeclaration decl = var.getDeclaration();
    boolean isGlobal = false;
    if (decl instanceof IASTDeclaration) {
      isGlobal = ((IASTDeclaration)decl).isGlobal();
    }

    if (isGlobal) {
      return var.getName();
    } else {
      return scoped(var.getName(), function);
    }
  }

  // prefixes function to variable name
  // Call only if you are sure you have a local variable!
  private static String scoped(String var, String function) {
    return function + "::" + var;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {
    // in this method we could access the abstract domains of other CPAs
    // if required.
    return null;
  }
}