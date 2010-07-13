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
/**
 *
 */
package org.sosy_lab.cpachecker.core.algorithm.cbmctools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAErrorNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.MultiDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.MultiStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;

/**
 * @author erkan
 *
 */
public class AbstractPathToCTranslator {

  private static List<String> mGlobalDefinitionsList = new ArrayList<String>();
  private static List<String> mFunctionDecls = null;
  private static int mFunctionIndex = 0;

  public static String translatePaths(Map<String, CFAFunctionDefinitionNode> pCfas, ARTElement artRoot, Collection<ARTElement> elementsOnErrorPath) {
    String ret = "";
    // Add the original function declarations to enable read-only use of function pointers;
    // there will be no code for these functions, so they can never be called via the function
    // pointer properly; a real solution requires function pointer support within the CPA
    // providing location/successor information
    mFunctionDecls = new ArrayList<String>();
    for (CFAFunctionDefinitionNode node : pCfas.values()) {
      FunctionDefinitionNode lFunctionDefinitionNode = (FunctionDefinitionNode)node;
      String lOriginalFunctionDecl = lFunctionDefinitionNode.getFunctionDefinition().getDeclSpecifier().getRawSignature() + " " + node.getFunctionName() + "(";

      boolean lFirstFunctionParameter = true;

      for (IASTParameterDeclaration lFunctionParameter : lFunctionDefinitionNode.getFunctionParameters()) {
        if (lFirstFunctionParameter) {
          lFirstFunctionParameter = false;
        }
        else {
          lOriginalFunctionDecl += ", ";
        }

        lOriginalFunctionDecl += lFunctionParameter.getRawSignature();
      }

      lOriginalFunctionDecl += ");";

      mFunctionDecls.add(lOriginalFunctionDecl);
    }

    List<StringBuffer> lTranslation = translatePath(artRoot, elementsOnErrorPath);

    if (mFunctionDecls != null) {
      for (String decl : mFunctionDecls) {
        mGlobalDefinitionsList.add(decl);
      }
    }

    for (String lGlobalString : mGlobalDefinitionsList) {
      ret = ret + lGlobalString + "\n";
    }

    for (StringBuffer lProgramString : lTranslation) {
      ret = ret + lProgramString + "\n";
    }

    return ret;
  }

  public static PrintWriter startFunction(int pFunctionIndex, CFANode pNode, Stack<StringWriter> pProgramTextStack) {
    assert(pNode != null);
    assert(pNode instanceof FunctionDefinitionNode);
    assert(pProgramTextStack != null);

    FunctionDefinitionNode lFunctionDefinitionNode = (FunctionDefinitionNode)pNode;

    IASTFunctionDefinition lFunctionDefinition = lFunctionDefinitionNode.getFunctionDefinition();

    List<IASTParameterDeclaration> lFunctionParameters = lFunctionDefinitionNode.getFunctionParameters();

    String lFunctionHeader = lFunctionDefinition.getDeclSpecifier().getRawSignature() + " " + lFunctionDefinitionNode.getFunctionName() + "_" + pFunctionIndex + "(";

    boolean lFirstFunctionParameter = true;

    for (IASTParameterDeclaration lFunctionParameter : lFunctionParameters) {
      if (lFirstFunctionParameter) {
        lFirstFunctionParameter = false;
      }
      else {
        lFunctionHeader += ", ";
      }

      lFunctionHeader += lFunctionParameter.getRawSignature();
    }

    lFunctionHeader += ")";

    StringWriter lFunctionStringWriter = new StringWriter();

    pProgramTextStack.add(lFunctionStringWriter);

    PrintWriter lProgramText = new PrintWriter(lFunctionStringWriter);

    lProgramText.println(lFunctionHeader);

    lProgramText.println("{");

    return lProgramText;
  }

