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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cpa.common.automaton.Label;
import cpa.common.automaton.cfa.EnteringCFANodeLabel;

/**
 * @author holzera
 *
 */
public class BasicBlockPredicateGenerator implements PredicateGenerator {

  /* (non-Javadoc)
   * @see programtesting.PredicateGenerator#getPredicateLabels(cfa.objectmodel.CFAFunctionDefinitionNode)
   * 
   * CAUTION: We assume a function definition node has a single leaving edge (a dummy edge)!
   */
  @Override
  public Collection<Label<CFAEdge>> getPredicateLabels(CFAFunctionDefinitionNode pCFANode) {
    Collection<Label<CFAEdge>> lResult = new HashSet<Label<CFAEdge>>();
    
    Set<CFANode> lVisitedNodes = new HashSet<CFANode>();
    
    Stack<CFANode> lWorklist = new Stack<CFANode>();
    
    lWorklist.add(pCFANode);
    
    while (!lWorklist.empty()) {
      CFANode lCurrentNode = lWorklist.pop();
      
      if (lVisitedNodes.contains(lCurrentNode)) {
        continue;
      }
      
      lVisitedNodes.add(lCurrentNode);
      
      // determine end of "basic block"
      CFANode lTmpNode = lCurrentNode;
      
      while (true) {
        if (lTmpNode.getNumLeavingEdges() != 1) {
          break;
        }
        
        CFAEdge lEdge = lTmpNode.getLeavingEdge(0);
        
        boolean lEndLoop;
        
        switch (lEdge.getEdgeType()) {
        case BlankEdge:
        case StatementEdge:
        case DeclarationEdge:
        case MultiStatementEdge:
        case MultiDeclarationEdge:
        //case ReturnEdge:
          lEndLoop = false;
          break;
        default:
          // any edge not listed above ends a basic block
          lEndLoop = true;
          break;
        }
        
        if (lEndLoop) {
          break;
        }
        
        CFANode lSuccessor = lEdge.getSuccessor();
        
        // just in case a back loop to function start
        if (lSuccessor instanceof CFAFunctionDefinitionNode) {
          break;
        }
        
        if (lSuccessor.getNumEnteringEdges() > 1) {
          break;
        }
        
        lTmpNode = lSuccessor;
      }
      
      // add successors to worklist
      for (int lIndex = 0; lIndex < lCurrentNode.getNumLeavingEdges(); lIndex++) {
        CFANode lSuccessorNode = lCurrentNode.getLeavingEdge(lIndex).getSuccessor();
        
        lWorklist.add(lSuccessorNode);
      }
      
      // create test goal
      lResult.add(new EnteringCFANodeLabel(lTmpNode));
    }
    
    return lResult;
  }

}
