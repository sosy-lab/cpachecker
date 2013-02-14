/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
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
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    final FunctionPointerState oldState = (FunctionPointerState)pElement;
    Collection<FunctionPointerState> results;

    if (pCfaEdge == null) {
      CFANode node = extractLocation(oldState);
      results = new ArrayList<>(node.getNumLeavingEdges());

      for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges(); edgeIdx++) {
        CFAEdge edge = node.getLeavingEdge(edgeIdx);
        if (!(edge instanceof FunctionPointerCallEdge)) {
          // ignore FunctionPointerCallEdges, they are from previous passes
          getAbstractSuccessorForEdge(oldState, pPrecision, edge, results);
        }
      }

    } else {
      results = new ArrayList<>(1);
      getAbstractSuccessorForEdge(oldState, pPrecision, pCfaEdge, results);

    }
    return results;
  }

  private void getAbstractSuccessorForEdge(
      FunctionPointerState oldState, Precision pPrecision, CFAEdge pCfaEdge, Collection<FunctionPointerState> results)
      throws CPATransferException, InterruptedException {
    CFAEdge cfaEdge;

    // first, check if this is a function pointer call
    String functionCallVariable = getFunctionPointerCall(pCfaEdge);
    if (functionCallVariable != null) {
      // this is indeed a function call via a function pointer

      FunctionPointerTarget target = oldState.getTarget(functionCallVariable);
      if (target instanceof NamedFunctionTarget) {
        String functionName = ((NamedFunctionTarget)target).getFunctionName();
        FunctionEntryNode fDefNode = functions.getFunctionHead(functionName);
        if (fDefNode != null) {
          logger.log(Level.FINEST, "Function pointer", functionCallVariable, "points to", target, "while it is used.");

          CStatementEdge edge = (CStatementEdge)pCfaEdge;
          CFunctionCall functionCall = (CFunctionCall)edge.getStatement();

          // check parameters
          int numberOfFormalParameters = fDefNode.getFunctionParameters().size();
          int numberOfActualParameters = functionCall.getFunctionCallExpression().getParameterExpressions().size();
          boolean varargs = fDefNode.getFunctionDefinition().getType().takesVarArgs();
          if ((numberOfActualParameters < numberOfFormalParameters)
              || (!varargs && (numberOfActualParameters > numberOfFormalParameters))) {
            throw new UnrecognizedCCodeException(
                String.format("Function pointer \"%s\" points to function \"%s\" which takes %d parameter(s) but is called with %d parameter(s)",
                              functionCallVariable, functionName, numberOfFormalParameters, numberOfActualParameters),
                edge);
          }


          CFANode predecessorNode = edge.getPredecessor();
          CFANode successorNode = edge.getSuccessor();
          int lineNumber = edge.getLineNumber();

          FunctionExitNode fExitNode = fDefNode.getExitNode();

          // Create new edges.
          CFunctionSummaryEdge calltoReturnEdge = new CFunctionSummaryEdge(edge.getRawStatement(),
              lineNumber, predecessorNode, successorNode, functionCall);

          FunctionPointerCallEdge callEdge = new FunctionPointerCallEdge(edge.getRawStatement(), lineNumber, predecessorNode, (CFunctionEntryNode)fDefNode, functionCall, calltoReturnEdge);
          predecessorNode.addLeavingEdge(callEdge);
          fDefNode.addEnteringEdge(callEdge);

          if (fExitNode.getNumEnteringEdges() > 0) {
            FunctionPointerReturnEdge returnEdge = new FunctionPointerReturnEdge(lineNumber, fExitNode, successorNode, callEdge, calltoReturnEdge);
            fExitNode.addLeavingEdge(returnEdge);
            successorNode.addEnteringEdge(returnEdge);

          } else {
            // exit node of called functions is not reachable, i.e. this function never returns
            // no need to add return edges
          }

          // now substitute the real edge with the fake edge
          cfaEdge = callEdge;
        } else {
          logger.log(Level.WARNING, "Ignoring function pointer call to external function", functionName);
          cfaEdge = pCfaEdge;
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
      CFunctionSummaryEdge calltoReturnEdge = ((FunctionPointerReturnEdge) cfaEdge).getSummaryEdge();
      calltoReturnEdge.getPredecessor().addLeavingSummaryEdge(calltoReturnEdge);
      calltoReturnEdge.getSuccessor().addEnteringSummaryEdge(calltoReturnEdge);
    }

    // now handle the edge, whether it is real or not
    Collection<? extends AbstractState> newWrappedStates = wrappedTransfer.getAbstractSuccessors(oldState.getWrappedState(), pPrecision, cfaEdge);

    for (AbstractState newWrappedState : newWrappedStates) {
      FunctionPointerState.Builder newState = oldState.createBuilderWithNewWrappedState(newWrappedState);

      newState = handleEdge(newState, cfaEdge);

      if (newState != null) {
        results.add(newState.build());
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

    CStatement statement = ((CStatementEdge)pCfaEdge).getStatement();
    if (!(statement instanceof CFunctionCall)) {
      return null;
    }

    CFunctionCallExpression funcCall = ((CFunctionCall)statement).getFunctionCallExpression();
    CExpression nameExp = funcCall.getFunctionNameExpression();
    String currentFunction = pCfaEdge.getPredecessor().getFunctionName();

    // functions may be called either as f() or as (*f)(),
    // so remove the star operator if its there
    if (nameExp instanceof CUnaryExpression) {
      CUnaryExpression unaryExp = (CUnaryExpression)nameExp;
      if (unaryExp.getOperator() == UnaryOperator.STAR) {
        // a = (*f)(b)
        nameExp = unaryExp.getOperand();

      } else {
        throw new UnrecognizedCCodeException("unknown function call expression with operator " + unaryExp.getOperator().getOperator(), pCfaEdge, nameExp);
      }
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
    } else if (nameExp instanceof CUnaryExpression && ((CUnaryExpression)nameExp).getOperator() == UnaryOperator.STAR) {
      // TODO double dereference (**f)()
      return null;
    } else {
      throw new UnrecognizedCCodeException("unknown function call expression of type " + nameExp.getClass().getSimpleName(), pCfaEdge, nameExp);
    }
  }

  private FunctionPointerState.Builder handleEdge(FunctionPointerState.Builder newState, CFAEdge pCfaEdge) throws CPATransferException {

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
        for(CFAEdge currentEdge : ((MultiEdge)pCfaEdge).getEdges()) {
          newState = handleEdge(newState, currentEdge);
        }
        break;
      }

      default:
        throw new UnrecognizedCFAEdgeException(pCfaEdge);
    }

    return newState;
  }

  private void handleDeclaration(FunctionPointerState.Builder pNewState, CDeclarationEdge declEdge) throws UnrecognizedCCodeException {

    if (!(declEdge.getDeclaration() instanceof CVariableDeclaration)) {
      // not a variable declaration
      return;
    }
    CVariableDeclaration decl = (CVariableDeclaration)declEdge.getDeclaration();

    String functionName = declEdge.getPredecessor().getFunctionName();

    // get name of declaration
    String name = decl.getName();
    if (name == null) {
      // not a variable declaration
      return;
    }
    if (!decl.isGlobal()) {
      name = scoped(name, functionName);
    }

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
      throw new UnrecognizedCCodeException(pCfaEdge, pStatement);
    }
  }

  private void handleFunctionCall(FunctionPointerState.Builder pNewState, CFunctionCallEdge callEdge) throws UnrecognizedCCodeException {

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<CExpression> arguments = callEdge.getArguments();

    if (functionEntryNode.getFunctionDefinition().getType().takesVarArgs()) {
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
      CExpression actualArgument = arguments.get(i);

      FunctionPointerTarget target = actualArgument.accept(v);
      pNewState.setTarget(paramName, target);

      // TODO only do this if declared type is function pointer?
    }
  }

  private void handleReturnStatement(FunctionPointerState.Builder pNewState, CExpression returnValue,
      CFAEdge pCfaEdge) throws UnrecognizedCCodeException {

    if (returnValue != null) {
      String functionName = pCfaEdge.getPredecessor().getFunctionName();
      FunctionPointerTarget target = getValue(returnValue, pNewState, functionName);

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

    } else if (lhsExpression instanceof CUnaryExpression
        && ((CUnaryExpression)lhsExpression).getOperator() == UnaryOperator.STAR) {
      // *a = ...
      // TODO: Support this statement.

    } else if (lhsExpression instanceof CFieldReference) {

      //String functionName = pCfaEdge.getPredecessor().getFunctionName();
      //handleAssignmentToVariable(op1.getRawSignature(), op2, v);

      // TODO: Support this statement.

    } else if (lhsExpression instanceof CArraySubscriptExpression) {
      // TODO assignment to array cell

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
    public FunctionPointerTarget visit(CUnaryExpression pE) {
      if ((pE.getOperator() == UnaryOperator.AMPER) && (pE.getOperand() instanceof CIdExpression)) {
        CIdExpression operand = (CIdExpression)pE.getOperand();
        return new NamedFunctionTarget(operand.getName());

      } else {
        return visitDefault(pE);
      }
    }

    @Override
    public FunctionPointerTarget visit(CIdExpression pE) {
      if (pE.getExpressionType() instanceof CFunctionType) {
        return new NamedFunctionTarget(pE.getName());
      }

      return state.getTarget(scopedIfNecessary(pE, function));
    }

    @Override
    public FunctionPointerTarget visit(CCastExpression pE) throws UnrecognizedCCodeException {
      return pE.getOperand().accept(this);
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
  }

  // looks up the variable in the current namespace
  private static String scopedIfNecessary(CIdExpression var, String function) {
    CSimpleDeclaration decl = var.getDeclaration();
    boolean isGlobal = false;
    if (decl instanceof CDeclaration) {
      isGlobal = ((CDeclaration)decl).isGlobal();
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
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {
    // in this method we could access the abstract domains of other CPAs
    // if required.
    return null;
  }
}