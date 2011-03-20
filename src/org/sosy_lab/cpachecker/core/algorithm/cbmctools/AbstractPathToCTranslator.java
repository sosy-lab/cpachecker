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

import static com.google.common.collect.Iterables.concat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * @author erkan
 *
 */
public class AbstractPathToCTranslator {

  private static final List<String> mGlobalDefinitionsList = new ArrayList<String>();
  private static final List<String> mFunctionDecls = new ArrayList<String>();
  private static int mFunctionIndex = 0;

  public static String translatePaths(Map<String, CFAFunctionDefinitionNode> pCfas, ARTElement artRoot, Collection<ARTElement> elementsOnErrorPath) {
    // TODO convert to non-static fields
    Preconditions.checkState(mFunctionIndex == 0);

    // Add the original function declarations to enable read-only use of function pointers;
    // there will be no code for these functions, so they can never be called via the function
    // pointer properly; a real solution requires function pointer support within the CPA
    // providing location/successor information
    for (CFAFunctionDefinitionNode node : pCfas.values()) {
      // this adds the function declaration to mFunctionDecls
      startFunction(node, false);
    }

    List<StringBuffer> lTranslation = translatePath(artRoot, elementsOnErrorPath);

    String ret = Joiner.on('\n').join(concat(mGlobalDefinitionsList, mFunctionDecls, lTranslation));

    // replace nondet keyword with cbmc nondet keyword
    ret = ret.replaceAll("__BLAST_NONDET___0", "nondet_int()");
    ret = ret.replaceAll("__BLAST_NONDET", "nondet_int()");
    
    // cleanup
    mGlobalDefinitionsList.clear();
    mFunctionDecls.clear();
    mFunctionIndex = 0;
    return ret;
  }

  private static List<StringBuffer> translatePath(final ARTElement firstElement,
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
        startFunction(firstElement.retrieveLocationElement().getLocationNode(), true));
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

        if (childElement.isTarget()) {
          lastStackElement.write("assert(0); // target state ");
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
              startFunction(firstFunctionElement.retrieveLocationElement().getLocationNode(), true));
          functions.add(firstFunctionStackElement);
          newFunctionStack.push(firstFunctionStackElement);
          stack.push(newFunctionStack);
        }
        else if(edge instanceof FunctionReturnEdge){
          stack.pop();
        }
        else{
          lastStackElement.write(processSimpleEdge(edge));
        }
      }

      // this is the end of the condition, determine whether we should continue or backtrack
      else if(sizeOfChildsParents > 1){
        CBMCStackElement lastStackElement = stack.peek().peek();
        int elemId = childElement.getElementId();

        if(!(edge instanceof BlankEdge)){
          lastStackElement.write(processSimpleEdge(edge));
        }

        lastStackElement.write("goto label_" + elemId + ";");


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
          // all branches are processed, now decide which nodes to remove from the stack
          List<Stack<CBMCStackElement>> incomingStacks = mergeNode.getIncomingElements();

          Stack<CBMCStackElement> lastStack = processIncomingStacks(incomingStacks);
          stack.pop();
          stack.push(lastStack);
          lastStack.peek().write("label_" + elemId + ": ;");
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
      retList.add(stackElem.getCode().append("\n}"));
    }

    return retList;
  }


  //  private static void processClosedBranches(CBMCStackElement pElem,
  //      Stack<CBMCStackElement> pPeek) {
  //    while(true){
  //      if(pPeek.pop().equals(pElem)){
  //        return;
  //      }
  //    }
  //  }

  private static Stack<CBMCStackElement> processIncomingStacks(
      List<Stack<CBMCStackElement>> pIncomingStacks) {

    Stack<CBMCStackElement> maxStack = null;
    int maxSizeOfStack = 0;
    
    for(Stack<CBMCStackElement> stack: pIncomingStacks){
      while(true){
        if(stack.peek().isClosedBefore()){
          stack.pop();
        }
        else{
          break;
        }
      }
      if(stack.size() > maxSizeOfStack){
        maxStack = stack;
        maxSizeOfStack = maxStack.size();
      }
    }

    return maxStack;

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
      //      return ("if(! (" + lAssumptionString + ")) { return (0); }");  
    }
    case StatementEdge: {
      StatementEdge lStatementEdge = (StatementEdge)pCFAEdge;

      IASTExpression lExpression = lStatementEdge.getExpression();

      String ret = "";

      if (lExpression != null) {
        ret = lStatementEdge.getExpression().getRawSignature() + ";";
      }

      return (ret);
    }
    case ReturnStatementEdge: {
      ReturnStatementEdge lStatementEdge = (ReturnStatementEdge)pCFAEdge;

      IASTExpression lExpression = lStatementEdge.getExpression();

      if (lExpression != null) {
        return "return " + lExpression.getRawSignature() + ";";
      } else {
        return "return;";
      }
    }
    case DeclarationEdge: {
      DeclarationEdge lDeclarationEdge = (DeclarationEdge)pCFAEdge;

      if (lDeclarationEdge.isGlobal()) {
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

  private static String processFunctionCall(CFAEdge pCFAEdge){

    FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge)pCFAEdge;

    String lFunctionName = lFunctionCallEdge.getSuccessor().getFunctionName();

    List<String> lArguments = Lists.transform(lFunctionCallEdge.getArguments(), new Function<IASTNode, String>() {
      @Override
      public String apply(IASTNode pArg0) {
        return pArg0.getRawSignature();
      }
    });
    String lArgumentString = "(" + Joiner.on(", ").join(lArguments) + ")";

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

  private static String startFunction(CFANode pNode, boolean pAddIndex) {
    assert(pNode != null);
    assert(pNode instanceof FunctionDefinitionNode);

    FunctionDefinitionNode lFunctionDefinitionNode = (FunctionDefinitionNode)pNode;

    List<String> parameters = new ArrayList<String>();
    for (IASTSimpleDeclaration lFunctionParameter : lFunctionDefinitionNode.getFunctionParameters()) {
      parameters.add(lFunctionParameter.getRawSignature());
    }

    String lFunctionHeader =
        lFunctionDefinitionNode.getFunctionDefinition().getDeclSpecifier().getRawSignature()
      + " "
      + lFunctionDefinitionNode.getFunctionName()
      + (pAddIndex ? "_" + mFunctionIndex++ : "") 
      + "(" + Joiner.on(", ").join(parameters) + ")";

    mFunctionDecls.add(lFunctionHeader + ";");

    return lFunctionHeader + " {\n";
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
