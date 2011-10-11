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
package org.sosy_lab.cpachecker.core.algorithm.cbmctools;

import static com.google.common.collect.Iterables.concat;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
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
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class AbstractPathToCTranslator {

  private static Function<IASTNode, String> RAW_SIGNATURE_FUNCTION = new Function<IASTNode, String>() {
    @Override
    public String apply(IASTNode pArg0) {
      return pArg0.getRawSignature();
    }
  };

  private final List<String> mGlobalDefinitionsList = new ArrayList<String>();
  private final List<String> mFunctionDecls = new ArrayList<String>();
  private int mFunctionIndex = 0;

  private AbstractPathToCTranslator() { }

  public static String translatePaths(ARTElement artRoot, Collection<ARTElement> elementsOnErrorPath) {
    AbstractPathToCTranslator translator = new AbstractPathToCTranslator();

    // Add the original function declarations to enable read-only use of function pointers;
    // there will be no code for these functions, so they can never be called via the function
    // pointer properly; a real solution requires function pointer support within the CPA
    // providing location/successor information
    // TODO: this set of function declarations could perhaps be cached

    Set<CFANode> allCFANodes = CFAUtils.transitiveSuccessors(extractLocation(artRoot), true);

    for (CFAFunctionDefinitionNode node : Iterables.filter(allCFANodes, CFAFunctionDefinitionNode.class)) {
      // this adds the function declaration to mFunctionDecls
      translator.startFunction(node, false);
    }

    List<String> lFunctionBodies = translator.translatePath(artRoot, elementsOnErrorPath);

    List<String> includeList = new ArrayList<String>();
    includeList.add("#include<stdlib.h>");
    includeList.add("#include<stdio.h>");
    String ret = Joiner.on('\n').join(concat(includeList, translator.mGlobalDefinitionsList, translator.mFunctionDecls, lFunctionBodies));

    // replace nondet keyword with cbmc nondet keyword
    ret = ret.replaceAll("__BLAST_NONDET___0", "nondet_int()");
    ret = ret.replaceAll("__BLAST_NONDET", "nondet_int()");

    return ret;
  }

  private List<String> translatePath(final ARTElement firstElement, Collection<ARTElement> pElementsOnPath) {

    // waitlist for the edges to be processed
    List<CBMCEdge> waitlist = new ArrayList<CBMCEdge>();
    // map of nodes to check end of a condition
    Map<Integer, CBMCMergeNode> mergeNodes = new HashMap<Integer, CBMCMergeNode>();
    // list of functions - a function is represented by its first stack element and we get
    // the code for the function recursively starting from that node
    List<CBMCStackElement> functions = new ArrayList<CBMCStackElement>();

    {
      // the first element should have one child
      // TODO add more children support later
      ARTElement firstElementsChild = Iterables.getOnlyElement(firstElement.getChildren());

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
    }

    while (!waitlist.isEmpty()) {
      // we need to sort the list based on art element id because we have to process
      // the edges in topological sort
      Collections.sort(waitlist);

      // get the first element in the list (this is the smallest element when topologically sorted)
      CBMCEdge nextCBMCEdge = waitlist.remove(0);

      //    parentElement = nextCBMCEdge.getParentElement();
      ARTElement childElement = nextCBMCEdge.getChildElement();
      CFAEdge edge = nextCBMCEdge.getEdge();
      Stack<Stack<CBMCStackElement>> stack = nextCBMCEdge.getStack();

      // clone stack to have a different representation of the function calls and conditions
      // every element
      stack = cloneStack(stack);
      CBMCStackElement lastStackElement = stack.peek().peek();

      // how many parents does the child have?
      int noOfParents = childElement.getParents().size();
      assert noOfParents >= 1;

      if (childElement.isTarget()) {
        lastStackElement.write("assert(0); // target state ");
        assert noOfParents == 1 : "Merging target states is not supported";
      }

      // handle the edge

      if (edge instanceof FunctionCallEdge) {
        // if this is a function call edge we need to create a new element and push
        // it to the topmost stack to represent the function
        assert noOfParents == 1 : "Merging elements directly after function calls is not supported";

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

      } else if (edge instanceof FunctionReturnEdge) {
        assert noOfParents == 1 : "Merging elements directly after function returns is not supported";
        stack.pop();

      } else {
        lastStackElement.write(processSimpleEdge(edge));
      }

      // handle merging if necessary

      if (noOfParents > 1) {
        // this is the end of a condition, determine whether we should continue or backtrack

        int elemId = childElement.getElementId();
        lastStackElement.write("goto label_" + elemId + ";");

        // get the merge node for that node
        CBMCMergeNode mergeNode = mergeNodes.get(elemId);
        // if null create new and put in the map
        if (mergeNode == null) {
          mergeNode = new CBMCMergeNode(elemId);
          mergeNodes.put(elemId, mergeNode);
        }

        // this tells us the number of edges (entering that node) processed so far
        int noOfProcessedBranches = mergeNode.addBranch(nextCBMCEdge);

        // if all edges are processed
        if (noOfParents == noOfProcessedBranches) {
          // all branches are processed, now decide which nodes to remove from the stack
          List<Stack<CBMCStackElement>> incomingStacks = mergeNode.getIncomingElements();

          Stack<CBMCStackElement> lastStack = processIncomingStacks(incomingStacks);
          stack.pop();
          stack.push(lastStack);
          lastStack.peek().write("label_" + elemId + ": ;");

        } else {
          continue;
        }
      }

      // find the next elements to add to the waitlist

      List<ARTElement> relevantChildrenOfElement = getRelevantChildrenOfElement(childElement, pElementsOnPath);

      // if there is only one child on the path
      if (relevantChildrenOfElement.size() == 1) {
        // get the next ART element, create a new edge using the same stack and add it to the waitlist
        ARTElement elem = relevantChildrenOfElement.get(0);
        CFAEdge e = childElement.getEdgeToChild(elem);
        CBMCEdge newEdge = new CBMCEdge(childElement, elem, e, stack);
        waitlist.add(newEdge);

      } else if (relevantChildrenOfElement.size() > 1) {
        // if there are more than one relevant child, then this is a condition
        // we need to update the stack
        assert relevantChildrenOfElement.size() == 2;
        int ind = 0;
        for (ARTElement elem: relevantChildrenOfElement) {
          Stack<Stack<CBMCStackElement>> newCondStack = cloneStack(stack);
          CFAEdge e = childElement.getEdgeToChild(elem);
          Stack<CBMCStackElement> lastStackOfFunction = newCondStack.peek();
          assert e instanceof AssumeEdge;
          AssumeEdge assumeEdge = (AssumeEdge)e;
          // create a new
          CBMCStackElement newStackElement = new CBMCStackElement(childElement.getElementId(), assumeEdge);

          boolean truthAssumption = assumeEdge.getTruthAssumption();

          String cond = "";

          if (ind == 0) {
            cond = "if";
          } else if (ind == 1) {
            cond = "else if";
          } else {
            assert false;
          }
          ind++;

          if (truthAssumption) {
            lastStackOfFunction.peek().write(cond + "(" + assumeEdge.getExpression().getRawSignature() + ") {");
            lastStackOfFunction.peek().write(newStackElement);
            lastStackOfFunction.peek().write("}");
          } else {
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


    List<String> retList = new ArrayList<String>();

    for (CBMCStackElement stackElem: functions) {
      retList.add(stackElem.getCode().append("\n}").toString());
    }

    return retList;
  }

  private List<ARTElement> getRelevantChildrenOfElement(ARTElement pElement,
      Collection<ARTElement> pElementsOnPath) {
    List<ARTElement> relevantChildrenOfElement = new ArrayList<ARTElement>();

    // if it has only one child it is on the path to error
    if (pElement.getChildren().size() == 1) {
      relevantChildrenOfElement.addAll(pElement.getChildren());

    } else {
      // else find out whether children are on the path to error
      for (ARTElement child: pElement.getChildren()) {
        if (pElementsOnPath.contains(child)) {
          relevantChildrenOfElement.add(child);
        }
      }
    }
    return relevantChildrenOfElement;
  }


  //  private static void processClosedBranches(CBMCStackElement pElem,
  //      Stack<CBMCStackElement> pPeek) {
  //    while (true) {
  //      if (pPeek.pop().equals(pElem)) {
  //        return;
  //      }
  //    }
  //  }

  private static Stack<CBMCStackElement> processIncomingStacks(
      List<Stack<CBMCStackElement>> pIncomingStacks) {

    Stack<CBMCStackElement> maxStack = null;
    int maxSizeOfStack = 0;

    for (Stack<CBMCStackElement> stack: pIncomingStacks) {
      while (true) {
        if (stack.peek().isClosedBefore()) {
          stack.pop();
        } else {
          break;
        }
      }
      if (stack.size() > maxSizeOfStack) {
        maxStack = stack;
        maxSizeOfStack = maxStack.size();
      }
    }

    return maxStack;

  }

  private String processSimpleEdge(CFAEdge pCFAEdge) {

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
      } else {
        lAssumptionString = "!(" + lExpressionString + ")";
      }

      return ("__CPROVER_assume(" + lAssumptionString + ");");
//    return ("if(! (" + lAssumptionString + ")) { return (0); }");
    }
    case StatementEdge: {
      StatementEdge lStatementEdge = (StatementEdge)pCFAEdge;

      return lStatementEdge.getStatement().getRawSignature() + ";";
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
      } else {
        return lDeclarationEdge.getRawStatement();
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
      assert false : "CallToReturnEdge in counterexample path: " + pCFAEdge;

      break;
    }

    default: {
      assert false  : "Unexpected edge " + pCFAEdge + " of type " + pCFAEdge.getEdgeType();
    }
    }

    return "";
  }

  private String processFunctionCall(CFAEdge pCFAEdge) {

    FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge)pCFAEdge;

    String lFunctionName = lFunctionCallEdge.getSuccessor().getFunctionName();

    List<String> lArguments = Lists.transform(lFunctionCallEdge.getArguments(), RAW_SIGNATURE_FUNCTION);
    String lArgumentString = "(" + Joiner.on(", ").join(lArguments) + ")";

    CFAEdge summaryEdge = lFunctionCallEdge.getPredecessor().getLeavingSummaryEdge();
    IASTFunctionCall expressionOnSummaryEdge = ((CallToReturnEdge)summaryEdge).getExpression();
    if (expressionOnSummaryEdge instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement assignExp = (IASTFunctionCallAssignmentStatement)expressionOnSummaryEdge;
      String assignedVarName = assignExp.getLeftHandSide().getRawSignature();
      return assignedVarName + " = " + lFunctionName + "_" + mFunctionIndex + lArgumentString + ";";

    } else {
      assert expressionOnSummaryEdge instanceof IASTFunctionCallStatement;
      return lFunctionName + "_" + mFunctionIndex + lArgumentString + ";";
    }
  }

  private String startFunction(CFANode pNode, boolean pAddIndex) {
    assert pNode != null;
    assert pNode instanceof FunctionDefinitionNode;

    FunctionDefinitionNode lFunctionDefinitionNode = (FunctionDefinitionNode)pNode;

    List<String> parameters = new ArrayList<String>();
    for (IASTSimpleDeclaration lFunctionParameter : lFunctionDefinitionNode.getFunctionParameters()) {
      parameters.add(lFunctionParameter.getRawSignature());
    }

    String lFunctionHeader = lFunctionDefinitionNode.getFunctionDefinition().getRawSignature();
    if (pAddIndex) {
      lFunctionHeader = lFunctionHeader.replaceFirst(
          lFunctionDefinitionNode.getFunctionName() + "\\(",
          lFunctionDefinitionNode.getFunctionName() + "_" + mFunctionIndex++ + "(");
    }

    mFunctionDecls.add(lFunctionHeader + ";");

    return lFunctionHeader + " {\n";
  }

  private Stack<Stack<CBMCStackElement>> cloneStack(Stack<Stack<CBMCStackElement>> pStack) {

    Stack<Stack<CBMCStackElement>>  ret = new Stack<Stack<CBMCStackElement>>();
    Iterator<Stack<CBMCStackElement>> it = pStack.iterator();
    while (it.hasNext()) {
      Stack<CBMCStackElement> stackItem = it.next();
      Stack<CBMCStackElement> newRetStack = new Stack<CBMCStackElement>();
      Iterator<CBMCStackElement> newIt = stackItem.iterator();
      while (newIt.hasNext()) {
        CBMCStackElement newStackElem = newIt.next();
        newRetStack.push(newStackElem);
      }
      ret.push(newRetStack);
    }
    return ret;
  }
}
