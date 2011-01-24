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
package org.sosy_lab.cpachecker.cfa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;

/**
 * This class takes several CFAs (each for a single function) and combines them
 * into one CFA by inserting the necessary function call and return edges.
 * @author erkan
 */
public class CPASecondPassBuilder {

  private final Map<String, CFAFunctionDefinitionNode> cfas;
  private final boolean createCallEdgesForExternalCalls;

  private final Map<CallToReturnEdge, CFAEdge> mReplacedEdges;
  
  /**
   * Class constructor.
   * @param map List of all CFA's in the program.
   */
  public CPASecondPassBuilder(Map<String, CFAFunctionDefinitionNode> cfas,
      boolean noCallEdgesForExternalCalls) {
    this.cfas = cfas;
    createCallEdgesForExternalCalls = !noCallEdgesForExternalCalls;
    
    mReplacedEdges = new HashMap<CallToReturnEdge, CFAEdge>();
  }
  
  /** 
   * @return A mapping of CallToReturnEdge objects to the cfa edges replaced by these objects
   */
  public Map<CallToReturnEdge, CFAEdge> getMappingToReplacedEdges() {
    return mReplacedEdges;
  }
  
  /**
   * Traverses a CFA and inserts call edges and return edges (@see {@link #insertCallEdges(CFANode)}.
   * This method starts with a function and recursively acts on all functions
   * reachable from the first one.
   * @param functionName  The function where to start processing.
   * @return A set of all functions reachable (including external functions and the function passed as argument).
   */
  public Set<String> insertCallEdgesRecursively(String functionName) {
    Deque<String> worklist = new ArrayDeque<String>();
    worklist.addLast(functionName);
    Set<String> reachedFunctions = new HashSet<String>();

    while (!worklist.isEmpty()) {
      String currentFunction = worklist.pollFirst();
      if (!reachedFunctions.add(currentFunction)) {
        // reachedFunctions already contained function
        continue;
      }
      CFAFunctionDefinitionNode functionStartNode = cfas.get(currentFunction);
      if (functionStartNode != null) {
        // otherwise it's an external call
        worklist.addAll(insertCallEdges(functionStartNode));
      }
    }
    return reachedFunctions;
  }

  /**
   * Traverses a CFA with the specified function name and insert call edges
   * and return edges from the call site and to the return site of the function
   * call.
   * @param initialNode CFANode where to start processing
   * @return a list of all function calls encountered (may contain duplicates)
   */
  private List<String> insertCallEdges(CFAFunctionDefinitionNode initialNode){
    // we use a worklist algorithm
    Deque<CFANode> workList = new ArrayDeque<CFANode> ();
    Deque<CFANode> processedList = new ArrayDeque<CFANode> ();
    ArrayList<String> calledFunctions = new ArrayList<String>();

    workList.addLast (initialNode);
    processedList.addLast (initialNode);

    while (!workList.isEmpty ())
    {
      CFANode node = workList.pollFirst ();
      int numLeavingEdges = node.getNumLeavingEdges ();

      for (int edgeIdx = 0; edgeIdx < numLeavingEdges; edgeIdx++)
      {
        CFAEdge edge = node.getLeavingEdge (edgeIdx);
        CFANode successorNode = edge.getSuccessor();
        if(edge instanceof StatementEdge)
        {
          IASTExpression expr = ((StatementEdge)edge).getExpression();

          // if expression is a binary expression
          if(expr instanceof IASTBinaryExpression){
            IASTExpression operand2 = ((IASTBinaryExpression)expr).getOperand2();
            // if statement is of the form x = call(a,b);
            if(operand2 instanceof IASTFunctionCallExpression &&
                shouldCreateCallEdges((IASTFunctionCallExpression)operand2)) {

              IASTFunctionCallExpression functionCall = (IASTFunctionCallExpression)operand2;
              calledFunctions.add(functionCall.getFunctionNameExpression().getRawSignature());
              createCallAndReturnEdges(node, successorNode, edge, expr, functionCall);
            }
          }

          // if expression is function call, e.g. call(a,b);
          else if(expr instanceof IASTFunctionCallExpression &&
              shouldCreateCallEdges((IASTFunctionCallExpression)expr)){
            IASTFunctionCallExpression functionCall = (IASTFunctionCallExpression)expr;
            calledFunctions.add(functionCall.getFunctionNameExpression().getRawSignature());
            createCallAndReturnEdges(node, successorNode, edge, expr, functionCall);
          }
        }

        // if the node is not already processed and if successor node is not
        // on a different CFA, add successor node to the worklist
        if(!processedList.contains(successorNode) &&
            node.getFunctionName().equals(successorNode.getFunctionName())){
          workList.add(successorNode);
        }
      }
      // node is processed
      processedList.add(node);
    }
    return calledFunctions;
  }

  private boolean shouldCreateCallEdges(IASTFunctionCallExpression f) {
    if (createCallEdgesForExternalCalls) return true;
    String name = f.getFunctionNameExpression().getRawSignature();
    CFAFunctionDefinitionNode fDefNode = cfas.get(name);
    return fDefNode != null;
  }

  /**
   * inserts call, return and summary edges from a node to its successor node.
   * @param node The node which is the call site.
   * @param successorNode The first node of the called function.
   * @param edge The function call edge.
   * @param expr The function call expression.
   * @param functionCall If the call was an assignment from the function call
   * this keeps only the function call expression, e.g. if statement is a = call(b);
   * then expr is a = call(b); and functionCall is call(b).
   */
  private void createCallAndReturnEdges(CFANode node, CFANode successorNode, CFAEdge edge, IASTExpression expr, IASTFunctionCallExpression functionCall) {
    String functionName = functionCall.getFunctionNameExpression().getRawSignature();
    CFAFunctionDefinitionNode fDefNode = cfas.get(functionName);

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

    // if the function definition node is null, then this is an external call
    if(fDefNode == null){
      assert(createCallEdgesForExternalCalls); // AG

      callEdge = new FunctionCallEdge(functionCall.getRawSignature(), expr, edge.getLineNumber(), node, edge.getSuccessor(), parameters, true);
      callEdge.addToCFA(null);
      CallToReturnEdge calltoReturnEdge = new CallToReturnEdge("External Call", edge.getLineNumber(), node, edge.getSuccessor(), expr);
      calltoReturnEdge.addToCFA(null);
      node.removeLeavingEdge(edge);
      successorNode.removeEnteringEdge(edge);
      
      mReplacedEdges.put(calltoReturnEdge, edge);
      
      return;
    }

    callEdge = new FunctionCallEdge(functionCall.getRawSignature(), expr, edge.getLineNumber(), node, fDefNode, parameters, false);
    callEdge.addToCFA(null);
    // set name of the function
    // set return edge from exit node of the function
    ReturnEdge returnEdge = new ReturnEdge("Return Edge to " + successorNode.getNodeNumber(), edge.getLineNumber(), cfas.get(functionName).getExitNode(), successorNode);
    returnEdge.addToCFA(null);

    CallToReturnEdge calltoReturnEdge = new CallToReturnEdge(expr.getRawSignature(), edge.getLineNumber(), node, successorNode, expr);
    calltoReturnEdge.addToCFA(null);

    node.removeLeavingEdge(edge);
    successorNode.removeEnteringEdge(edge);
    
    mReplacedEdges.put(calltoReturnEdge, edge);

  }
}