  public static List<StringBuffer> translatePath(final ARTElement firstElement,
                                        Collection<ARTElement> pElementsOnPath) {

//  ARTElement parentElement;
    ARTElement childElement;
    CFAEdge edge;
    Stack<Stack<CBMCStackElement>> stack;

    // waitlist for the edges to be processed
    List<CBMCEdge> waitlist = new ArrayList<CBMCEdge>();
    // map of nodes to check end of a condition
    Map<Integer, CBMCMergeNode> mergeNodes = new HashMap<Integer, CBMCMergeNode>();
    // list of functions - a function is represented by its first stack element and we get
    // the code for the function recursively starting from that node
    List<CBMCStackElement> functions = new ArrayList<CBMCStackElement>();

    // the first element should have one child
    // TODO add more children support later
    assert(firstElement.getChildren().size() == 1);
    ARTElement firstElementsChild = (ARTElement)firstElement.getChildren().toArray()[0];
    // create the first stack element using the first element of the initiating function
    CBMCStackElement firstStackElement = new CBMCStackElement(firstElement.getElementId(),
        startFunction(mFunctionIndex++, firstElement.retrieveLocationElement().getLocationNode()));
    functions.add(firstStackElement);

    Stack<Stack<CBMCStackElement>> newStack = new Stack<Stack<CBMCStackElement>>();
    Stack<CBMCStackElement> newElementsStack = new Stack<CBMCStackElement>();
    newElementsStack.add(firstStackElement);
    newStack.add(newElementsStack);

    // add the first edge and the first stack element
    CBMCEdge firstEdge = new CBMCEdge(firstElement, firstElementsChild,
        firstElement.getEdgeToChild(firstElementsChild), newStack);
    waitlist.add(firstEdge);

    while(waitlist.size() > 0){
      // we need to sort the list based on art element id because we have to process
      // the edges in topological sort
      Collections.sort(waitlist);

      // get the first element in the list (this is the smallest element when topologically sorted)
      CBMCEdge nextCBMCEdge = waitlist.remove(0);

//    parentElement = nextCBMCEdge.getParentElement();
      childElement = nextCBMCEdge.getChildElement();
      edge = nextCBMCEdge.getEdge();
      stack = nextCBMCEdge.getStack();

      // clone stack to have a different representation of the function calls and conditions
      // every element
      stack = cloneStack(stack);

      // how many parents does the child have?
      int sizeOfChildsParents = childElement.getParents().size();

      // if there is only one child this is not the end of the condition
      if(sizeOfChildsParents == 1){
        CBMCStackElement lastStackElement = stack.peek().peek();

        if(edge instanceof BlankEdge){
          lastStackElement.write(new CBMCLabelElement(((BlankEdge)edge).getRawStatement(), childElement));
        }

        // if this is a function call edge we need to create a new element and push
        // it to the topmost stack to represent the function
        if(edge instanceof FunctionCallEdge){
          // write summary edge to the caller site
          lastStackElement.write(processFunctionCall(edge));
          // create a new stack to save conditions in that function
          Stack<CBMCStackElement> newFunctionStack = new Stack<CBMCStackElement>();
          // create a new function
          ARTElement firstFunctionElement = nextCBMCEdge.getChildElement();
          CBMCStackElement firstFunctionStackElement = new CBMCStackElement(firstFunctionElement.getElementId(),
              startFunction(mFunctionIndex++, firstFunctionElement.retrieveLocationElement().getLocationNode()));
          functions.add(firstFunctionStackElement);
          newFunctionStack.push(firstFunctionStackElement);
          stack.push(newFunctionStack);
        }
        else if(edge instanceof ReturnEdge){
          lastStackElement.write("}");
          stack.pop();
        }
        else if(edge.getSuccessor() instanceof CFAErrorNode){
          lastStackElement.write("assert(0); // error location ");
//        lastStackElement.write("}");

          while(stack.size() > 0){
            CBMCStackElement stackElem = stack.pop().firstElement();
            stackElem.write("}");
          }
        }
        else{
          lastStackElement.write(processSimpleEdge(edge));
        }
      }

      // this is the end of the condition, determine whether we should continue or backtrack
      else if(sizeOfChildsParents > 1){
        if(edge instanceof BlankEdge){
          CBMCStackElement lastStackElement = stack.peek().peek();
//          if(((BlankEdge)edge).getRawStatement().contains("Label:")){
//            System.out.println(((BlankEdge)edge).getRawStatement());
//            System.out.println(stack.peek().size());
//            lastStackElement = stack.peek().get(stack.peek().size()-2);
//            lastStackElement.write(new CBMCLabelElement(((BlankEdge)edge).getRawStatement(), childElement));
//          }
//          else{
            lastStackElement.write(new CBMCLabelElement(((BlankEdge)edge).getRawStatement(), childElement));
//          }
        }
        int elemId = childElement.getElementId();
        // get the merge node for that node
        CBMCMergeNode mergeNode = mergeNodes.get(elemId);
        // if null create new and put in the map
        if(mergeNode == null){
          mergeNode = new CBMCMergeNode(elemId);
          mergeNodes.put(elemId, mergeNode);
        }

        // this tells us the number of edges (entering that node) processed so far
        int noOfProcessedBranches = mergeNode.addBranch(nextCBMCEdge);

        // if all edges are processed
        if(sizeOfChildsParents == noOfProcessedBranches){
          // all branches are processed, now decide which nodes to remove from the stack and
          // write to the file accordingly - this set is the set of conditions that should
          // terminate at this point
          Set<Integer> setOfEndedBranches = mergeNode.getProcessedConditions();

          // traverse on the last stack and remove all elements that are in
          // setOfEndedBranches set. The remaining elements to be transferred to
          // the next elements stack
          while(true){
            CBMCStackElement elem = stack.peek().peek();
            int idOfElem = elem.getElementId();
            if(setOfEndedBranches.contains(idOfElem)){
              // remove the condition from the stack
              stack.peek().pop();
            }
            else{
              break;
            }
          }
        }
        else{
          continue;
        }
      }

      int sizeOfChildsChilds = childElement.getChildren().size();

      List<ARTElement> relevantChildrenOfElement = new ArrayList<ARTElement>();

      // if it has only one child it is on the path to error
      if(sizeOfChildsChilds == 1){
        relevantChildrenOfElement.addAll(childElement.getChildren());
      }
      // else find out whether children are on the path to error
      else{
        for(ARTElement child: childElement.getChildren()){
          if(pElementsOnPath.contains(child)){
            relevantChildrenOfElement.add(child);
          }
        }
      }

      // if there is only one child on the path
      if(relevantChildrenOfElement.size() == 1){
        // get the next ART element, create a new edge using the same stack and add it to the waitlist
        ARTElement elem = relevantChildrenOfElement.get(0);
        CFAEdge e = childElement.getEdgeToChild(elem);
        CBMCEdge newEdge = new CBMCEdge(childElement, elem, e, stack);
        waitlist.add(newEdge);
      }

      // if there are more than one relevant child, then this is a condition
      // we need to update the stack
      else if(relevantChildrenOfElement.size() > 1){
        assert(relevantChildrenOfElement.size() == 2);
        int ind = 0;
        for(ARTElement elem: relevantChildrenOfElement){
          Stack<Stack<CBMCStackElement>> newCondStack = cloneStack(stack);
          CFAEdge e = childElement.getEdgeToChild(elem);
          Stack<CBMCStackElement> lastStackOfFunction = newCondStack.peek();
          assert(e instanceof AssumeEdge);
          AssumeEdge assumeEdge = (AssumeEdge)e;
          // create a new
          CBMCStackElement newStackElement = new CBMCStackElement(childElement.getElementId(), assumeEdge);

          boolean truthAssumption = assumeEdge.getTruthAssumption();

          String cond = "";

          if(ind == 0){
            cond = "if";
          }
          else if(ind == 1){
            cond = "else if";
          }
          else{
            assert(false);
          }
          ind++;

          if(truthAssumption){
            lastStackOfFunction.peek().write(cond + "(" + assumeEdge.getExpression().getRawSignature() + ") {");
            lastStackOfFunction.peek().write(newStackElement);
            lastStackOfFunction.peek().write("}");
          }
          else{
            lastStackOfFunction.peek().write(cond + "(!(" + assumeEdge.getExpression().getRawSignature() + ")) {");
            lastStackOfFunction.peek().write(newStackElement);
            lastStackOfFunction.peek().write("}");
          }

          lastStackOfFunction.push(newStackElement);
          CBMCEdge newEdge = new CBMCEdge(childElement, elem, e, newCondStack);
          waitlist.add(newEdge);
        }
      }
    }

    List<StringBuffer> retList = new ArrayList<StringBuffer>();

    for(CBMCStackElement stackElem: functions){
      retList.add(stackElem.getCode());
    }

    return retList;
  }


