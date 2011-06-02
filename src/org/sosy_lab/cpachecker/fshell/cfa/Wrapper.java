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
package org.sosy_lab.cpachecker.fshell.cfa;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;

public class Wrapper {

  private LogManager mLogManager;
  private CFAFunctionDefinitionNode mEntry;
  private CFAEdge mAlphaEdge;
  private CFAEdge mOmegaEdge;
  
  private TranslationUnit mTranslationUnit;

  public Wrapper(FunctionDefinitionNode pMainFunction, Map<String, CFAFunctionDefinitionNode> pCFAs, LogManager pLogManager) {
    this(pMainFunction, pCFAs, pLogManager, getWrapperCFunction(pMainFunction));
  }
  
  public Wrapper(FunctionDefinitionNode pMainFunction, Map<String, CFAFunctionDefinitionNode> pCFAs, LogManager pLogManager, String pWrapperSource) {
    this(pMainFunction, pCFAs, pLogManager, pWrapperSource, "__FLLESH__main");
  }
  
  public Wrapper(FunctionDefinitionNode pMainFunction, Map<String, CFAFunctionDefinitionNode> pCFAs, LogManager pLogManager, String pWrapperSource, String pEntryFunction) {
    mLogManager = pLogManager;
    
    TranslationUnit lWrapper = getWrapper(pWrapperSource);
    
    mTranslationUnit = new TranslationUnit();
    mTranslationUnit.add(lWrapper);
    mTranslationUnit.add(pCFAs);
    
    for (String lFunctionName : mTranslationUnit.functionNames()) {
      mTranslationUnit.insertCallEdgesRecursively(lFunctionName);
    }
    
    mEntry = mTranslationUnit.getFunction(pEntryFunction);
    
    CFACreator.insertGlobalDeclarations(mEntry, lWrapper.getGlobalDeclarations(), mLogManager);
    
    determineAlphaAndOmegaEdges(mEntry, pMainFunction);
  }
  
  private void determineAlphaAndOmegaEdges(CFANode pInitialNode, CFANode pOriginalInitialNode) {
    assert(pInitialNode != null);

    Set<CFANode> lWorklist = new LinkedHashSet<CFANode>();
    Set<CFANode> lVisitedNodes = new HashSet<CFANode>();

    lWorklist.add(pInitialNode);

    while (!lWorklist.isEmpty()) {
      CFANode lCFANode = lWorklist.iterator().next();
      lWorklist.remove(lCFANode);

      if (lVisitedNodes.contains(lCFANode)) {
        continue;
      }

      lVisitedNodes.add(lCFANode);

      // determine successors
      CallToReturnEdge lCallToReturnEdge = lCFANode.getLeavingSummaryEdge();

      if (lCallToReturnEdge != null) {

        if (lCFANode.getNumLeavingEdges() != 1) {
          throw new IllegalArgumentException();
        }
        
        CFAEdge lEdge = lCFANode.getLeavingEdge(0);
        
        CFANode lPredecessor = lEdge.getPredecessor();
        CFANode lSuccessor = lEdge.getSuccessor();
        
        if (lSuccessor.equals(pOriginalInitialNode)) {
          if (!lEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
            throw new RuntimeException();
          }
          
          mAlphaEdge = lEdge;
          
          CFAEdge lSummaryEdge = lPredecessor.getLeavingSummaryEdge();
          
          if (lSummaryEdge == null) {
            throw new RuntimeException();
          }
          
          CFANode lSummarySuccessor = lSummaryEdge.getSuccessor();
          
          if (lSummarySuccessor.getNumEnteringEdges() != 1) {
            throw new RuntimeException("Summary successor has " + lSummarySuccessor.getNumEnteringEdges() + " entering CFA edges!");
          }
          
          mOmegaEdge = lSummarySuccessor.getEnteringEdge(0);
        }
        
        lWorklist.add(lCallToReturnEdge.getSuccessor());
      }
      else {
        int lNumberOfLeavingEdges = lCFANode.getNumLeavingEdges();

        for (int lEdgeIndex = 0; lEdgeIndex < lNumberOfLeavingEdges; lEdgeIndex++) {
          CFAEdge lEdge = lCFANode.getLeavingEdge(lEdgeIndex);

          CFANode lSuccessor = lEdge.getSuccessor();
          lWorklist.add(lSuccessor);
        }
      }
    }
  }

  public CFAEdge getAlphaEdge() {
    return mAlphaEdge;
  }

  public CFAEdge getOmegaEdge() {
    return mOmegaEdge;
  }
  
  public CFAFunctionDefinitionNode getCFA(String pFunctionName) {
    return mTranslationUnit.getFunction(pFunctionName);
  }
  
  public void toDot(String pFileName) throws IOException {
    toDot(new File(pFileName));
  }
  
  public void toDot(File pFile) throws IOException {
    mTranslationUnit.toDot(mEntry.getFunctionName(), pFile);
  }

  public CFAFunctionDefinitionNode getEntry() {
    return mEntry;
  }

  private static String getWrapperCFunction(FunctionDefinitionNode pMainFunction) {
    StringWriter lWrapperFunction = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lWrapperFunction);

    // TODO interpreter is not capable of handling initialization of global declarations
    
    lWriter.println("void __FLLESH__main()");
    lWriter.println("{");
    lWriter.println("  int __BLAST_NONDET;");
    
    for (IASTSimpleDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      lWriter.println("  " + lDeclaration.getRawSignature() + ";");
    }
        
    for (IASTSimpleDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      // TODO do we need to handle lDeclaration more specifically?
      lWriter.println("  " + lDeclaration.getName() + " = __BLAST_NONDET;");
    }
    
    lWriter.println();
    lWriter.print("  " + pMainFunction.getFunctionName() + "(");

    boolean isFirst = true;

    for (IASTSimpleDeclaration lDeclaration : pMainFunction.getFunctionParameters()) {
      if (isFirst) {
        isFirst = false;
      }
      else {
        lWriter.print(", ");
      }

      lWriter.print(lDeclaration.getName());
    }

    lWriter.println(");");
    lWriter.println("  return;");
    lWriter.println("}");

    return lWrapperFunction.toString();
  }
  
  private TranslationUnit getWrapper(String pWrapperFunction) {
    return TranslationUnit.parseString(pWrapperFunction, mLogManager);
  }

}
