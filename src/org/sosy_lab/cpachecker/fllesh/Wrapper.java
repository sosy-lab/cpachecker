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
package org.sosy_lab.cpachecker.fllesh;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFABuilder;
import org.sosy_lab.cpachecker.cfa.CFATopologicalSort;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.fllesh.cpa.edgevisit.Annotations;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.CFATraversal;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.CFAVisitor;
import org.sosy_lab.cpachecker.util.CParser;
import org.sosy_lab.cpachecker.util.CParser.Dialect;

public class Wrapper {

  private LogManager mLogManager;
  private Map<String, CFAFunctionDefinitionNode> mCFAs;
  private CFAFunctionDefinitionNode mEntry;
  private AddFunctionCallVisitor mVisitor;

  private class AddFunctionCallVisitor implements CFAVisitor {

    private CFAEdge mAlphaEdge;
    private CFAEdge mOmegaEdge;
    private CFAEdge mAlphaToOmegaEdge;
    //private Map<String, CFAFunctionDefinitionNode> mCFAs;

    private Set<CFAEdge> mCFAEdges;

    //private AddFunctionCallVisitor(Map<String, CFAFunctionDefinitionNode> pCFAs) {
    private AddFunctionCallVisitor() {
      mCFAEdges = new HashSet<CFAEdge>();
      //mCFAs = pCFAs;
    }

    public CFAEdge getAlphaEdge() {
      return mAlphaEdge;
    }

    public CFAEdge getOmegaEdge() {
      return mOmegaEdge;
    }

    public Set<CFAEdge> getEdges() {
      return mCFAEdges;
    }

    @Override
    public void init(CFANode pInitialNode) {

    }

    @Override
    public void visit(CFAEdge pP) {
      if (pP instanceof StatementEdge) {
        IASTExpression expr = ((StatementEdge)pP).getExpression();

        if (expr instanceof IASTFunctionCallExpression) {
          createCallAndReturnEdges(pP.getPredecessor(), pP.getSuccessor(), pP, expr, (IASTFunctionCallExpression)expr);

          mCFAEdges.add(mAlphaEdge);
          mCFAEdges.add(mOmegaEdge);
          mCFAEdges.add(mAlphaToOmegaEdge);
        }
        else {
          mCFAEdges.add(pP);
        }

      }
      else {
        mCFAEdges.add(pP);
      }
    }

    private void createCallAndReturnEdges(CFANode node, CFANode successorNode, CFAEdge edge, IASTExpression expr, IASTFunctionCallExpression functionCall) {
      String functionName = functionCall.getFunctionNameExpression().getRawSignature();
      CFAFunctionDefinitionNode fDefNode = mCFAs.get(functionName);

      //get the parameter expression
      IASTExpression parameterExpression = functionCall.getParameterExpression();
      IASTExpression[] parameters = null;
      //in case of an expression list, get the corresponding array
      if (parameterExpression instanceof IASTExpressionList) {
        IASTExpressionList paramList = (IASTExpressionList)parameterExpression;
        parameters = paramList.getExpressions();
      //in case of a single parameter, use a single-entry array
      } else if (parameterExpression != null) {
        parameters = new IASTExpression[] {parameterExpression};
      }
      FunctionCallEdge callEdge;

      callEdge = new FunctionCallEdge(functionCall, edge.getLineNumber(), node, fDefNode, parameters, false);
      callEdge.addToCFA();
      mAlphaEdge = callEdge;

      // set name of the function
      fDefNode.setFunctionName(functionName);
      // set return edge from exit node of the function
      ReturnEdge returnEdge = new ReturnEdge("Return Edge to " + successorNode.getNodeNumber(), edge.getLineNumber(), mCFAs.get(functionName).getExitNode(), successorNode);
      returnEdge.addToCFA();
      returnEdge.getSuccessor().setFunctionName(node.getFunctionName());

      mOmegaEdge = returnEdge;

      CallToReturnEdge calltoReturnEdge = new CallToReturnEdge(expr.getRawSignature(), edge.getLineNumber(), node, successorNode, expr);
      calltoReturnEdge.addToCFA();

      mAlphaToOmegaEdge = calltoReturnEdge;

      node.removeLeavingEdge(edge);
      successorNode.removeEnteringEdge(edge);
    }
  }

