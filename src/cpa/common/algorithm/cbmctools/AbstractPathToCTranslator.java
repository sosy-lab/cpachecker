/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
/**
 * 
 */
package cpa.common.algorithm.cbmctools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
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

import cfa.CFAMap;
import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.MultiDeclarationEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import cmdline.CPAMain;

import common.Pair;

import cpa.art.ARTElement;

/**
 * @author holzera
 *
 */
public class AbstractPathToCTranslator {
  private static PrintWriter mGlobalThingsWriter = null;
  private static List<String> mFunctionDecls = null;
  private static int mFunctionIndex = 0;
  private static List<String> mProgramTexts;

  public static String translatePaths(CFAMap pCfas, List<ARTElement> elementsOnErrorPath) {

    String ret = "";
    // Add the original function declarations to enable read-only use of function pointers;
    // there will be no code for these functions, so they can never be called via the function
    // pointer properly; a real solution requires function pointer support within the CPA
    // providing location/successor information
    mFunctionDecls = new ArrayList<String>();
    for (CFAFunctionDefinitionNode node : pCfas.cfaMapIterator()) {
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

    List<String> lTranslation = translatePath(elementsOnErrorPath);

    // TODO remove output
    //  System.out.println("Written program text:");

    //try {

    //PrintWriter lWriter = new PrintWriter(new FileWriter("andi_tmp.c"));

    for (String lProgramString : lTranslation) {
      ret = ret + lProgramString + "\n";
      //lWriter.println(lProgramString);
    }

    //lWriter.close();

    //}
    // If this is activated again, it should call CPAMain.logManager.logException for
    // documenting the exception. This automatically prints the stack trace as well.
    //catch(Exception e) {
    //  e.printStackTrace();
    //}
    return ret;
  }

  //public static List<String> translatePath(Deque<AbstractElementWithLocation> pAbstractPath) {
  //assert(pAbstractPath != null);
  //assert(pAbstractPath.size() > 0);



  //AbstractElementWithLocation lPredecessorElement = pAbstractPath.getFirst();

  ////check for special case (= path of length zero = path with only one element)
  //if (pAbstractPath.size() == 1) {
  ////special case

  ////stack for program texts of different functions
  //Stack<StringWriter> lProgramTextStack = new Stack<StringWriter>();

  ////list of already finished program texts
  //mProgramTexts = new ArrayList<String>();

  //startFunction(0, lPredecessorElement.getLocationNode(), lProgramTextStack);

  //endFunction(lProgramTextStack);

  //return mProgramTexts;
  //}


  //boolean first = true;

  //List<CFAEdge> lEdges = new ArrayList<CFAEdge>();

  //for (AbstractElementWithLocation lElement : pAbstractPath) {
  //if (first) {
  //first = false;
  //continue;
  //}

  //CFANode lPredecessorNode = lPredecessorElement.getLocationNode();
  //CFANode lNode = lElement.getLocationNode();

  ////reconstruct edge
  //int lNumberOfFoundEdges = 0;

  //for (int lIndex = 0; lIndex < lPredecessorNode.getNumLeavingEdges(); lIndex++) {
  //CFAEdge lEdge = lPredecessorNode.getLeavingEdge(lIndex);

  //if (lEdge.getSuccessor().equals(lNode)) {
  //lEdges.add(lEdge);
  //lNumberOfFoundEdges++;
  //}
  //}

  //assert(lNumberOfFoundEdges == 1);

  //lPredecessorElement = lElement;
  //}

  //return translatePath(lEdges);
  //}

  public static void endFunction(Stack<StringWriter> pProgramTextStack) {
    assert(pProgramTextStack != null);

    StringWriter lStringWriter = pProgramTextStack.pop();
    PrintWriter pProgramText = new PrintWriter(lStringWriter);

    // TODO This is a hack
    /*if (pProgramTextStack.isEmpty()) {
      pProgramText.println("__CPROVER_assert(0, \"path feasible\");");
    }*/

    // finish function
    pProgramText.println("}");

    // function program text is finished, add it to set of program texts
    mProgramTexts.add(lStringWriter.toString());
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

  //public static List<String> translatePath(List<CFAEdge> pAbstractPath) {
  //int lFunctionIndex = 0;

  ////create print writer for global typedefs and declarations
  //StringWriter lGlobalThingsStringWriter = new StringWriter();
  //mGlobalThingsWriter = new PrintWriter(lGlobalThingsStringWriter);

  ////stack for program texts of different functions
  //Stack<StringWriter> lProgramTextStack = new Stack<StringWriter>();

  ////list of already finished program texts
  //List<String> lProgramTexts = new ArrayList<String>();

  ////program text for start function 
  //PrintWriter lProgramText = startFunction(lFunctionIndex, pAbstractPath.get(0).getPredecessor(), lProgramTextStack);

  //lFunctionIndex++;


  ////process edges
  //for (CFAEdge lEdge : pAbstractPath) {
  //if(lEdge.getSuccessor() instanceof CFAErrorNode){
  //lProgramText.println("assert(0);");
  //continue;
  //}
  //switch (lEdge.getEdgeType()) {
  //case BlankEdge: {
  ////nothing to do
  //break;
  //}
  //case AssumeEdge: {
  //AssumeEdge lAssumeEdge = (AssumeEdge)lEdge;

  //String lExpressionString = lAssumeEdge.getExpression().getRawSignature();

  //String lAssumptionString;

  //if (lAssumeEdge.getTruthAssumption()) {
  //lAssumptionString = lExpressionString;
  //}
  //else {
  //lAssumptionString = "!(" + lExpressionString + ")";
  //}

  //lProgramText.println("__CPROVER_assume(" + lAssumptionString + ");");

  //break;
  //}
  //case StatementEdge: {
  //StatementEdge lStatementEdge = (StatementEdge)lEdge;

  //IASTExpression lExpression = lStatementEdge.getExpression();

  //if (lExpression != null) {
  //if(lStatementEdge.isJumpEdge()){
  //lProgramText.print("return ");
  //}
  //lProgramText.println(lStatementEdge.getExpression().getRawSignature() + ";");
  //}

  //break;
  //}
  //case DeclarationEdge: {
  //DeclarationEdge lDeclarationEdge = (DeclarationEdge)lEdge;
  //if (lDeclarationEdge instanceof cfa.objectmodel.c.GlobalDeclarationEdge) {
  //mGlobalThingsWriter.println(lDeclarationEdge.getRawStatement());
  //}
  //else {
  //lProgramText.println(lDeclarationEdge.getRawStatement());
  //}

  ///*IASTDeclarator[] lDeclarators = lDeclarationEdge.getDeclarators();

  //assert(lDeclarators.length == 1);

  ////TODO what about function pointers?
  //lProgramText.println(lDeclarationEdge.getDeclSpecifier().getRawSignature() + " " + lDeclarators[0].getRawSignature() + ";");
  //*/      
  //break;
  //}
  //case FunctionCallEdge: {
  //FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge)lEdge;

  //String lFunctionName = lFunctionCallEdge.getSuccessor().getFunctionName();


  //String lArgumentString = "(";

  //boolean lFirstArgument = true;

  //IASTExpression[] lArguments = lFunctionCallEdge.getArguments();

  //if (lArguments != null) {
  //for (IASTExpression lArgument : lArguments) {
  //if (lFirstArgument) {
  //lFirstArgument = false;
  //}
  //else {
  //lArgumentString += ", ";
  //}

  //lArgumentString += lArgument.getRawSignature();
  //}
  //}

  //lArgumentString += ")";

  //CFAEdge summaryEdge = lFunctionCallEdge.getPredecessor().getLeavingSummaryEdge();
  //IASTExpression expressionOnSummaryEdge = ((CallToReturnEdge)summaryEdge).getExpression();
  //if(expressionOnSummaryEdge instanceof IASTBinaryExpression){
  //IASTBinaryExpression binaryExp = (IASTBinaryExpression) expressionOnSummaryEdge;
  //assert(binaryExp.getOperator() == IASTBinaryExpression.op_assign);
  //String assignedVarName = binaryExp.getOperand1().getRawSignature();
  //lProgramText.println(assignedVarName + " = " + lFunctionName + "_" + lFunctionIndex + lArgumentString + ";");

  //}
  //else{
  //assert(expressionOnSummaryEdge instanceof IASTFunctionCallExpression);
  //lProgramText.println(lFunctionName + "_" + lFunctionIndex + lArgumentString + ";");
  //}


  //lProgramText = startFunction(lFunctionIndex, lFunctionCallEdge.getSuccessor(), lProgramTextStack);

  //lFunctionIndex++;

  //break;
  //}
  //case ReturnEdge: {
  //endFunction(lProgramTextStack, lProgramTexts);

  //lProgramText = new PrintWriter(lProgramTextStack.peek());

  //break;
  //}
  //case MultiStatementEdge: {
  //MultiStatementEdge lMultiStatementEdge = (MultiStatementEdge)lEdge;

  //for (IASTExpression lExpression : lMultiStatementEdge.getExpressions()) {
  //lProgramText.println(lExpression.getRawSignature() + ";");
  //}

  //break;
  //}
  //case MultiDeclarationEdge: {
  //MultiDeclarationEdge lMultiDeclarationEdge = (MultiDeclarationEdge)lEdge;

  //lProgramText.println(lMultiDeclarationEdge.getRawStatement());

  ///*List<IASTDeclarator[]> lDecls = lMultiDeclarationEdge.getDeclarators();

  //lMultiDeclarationEdge.getRawStatement()

  //for (IASTDeclarator[] lDeclarators : lDecls) {

  //}*/

  //break;
  //}
  //case CallToReturnEdge: {
  ////this should not have been taken
  //assert(false);

  //break;
  //}
  //default: {
  //assert(false);
  //}
  //}
  //}

  ////clean stack and finish functions
  //while (!lProgramTextStack.isEmpty()) {
  //endFunction(lProgramTextStack, lProgramTexts);
  //}

  //if (mFunctionDecls != null) {
  //for (String decl : mFunctionDecls) {
  //mGlobalThingsWriter.println(decl);
  //}
  //}
  //lProgramTexts.add(0, lGlobalThingsStringWriter.toString());

  //return lProgramTexts;
  //}

  public static List<String> translatePath(List<ARTElement> pElementsOnErrorPath){

    ARTElement firstElement = pElementsOnErrorPath.get(pElementsOnErrorPath.size()-1);

    // create print writer for global typedefs and declarations
    StringWriter lGlobalThingsStringWriter = new StringWriter();
    mGlobalThingsWriter = new PrintWriter(lGlobalThingsStringWriter);

    // stack for program texts of different functions
    Stack<StringWriter> lProgramTextStack = new Stack<StringWriter>();

    // list of already finished program texts
    mProgramTexts = new ArrayList<String>();

    // program text for start function 
    PrintWriter lProgramText = startFunction(mFunctionIndex, firstElement.getLocationNode(), lProgramTextStack);

    mFunctionIndex++;

    translatePath(pElementsOnErrorPath, lProgramText, lProgramTextStack);

    // clean stack and finish functions
    while (!lProgramTextStack.isEmpty()) {
      endFunction(lProgramTextStack);
    }

    if (mFunctionDecls != null) {
      for (String decl : mFunctionDecls) {
        mGlobalThingsWriter.println(decl);
      }
    }
    mProgramTexts.add(0, lGlobalThingsStringWriter.toString());
    return mProgramTexts;

  }

  public static void translatePath(List<ARTElement> pElementsOnPath, PrintWriter pProgramText, Stack<StringWriter> pProgramTextStack){

    ARTElement parentElement = pElementsOnPath.get(pElementsOnPath.size()-1);
    Stack<Pair<ARTElement, ARTElement>> elementsStack = new Stack<Pair<ARTElement,ARTElement>>();

    PrintWriter lProgramText = pProgramText;
    Stack<StringWriter> lProgramTextStack = pProgramTextStack;

    while(true){
      List<ARTElement> relevantChildrenOfElement = new ArrayList<ARTElement>();

      for(ARTElement child: parentElement.getChildren()){
        if(pElementsOnPath.contains(child)){
          relevantChildrenOfElement.add(child);
        }
      }

      ARTElement child = null;

      if(relevantChildrenOfElement.size() == 0){
        return;
      }

      else if(relevantChildrenOfElement.size() == 1){
        child = relevantChildrenOfElement.get(0);
      }

      else if(relevantChildrenOfElement.size() > 1){

        for(int i=relevantChildrenOfElement.size()-1; i>=0; i--){
          ARTElement otherChild = relevantChildrenOfElement.get(i);
          elementsStack.push(new Pair<ARTElement, ARTElement>(parentElement, otherChild));
        }

        Pair<ARTElement, ARTElement> elementPair = elementsStack.peek();
        ARTElement parent = elementPair.getFirst();
        child = elementPair.getSecond();

        assert(parentElement == parent);
        CFAEdge edge = CPAMain.getEdgeBetween(parent, child);

        assert(edge instanceof AssumeEdge);
        AssumeEdge assumeEdge = (AssumeEdge) edge;

        if(assumeEdge.getTruthAssumption()){
          lProgramText.println("if(" + assumeEdge.getExpression().getRawSignature() + ") {");
        }
        else{
          lProgramText.println("if(!(" + assumeEdge.getExpression().getRawSignature() + ")) {");
        }

        //      parentElement = child;
        //      continue;
      }

      System.out.println("==========");
      System.out.println("parent >>> " + parentElement);
      System.out.println("<<<<<<<<<<<");
      System.out.println("child >>> " + child);
      System.out.println("......................");

      assert(child != null);

      if(child.getParents().size() == 1){

        CFAEdge lEdge = CPAMain.getEdgeBetween(parentElement, child);
        assert(lEdge != null);

        //      process edges
        if(lEdge.getSuccessor() instanceof CFAErrorNode){
          //          assert(elementsStack.size() == 0);
          lProgramText.println("assert(0); // " +elementsStack.size() );
        }
        else{
          switch (lEdge.getEdgeType()) {
          case BlankEdge: {
            //          nothing to do
            break;
          }
          case AssumeEdge: {
            AssumeEdge lAssumeEdge = (AssumeEdge)lEdge;

            String lExpressionString = lAssumeEdge.getExpression().getRawSignature();

            String lAssumptionString;

            if (lAssumeEdge.getTruthAssumption()) {
              lAssumptionString = lExpressionString;
            }
            else {
              lAssumptionString = "!(" + lExpressionString + ")";
            }

            lProgramText.println("__CPROVER_assume(" + lAssumptionString + ");");

            break;
          }
          case StatementEdge: {
            StatementEdge lStatementEdge = (StatementEdge)lEdge;

            IASTExpression lExpression = lStatementEdge.getExpression();

            if (lExpression != null) {
              if(lStatementEdge.isJumpEdge()){
                lProgramText.print("return ");
              }
              lProgramText.println(lStatementEdge.getExpression().getRawSignature() + ";");
            }

            break;
          }
          case DeclarationEdge: {
            DeclarationEdge lDeclarationEdge = (DeclarationEdge)lEdge;
            if (lDeclarationEdge instanceof cfa.objectmodel.c.GlobalDeclarationEdge) {
              mGlobalThingsWriter.println(lDeclarationEdge.getRawStatement());
            }
            else {
              lProgramText.println(lDeclarationEdge.getRawStatement());
            }

            /*IASTDeclarator[] lDeclarators = lDeclarationEdge.getDeclarators();

    assert(lDeclarators.length == 1);

    // TODO what about function pointers?
    lProgramText.println(lDeclarationEdge.getDeclSpecifier().getRawSignature() + " " + lDeclarators[0].getRawSignature() + ";");
             */      
            break;
          }
          case FunctionCallEdge: {
            FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge)lEdge;

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
              lProgramText.println(assignedVarName + " = " + lFunctionName + "_" + mFunctionIndex + lArgumentString + ";");
            }
            else{
              assert(expressionOnSummaryEdge instanceof IASTFunctionCallExpression);
              lProgramText.println(lFunctionName + "_" + mFunctionIndex + lArgumentString + ";");
            }

            lProgramText = startFunction(mFunctionIndex, lFunctionCallEdge.getSuccessor(), lProgramTextStack);
            mFunctionIndex++;
            break;
          }
          case ReturnEdge: {
            endFunction(lProgramTextStack);
            lProgramText = new PrintWriter(lProgramTextStack.peek());
            break;
          }
          case MultiStatementEdge: {
            MultiStatementEdge lMultiStatementEdge = (MultiStatementEdge)lEdge;

            for (IASTExpression lExpression : lMultiStatementEdge.getExpressions()) {
              lProgramText.println(lExpression.getRawSignature() + ";");
            }

            break;
          }
          case MultiDeclarationEdge: {
            MultiDeclarationEdge lMultiDeclarationEdge = (MultiDeclarationEdge)lEdge;

            lProgramText.println(lMultiDeclarationEdge.getRawStatement());

            /*List<IASTDeclarator[]> lDecls = lMultiDeclarationEdge.getDeclarators();

    lMultiDeclarationEdge.getRawStatement()

    for (IASTDeclarator[] lDeclarators : lDecls) {

    }*/

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
        }
        parentElement = child;
      }

      while(true){
        if(child.getParents().size() > 1){

          System.out.println();
          System.out.println(" here             ---------------- ");
          System.out.println();

          lProgramText.println("}");

          assert(elementsStack.size() > 0);

          Pair<ARTElement, ARTElement> lastPairInStack = elementsStack.pop();
          ARTElement lastElementsParentInStack = lastPairInStack.getFirst();

          if(elementsStack.size() > 0){
            Pair<ARTElement, ARTElement> secondLastPairInStack = elementsStack.peek();
            ARTElement secondLastElementsParentInStack = secondLastPairInStack.getFirst();

            if(lastElementsParentInStack == secondLastElementsParentInStack){
              parentElement = secondLastPairInStack.getFirst();
              child = secondLastPairInStack.getSecond();

              CFAEdge edge = CPAMain.getEdgeBetween(parentElement, child);

              assert(edge instanceof AssumeEdge);
              AssumeEdge assumeEdge = (AssumeEdge) edge;

              System.out.println("writing if ///////////////////////// ");
              if(assumeEdge.getTruthAssumption()){
                lProgramText.println("else if(" + assumeEdge.getExpression().getRawSignature() + ") {");
              }
              else{
                lProgramText.println("else if(!(" + assumeEdge.getExpression().getRawSignature() + ")) {");
              }
            }

            if(parentElement.getParents().size() > 1){
              continue;
            }

          }
        }
        parentElement = child;
        break;
      }
    }
  }

  public static void translatePath(List<ARTElement> pElementsOnPath){

    ARTElement parentElement;
    ARTElement childElement;
    CFAEdge edge;
    Stack<Stack<CBMCStackElement>> stack;
    List<CBMCEdge> waitlist = new ArrayList<CBMCEdge>();
    Map<Integer, CBMCMergeNode> mergeNodes = new HashMap<Integer, CBMCMergeNode>();

    Map<String, CBMCStackElement> functions = new HashMap<String, CBMCStackElement>();

    ARTElement firstElement = pElementsOnPath.get(pElementsOnPath.size()-1);
    // the first element should have one child
    // TODO add more children support later
    assert(firstElement.getChildren().size() == 1);
    ARTElement firstElementsChild = (ARTElement)firstElement.getChildren().toArray()[0];
    CBMCStackElement firstStackElement = new CBMCStackElement(firstElement.getElementId(), 
        startFunction(mFunctionIndex++, firstElement.getLocationNode()));
    Stack<Stack<CBMCStackElement>> newStack = new Stack<Stack<CBMCStackElement>>();
    Stack<CBMCStackElement> newElementsStack = new Stack<CBMCStackElement>();
    newElementsStack.add(firstStackElement);
    newStack.add(newElementsStack);

    CBMCEdge firstEdge = new CBMCEdge(firstElement, firstElementsChild, 
        CPAMain.getEdgeBetween(firstElement, firstElementsChild), newStack);

    //  ARTElement firstElement = pElementsOnPath.get(pElementsOnPath.size()-1);
    //  parentElement = firstElement;

    // add the first edge and the first stack element
    waitlist.add(firstEdge);

    while(waitlist.size() > 0){

      Collections.sort(waitlist);
      CBMCEdge nextCBMCEdge = waitlist.remove(0);

      parentElement = nextCBMCEdge.getParentElement();
      childElement = nextCBMCEdge.getChildElement();
      edge = nextCBMCEdge.getEdge();
      stack = nextCBMCEdge.getStack();

      int sizeOfChildsParents = childElement.getParents().size();

      // this is not the end of the condition
      if(sizeOfChildsParents == 1){
        CBMCStackElement lastStackElement = stack.pop().peek();
        if(edge instanceof FunctionCallEdge){
          lastStackElement.write(processFunctionCall(edge));
          Stack<CBMCStackElement> newFunctionStack = new Stack<CBMCStackElement>();
          ARTElement firstFunctionElement = nextCBMCEdge.getChildElement();
          CBMCStackElement firstFunctionStackElement = new CBMCStackElement(firstFunctionElement.getElementId(), 
              startFunction(mFunctionIndex++, firstFunctionElement.getLocationNode()));
          newFunctionStack.push(firstFunctionStackElement);
          stack.push(newFunctionStack);
        }
        else if(edge instanceof ReturnEdge){
          lastStackElement.write("}");
        }
        else{
          lastStackElement.write(processSimpleEdge(edge));
        }
      }

      // this is the end of the condition, determine whether we should continue or backtrack
      else if(sizeOfChildsParents > 1){
        int nodeNumber = childElement.getLocationNode().getNodeNumber();
        CBMCMergeNode mergeNode = mergeNodes.get(nodeNumber);
        // if null create new and put in the map
        if(mergeNode == null){
          mergeNode = new CBMCMergeNode(nodeNumber);
          mergeNodes.put(nodeNumber, mergeNode);
        }

        int noOfProcessedBranches = mergeNode.addBranch(nextCBMCEdge);

        if(sizeOfChildsParents == noOfProcessedBranches){
          // all branches are processed, now decide which nodes to remove from the stack and
          // write to the file accordingly
          Set<Integer> setOfEndedBranches = mergeNode.getProcessedConditions();

          // traverse on the last stack and remove all elements that are in
          // setOfEndedBranches set. The remaining elements to be transferred to
          // the next elements stack
          while(true){
            CBMCStackElement elem = stack.peek().peek();
            int idOfElem = elem.getElementId();
            if(setOfEndedBranches.contains(idOfElem)){
              stack.pop();
              // close the bracket
              elem.write("}");
            }
            break;
          }
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
        ARTElement elem = relevantChildrenOfElement.get(0);
        CFAEdge e = CPAMain.getEdgeBetween(childElement, elem);
        Stack<Stack<CBMCStackElement>> clonedStack = cloneStack(stack);
        CBMCEdge newEdge = new CBMCEdge(childElement, elem, e, clonedStack);
        waitlist.add(newEdge);
      }

      else if(relevantChildrenOfElement.size() > 1){
        for(ARTElement elem: relevantChildrenOfElement){
          CFAEdge e = CPAMain.getEdgeBetween(childElement, elem);
          Stack<Stack<CBMCStackElement>> clonedStack = cloneStack(stack);
          Stack<CBMCStackElement> lastStackOfFunction = clonedStack.peek();
          assert(e instanceof AssumeEdge);
          CBMCStackElement newStackElement = new CBMCStackElement(elem.getElementId(), (AssumeEdge)e);
          lastStackOfFunction.push(newStackElement);
          CBMCEdge newEdge = new CBMCEdge(childElement, elem, e, clonedStack);
          waitlist.add(newEdge);
        }
      }
    }
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
      if (lDeclarationEdge instanceof cfa.objectmodel.c.GlobalDeclarationEdge) {
        mGlobalThingsWriter.println(lDeclarationEdge.getRawStatement());
      }
      else {
        return (lDeclarationEdge.getRawStatement());
      }

      /*IASTDeclarator[] lDeclarators = lDeclarationEdge.getDeclarators();

assert(lDeclarators.length == 1);

// TODO what about function pointers?
lProgramText.println(lDeclarationEdge.getDeclSpecifier().getRawSignature() + " " + lDeclarators[0].getRawSignature() + ";");
       */
    }

    case MultiStatementEdge: {
      //      MultiStatementEdge lMultiStatementEdge = (MultiStatementEdge)lEdge;
      //
      //      for (IASTExpression lExpression : lMultiStatementEdge.getExpressions()) {
      //        lProgramText.println(lExpression.getRawSignature() + ";");
      //      }
      //
      //      break;
      assert(false);
    }
    case MultiDeclarationEdge: {
      assert(false);
      //      MultiDeclarationEdge lMultiDeclarationEdge = (MultiDeclarationEdge)lEdge;
      //
      //      lProgramText.println(lMultiDeclarationEdge.getRawStatement());

      /*List<IASTDeclarator[]> lDecls = lMultiDeclarationEdge.getDeclarators();

lMultiDeclarationEdge.getRawStatement()

for (IASTDeclarator[] lDeclarators : lDecls) {

}*/

      //      break;
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

    lFunctionHeader += ") /n";

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
      Iterator<CBMCStackElement> newIt = newRetStack.iterator();
      while(newIt.hasNext()){
        CBMCStackElement newStackElem = newIt.next();
        newRetStack.push(newStackElem);
      }
      ret.push(newRetStack);
    }
    return ret;
  }
}
