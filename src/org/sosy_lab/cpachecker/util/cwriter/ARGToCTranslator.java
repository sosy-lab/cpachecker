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

import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix="cpa.arg.export.code")
public class ARGToCTranslator {
  private static String ASSERTFAIL = "__assert_fail";

  private static abstract class Statement {
    public abstract void translateToCode(StringBuffer buffer, int indent);

    protected static void writeIndent(StringBuffer buffer, int indent) {
      for(int i = 0; i < indent; i++) {
        // buffer.append(" ");
      }
      buffer.append(" ");
    }
  }

  private static class CompoundStatement extends Statement {
    private final List<Statement> statements;
    private final CompoundStatement outerBlock;

    public CompoundStatement() {
      this(null);
    }

    public CompoundStatement(CompoundStatement pOuterBlock) {
      statements = new ArrayList<>();
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

  private static class ARGEdge {
    private final ARGState parent;
    private final ARGState child;
    private final CFAEdge cfaEdge;
    private final CompoundStatement currentBlock;

    public ARGEdge(ARGState pParent, ARGState pChild, CFAEdge pCfaEdge, CompoundStatement pCurrentBlock) {
      parent = pParent;
      child = pChild;
      cfaEdge = pCfaEdge;
      currentBlock = pCurrentBlock;
    }

    public ARGState getParentElement() {
      return parent;
    }

    public ARGState getChildElement() {
      return child;
    }

    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }

    public CompoundStatement getCurrentBlock() {
      return currentBlock;
    }
  }

  private final LogManager logger;
  private final List<String> globalDefinitionsList = new ArrayList<>();
  private final Set<ARGState> discoveredElements = new HashSet<>();
  private final Set<ARGState> mergeElements = new HashSet<>();
  private FunctionBody mainFunctionBody;
  // private static Collection<AbstractState> reached;

  private @Nullable Set<CFANode> addPragmaAfter;


  @Option(secure=true, name="header", description="write include directives")
  private boolean includeHeader = true;

  public ARGToCTranslator(LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
  }

  public String translateARG(ARGState argRoot) {

    return translateARG(argRoot, null);
  }

  public String translateARG(ARGState argRoot, @Nullable Set<CFANode> pAddPragma) {

    addPragmaAfter = pAddPragma == null ? Collections.emptySet() : pAddPragma;

    translate(argRoot);

    return generateCCode(includeHeader);
  }


  private String generateCCode(boolean includeHeader) {
    StringBuffer buffer = new StringBuffer();

    if (includeHeader) {
      buffer.append("#include <stdio.h>\n");
      buffer.append("#include <assert.h>\n");
    }
    for(String globalDef : globalDefinitionsList) {
      buffer.append(globalDef + "\n");
    }

    mainFunctionBody.translateToCode(buffer, 0);

    return buffer.toString();
  }

  private void translate(ARGState rootElement) {
    // waitlist for the edges to be processed
    Deque<ARGEdge> waitlist = new ArrayDeque<>(); //TODO: used to be sorted list and I don't know why yet ;-)

    startMainFunction(rootElement);
    getRelevantChildrenOfElement(rootElement, waitlist, mainFunctionBody.getFunctionBody());

    while (!waitlist.isEmpty()) {
      ARGEdge nextEdge = waitlist.pop();
      handleEdge(nextEdge, waitlist);
    }
  }

  private void startMainFunction(ARGState firstFunctionElement) {
    CFunctionEntryNode functionStartNode = (CFunctionEntryNode) AbstractStates.extractStateByType(firstFunctionElement, LocationState.class).getLocationNode();
    String lFunctionHeader = functionStartNode.getFunctionDefinition().toASTString().replace(";", "");
    mainFunctionBody = new FunctionBody(lFunctionHeader, new CompoundStatement());
  }

