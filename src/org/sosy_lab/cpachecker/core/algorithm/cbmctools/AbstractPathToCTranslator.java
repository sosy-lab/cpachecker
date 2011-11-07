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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class AbstractPathToCTranslator {

  private static Function<IASTNode, String> RAW_SIGNATURE_FUNCTION = new Function<IASTNode, String>() {
    @Override
    public String apply(IASTNode pArg0) {
      return pArg0.getRawSignature();
    }
  };

  // This set contains all ARTElements on the path(s) to the error
  // (we ignore all other parts of the ART when creating the C program).
  private final Set<ARTElement> elementsOnPath;

  private final List<String> mGlobalDefinitionsList = new ArrayList<String>();
  private final List<String> mFunctionDecls = new ArrayList<String>();
  private int mFunctionIndex = 0;

  // list of functions - a function is represented by its first stack element and we get
  // the code for the function recursively starting from that node
  private final List<FunctionBody> mFunctionBodies = new ArrayList<FunctionBody>();

  private AbstractPathToCTranslator(Set<ARTElement> pElementsOnPath) {
    elementsOnPath = pElementsOnPath;
  }

  public static String translatePaths(CFA cfa, ARTElement artRoot, Set<ARTElement> elementsOnErrorPath) {
    AbstractPathToCTranslator translator = new AbstractPathToCTranslator(elementsOnErrorPath);

    // Add the original function declarations to enable read-only use of function pointers;
    // there will be no code for these functions, so they can never be called via the function
    // pointer properly; a real solution requires function pointer support within the CPA
    // providing location/successor information
    for (CFAFunctionDefinitionNode node : cfa.getAllFunctionHeads()) {
      FunctionDefinitionNode pNode = (FunctionDefinitionNode)node;

      String lFunctionHeader = pNode.getFunctionDefinition().getRawSignature();

      translator.mFunctionDecls.add(lFunctionHeader + ";");
    }

    translator.translatePath(artRoot); // this fills all the lists in translator

    List<String> includeList = new ArrayList<String>();

    // do not include stdlib.h, as some examples (ntdrivers) define
    // "typedef unsigned short wchar_t;" that is also defined in stdlib.h
    // as "typedef int wchar_t;" - these contradicting definitions make cbmc fail
//  includeList.add("#include<stdlib.h>");
    includeList.add("#include<stdio.h>");
    return Joiner.on('\n').join(concat(includeList,
                                             translator.mGlobalDefinitionsList,
                                             translator.mFunctionDecls,
                                             translator.mFunctionBodies));
  }

  private void translatePath(final ARTElement firstElement) {
    // waitlist for the edges to be processed
    List<CBMCEdge> waitlist = new ArrayList<CBMCEdge>();

    // map of nodes to check end of a condition
    Map<Integer, CBMCMergeNode> mergeNodes = new HashMap<Integer, CBMCMergeNode>();

    // create initial element
    {
      Stack<Stack<CBMCStackElement>> newStack = new Stack<Stack<CBMCStackElement>>();

      // create the first function and put in into newStack
      startFunction(firstElement, newStack);

      waitlist.addAll(getRelevantChildrenOfElement(firstElement, newStack));
    }

    while (!waitlist.isEmpty()) {
      // we need to sort the list based on art element id because we have to process
      // the edges in topological sort
      Collections.sort(waitlist);

      // get the first element in the list (this is the smallest element when topologically sorted)
      CBMCEdge nextCBMCEdge = waitlist.remove(0);

      waitlist.addAll(handleEdge(nextCBMCEdge, mergeNodes));
    }
  }

  private String startFunction(ARTElement firstFunctionElement, Stack<Stack<CBMCStackElement>> currentStack) {
    // create the first stack element using the first element of the function
    FunctionDefinitionNode functionStartNode = (FunctionDefinitionNode) firstFunctionElement.retrieveLocationElement().getLocationNode();
    String freshFunctionName = getFreshFunctionName(functionStartNode);

    String lFunctionHeader = functionStartNode.getFunctionDefinition().getRawSignature();
    lFunctionHeader = lFunctionHeader.replaceFirst(
          Pattern.quote(functionStartNode.getFunctionName() + "("),
          freshFunctionName + "(");
    // lFunctionHeader is for example "void foo_99(int a)"

    CBMCStackElement firstFunctionStackElement = new CBMCStackElement(firstFunctionElement.getElementId(),
        lFunctionHeader);

    // create a new stack to save conditions in that function
    Stack<CBMCStackElement> newFunctionStack = new Stack<CBMCStackElement>();
    newFunctionStack.push(firstFunctionStackElement);

    // register function
    mFunctionDecls.add(lFunctionHeader + ";");
    mFunctionBodies.add(new FunctionBody(firstFunctionStackElement));
    currentStack.push(newFunctionStack); // add function to current stack
    return freshFunctionName;
  }

  private Collection<CBMCEdge> handleEdge(CBMCEdge nextCBMCEdge, Map<Integer, CBMCMergeNode> mergeNodes) {
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

      // create function and put in onto stack
      String freshFunctionName = startFunction(childElement, stack);

      // write summary edge to the caller site (with the new unique function name)
      lastStackElement.write(processFunctionCall(edge, freshFunctionName));

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
        return Collections.emptySet();
      }
    }

    return getRelevantChildrenOfElement(childElement, stack);
  }

  private Collection<CBMCEdge> getRelevantChildrenOfElement(
      ARTElement currentElement, Stack<Stack<CBMCStackElement>> currentStack) {
    // find the next elements to add to the waitlist

    Set<ARTElement> relevantChildrenOfElement = Sets.intersection(currentElement.getChildren(), elementsOnPath).immutableCopy();

    // if there is only one child on the path
    if (relevantChildrenOfElement.size() == 1) {
      // get the next ART element, create a new edge using the same stack and add it to the waitlist
      ARTElement elem = Iterables.getOnlyElement(relevantChildrenOfElement);
      CFAEdge e = currentElement.getEdgeToChild(elem);
      CBMCEdge newEdge = new CBMCEdge(currentElement, elem, e, currentStack);
      return Collections.singleton(newEdge);

    } else if (relevantChildrenOfElement.size() > 1) {
      // if there are more than one relevant child, then this is a condition
      // we need to update the stack
      assert relevantChildrenOfElement.size() == 2;
      Collection<CBMCEdge> result = new ArrayList<CBMCEdge>(2);
      int ind = 0;
      for (ARTElement elem: relevantChildrenOfElement) {
        Stack<Stack<CBMCStackElement>> newCondStack = cloneStack(currentStack);
        CFAEdge e = currentElement.getEdgeToChild(elem);
        Stack<CBMCStackElement> lastStackOfFunction = newCondStack.peek();
        assert e instanceof AssumeEdge;
        AssumeEdge assumeEdge = (AssumeEdge)e;
        boolean truthAssumption = assumeEdge.getTruthAssumption();

        String cond = "";

        if (ind == 0) {
          cond = "if ";
        } else if (ind == 1) {
          cond = "else if ";
        } else {
          assert false;
        }
        ind++;

        if (truthAssumption) {
          cond += "(" + assumeEdge.getExpression().getRawSignature() + ")";
        } else {
          cond += "(!(" + assumeEdge.getExpression().getRawSignature() + "))";
        }

        // create a new stack element
        CBMCStackElement newStackElement = new CBMCStackElement(currentElement.getElementId(), assumeEdge, cond);
        lastStackOfFunction.peek().write(newStackElement);

        lastStackOfFunction.push(newStackElement);
        CBMCEdge newEdge = new CBMCEdge(currentElement, elem, e, newCondStack);
        result.add(newEdge);
      }
      return result;
    }
    return Collections.emptyList();
  }

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

  private String processFunctionCall(CFAEdge pCFAEdge, String functionName) {

    FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge)pCFAEdge;

    List<String> lArguments = Lists.transform(lFunctionCallEdge.getArguments(), RAW_SIGNATURE_FUNCTION);
    String lArgumentString = "(" + Joiner.on(", ").join(lArguments) + ")";

    CallToReturnEdge summaryEdge = lFunctionCallEdge.getPredecessor().getLeavingSummaryEdge();
    if (summaryEdge == null) {
      // no summary edge, i.e., no return to this function (CFA was pruned)
      // we don't need to care whether this was an assignment or just a function call
      return functionName + lArgumentString + ";";
    }

    IASTFunctionCall expressionOnSummaryEdge = summaryEdge.getExpression();
    if (expressionOnSummaryEdge instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement assignExp = (IASTFunctionCallAssignmentStatement)expressionOnSummaryEdge;
      String assignedVarName = assignExp.getLeftHandSide().getRawSignature();
      return assignedVarName + " = " + functionName + lArgumentString + ";";

    } else {
      assert expressionOnSummaryEdge instanceof IASTFunctionCallStatement;
      return functionName + lArgumentString + ";";
    }
  }

  private String getFreshFunctionName(CFAFunctionDefinitionNode functionStartNode) {
    return functionStartNode.getFunctionName() + "_" + mFunctionIndex++;
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


  private static class FunctionBody {
    // a function is represented by its first stack element and we get
    // the code for the function recursively starting from that node
    private final CBMCStackElement firstElement;

    private FunctionBody(CBMCStackElement pFirstElement) {
      firstElement = pFirstElement;
    }

    @Override
    public String toString() {
      return firstElement.getCode();
    }
  }
}