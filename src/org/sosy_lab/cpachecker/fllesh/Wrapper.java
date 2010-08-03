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

import java.io.File;
import java.io.IOException;
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

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFABuilder;
import org.sosy_lab.cpachecker.cfa.CFATopologicalSort;
import org.sosy_lab.cpachecker.cfa.DOTBuilder;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.fllesh.util.CFATraversal;
import org.sosy_lab.cpachecker.fllesh.util.CFAVisitor;
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

    private Set<CFAEdge> mCFAEdges;

    private AddFunctionCallVisitor() {
      mCFAEdges = new HashSet<CFAEdge>();
    }

    public CFAEdge getAlphaEdge() {
      return mAlphaEdge;
    }

    public CFAEdge getOmegaEdge() {
      return mOmegaEdge;
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

      callEdge = new FunctionCallEdge(functionCall.getRawSignature(), expr, edge.getLineNumber(), node, fDefNode, parameters, false);
      callEdge.addToCFA(null);
      mAlphaEdge = callEdge;

      // set return edge from exit node of the function
      ReturnEdge returnEdge = new ReturnEdge("Return Edge to " + successorNode.getNodeNumber(), edge.getLineNumber(), mCFAs.get(functionName).getExitNode(), successorNode);
      returnEdge.addToCFA(null);

      mOmegaEdge = returnEdge;

      CallToReturnEdge calltoReturnEdge = new CallToReturnEdge(expr.getRawSignature(), edge.getLineNumber(), node, successorNode, expr);
      calltoReturnEdge.addToCFA(null);

      mAlphaToOmegaEdge = calltoReturnEdge;

      node.removeLeavingEdge(edge);
      successorNode.removeEnteringEdge(edge);
    }
  }

  public Wrapper(FunctionDefinitionNode pMainFunction, Map<String, CFAFunctionDefinitionNode> pCFAs, LogManager pLogManager) {
    this(pMainFunction, pCFAs, pLogManager, getWrapperCFunction(pMainFunction));
  }
  
  public Wrapper(FunctionDefinitionNode pMainFunction, Map<String, CFAFunctionDefinitionNode> pCFAs, LogManager pLogManager, String pWrapperSource) {
    this(pMainFunction, pCFAs, pLogManager, pWrapperSource, "__FLLESH__main");
  }
  
  public Wrapper(FunctionDefinitionNode pMainFunction, Map<String, CFAFunctionDefinitionNode> pCFAs, LogManager pLogManager, String pWrapperSource, String pEntryFunction) {
    mLogManager = pLogManager;
    mCFAs = new HashMap<String, CFAFunctionDefinitionNode>();
    mCFAs.putAll(this.getWrapper(pWrapperSource));
    mCFAs.putAll(pCFAs);
    
    mEntry = mCFAs.get(pEntryFunction);

    // correct call to main function
    mVisitor = new AddFunctionCallVisitor();

    CFATraversal.traverse(mEntry, mVisitor);
  }

  public CFAEdge getAlphaEdge() {
    return mVisitor.getAlphaEdge();
  }

  public CFAEdge getOmegaEdge() {
    return mVisitor.getOmegaEdge();
  }
  
  public CFAFunctionDefinitionNode getCFA(String pFunctionName) {
    return mCFAs.get(pFunctionName);
  }
  
  public void toDot(String pFileName) throws IOException {
    toDot(new File(pFileName));
  }
  
  public void toDot(File pFile) throws IOException {
    Files.writeFile(pFile, DOTBuilder.generateDOT(mCFAs.values(), mEntry), false);
  }

  public CFAFunctionDefinitionNode getEntry() {
    return mEntry;
  }

  private static String getWrapperCFunction(FunctionDefinitionNode pMainFunction) {
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
  
  private Map<String, CFAFunctionDefinitionNode> getWrapper(String pWrapperFunction) {
    IASTTranslationUnit ast;
    try {
       ast = CParser.parseString(pWrapperFunction, Dialect.C99);
    } catch (CoreException e) {
      throw new RuntimeException("Error during parsing C code \""
          + pWrapperFunction.toString() + "\": " + e.getMessage());
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

}