  private void getRelevantChildrenOfElement(ARGState currentElement, Deque<ARGEdge> waitlist, CompoundStatement currentBlock) {
    discoveredElements.add(currentElement);
    // generate label for element and add to current block if needed
    generateLabel(currentElement, currentBlock);

    // find the next elements to add to the waitlist
    Collection<ARGState> childrenOfElement = currentElement.getChildren();

    if (childrenOfElement.size() == 0) {
      // if there is no child of the element, maybe it was covered by other?
      if(currentElement.isCovered()) {
        //it was indeed covered; jump to element it was covered by
        currentBlock.addStatement(new SimpleStatement("goto label_" + currentElement.getCoveringState().getStateId() + ";"));
      } else {
        currentBlock.addStatement(new SimpleStatement("return 1;"));
      }
    } else if (childrenOfElement.size() == 1) {
      // get the next ARG element, create a new edge using the same stack and add it to the waitlist
      ARGState child = Iterables.getOnlyElement(childrenOfElement);
      CFAEdge edgeToChild = currentElement.getEdgeToChild(child);
      pushToWaitlist(waitlist, currentElement, child, edgeToChild, currentBlock);
    } else if (childrenOfElement.size() > 1) {
      // if there are more than one children, then this is a condition
      assert childrenOfElement.size() == 2 : "branches with more than two options not supported yet (was the program prepocessed with CIL?)"; //TODO: why not btw?

      //collect edges of condition branch
      ArrayList<ARGEdge> result = new ArrayList<>(2);
      int ind = 0;
      for (ARGState child : childrenOfElement) {
        CFAEdge edgeToChild = currentElement.getEdgeToChild(child);
        assert edgeToChild instanceof CAssumeEdge : "something wrong: branch in ARG without condition: " + edgeToChild;
        CAssumeEdge assumeEdge = (CAssumeEdge)edgeToChild;
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
            cond += "(" + assumeEdge.getExpression().toASTString() + ")";
          } else {
            cond += "(!(" + assumeEdge.getExpression().toASTString() + "))";
          }
        }

        ind++;

        // create a new block starting with this condition
        CompoundStatement newBlock = addIfStatement(currentBlock, cond);
        result.add(new ARGEdge(currentElement, child, edgeToChild, newBlock));
      }

