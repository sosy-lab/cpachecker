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
package org.sosy_lab.cpachecker.util.cwriter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;

import com.google.common.collect.Iterables;

public class ARTToCTranslator {
  private static abstract class Statement {
    public abstract void translateToCode(StringBuffer buffer, int indent);

    protected static void writeIndent(StringBuffer buffer, int indent) {
      for(int i = 0; i < indent; i++) {
        buffer.append(" ");
      }
    }
  }

  private static class CompoundStatement extends Statement {
    private final List<Statement> statements;
    private final CompoundStatement outerBlock;

    public CompoundStatement() {
      this(null);
    }

    public CompoundStatement(CompoundStatement pOuterBlock) {
      statements = new ArrayList<Statement>();
      outerBlock = pOuterBlock;
    }

    public void addStatement(Statement statement) {
      statements.add(statement);
    }

    @Override
    public void translateToCode(StringBuffer buffer, int indent) {
      writeIndent(buffer, indent);
      buffer.append("{\n");

      for(Statement statement : statements) {
        statement.translateToCode(buffer, indent + 4);
      }

      writeIndent(buffer, indent);
      buffer.append("}\n");
    }

    public CompoundStatement getSurroundingBlock() {
      return outerBlock;
    }
  }

  private static class SimpleStatement extends Statement {
    private final String code;

    public SimpleStatement(String pCode) {
      code = pCode;
    }

    @Override
    public void translateToCode(StringBuffer buffer, int indent) {
      writeIndent(buffer, indent);
      buffer.append(code);
      buffer.append("\n");
    }
  }

  private static class FunctionBody extends Statement {
    private final String functionHeader;
    private final CompoundStatement functionBody;

    public FunctionBody(String pFunctionHeader, CompoundStatement pFunctionBody) {
      functionHeader = pFunctionHeader;
      functionBody = pFunctionBody;
    }

    public CompoundStatement getFunctionBody() {
      return functionBody;
    }

    @Override
    public void translateToCode(StringBuffer buffer, int indent) {
      writeIndent(buffer, indent);
      buffer.append(functionHeader);
      buffer.append("\n");

      functionBody.translateToCode(buffer, indent);
    }
  }

  private static class ARTEdge {
    private final ARTElement parent;
    private final ARTElement child;
    private final CFAEdge cfaEdge;
    private final CompoundStatement currentBlock;

    public ARTEdge(ARTElement pParent, ARTElement pChild, CFAEdge pCfaEdge, CompoundStatement pCurrentBlock) {
      parent = pParent;
      child = pChild;
      cfaEdge = pCfaEdge;
      currentBlock = pCurrentBlock;
    }

    public ARTElement getParentElement() {
      return parent;
    }

    public ARTElement getChildElement() {
      return child;
    }

    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }

    public CompoundStatement getCurrentBlock() {
      return currentBlock;
    }
  }

  private final List<String> globalDefinitionsList = new ArrayList<String>();
  private final Set<ARTElement> discoveredElements = new HashSet<ARTElement>();
  private final Set<ARTElement> mergeElements = new HashSet<ARTElement>();
  private FunctionBody mainFunctionBody;
  private static Collection<AbstractElement> reached;

  private ARTToCTranslator() { }

  public static String translateART(ARTElement artRoot, ReachedSet pReached) {
    reached = pReached.getReached();
    ARTToCTranslator translator = new ARTToCTranslator();

    translator.translate(artRoot);

    return translator.generateCCode();
  }

  private String generateCCode() {
    StringBuffer buffer = new StringBuffer();

    buffer.append("#include <stdio.h>\n");
    for(String globalDef : globalDefinitionsList) {
      buffer.append(globalDef + "\n");
    }

    mainFunctionBody.translateToCode(buffer, 0);

    return buffer.toString();
  }

  private void translate(ARTElement rootElement) {
    // waitlist for the edges to be processed
    Deque<ARTEdge> waitlist = new ArrayDeque<ARTEdge>(); //TODO: used to be sorted list and I don't know why yet ;-)

    startMainFunction(rootElement);
    getRelevantChildrenOfElement(rootElement, waitlist, mainFunctionBody.getFunctionBody());

    while (!waitlist.isEmpty()) {
      ARTEdge nextEdge = waitlist.pop();
      handleEdge(nextEdge, waitlist);
    }
  }

  private void startMainFunction(ARTElement firstFunctionElement) {
    FunctionDefinitionNode functionStartNode = (FunctionDefinitionNode) firstFunctionElement.retrieveLocationElement().getLocationNode();
    String lFunctionHeader = functionStartNode.getFunctionDefinition().getRawSignature();
    mainFunctionBody = new FunctionBody(lFunctionHeader, new CompoundStatement());
  }

  private void getRelevantChildrenOfElement(ARTElement currentElement, Deque<ARTEdge> waitlist, CompoundStatement currentBlock) {
    discoveredElements.add(currentElement);
    // generate label for element and add to current block if needed
    generateLabel(currentElement, currentBlock);

    // find the next elements to add to the waitlist
    Set<ARTElement> childrenOfElement = currentElement.getChildren();

    if (childrenOfElement.size() == 0) {
      // if there is no child of the element, maybe it was covered by other?
      if(currentElement.isCovered()) {
        //it was indeed covered; jump to element it was covered by
        currentBlock.addStatement(new SimpleStatement("goto label_" + currentElement.getCoveringElement().getElementId() + ";"));
      } else {
        currentBlock.addStatement(new SimpleStatement("return 1;"));
      }
    } else if (childrenOfElement.size() == 1) {
      // get the next ART element, create a new edge using the same stack and add it to the waitlist
      ARTElement child = Iterables.getOnlyElement(childrenOfElement);
      CFAEdge edgeToChild = currentElement.getEdgeToChild(child);
      pushToWaitlist(waitlist, currentElement, child, edgeToChild, currentBlock);
    } else if (childrenOfElement.size() > 1) {
      // if there are more than one children, then this is a condition
      assert childrenOfElement.size() == 2 : "branches with more than two options not supported yet (was the program prepocessed with CIL?)"; //TODO: why not btw?

      //collect edges of condition branch
      ArrayList<ARTEdge> result = new ArrayList<ARTEdge>(2);
      int ind = 0;
      for (ARTElement child : childrenOfElement) {
        CFAEdge edgeToChild = currentElement.getEdgeToChild(child);
        assert edgeToChild instanceof AssumeEdge : "something wrong: branch in ART without condition: " + edgeToChild;
        AssumeEdge assumeEdge = (AssumeEdge)edgeToChild;
        boolean truthAssumption = assumeEdge.getTruthAssumption();

        String cond = "";

        if (ind == 0) {
          cond = "if ";
        } else if (ind == 1) {
          cond = "else ";
        } else {
          assert false;
        }

        if(ind == 0) {
          if (truthAssumption) {
            cond += "(" + assumeEdge.getExpression().getRawSignature() + ")";
          } else {
            cond += "(!(" + assumeEdge.getExpression().getRawSignature() + "))";
          }
        }

        ind++;

        // create a new block starting with this condition
        CompoundStatement newBlock = addIfStatement(currentBlock, cond);
        result.add(new ARTEdge(currentElement, child, edgeToChild, newBlock));
      }

      //add edges in reversed order to waitlist
      for(int i = result.size()-1; i >= 0; i--) {
        ARTEdge e = result.get(i);
        pushToWaitlist(waitlist, e.getParentElement(), e.getChildElement(), e.getCfaEdge(), e.getCurrentBlock());
      }
    }
  }

  private void pushToWaitlist(Deque<ARTEdge> pWaitlist, ARTElement pCurrentElement, ARTElement pChild, CFAEdge pEdgeToChild, CompoundStatement pCurrentBlock) {
    assert (!pChild.isDestroyed());
    pWaitlist.push(new ARTEdge(pCurrentElement, pChild, pEdgeToChild, pCurrentBlock));
  }

  private CompoundStatement addIfStatement(CompoundStatement block, String conditionCode) {
    block.addStatement(new SimpleStatement(conditionCode));
    CompoundStatement newBlock = new CompoundStatement(block);
    block.addStatement(newBlock);
    return newBlock;
  }

  private void generateLabel(ARTElement currentElement, CompoundStatement block) {
    if(!currentElement.getCoveredByThis().isEmpty() || mergeElements.contains(currentElement)) {
      //this element covers others; they may want to jump to it
      block.addStatement(new SimpleStatement("label_"+currentElement.getElementId()+":; "));
    }
  }

  private void handleEdge(ARTEdge nextEdge, Deque<ARTEdge> waitlist) {
    ARTElement parentElement = nextEdge.getParentElement();
    ARTElement childElement = nextEdge.getChildElement();
    CFAEdge edge = nextEdge.getCfaEdge();
    CompoundStatement currentBlock = nextEdge.getCurrentBlock();

    currentBlock = processEdge(parentElement, childElement, edge, currentBlock);

    if (childElement.getParents().size() > 1) {
      mergeElements.add(childElement);
    }

    if(!discoveredElements.contains(childElement)) {
      // this element was not already processed; find children of it
      getRelevantChildrenOfElement(childElement, waitlist, currentBlock);
    } else {
      //this element was already processed and code generated somewhere; jump to it
      currentBlock.addStatement(new SimpleStatement("goto label_" + childElement.getElementId() + ";"));
    }
  }


  private CompoundStatement processEdge(ARTElement currentElement, ARTElement childElement, CFAEdge edge, CompoundStatement currentBlock) {
    if (edge instanceof FunctionCallEdge) {
      // if this is a function call edge we need to inline it
      currentBlock = processFunctionCall(edge, currentBlock);
    }
    else if (edge instanceof ReturnStatementEdge) {
      ReturnStatementEdge returnEdge = (ReturnStatementEdge)edge;

      if(returnEdge.getExpression() != null) {
        addGlobalReturnValueDecl(returnEdge, childElement.getElementId());

        String retval = returnEdge.getExpression().getRawSignature();
        String returnVar = " __return_" + childElement.getElementId();
        currentBlock.addStatement(new SimpleStatement(returnVar + " = " + retval + ";"));
      }
    }
    else if (edge instanceof FunctionReturnEdge) {
      // assumes that ReturnStateEdge is followed by FunctionReturnEdge
      FunctionReturnEdge returnEdge = (FunctionReturnEdge)edge;
      currentBlock = processReturnStatementCall(returnEdge.getSummaryEdge(), currentBlock, currentElement.getElementId());
    } else {
      String statement = processSimpleEdge(edge);
      if(!statement.isEmpty()) {
        currentBlock.addStatement(new SimpleStatement(statement));
      }
    }

    if (childElement.isTarget()) {
      System.out.println("HALT for line no " + edge.getLineNumber());
      currentBlock.addStatement(new SimpleStatement("HALT" + childElement.getElementId() + ": goto HALT" + childElement.getElementId() + ";"));
    }

    return currentBlock;
  }

  private void addGlobalReturnValueDecl(ReturnStatementEdge pReturnEdge, int pElementId) {
    //derive return type of function
    String returnType;

    if(pReturnEdge.getSuccessor().getNumLeavingEdges() == 0) {
      //default to int
      returnType = "int ";
    } else {
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge)pReturnEdge.getSuccessor().getLeavingEdge(0);
      CFANode functionDefNode = functionReturnEdge.getSummaryEdge().getPredecessor();
      assert functionDefNode.getNumLeavingEdges() == 1;
      assert functionDefNode.getLeavingEdge(0) instanceof FunctionCallEdge;
      FunctionCallEdge callEdge = (FunctionCallEdge)functionDefNode.getLeavingEdge(0);
      FunctionDefinitionNode fn = callEdge.getSuccessor();
      returnType = fn.getFunctionDefinition().getDeclSpecifier().getReturnType().toASTString();
    }

    globalDefinitionsList.add(returnType + "__return_" + pElementId + ";");
  }

  private String processSimpleEdge(CFAEdge pCFAEdge) {
    if(pCFAEdge == null) {
      return "";
    }

    switch (pCFAEdge.getEdgeType()) {
    case BlankEdge: {
      //nothing to do
      break;
    }

    case AssumeEdge: {
      //nothing to do
      break;
    }

    case StatementEdge: {
      StatementEdge lStatementEdge = (StatementEdge)pCFAEdge;
      return lStatementEdge.getStatement().getRawSignature() + ";";
    }

    case DeclarationEdge: {
      DeclarationEdge lDeclarationEdge = (DeclarationEdge)pCFAEdge;

      if (lDeclarationEdge.isGlobal()) {
        globalDefinitionsList.add(lDeclarationEdge.getRawStatement());
      } else {
        return lDeclarationEdge.getRawStatement();
      }

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

  private CompoundStatement processFunctionCall(CFAEdge pCFAEdge, CompoundStatement currentBlock) {
    CompoundStatement newBlock = new CompoundStatement(currentBlock);
    currentBlock.addStatement(newBlock);

    FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge)pCFAEdge;

    List<IASTExpression> actualParams = lFunctionCallEdge.getArguments();
    FunctionDefinitionNode fn = lFunctionCallEdge.getSuccessor();
    List<IASTParameterDeclaration> formalParams = fn.getFunctionParameters();

    List<Statement> actualParamAssignStatements = new ArrayList<Statement>();
    List<Statement> formalParamAssignStatements = new ArrayList<Statement>();

    int i = 0;
    for (IASTParameterDeclaration formalParam : formalParams) {
      // get formal parameter name
      String formalParamSignature = formalParam.getRawSignature();
      String actualParamSignature = actualParams.get(i++).getRawSignature();

      // create temp variable to avoid name clashes
      String tempVariableName = "__tmp_" + getFreshIndex();
      String tempVariableType = formalParam.getDeclSpecifier().toASTString();

      actualParamAssignStatements.add(new SimpleStatement(tempVariableType + " " + tempVariableName + " = " + actualParamSignature + ";"));
      formalParamAssignStatements.add(new SimpleStatement(formalParamSignature + " = " + tempVariableName + ";"));
    }

    for(Statement stmt : actualParamAssignStatements) {
      newBlock.addStatement(stmt);
    }
    for(Statement stmt : formalParamAssignStatements) {
      newBlock.addStatement(stmt);
    }

    return newBlock;
  }

  private CompoundStatement processReturnStatementCall(CallToReturnEdge pEdge, CompoundStatement pCurrentBlock, int id) {
    IASTFunctionCall retExp = pEdge.getExpression();
    if(retExp instanceof IASTFunctionCallStatement) {
      //end of void function, just leave block (no assignment needed)
      return pCurrentBlock.getSurroundingBlock();
    } else if (retExp instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement exp = (IASTFunctionCallAssignmentStatement)retExp;

      String returnVar = "__return_" + id;
      String leftHandSide = exp.getLeftHandSide().getRawSignature();

      pCurrentBlock = pCurrentBlock.getSurroundingBlock();
      pCurrentBlock.addStatement(new SimpleStatement(leftHandSide + " = " + returnVar + ";"));

      return pCurrentBlock;
    } else {
      assert false : "unknown function exit expression";
    }

    return null;
  }

  private int freshIndex = 0;
  private int getFreshIndex() {
    return ++freshIndex;
  }
}