  private static String processSimpleEdge(CFAEdge pCFAEdge){

    switch (pCFAEdge.getEdgeType()) {
    case BlankEdge: {
      //          nothing to do
      break;
    }
    case AssumeEdge: {
      AssumeEdge lAssumeEdge = (AssumeEdge)pCFAEdge;

      String lExpressionString = lAssumeEdge.getExpression().getRawSignature();

      String lAssumptionString;

      if (lAssumeEdge.getTruthAssumption()) {
        lAssumptionString = lExpressionString;
      }
      else {
        lAssumptionString = "!(" + lExpressionString + ")";
      }

      return ("__CPROVER_assume(" + lAssumptionString + ");");
    }
    case StatementEdge: {
      StatementEdge lStatementEdge = (StatementEdge)pCFAEdge;

      IASTExpression lExpression = lStatementEdge.getExpression();

      String ret = "";

      if (lExpression != null) {
        if(lStatementEdge.isJumpEdge()){
          ret = ret + "return ";
        }
        ret = ret + lStatementEdge.getExpression().getRawSignature() + ";";
      }

      return (ret);
    }
    case DeclarationEdge: {
      DeclarationEdge lDeclarationEdge = (DeclarationEdge)pCFAEdge;

      if (lDeclarationEdge instanceof org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge) {
        mGlobalDefinitionsList.add(lDeclarationEdge.getRawStatement());
      }
      else {
        return (lDeclarationEdge.getRawStatement());
      }

      /*IASTDeclarator[] lDeclarators = lDeclarationEdge.getDeclarators();

assert(lDeclarators.length == 1);

// TODO what about function pointers?
lProgramText.println(lDeclarationEdge.getDeclSpecifier().getRawSignature() + " " + lDeclarators[0].getRawSignature() + ";");
       */
      break;
    }

    case MultiStatementEdge: {
      MultiStatementEdge lMultiStatementEdge = (MultiStatementEdge)pCFAEdge;

      String ret = "";

      for (IASTExpression lExpression : lMultiStatementEdge.getExpressions()) {
        ret = ret + (lExpression.getRawSignature() + ";");
      }

      return ret;
    }
    case MultiDeclarationEdge: {
      MultiDeclarationEdge lMultiDeclarationEdge = (MultiDeclarationEdge)pCFAEdge;

      return (lMultiDeclarationEdge.getRawStatement());

      /*List<IASTDeclarator[]> lDecls = lMultiDeclarationEdge.getDeclarators();
      lMultiDeclarationEdge.getRawStatement()
      for (IASTDeclarator[] lDeclarators : lDecls) {
      }*/
    }
    case CallToReturnEdge: {
      //          this should not have been taken
      assert(false);

      break;
    }
    default: {
      assert(false);
    }
    }

    return "";
  }

