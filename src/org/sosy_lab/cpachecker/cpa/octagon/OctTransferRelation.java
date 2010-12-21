/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
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
import org.sosy_lab.cpachecker.exceptions.OctagonTransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.octagon.Octagon;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;
/**
 * Handles transfer relation for Octagon abstract domain library.
 * See <a href="http://www.di.ens.fr/~mine/oct/">Octagon abstract domain library</a>
 * @author Erkan
 *
 */
public class OctTransferRelation implements TransferRelation{

  // set to set global variables
  private List<String> globalVars;

  private String missingInformationLeftVariable = null;
  private String missingInformationRightPointer = null;
  private String missingInformationLeftPointer  = null;
  private IASTExpression missingInformationRightExpression = null;
  
  /**
   * Class constructor.
   */
  public OctTransferRelation ()
  {
    globalVars = new ArrayList<String>();
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors (AbstractElement element, Precision prec, CFAEdge cfaEdge) throws OctagonTransferException {

    //System.out.println(cfaEdge);
    // octElement is the region of the current state
    // this state will be updated using the edge
    OctElement octElement = null;
    try {
      octElement = (OctElement)((OctElement)element).clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    assert(octElement != null);

    // check the type of the edge
    switch (cfaEdge.getEdgeType ())
    {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
    {
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      IASTExpression expression = statementEdge.getExpression ();
      try {
        handleStatement (octElement, expression, cfaEdge);
      } catch (UnrecognizedCCodeException e) {
        e.printStackTrace();
      }
      break;
    }

    case ReturnStatementEdge:
    {
      ReturnStatementEdge statementEdge = (ReturnStatementEdge) cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      // TODO implement
//      try {
//        handleExitFromFunction(octElement, statementEdge.getExpression(), statementEdge);
//      } catch (UnrecognizedCCodeException e) {
//        e.printStackTrace();
//      }
      break;
    }

    // edge is a decleration edge, e.g. int a;
    case DeclarationEdge:
    {
      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      try {
        handleDeclaration (octElement, declarationEdge);
      } catch (UnrecognizedCCodeException e) {
        e.printStackTrace();
      }
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge:
    {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      IASTExpression expression = assumeEdge.getExpression();
      try {
        handleAssumption (octElement, expression, cfaEdge, assumeEdge.getTruthAssumption());
      } catch (UnrecognizedCFAEdgeException e) {
        e.printStackTrace();
      }
      break;

    }

    case BlankEdge:
    {
      break;
    }

    case FunctionCallEdge:
    {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
   // TODO implement
//      try {
//        
//        handleFunctionCall(octElement, functionCallEdge);
//      } catch (UnrecognizedCCodeException e) {
//        e.printStackTrace();
//      }
      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
    {
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) cfaEdge;
   // TODO implement
//      try {
//        handleFunctionReturn(octElement, functionReturnEdge);
//      } catch (UnrecognizedCCodeException e) {
//        e.printStackTrace();
//      }
      break;
    }

    // Summary edge, we handle this on function return, do nothing
    case CallToReturnEdge:
    {
      assert(false);
      break;
    }
    }
    return Collections.singleton(octElement);
  }

//  /**
//   * Handles return from one function to another function.
//   * @param element previous abstract element.
//   * @param functionReturnEdge return edge from a function to its call site.
//   * @return new abstract element.
//   */
//  private OctElement handleFunctionReturn(OctElement pElement,
//      FunctionReturnEdge functionReturnEdge)
//  throws UnrecognizedCCodeException {
//
//    CallToReturnEdge summaryEdge =
//      functionReturnEdge.getSuccessor().getEnteringSummaryEdge();
//    IASTExpression exprOnSummary = summaryEdge.getExpression();
//    // TODO get from stack
//    OctElement previousElem = pElement.getPreviousElement();
//    OctElement newElement = previousElem.clone();
//    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
//    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();
//    //System.out.println(exprOnSummary.getRawSignature());
//    //expression is a binary operation, e.g. a = g(b);
//    if (exprOnSummary instanceof IASTBinaryExpression) {
//      IASTBinaryExpression binExp = ((IASTBinaryExpression)exprOnSummary);
//      int opType = binExp.getOperator ();
//      IASTExpression op1 = binExp.getOperand1();
//
//      assert(opType == IASTBinaryExpression.op_assign);
//
//      //we expect left hand side of the expression to be a variable
//      if(op1 instanceof IASTIdExpression ||
//          op1 instanceof IASTFieldReference)
//      {
//        //      IASExpression leftHandSideVar = op1;
//        String varName = op1.getRawSignature();
//        String returnVarName = calledFunctionName + "::" + "___cpa_temp_result_var_";
//
//        for(String globalVar:globalVars){
//          if(globalVar.equals(varName)){
//            if(element.getNoOfReferences().containsKey(globalVar) &&
//                element.getNoOfReferences().get(globalVar).intValue() >= this.threshold){
//              newElement.forget(globalVar);
//              newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
//            }
//            else{
//              if(element.contains(returnVarName)){
//                newElement.assignConstant(varName, element.getValueFor(returnVarName), this.threshold);
//              }
//              else{
//                newElement.forget(varName);
//              }
//            }
//          }
//          else{
//            if(element.getNoOfReferences().containsKey(globalVar) &&
//                element.getNoOfReferences().get(globalVar).intValue() >= this.threshold){
//              newElement.forget(globalVar);
//              newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
//            }
//            else{
//              if(element.contains(globalVar)){
//                newElement.assignConstant(globalVar, element.getValueFor(globalVar), this.threshold);
//                newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
//              }
//              else{
//                newElement.forget(varName);
//              }
//            }
//          }
//        }
//
//        if(!globalVars.contains(varName)){
//          String assignedVarName = getvarName(varName, callerFunctionName);
//          if(element.contains(returnVarName)){
//            newElement.assignConstant(assignedVarName, element.getValueFor(returnVarName), this.threshold);
//          }
//          else{
//            newElement.forget(assignedVarName);
//          }
//        }
//      }
//      else{
//        throw new UnrecognizedCCodeException("on function return", summaryEdge, op1);
//      }
//    }
//    // TODO this is not called -- expression is a unary operation, e.g. g(b);
//    else if (exprOnSummary instanceof IASTUnaryExpression)
//    {
//      // only globals
//      for(String globalVar:globalVars){
//        if(element.getNoOfReferences().containsKey(globalVar) &&
//            element.getNoOfReferences().get(globalVar).intValue() >= this.threshold){
//          newElement.forget(globalVar);
//          newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
//        }
//        else{
//          if(element.contains(globalVar)){
//            newElement.assignConstant(globalVar, element.getValueFor(globalVar), this.threshold);
//            newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
//          }
//          else{
//            newElement.forget(globalVar);
//          }
//        }
//      }
//    }
//    // g(b)
//    else if (exprOnSummary instanceof IASTFunctionCallExpression)
//    {
//      // only globals
//      for(String globalVar:globalVars){
//        if(element.getNoOfReferences().containsKey(globalVar) &&
//            element.getNoOfReferences().get(globalVar).intValue() >= this.threshold){
//          newElement.forget(globalVar);
//          newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
//        }
//        else{
//          if(element.contains(globalVar)){
//            newElement.assignConstant(globalVar, element.getValueFor(globalVar), this.threshold);
//            newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
//          }
//          else{
//            newElement.forget(globalVar);
//          }
//        }
//      }
//    }
//    else{
//      throw new UnrecognizedCCodeException("on function return", summaryEdge, exprOnSummary);
//    }
//
//    return newElement;
//  }
//
//  private OctElement handleFunctionCall(OctElement pElement,
//      FunctionCallEdge callEdge)
//  throws UnrecognizedCCodeException {
//
//    FunctionDefinitionNode functionEntryNode = callEdge.getSuccessor();
//    String calledFunctionName = functionEntryNode.getFunctionName();
//    String callerFunctionName = callEdge.getPredecessor().getFunctionName();
//
//    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
//    List<IASTExpression> arguments = callEdge.getArguments();
//
//    assert (paramNames.size() == arguments.size());
//
//    OctElement newElement = new OctElement(element);
//
//    for(String globalVar:globalVars){
//      if(element.contains(globalVar)){
//        newElement.getConstantsMap().put(globalVar, element.getValueFor(globalVar));
//        newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
//      }
//    }
//
//    for (int i=0; i<arguments.size(); i++){
//      IASTExpression arg = arguments.get(i);
//      if (arg instanceof IASTCastExpression) {
//        // ignore casts
//        arg = ((IASTCastExpression)arg).getOperand();
//      }
//
//      String nameOfParam = paramNames.get(i);
//      String formalParamName = getvarName(nameOfParam, calledFunctionName);
//      if(arg instanceof IASTIdExpression){
//        IASTIdExpression idExp = (IASTIdExpression) arg;
//        String nameOfArg = idExp.getRawSignature();
//        String actualParamName = getvarName(nameOfArg, callerFunctionName);
//
//        if(element.contains(actualParamName)){
//          newElement.assignConstant(formalParamName, element.getValueFor(actualParamName), this.threshold);
//        }
//      }
//
//      else if(arg instanceof IASTLiteralExpression){
//        Long val = parseLiteral(arg);
//
//        if (val != null) {
//          newElement.assignConstant(formalParamName, val, this.threshold);
//        } else {
//          // TODO forgetting
//          newElement.forget(formalParamName);
//        }
//      }
//
//      else if(arg instanceof IASTTypeIdExpression){
//        newElement.forget(formalParamName);
//      }
//
//      else if(arg instanceof IASTUnaryExpression){
//        IASTUnaryExpression unaryExp = (IASTUnaryExpression) arg;
//        assert(unaryExp.getOperator() == IASTUnaryExpression.op_star || unaryExp.getOperator() == IASTUnaryExpression.op_amper);
//      }
//
//      else if(arg instanceof IASTFunctionCallExpression){
//        assert(false);
//      }
//
//      else if(arg instanceof IASTFieldReference){
//        newElement.forget(formalParamName);
//      }
//
//      else{
//        // TODO forgetting
//        newElement.forget(formalParamName);
//        //      throw new ExplicitTransferException("Unhandled case");
//      }
//    }
//
//    return newElement;
//  }
//
//  private OctElement handleExitFromFunction(OctElement pElement,
//      IASTExpression expression,
//      ReturnStatementEdge returnEdge)
//  throws UnrecognizedCCodeException {
//
//    return handleAssignmentToVariable(element, "___cpa_temp_result_var_", expression, returnEdge);
//  }

  private AbstractElement handleAssumption (OctElement pElement,
      IASTExpression expression, CFAEdge cfaEdge, boolean truthValue)
  throws UnrecognizedCFAEdgeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // Binary operation
    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
      int opType = binExp.getOperator ();

      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();
      if(op1.getRawSignature().equals("l")){
//        System.out.println("EXP: " + expression.getRawSignature() + " tv " + truthValue);
      }
      return propagateBooleanExpression(pElement, opType, op1, op2, functionName, truthValue);
    }
    // Unary operation
    else if (expression instanceof IASTUnaryExpression)
    {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);
      // ! exp
      if(unaryExp.getOperator() == IASTUnaryExpression.op_not)
      {
//        System.out.println("here " + expression.getRawSignature() + " tv " + truthValue);
        IASTExpression exp1 = unaryExp.getOperand();
        // ! unaryExp
        if(exp1 instanceof IASTUnaryExpression){
//          System.out.println("here2 " + expression.getRawSignature() + " tv " + truthValue);
          IASTUnaryExpression unaryExp1 = ((IASTUnaryExpression)exp1);
          // (exp)
          if (unaryExp1.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
//            System.out.println("here3 " + expression.getRawSignature() + " tv " + truthValue);
            IASTExpression exp2 = unaryExp1.getOperand();
            // (binaryExp)
            if(exp2 instanceof IASTBinaryExpression){
//              System.out.println("here4 " + expression.getRawSignature() + " tv " + truthValue);
              IASTBinaryExpression binExp2 = (IASTBinaryExpression)exp2;
              return handleAssumption(pElement, binExp2, cfaEdge, !truthValue);
            }
            else {
              throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
            }
          }
          else {
            throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }

        if(exp1 instanceof IASTIdExpression ||
            exp1 instanceof IASTFieldReference){
          return handleAssumption(pElement, exp1, cfaEdge, !truthValue);
        }
        else {
          throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }
      else if(unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
        return handleAssumption(pElement, unaryExp.getOperand(), cfaEdge, truthValue);
      }
      else if(unaryExp instanceof IASTCastExpression){
        return handleAssumption(pElement, ((IASTCastExpression)expression).getOperand(), cfaEdge, truthValue);
      }
      else {
        throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
      }
    }

    else if(expression instanceof IASTIdExpression
        || expression instanceof IASTFieldReference){
      return propagateBooleanExpression(pElement, -999, expression, null, functionName, truthValue);
    }

    else{
      throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
    }

  }

  private AbstractElement propagateBooleanExpression(OctElement pElement, 
      int opType,IASTExpression op1, 
      IASTExpression op2, String functionName, boolean truthValue) 
  throws UnrecognizedCFAEdgeException {

//    if(op1 != null && op2 != null & op1.getRawSignature().equals("l")){
//      System.out.println("op1 " + op1.getRawSignature() + " op2 " + 
//          op2.getRawSignature() + " optype " + opType + " tv " +truthValue);
//    }
    // a (bop) ?
    if(op1 instanceof IASTIdExpression || 
        op1 instanceof IASTFieldReference ||
        op1 instanceof IASTArraySubscriptExpression)
    {
      // [literal]
      if(op2 == null && opType == -999){
        String varName = op1.getRawSignature();
        if(truthValue){
          String variableName = getvarName(varName, functionName);
          return addIneqConstraint(pElement, variableName, 0);
        }
        // ! [literal]
        else {
          String variableName = getvarName(varName, functionName);
          return addEqConstraint(pElement, variableName, 0);
        }
      }
      // a (bop) 9
      else if(op2 instanceof IASTLiteralExpression)
      {
        String varName = op1.getRawSignature();
        String variableName = getvarName(varName, functionName);
        int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
        if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
            || typeOfLiteral == IASTLiteralExpression.lk_float_constant
            || typeOfLiteral == IASTLiteralExpression.lk_char_constant
        )
        {
          long valueOfLiteral = parseLiteral(op2);
          // a == 9
          if(opType == IASTBinaryExpression.op_equals) {
            if(truthValue){
              return addEqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a == 9
            else {
              return propagateBooleanExpression(pElement, IASTBinaryExpression.op_notequals, op1, op2, functionName, !truthValue);
            }
          }
          // a != 9
          else if(opType == IASTBinaryExpression.op_notequals)
          {
//            System.out.println(" >>>>> " + varName + " op2 " + op2.getRawSignature());
            if(truthValue){
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a != 9
            else {
              return propagateBooleanExpression(pElement, IASTBinaryExpression.op_equals, op1, op2, functionName, !truthValue);
            }
          }

          // a > 9
          else if(opType == IASTBinaryExpression.op_greaterThan)
          {
            if(truthValue){
              return addGreaterConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, IASTBinaryExpression.op_lessEqual, op1, op2, functionName, !truthValue);
            }
          }
          // a >= 9
          else if(opType == IASTBinaryExpression.op_greaterEqual)
          {
            if(truthValue){
              return addGreaterEqConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, IASTBinaryExpression.op_lessThan, op1, op2, functionName, !truthValue);
            }
          }
          // a < 9
          else if(opType == IASTBinaryExpression.op_lessThan)
          {
            if(truthValue){
              return addSmallerConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, IASTBinaryExpression.op_greaterEqual, op1, op2, functionName, !truthValue);
            }
          }
          // a <= 9
          else if(opType == IASTBinaryExpression.op_lessEqual)
          {
            if(truthValue){
              return addSmallerEqConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, IASTBinaryExpression.op_greaterThan, op1, op2, functionName, !truthValue);
            }
          }
          // [a - 9]
          else if(opType == IASTBinaryExpression.op_minus)
          {
            if(truthValue){
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a != 9
            else {
              return propagateBooleanExpression(pElement, IASTBinaryExpression.op_equals, op1, op2, functionName, !truthValue);
            }
          }

          // [a + 9]
          else if(opType == IASTBinaryExpression.op_plus)
          {
            valueOfLiteral = parseLiteralWithOppositeSign(op2);
            if(truthValue){
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              valueOfLiteral = parseLiteralWithOppositeSign(op2);
              
              return addEqConstraint(pElement, variableName, valueOfLiteral);
            }
          }

          // TODO nothing
          else if(opType == IASTBinaryExpression.op_binaryAnd ||
              opType == IASTBinaryExpression.op_binaryOr ||
              opType == IASTBinaryExpression.op_binaryXor){
            return pElement;
          }

          else{
            throw new UnrecognizedCFAEdgeException("Unhandled case ");
          }
        }
        else{
          throw new UnrecognizedCFAEdgeException("Unhandled case ");
        }
      }
      // a (bop) b
      else if(op2 instanceof IASTIdExpression ||
          (op2 instanceof IASTUnaryExpression && (
              (((IASTUnaryExpression)op2).getOperator() == IASTUnaryExpression.op_amper) || 
              (((IASTUnaryExpression)op2).getOperator() == IASTUnaryExpression.op_star))))
      {
        String leftVarName = op1.getRawSignature();
        String rightVarName = op2.getRawSignature();

        String leftVariableName = getvarName(leftVarName, functionName);
        String rightVariableName = getvarName(rightVarName, functionName);
        
        // a == b
        if(opType == IASTBinaryExpression.op_equals)
        {
          if(truthValue){
            return addEqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, IASTBinaryExpression.op_notequals, op1, op2, functionName, !truthValue);
          }
        }
        // a != b
        else if(opType == IASTBinaryExpression.op_notequals)
        {
          if(truthValue){
            return addIneqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, IASTBinaryExpression.op_equals, op1, op2, functionName, !truthValue);
          }
        }
        // a > b
        else if(opType == IASTBinaryExpression.op_greaterThan)
        {
          if(truthValue){
            return addGreaterConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return  propagateBooleanExpression(pElement, IASTBinaryExpression.op_lessEqual, op1, op2, functionName, !truthValue);
          }
        }
        // a >= b
        else if(opType == IASTBinaryExpression.op_greaterEqual)
        {
          if(truthValue){
            return addGreaterEqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, IASTBinaryExpression.op_lessThan, op1, op2, functionName, !truthValue);
          }
        }
        // a < b
        else if(opType == IASTBinaryExpression.op_lessThan)
        {
          if(truthValue){
            return addSmallerConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, IASTBinaryExpression.op_greaterEqual, op1, op2, functionName, !truthValue);
          }
        }
        // a <= b
        else if(opType == IASTBinaryExpression.op_lessEqual)
        {
          if(truthValue){
            return addSmallerEqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, IASTBinaryExpression.op_greaterThan, op1, op2, functionName, !truthValue);
          }
        }
        else{
          throw new UnrecognizedCFAEdgeException("Unhandled case ");
        }
      }
      else if(op2 instanceof IASTUnaryExpression)
      {
        String varName = op1.getRawSignature();

        IASTUnaryExpression unaryExp = (IASTUnaryExpression)op2;
        IASTExpression unaryExpOp = unaryExp.getOperand();

        int operatorType = unaryExp.getOperator();
        // a == -8
        if(operatorType == IASTUnaryExpression.op_minus){

          if(unaryExpOp instanceof IASTLiteralExpression){
            IASTLiteralExpression literalExp = (IASTLiteralExpression)unaryExpOp;
            int typeOfLiteral = literalExp.getKind();
            if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
                || typeOfLiteral == IASTLiteralExpression.lk_float_constant
                || typeOfLiteral == IASTLiteralExpression.lk_char_constant
            )
            {
              long valueOfLiteral = parseLiteralWithOppositeSign(literalExp);
              String variableName = getvarName(varName, functionName);
              
              // a == 9
              if(opType == IASTBinaryExpression.op_equals) {
                if(truthValue){
                  return addEqConstraint(pElement, variableName, valueOfLiteral);
                }
                // ! a == 9
                else {
                  return propagateBooleanExpression(pElement, IASTBinaryExpression.op_notequals, op1, op2, functionName, !truthValue);
                }
              }
              // a != 9
              else if(opType == IASTBinaryExpression.op_notequals)
              {
                if(truthValue){
                  return addIneqConstraint(pElement, variableName, valueOfLiteral);
                }
                // ! a != 9
                else {
                  return propagateBooleanExpression(pElement, IASTBinaryExpression.op_equals, op1, op2, functionName, !truthValue);
                }
              }

              // a > 9
              else if(opType == IASTBinaryExpression.op_greaterThan)
              {
                if(truthValue){
                  return addGreaterConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, IASTBinaryExpression.op_lessEqual, op1, op2, functionName, !truthValue);
                }
              }
              // a >= 9
              else if(opType == IASTBinaryExpression.op_greaterEqual)
              {
                if(truthValue){
                  return addGreaterEqConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, IASTBinaryExpression.op_lessThan, op1, op2, functionName, !truthValue);
                }
              }
              // a < 9
              else if(opType == IASTBinaryExpression.op_lessThan)
              {
                if(truthValue){
                  return addSmallerConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, IASTBinaryExpression.op_greaterEqual, op1, op2, functionName, !truthValue);
                }
              }
              // a <= 9
              else if(opType == IASTBinaryExpression.op_lessEqual)
              {
                if(truthValue){
                  return addSmallerEqConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, IASTBinaryExpression.op_greaterThan, op1, op2, functionName, !truthValue);
                }
              }
              else{
                throw new UnrecognizedCFAEdgeException("Unhandled case ");
              }
            }
            else{
              throw new UnrecognizedCFAEdgeException("Unhandled case ");
            }
          }
          else{
            throw new UnrecognizedCFAEdgeException("Unhandled case ");
          }
        }
        else if(operatorType == IASTUnaryExpression.op_bracketedPrimary){
          IASTUnaryExpression unaryExprInPar = (IASTUnaryExpression)op2;
          IASTExpression exprInParanhesis = unaryExprInPar.getOperand();
          return propagateBooleanExpression(pElement, opType, op1, exprInParanhesis, functionName, truthValue);
        }
        // right hand side is a cast exp
        else if(unaryExp instanceof IASTCastExpression){
          IASTCastExpression castExp = (IASTCastExpression)unaryExp;
          IASTExpression exprInCastOp = castExp.getOperand();
          return propagateBooleanExpression(pElement, opType, op1, exprInCastOp, functionName, truthValue);
        }
        else{
          throw new UnrecognizedCFAEdgeException("Unhandled case ");
        }
      }
      else if(op2 instanceof IASTBinaryExpression){
        String varName = op1.getRawSignature();
        String variableName = getvarName(varName, functionName);
        return forgetElement(pElement, variableName);
      }
      else{
        String varName = op1.getRawSignature();
        String variableName = getvarName(varName, functionName);
        return forgetElement(pElement, variableName);
      }
    }
    else if(op1 instanceof IASTCastExpression){
      IASTCastExpression castExp = (IASTCastExpression) op1;
      IASTExpression castOperand = castExp.getOperand();
      return propagateBooleanExpression(pElement, opType, castOperand, op2, functionName, truthValue);
    }
    else{
      String varName = op1.getRawSignature();
      String variableName = getvarName(varName, functionName);
      return forgetElement(pElement, variableName);
    }
  }


  private AbstractElement forgetElement(OctElement pElement,
      String pVariableName) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addSmallerEqConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addSmallerConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addGreaterEqConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addGreaterConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addIneqConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addEqConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addSmallerEqConstraint(OctElement pElement,
      String pVariableName, long pValueOfLiteral) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addSmallerConstraint(OctElement pElement,
      String pVariableName, long pValueOfLiteral) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addGreaterEqConstraint(OctElement pElement,
      String pVariableName, long pValueOfLiteral) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addGreaterConstraint(OctElement pElement,
      String pVariableName, long pValueOfLiteral) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addEqConstraint(OctElement pElement,
      String pVariableName, long pI) {
    // TODO Auto-generated method stub
    return null;
  }

  private AbstractElement addIneqConstraint(OctElement pElement,
      String pVariableName, long pI) {
    // TODO Auto-generated method stub
    return null;
  }

  private OctElement handleDeclaration(OctElement pElement,
      DeclarationEdge declarationEdge) throws UnrecognizedCCodeException {

    List<IASTDeclarator> declarators = declarationEdge.getDeclarators();
    // IASTDeclSpecifier specifier = declarationEdge.getDeclSpecifier();

    for (IASTDeclarator declarator : declarators)
    {
        // get the variable name in the declarator
        String varName = declarator.getName().toString();

        // TODO check other types of variables later - just handle primitive
        // types for the moment
        // get pointer operators of the declaration
        IASTPointerOperator[] pointerOps = declarator.getPointerOperators();
        // don't add pointer variables to the list since we don't track them
        if(pointerOps.length > 0)
        {
          continue;
        }
        // if this is a global variable, add to the list of global variables
        if(declarationEdge.isGlobal())
        {
          globalVars.add(varName);
          
          Long v;

          IASTInitializer init = declarator.getInitializer();
          if (init != null) {
            if (init instanceof IASTInitializerExpression) {
              IASTExpression exp = ((IASTInitializerExpression)init).getExpression();

              v = getExpressionValue(pElement, exp, varName, declarationEdge);
            } else {
              // TODO show warning
              v = null;
            }
          } else {
            // global variables without initializer are set to 0 in C
            v = 0L;
          }
          if (v != null) {
            return assignConstant(pElement, varName, v.longValue());
          }
        }
    }
    assert(false);
    return null;
  }

  private OctElement assignConstant(OctElement pElement, String pVarName,
      long pLongValue) {
    // TODO Auto-generated method stub
    return null;
  }

  private OctElement handleStatement(OctElement pElement,
      IASTExpression expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof IASTBinaryExpression) {
      return handleBinaryStatement(pElement, expression, cfaEdge);
    }
    // expression is a unary operation, e.g. a++;
    else if (expression instanceof IASTUnaryExpression)
    {
      return handleUnaryStatement(pElement, expression, cfaEdge);
    }
    // external function call
    else if(expression instanceof IASTFunctionCallExpression){
      // do nothing
    }
    // there is such a case
    else if(expression instanceof IASTIdExpression){
   // do nothing
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
    assert(false);
    return null;
  }

  private OctElement handleUnaryStatement(OctElement pElement,
      IASTExpression expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;
    int operator = unaryExpression.getOperator();

    int shift;
    if (operator == IASTUnaryExpression.op_postFixIncr ||
        operator == IASTUnaryExpression.op_prefixIncr) {
      // a++, ++a
      shift = 1;

    } else if(operator == IASTUnaryExpression.op_prefixDecr ||
        operator == IASTUnaryExpression.op_postFixDecr) {
      // a--, --a
      shift = -1;
    } else {
      throw new UnrecognizedCCodeException(cfaEdge, unaryExpression);
    }

    IASTExpression operand = unaryExpression.getOperand();
    if (operand instanceof IASTIdExpression) {
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      String varName = getvarName(operand.getRawSignature(), functionName);

      return shiftConstant(pElement, varName, shift);

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, operand);
    }
  }

  private OctElement shiftConstant(OctElement pElement, String pVarName,
      int pShift) {
    // TODO Auto-generated method stub
    return null;
  }

  private OctElement handleBinaryStatement(OctElement pElement,
      IASTExpression expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException
  {
    IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;
    switch (binaryExpression.getOperator ())
    {
    // a = ?
    case IASTBinaryExpression.op_assign:
    {
      return handleAssignment(pElement, binaryExpression, cfaEdge);
    }
    // a += 2
    case IASTBinaryExpression.op_plusAssign:
    case IASTBinaryExpression.op_minusAssign:
    case IASTBinaryExpression.op_multiplyAssign:
    case IASTBinaryExpression.op_shiftLeftAssign:
    case IASTBinaryExpression.op_shiftRightAssign:
    case IASTBinaryExpression.op_binaryAndAssign:
    case IASTBinaryExpression.op_binaryXorAssign:
    case IASTBinaryExpression.op_binaryOrAssign:
    {
      return handleOperationAndAssign(pElement, binaryExpression, cfaEdge);
    }
    default:
      throw new UnrecognizedCCodeException(cfaEdge, binaryExpression);
    }
  }

  private OctElement handleOperationAndAssign(OctElement pElement,
      IASTBinaryExpression binaryExpression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    IASTExpression leftOp = binaryExpression.getOperand1();
    IASTExpression rightOp = binaryExpression.getOperand2();
    int operator = binaryExpression.getOperator();

    if (!(leftOp instanceof IASTIdExpression)) {
      // TODO handle fields, arrays
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, leftOp);
    }

    int newOperator;
    switch (operator) {
    case IASTBinaryExpression.op_plusAssign:
      newOperator = IASTBinaryExpression.op_plus;
      break;
    case IASTBinaryExpression.op_minusAssign:
      newOperator = IASTBinaryExpression.op_minus;
      break;
    case IASTBinaryExpression.op_multiplyAssign:
      newOperator = IASTBinaryExpression.op_multiply;
      break;
    case IASTBinaryExpression.op_shiftLeftAssign:
      newOperator = IASTBinaryExpression.op_shiftLeft;
      break;
    case IASTBinaryExpression.op_shiftRightAssign:
      newOperator = IASTBinaryExpression.op_shiftRight;
      break;
    case IASTBinaryExpression.op_binaryAndAssign:
      newOperator = IASTBinaryExpression.op_binaryAnd;
      break;
    case IASTBinaryExpression.op_binaryXorAssign:
      newOperator = IASTBinaryExpression.op_binaryXor;
      break;
    case IASTBinaryExpression.op_binaryOrAssign:
      newOperator = IASTBinaryExpression.op_binaryOr;
      break;
    default:
      throw new UnrecognizedCCodeException("unknown binary operator", cfaEdge, binaryExpression);
    }

    return handleAssignmentOfBinaryExp(pElement, leftOp.getRawSignature(), leftOp,
        rightOp, newOperator, cfaEdge);
  }

  private OctElement handleAssignment(OctElement pElement,
      IASTBinaryExpression binaryExpression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    IASTExpression op1 = binaryExpression.getOperand1();
    IASTExpression op2 = binaryExpression.getOperand2();

    if(op1 instanceof IASTIdExpression) {
      // a = ...
      return handleAssignmentToVariable(pElement, op1.getRawSignature(), op2, cfaEdge);

    } else if (op1 instanceof IASTUnaryExpression
        && ((IASTUnaryExpression)op1).getOperator() == IASTUnaryExpression.op_star) {
      // *a = ...

      op1 = ((IASTUnaryExpression)op1).getOperand();

      // Cil produces code like
      // *((int*)__cil_tmp5) = 1;
      // so remove parentheses and cast
      if (op1 instanceof IASTUnaryExpression
          && ((IASTUnaryExpression)op1).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {

        op1 = ((IASTUnaryExpression)op1).getOperand();
      }
      if (op1 instanceof IASTCastExpression) {
        op1 = ((IASTCastExpression)op1).getOperand();
      }

      if (op1 instanceof IASTIdExpression) {
        missingInformationLeftPointer = op1.getRawSignature();
        missingInformationRightExpression = op2;

      } else {
        throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
      }
//      return element.clone();

    } else if (op1 instanceof IASTFieldReference) {
      // TODO assignment to field
//      return element.clone();

    } else if (op1 instanceof IASTArraySubscriptExpression) {
      // TODO assignment to array cell
//      return element.clone();

    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }
    
    assert(false);
    return null;
  }

  private OctElement handleAssignmentToVariable(OctElement pElement,
      String lParam, IASTExpression rightExp, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // a = 8.2 or "return;" (when rightExp == null)
    if(rightExp == null || rightExp instanceof IASTLiteralExpression){
      return handleAssignmentOfLiteral(pElement, lParam, rightExp, functionName);
    }
    // a = b
    else if (rightExp instanceof IASTIdExpression){
      return handleAssignmentOfVariable(pElement, lParam, rightExp, functionName);
    }
    // a = (cast) ?
    else if(rightExp instanceof IASTCastExpression) {
      return handleAssignmentOfCast(pElement, lParam, (IASTCastExpression)rightExp, cfaEdge);
    }
    // a = -b
    else if(rightExp instanceof IASTUnaryExpression){
      return handleAssignmentOfUnaryExp(pElement, lParam, (IASTUnaryExpression)rightExp, cfaEdge);
    }
    // a = b op c
    else if(rightExp instanceof IASTBinaryExpression){
      IASTBinaryExpression binExp = (IASTBinaryExpression)rightExp;

      return handleAssignmentOfBinaryExp(pElement, lParam, binExp.getOperand1(),
          binExp.getOperand2(), binExp.getOperator(), cfaEdge);
    }
    // a = extCall();  or  a = b->c;
    else if(rightExp instanceof IASTFunctionCallExpression
        || rightExp instanceof IASTFieldReference){
//      OctElement newElement = element.clone();
      String lvarName = getvarName(lParam, functionName);
      return forget(pElement, lvarName);
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, rightExp);
    }
  }

  private OctElement forget(OctElement pElement, String pLvarName) {
    // TODO Auto-generated method stub
    return null;
  }

  private OctElement handleAssignmentOfCast(OctElement pElement,
      String lParam, IASTCastExpression castExp, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException
  {
    IASTExpression castOperand = castExp.getOperand();
    return handleAssignmentToVariable(pElement, lParam, castOperand, cfaEdge);
  }

  private OctElement handleAssignmentOfUnaryExp(OctElement pElement,
      String lParam, IASTUnaryExpression unaryExp, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = -b is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
//    OctElement newElement = element.clone();

    IASTExpression unaryOperand = unaryExp.getOperand();
    int unaryOperator = unaryExp.getOperator();

    if (unaryOperator == IASTUnaryExpression.op_star) {
      // a = * b
      // TODO what is this?
      OctElement newElement = forget(pElement, assignedVar);

      // Cil produces code like
      // __cil_tmp8 = *((int *)__cil_tmp7);
      // so remove parentheses and cast
      if (unaryOperand instanceof IASTUnaryExpression
          && ((IASTUnaryExpression)unaryOperand).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
        unaryOperand = ((IASTUnaryExpression)unaryOperand).getOperand();
      }
      if (unaryOperand instanceof IASTCastExpression) {
        unaryOperand = ((IASTCastExpression)unaryOperand).getOperand();
      }

      if (unaryOperand instanceof IASTIdExpression) {
        missingInformationLeftVariable = assignedVar;
        missingInformationRightPointer = unaryOperand.getRawSignature();
      } else{
        throw new UnrecognizedCCodeException(cfaEdge, unaryOperand);
      }

    } else if (unaryOperator == IASTUnaryExpression.op_bracketedPrimary) {
      // a = (b + c)
      return handleAssignmentToVariable(pElement, lParam, unaryOperand, cfaEdge);

    } 
    else {
      // a = -b or similar
      Long value = getExpressionValue(pElement, unaryExp, functionName, cfaEdge);
      if (value != null) {
        return assignConstant(pElement, assignedVar, value);
      } else {
        return forget(pElement, assignedVar);
      }
    }

    //TODO ?
    return null;
  }

  private OctElement handleAssignmentOfBinaryExp(OctElement pElement,
      String lParam, IASTExpression lVarInBinaryExp, IASTExpression rVarInBinaryExp,
      int binaryOperator, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = b + c is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
//    OctElement newElement = element.clone();

    switch (binaryOperator) {
    case IASTBinaryExpression.op_divide:
    case IASTBinaryExpression.op_modulo:
    case IASTBinaryExpression.op_lessEqual:
    case IASTBinaryExpression.op_greaterEqual:
    case IASTBinaryExpression.op_binaryAnd:
    case IASTBinaryExpression.op_binaryOr:
      // TODO check which cases can be handled (I think all)
      return forget(pElement, assignedVar);

    case IASTBinaryExpression.op_plus:
    case IASTBinaryExpression.op_minus:
    case IASTBinaryExpression.op_multiply:

      Long val1;
      Long val2;

      if(lVarInBinaryExp instanceof IASTUnaryExpression
          && ((IASTUnaryExpression)lVarInBinaryExp).getOperator() == IASTUnaryExpression.op_star) {
        // a = *b + c
        // TODO prepare for using strengthen operator to dereference pointer
        val1 = null;
      } else {
        val1 = getExpressionValue(pElement, lVarInBinaryExp, functionName, cfaEdge);
      }

      if (val1 != null) {
        val2 = getExpressionValue(pElement, rVarInBinaryExp, functionName, cfaEdge);
      } else {
        val2 = null;
      }

      if (val2 != null) { // this implies val1 != null

        long value;
        switch (binaryOperator) {

        case IASTBinaryExpression.op_plus:
          value = val1 + val2;
          break;

        case IASTBinaryExpression.op_minus:
          value = val1 - val2;
          break;

        case IASTBinaryExpression.op_multiply:
          value = val1 * val2;
          break;

        default:
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge, rVarInBinaryExp.getParent());
        }
        return assignConstant(pElement, assignedVar, value);
      } else {
        return forget(pElement, assignedVar);
      }
//      break;
      
    case IASTBinaryExpression.op_equals:
    case IASTBinaryExpression.op_notequals:

      Long lVal = getExpressionValue(pElement, lVarInBinaryExp, functionName, cfaEdge);
      Long rVal = getExpressionValue(pElement, rVarInBinaryExp, functionName, cfaEdge);
      
      if (lVal == null || rVal == null) {
        return forget(pElement, assignedVar);
      
      } else {
        // assign 1 if expression holds, 0 otherwise
        long result = (lVal.equals(rVal) ? 1 : 0);
        
        if (binaryOperator == IASTBinaryExpression.op_notequals) {
          // negate
          result = 1 - result;
        }
        return assignConstant(pElement, assignedVar, result);
      }
//      break;
      
    default:
      // TODO warning
      return forget(pElement, assignedVar);
    }
    // TODO ?
//    return null;
  }

  // TODO modify this.
  private Long getExpressionValue(OctElement pElement, IASTExpression expression,
      String functionName, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    if (expression instanceof IASTLiteralExpression) {
      return parseLiteral(expression);

    } else if (expression instanceof IASTIdExpression) {
      // TODO we should not need this
//      String varName = getvarName(expression.getRawSignature(), functionName);
//      if (element.contains(varName)) {
//        return element.getValueFor(varName);
//      } else {
//        return null;
//      }
      return null;
    } else if (expression instanceof IASTCastExpression) {
      return getExpressionValue(pElement, ((IASTCastExpression)expression).getOperand(),
          functionName, cfaEdge);

    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      int unaryOperator = unaryExpression.getOperator();
      IASTExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case IASTUnaryExpression.op_minus:
        Long val = getExpressionValue(pElement, unaryOperand, functionName, cfaEdge);
        return (val != null) ? -val : null;

      case IASTUnaryExpression.op_bracketedPrimary:
        return getExpressionValue(pElement, unaryOperand, functionName, cfaEdge);

      case IASTUnaryExpression.op_amper:
        return null; // valid expresion, but it's a pointer value

      default:
        throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
      }
    } else {
      // TODO fields, arrays
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private OctElement handleAssignmentOfVariable(OctElement pElement,
      String lParam, IASTExpression op2, String functionName)
  {
    String rParam = op2.getRawSignature();

    String leftVarName = getvarName(lParam, functionName);
    String rightVarName = getvarName(rParam, functionName);

    return assignVariable(pElement, leftVarName, rightVarName);
  }

  private OctElement assignVariable(OctElement pElement, String pLeftVarName,
      String pRightVarName) {
    // TODO Auto-generated method stub
    return null;
  }

  private OctElement handleAssignmentOfLiteral(OctElement pElement,
      String lParam, IASTExpression op2, String functionName)
  throws UnrecognizedCCodeException
  {
//    OctElement newElement = element.clone();

    // op2 may be null if this is a "return;" statement
    Long val = (op2 == null ? Long.valueOf(0L) : parseLiteral(op2));

    String assignedVar = getvarName(lParam, functionName);
    if (val != null) {
      return assignConstant(pElement, assignedVar, val);
    } else {
      return forget(pElement, assignedVar);
    }
//TODO
//    return null;
  }

  private Long parseLiteral(IASTExpression expression) {
    if (expression instanceof IASTLiteralExpression) {
//      System.out.println("expr " + expression.getRawSignature());

      //      int typeOfLiteral = ((IASTLiteralExpression)expression).getKind();
      //      if (typeOfLiteral == IASTLiteralExpression.lk_integer_constant) {
      //
      //        String s = expression.getRawSignature();
      //        if(s.endsWith("L") || s.endsWith("U") || s.endsWith("UL")){
      //          s = s.replace("L", "");
      //          s = s.replace("U", "");
      //          s = s.replace("UL", "");
      //        }
      //        try {
      //          return Long.valueOf(s);
      //        } catch (NumberFormatException e) {
      //          throw new UnrecognizedCCodeException("invalid integer literal", null, expression);
      //        }
      //      }
      //      if (typeOfLiteral == IASTLiteralExpression.lk_string_literal) {
      //        return (long) expression.hashCode();
      //      }

      // this should be a number...
      IASTLiteralExpression lexp = (IASTLiteralExpression)expression;
      String num = lexp.getRawSignature();
      Long retVal = null;
      switch (lexp.getKind()) {
      case IASTLiteralExpression.lk_integer_constant:
      case IASTLiteralExpression.lk_float_constant:
        if (num.startsWith("0x")) {
          // this should be in hex format
          // we use Long instead of Integer to avoid getting negative
          // numbers (e.g. for 0xffffff we would get -1)
          num = Long.valueOf(num, 16).toString();
        } else {
          // this might have some modifiers attached (e.g. 0UL), we
          // have to get rid of them
          int pos = num.length()-1;
          while (!Character.isDigit(num.charAt(pos))) {
            --pos;
          }
          num = num.substring(0, pos+1);
//          System.out.println("num is " + num);
        }
        break;
      case IASTLiteralExpression.lk_char_constant: {
        // we convert to a byte, and take the integer value
        String s = expression.getRawSignature();
        int length = s.length();
        assert(s.charAt(0) == '\'');
        assert(s.charAt(length-1) == '\'');
        int n;

        if (s.charAt(1) == '\\') {
          n = Integer.parseInt(s.substring(2, length-1));
        } else {
          assert (expression.getRawSignature().length() == 3);
          n = expression.getRawSignature().charAt(1);
        }
        num = "" + n;

      }
      break;
      case IASTLiteralExpression.lk_string_literal: {
        // can't handle
        return null;
      }
      default:
        assert(false) : expression;
        return null;
      }
      // TODO here we assume 32 bit integers!!! This is because CIL
      // seems to do so as well...
      try {
        int i = Integer.parseInt(num);
        retVal = (long)i;
      } catch (NumberFormatException nfe) {
//        System.out.print("catching ");
        long l = Long.parseLong(num);
//        System.out.println(l);
        if (l < 0) {
          retVal = Integer.MAX_VALUE + l;
        } else {
          //retVal = (l - ((long)Integer.MAX_VALUE + 1)*2);
          retVal = l;
        }
      }
      return retVal;
    }
    return null;
  }

  private Long parseLiteralWithOppositeSign(IASTExpression expression){
    if (expression instanceof IASTLiteralExpression) {
      // this should be a number...
      IASTLiteralExpression lexp = (IASTLiteralExpression)expression;
      String num = lexp.getRawSignature();
      Long retVal = null;
      boolean isUnsigned = false;
      switch (lexp.getKind()) {
      case IASTLiteralExpression.lk_integer_constant:
      case IASTLiteralExpression.lk_float_constant:
        if (num.startsWith("0x")) {
          // this should be in hex format
          // we use Long instead of Integer to avoid getting negative
          // numbers (e.g. for 0xffffff we would get -1)
          num = Long.valueOf(num, 16).toString();
        } else {
          // this might have some modifiers attached (e.g. 0UL), we
          // have to get rid of them
          int pos = num.length()-1;
          if(num.contains("U")){
            isUnsigned = true;
          }
          while (!Character.isDigit(num.charAt(pos))) {
            --pos;
          }
          num = num.substring(0, pos+1);
        }
        break;
      case IASTLiteralExpression.lk_char_constant: {
        // we convert to a byte, and take the integer value
        String s = expression.getRawSignature();
        int length = s.length();
        assert(s.charAt(0) == '\'');
        assert(s.charAt(length-1) == '\'');
        int n;

        if (s.charAt(1) == '\\') {
          n = Integer.parseInt(s.substring(2, length-1));
        } else {
          assert (expression.getRawSignature().length() == 3);
          n = expression.getRawSignature().charAt(1);
        }
        num = "" + n;

      }
      break;
      default:
        assert(false);
        return null;
      }
      // TODO here we assume 32 bit integers!!! This is because CIL
      // seems to do so as well...
      try {
        int i = Integer.parseInt(num);
        retVal = 0 - (long)i;
        if(isUnsigned){
          if (retVal < 0) {
            retVal = ((long)Integer.MAX_VALUE + 1)*2 + retVal;
          } else {
            retVal = (retVal - ((long)Integer.MAX_VALUE + 1)*2);
          }
        }
      } catch (NumberFormatException nfe) {
        long l = Long.parseLong(num);
        l = 0 - l;
        if (l < 0) {
          retVal = Integer.MAX_VALUE + l;
        } else {
          //retVal = (l - ((long)Integer.MAX_VALUE + 1)*2);
          retVal = l;
        }
      }
      return retVal;
    }
    return null;

  }

  public String getvarName(String variableName, String functionName){
    if(globalVars.contains(variableName)){
      return variableName;
    }
    return functionName + "::" + variableName;
  }
  
  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    return null;
    // TODO implement error detection algo.
  }
}
