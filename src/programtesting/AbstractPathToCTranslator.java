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
package programtesting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import cfa.CFAMap;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.MultiDeclarationEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.StatementEdge;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;

import cpa.symbpredabs.explicit.ExplicitAbstractElement;

/**
 * @author holzera
 *
 */
public class AbstractPathToCTranslator {
  private static PrintWriter mGlobalThingsWriter = null;
  private static List<String> mFunctionDecls = null;

  public static Map<Deque<ExplicitAbstractElement>, List<String>> translatePaths(CFAMap pCfas, Collection<Deque<ExplicitAbstractElement>> pPaths) {
    assert(pPaths != null);
    
    int i = 0;
    
    Map<Deque<ExplicitAbstractElement>, List<String>> lPathPrograms = new HashMap<Deque<ExplicitAbstractElement>, List<String>>();
    
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
    
    
    for (Deque<ExplicitAbstractElement> lAbstractPath : pPaths) {
      System.out.println("#### PATH " + i + " ####");
      
      List<String> lTranslation = translatePath(lAbstractPath);
      
      // TODO remove output
      System.out.println("Written program text:");
     
      //try {

      //PrintWriter lWriter = new PrintWriter(new FileWriter("andi_tmp.c"));

      for (String lProgramString : lTranslation) {
        System.out.println(lProgramString);
	//lWriter.println(lProgramString);
      }

      //lWriter.close();
      
      //}
      //catch(Exception e) {
      //  e.printStackTrace();
      //}

      lPathPrograms.put(lAbstractPath, lTranslation);
      
      i++;

      //System.exit(1);
    }
    
    return lPathPrograms;
  }
  
  public static List<String> translatePath(Deque<ExplicitAbstractElement> pAbstractPath) {
    assert(pAbstractPath != null);
    assert(pAbstractPath.size() > 0);
    
    
    
    ExplicitAbstractElement lPredecessorElement = pAbstractPath.getFirst();
   
    // check for special case (= path of length zero = path with only one element)
    if (pAbstractPath.size() == 1) {
      // special case
      
      // stack for program texts of different functions
      Stack<StringWriter> lProgramTextStack = new Stack<StringWriter>();
      
      // list of already finished program texts
      List<String> lProgramTexts = new ArrayList<String>();
      
      startFunction(0, lPredecessorElement.getLocationNode(), lProgramTextStack);
      
      endFunction(lProgramTextStack, lProgramTexts);
      
      return lProgramTexts;
    }
    
    
    boolean first = true;
    
    List<CFAEdge> lEdges = new ArrayList<CFAEdge>();
    
    for (ExplicitAbstractElement lElement : pAbstractPath) {
      if (first) {
        first = false;
        continue;
      }
      
      CFANode lPredecessorNode = lPredecessorElement.getLocationNode();
      CFANode lNode = lElement.getLocationNode();
      
      // reconstruct edge
      int lNumberOfFoundEdges = 0;
      
      for (int lIndex = 0; lIndex < lPredecessorNode.getNumLeavingEdges(); lIndex++) {
        CFAEdge lEdge = lPredecessorNode.getLeavingEdge(lIndex);
        
        if (lEdge.getSuccessor().equals(lNode)) {
          lEdges.add(lEdge);
          lNumberOfFoundEdges++;
        }
      }
      
      assert(lNumberOfFoundEdges == 1);
      
      lPredecessorElement = lElement;
    }
    
    return translatePath(lEdges);
  }
  
  public static void endFunction(Stack<StringWriter> pProgramTextStack, List<String> pProgramTexts) {
    assert(pProgramTextStack != null);

    StringWriter lStringWriter = pProgramTextStack.pop();
    PrintWriter pProgramText = new PrintWriter(lStringWriter);
    
    // TODO This is a hack
    if (pProgramTextStack.isEmpty()) {
      pProgramText.println("__CPROVER_assert(0, \"path feasible\");");
    }
    
    // finish function
    pProgramText.println("}");
    
    // function program text is finished, add it to set of program texts
    pProgramTexts.add(lStringWriter.toString());
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
  
  public static List<String> translatePath(List<CFAEdge> pAbstractPath) {
    int lFunctionIndex = 0;

    // create print writer for global typedefs and declarations
    StringWriter lGlobalThingsStringWriter = new StringWriter();
    mGlobalThingsWriter = new PrintWriter(lGlobalThingsStringWriter);

    // stack for program texts of different functions
    Stack<StringWriter> lProgramTextStack = new Stack<StringWriter>();
    
    // list of already finished program texts
    List<String> lProgramTexts = new ArrayList<String>();
    
    
    // program text for start function 
    PrintWriter lProgramText = startFunction(lFunctionIndex, pAbstractPath.get(0).getPredecessor(), lProgramTextStack);
    
    lFunctionIndex++;
    
    
    // process edges
    for (CFAEdge lEdge : pAbstractPath) {
      System.out.println(lEdge.getRawStatement());
      
      switch (lEdge.getEdgeType()) {
      case BlankEdge: {
        // nothing to do
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
        
        
        lProgramText.println(lFunctionName + "_" + lFunctionIndex + lArgumentString + ";");
        
        
        lProgramText = startFunction(lFunctionIndex, lFunctionCallEdge.getSuccessor(), lProgramTextStack);
        
        lFunctionIndex++;
        
        break;
      }
      case ReturnEdge: {
        endFunction(lProgramTextStack, lProgramTexts);
        
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
        // this should not have been taken
        assert(false);
        
        break;
      }
      default: {
        assert(false);
      }
      }
    }
    
    // clean stack and finish functions
    while (!lProgramTextStack.isEmpty()) {
      endFunction(lProgramTextStack, lProgramTexts);
    }

    if (mFunctionDecls != null) {
      for (String decl : mFunctionDecls) {
        mGlobalThingsWriter.println(decl);
      }
    }
    lProgramTexts.add(0, lGlobalThingsStringWriter.toString());
    
    return lProgramTexts;
  }
}