  public static String processFunctionCall(CFAEdge pCFAEdge){

    FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge)pCFAEdge;

    String lFunctionName = lFunctionCallEdge.getSuccessor().getFunctionName();


    String lArgumentString = "(";

    boolean lFirstArgument = true;

    IASTExpression[] lArguments = lFunctionCallEdge.getArguments();

    if (lArguments != null) {
      for (IASTExpression lArgument : lArguments) {
        if (lFirstArgument) {
          lFirstArgument = false;
        }
        else {
          lArgumentString += ", ";
        }

        lArgumentString += lArgument.getRawSignature();
      }
    }

    lArgumentString += ")";

    CFAEdge summaryEdge = lFunctionCallEdge.getPredecessor().getLeavingSummaryEdge();
    IASTExpression expressionOnSummaryEdge = ((CallToReturnEdge)summaryEdge).getExpression();
    if(expressionOnSummaryEdge instanceof IASTBinaryExpression){
      IASTBinaryExpression binaryExp = (IASTBinaryExpression) expressionOnSummaryEdge;
      assert(binaryExp.getOperator() == IASTBinaryExpression.op_assign);
      String assignedVarName = binaryExp.getOperand1().getRawSignature();
      return(assignedVarName + " = " + lFunctionName + "_" + mFunctionIndex + lArgumentString + ";");
    }
    else{
      assert(expressionOnSummaryEdge instanceof IASTFunctionCallExpression);
      return(lFunctionName + "_" + mFunctionIndex + lArgumentString + ";");
    }
  }

  public static String startFunction(int pFunctionIndex, CFANode pNode) {
    assert(pNode != null);
    assert(pNode instanceof FunctionDefinitionNode);

    FunctionDefinitionNode lFunctionDefinitionNode = (FunctionDefinitionNode)pNode;

    IASTFunctionDefinition lFunctionDefinition = lFunctionDefinitionNode.getFunctionDefinition();

    List<IASTParameterDeclaration> lFunctionParameters = lFunctionDefinitionNode.getFunctionParameters();

    String lFunctionHeader = lFunctionDefinition.getDeclSpecifier().getRawSignature() + " " + lFunctionDefinitionNode.getFunctionName() + "_" + pFunctionIndex + "(";

    boolean lFirstFunctionParameter = true;

    for (IASTParameterDeclaration lFunctionParameter : lFunctionParameters) {
      if (lFirstFunctionParameter) {
        lFirstFunctionParameter = false;
      }
      else {
        lFunctionHeader += ", ";
      }

      lFunctionHeader += lFunctionParameter.getRawSignature();
    }

    lFunctionHeader += ") \n";

    lFunctionHeader += "{";

    return lFunctionHeader;
  }

  private static Stack<Stack<CBMCStackElement>> cloneStack(
      Stack<Stack<CBMCStackElement>> pStack) {
    Stack<Stack<CBMCStackElement>>  ret = new Stack<Stack<CBMCStackElement>>();
    Iterator<Stack<CBMCStackElement>> it = pStack.iterator();
    while(it.hasNext()){
      Stack<CBMCStackElement> stackItem = it.next();
      Stack<CBMCStackElement> newRetStack = new Stack<CBMCStackElement>();
      Iterator<CBMCStackElement> newIt = stackItem.iterator();
      while(newIt.hasNext()){
        CBMCStackElement newStackElem = newIt.next();
        newRetStack.push(newStackElem);
      }
      ret.push(newRetStack);
    }
    return ret;
  }
}