      //add edges in reversed order to waitlist
      for(int i = result.size()-1; i >= 0; i--) {
        ARGEdge e = result.get(i);
        pushToWaitlist(waitlist, e.getParentElement(), e.getChildElement(), e.getCfaEdge(), e.getCurrentBlock());
      }
    }
  }

  private void pushToWaitlist(Deque<ARGEdge> pWaitlist, ARGState pCurrentElement, ARGState pChild, CFAEdge pEdgeToChild, CompoundStatement pCurrentBlock) {
    assert (!pChild.isDestroyed());
    pWaitlist.push(new ARGEdge(pCurrentElement, pChild, pEdgeToChild, pCurrentBlock));
  }

  private CompoundStatement addIfStatement(CompoundStatement block, String conditionCode) {
    block.addStatement(new SimpleStatement(conditionCode));
    CompoundStatement newBlock = new CompoundStatement(block);
    block.addStatement(newBlock);
    return newBlock;
  }

  private void generateLabel(ARGState currentElement, CompoundStatement block) {
    if(!currentElement.getCoveredByThis().isEmpty() || mergeElements.contains(currentElement)) {
      //this element covers others; they may want to jump to it
      block.addStatement(new SimpleStatement("label_" + currentElement.getStateId() + ":; "));
    }
  }

  private void handleEdge(ARGEdge nextEdge, Deque<ARGEdge> waitlist) {
    ARGState parentElement = nextEdge.getParentElement();
    ARGState childElement = nextEdge.getChildElement();
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
      currentBlock.addStatement(new SimpleStatement("goto label_" + childElement.getStateId() + ";"));
    }
  }


  private CompoundStatement processEdge(ARGState currentElement, ARGState childElement, CFAEdge edge, CompoundStatement currentBlock) {
    if (edge instanceof CFunctionCallEdge) {
      // if this is a function call edge we need to inline it
      currentBlock = processFunctionCall(edge, currentBlock);
    }
    else if (edge instanceof CReturnStatementEdge) {
      CReturnStatementEdge returnEdge = (CReturnStatementEdge)edge;

      if(returnEdge.getExpression() != null && returnEdge.getExpression().isPresent()) {
        addGlobalReturnValueDecl(returnEdge, childElement.getStateId());

        String retval = returnEdge.getExpression().get().toASTString();
        String returnVar = " __return_" + childElement.getStateId();
        currentBlock.addStatement(new SimpleStatement(returnVar + " = " + retval + ";"));
      }
    }
    else if (edge instanceof CFunctionReturnEdge) {
      // assumes that ReturnStateEdge is followed by FunctionReturnEdge
      CFunctionReturnEdge returnEdge = (CFunctionReturnEdge)edge;
      currentBlock = processReturnStatementCall(returnEdge.getSummaryEdge(), currentBlock, currentElement.getStateId());
    } else if (edge == null) {
      // assume that this is the case due to dynamic multi edges
      List<CFAEdge> innerEdges = currentElement.getEdgesToChild(childElement);
      StringBuilder edgeStatementCodes = new StringBuilder();
      for (CFAEdge innerEdge : innerEdges) {
        assert innerEdge
            .getEdgeType() != CFAEdgeType.AssumeEdge : "Unexpected assume edge in dynamic multi edge "
                + innerEdge;
        assert !(innerEdge instanceof CFunctionCallEdge
            || innerEdge instanceof CFunctionReturnEdge) : "Unexpected edge " + innerEdge
                + " in dynmaic multi edge";
        if (innerEdge instanceof CReturnStatementEdge) {
          assert (innerEdges.get(innerEdges.size() - 1) == innerEdge);
          CReturnStatementEdge returnEdge = (CReturnStatementEdge) innerEdge;
          addGlobalReturnValueDecl(returnEdge, childElement.getStateId());

          String retval = returnEdge.getExpression().get().toASTString();
          edgeStatementCodes.append(" __return_" + childElement.getStateId() + " = " + retval + ";");
        } else {
          edgeStatementCodes.append(processSimpleEdge(innerEdge));
        }
        edgeStatementCodes.append("\n");
      }
      currentBlock.addStatement(new SimpleStatement(edgeStatementCodes.toString()));
    } else {
      String statement = processSimpleEdge(edge);
      if (!statement.isEmpty()) {
        currentBlock.addStatement(new SimpleStatement(statement));
      }
    }

    if (childElement.isTarget()) {
      logger.log(Level.ALL, "HALT for line no ", edge.getLineNumber());
      currentBlock.addStatement(new SimpleStatement("HALT" + childElement.getStateId() + ": goto HALT" + childElement.getStateId() + ";"));
    }

    return currentBlock;
  }

  private void addGlobalReturnValueDecl(CReturnStatementEdge pReturnEdge, int pElementId) {
    //derive return type of function
    String returnType;

    String varName = "__return_" + pElementId;
    if(pReturnEdge.getSuccessor().getNumLeavingEdges() == 0) {
      //default to int
      globalDefinitionsList.add("int " + varName + ";");
    } else {
      CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge)pReturnEdge.getSuccessor().getLeavingEdge(0);
      CFANode functionDefNode = functionReturnEdge.getSummaryEdge().getPredecessor();
      assert functionDefNode.getNumLeavingEdges() == 1;
      assert functionDefNode.getLeavingEdge(0) instanceof CFunctionCallEdge;
      CFunctionCallEdge callEdge = (CFunctionCallEdge)functionDefNode.getLeavingEdge(0);
      CFunctionEntryNode fn = callEdge.getSuccessor();
      returnType = fn.getFunctionDefinition().getType().getReturnType().toASTString(varName);
      globalDefinitionsList.add(returnType + ";");
    }


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
      CStatementEdge lStatementEdge = (CStatementEdge)pCFAEdge;
      String statementText = lStatementEdge.getStatement().toASTString();
        if (statementText.startsWith(ASSERTFAIL)) {
          return "assert(false);";

        }
        return statementText + (statementText.endsWith(";") ? "" : ";");
    }

    case DeclarationEdge: {
      CDeclarationEdge lDeclarationEdge = (CDeclarationEdge)pCFAEdge;
      String declaration;
      // TODO adapt if String in org.sosy_lab.cpachecker.cfa.parser.eclipse.c.ASTConverter#createInitializedTemporaryVariable is changed
      if (lDeclarationEdge.getDeclaration().toASTString().contains("__CPAchecker_TMP_")) {
        declaration = lDeclarationEdge.getDeclaration().toASTString();
      } else {
        declaration = lDeclarationEdge.getRawStatement();
      }

      if (lDeclarationEdge.getDeclaration().isGlobal()) {
        globalDefinitionsList.add(declaration + (declaration.endsWith(";") ? "" : ";"));
      } else {
        return declaration;
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

    CFunctionCallEdge lFunctionCallEdge = (CFunctionCallEdge)pCFAEdge;

    List<CExpression> actualParams = lFunctionCallEdge.getArguments();
    CFunctionEntryNode fn = lFunctionCallEdge.getSuccessor();
    List<CParameterDeclaration> formalParams = fn.getFunctionParameters();

    List<Statement> actualParamAssignStatements = new ArrayList<>();
    List<Statement> formalParamAssignStatements = new ArrayList<>();

    int i = 0;
    for (CParameterDeclaration formalParam : formalParams) {
      // get formal parameter name
      String formalParamSignature = formalParam.toASTString();
      String actualParamSignature = actualParams.get(i++).toASTString();

      // create temp variable to avoid name clashes
      String tempVariableName = "__tmp_" + getFreshIndex();
      String tempVariableType = formalParam.getType().toASTString(tempVariableName);

      actualParamAssignStatements.add(new SimpleStatement(tempVariableType + " = " + actualParamSignature + ";"));
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

  private CompoundStatement processReturnStatementCall(CFunctionSummaryEdge pEdge, CompoundStatement pCurrentBlock, int id) {
    CFunctionCall retExp = pEdge.getExpression();
    if (retExp instanceof CFunctionCallStatement) {
      //end of void function, just leave block (no assignment needed)
      return pCurrentBlock.getSurroundingBlock();
    } else if (retExp instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement exp = (CFunctionCallAssignmentStatement)retExp;

      String returnVar = "__return_" + id;
      String leftHandSide = exp.getLeftHandSide().toASTString();

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