  public Wrapper(FunctionDefinitionNode pMainFunction, Map<String, CFAFunctionDefinitionNode> pCFAs, Annotations pAnnotations, LogManager pLogManager) {
    mLogManager = pLogManager;
    mCFAs = new HashMap<String, CFAFunctionDefinitionNode>();
    mCFAs.putAll(this.getWrapper(pMainFunction));
    mCFAs.putAll(pCFAs);

    mEntry = mCFAs.get("__FLLESH__main");

    // set function names
    CFATraversal.traverse(mEntry, Wrapper.FunctionNameSetter.getInstance());

    // correct call to main function
    //mVisitor = new AddFunctionCallVisitor(this.getCFAs());
    mVisitor = new AddFunctionCallVisitor();

    CFATraversal.traverse(mEntry, mVisitor);

    for (CFAEdge lEdge : mVisitor.getEdges()) {
      pAnnotations.getId(lEdge);
    }

    //DOTBuilder dotBuilder = new DOTBuilder();
    //dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));

  }

  public CFAEdge getAlphaEdge() {
    return mVisitor.getAlphaEdge();
  }

  public CFAEdge getOmegaEdge() {
    return mVisitor.getOmegaEdge();
  }

  public Map<String, CFAFunctionDefinitionNode> getCFAs() {
    return mCFAs;
  }

  public CFAFunctionDefinitionNode getEntry() {
    return mEntry;
  }

  private String getWrapperCFunction(FunctionDefinitionNode pMainFunction) {
    StringWriter lWrapperFunction = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lWrapperFunction);

    lWriter.println("void __FLLESH__main()");
    lWriter.println("{");

    for (IASTParameterDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      lWriter.println("  " + lDeclaration.getRawSignature() + ";");
    }

    lWriter.println();
    lWriter.print("  " + pMainFunction.getFunctionName() + "(");

    boolean isFirst = true;

    for (IASTParameterDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      if (isFirst) {
        isFirst = false;
      }
      else {
        lWriter.print(", ");
      }

      lWriter.print(lDeclaration.getDeclarator().getName());
    }

    lWriter.println(");");
    lWriter.println("  return;");
    lWriter.println("}");

    return lWrapperFunction.toString();
  }

  private Map<String, CFAFunctionDefinitionNode> getWrapper(FunctionDefinitionNode pMainFunction) {
    String lWrapperFunction = getWrapperCFunction(pMainFunction);

    IASTTranslationUnit ast;
    try {
       ast = CParser.parseString(lWrapperFunction, Dialect.C99);
    } catch (CoreException e) {
      throw new RuntimeException("Error during parsing C code \""
          + lWrapperFunction.toString() + "\": " + e.getMessage());
    }

    checkForASTProblems(ast);

    CFABuilder lCFABuilder = new CFABuilder(mLogManager);

    ast.accept(lCFABuilder);

    Map<String, CFAFunctionDefinitionNode> cfas = lCFABuilder.getCFAs();

    // annotate CFA nodes with topological information for later use
    for(CFAFunctionDefinitionNode cfa : cfas.values()){
      CFATopologicalSort topSort = new CFATopologicalSort();
      topSort.topologicalSort(cfa);
    }

    return cfas;
  }

  private static void checkForASTProblems(IASTNode pAST) {
    if (pAST instanceof IASTProblem) {
      throw new RuntimeException("Error during parsing C code \""
          + pAST.getRawSignature() + "\": " + ((IASTProblem)pAST).getMessage());
    } else {
      for (IASTNode n : pAST.getChildren()) {
        checkForASTProblems(n);
      }
    }
  }

  public static class FunctionNameSetter implements CFAVisitor {

    private String mFunctionName;
    private static FunctionNameSetter mInstance = new FunctionNameSetter();

    private FunctionNameSetter() {

    }

    public static FunctionNameSetter getInstance() {
      return mInstance;
    }

    @Override
    public void init(CFANode pInitialNode) {
      mFunctionName = pInitialNode.getFunctionName();
    }

    @Override
    public void visit(CFAEdge pP) {
      pP.getSuccessor().setFunctionName(mFunctionName);
    }

  };

}
