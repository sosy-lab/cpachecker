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
import java.util.Stack;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cpa.common.automaton.Label;
import cpa.common.automaton.cfa.CFAEdgeLabel;

/**
 * @author holzera
 *
 */
public class LabelBasedPredicateGenerator implements PredicateGenerator {

  Label<CFAEdge> mLabel;
  
  public LabelBasedPredicateGenerator(Label<CFAEdge> pLabel) {
    assert(pLabel != null);
    
    mLabel = pLabel;
  }
  
  /* (non-Javadoc)
   * @see programtesting.PredicateGenerator#getPredicateLabels(cfa.objectmodel.CFAFunctionDefinitionNode)
   */
  @Override
  public Collection<Label<CFAEdge>> getPredicateLabels(CFAFunctionDefinitionNode pCFANode) {
    Collection<Label<CFAEdge>> lResult = new HashSet<Label<CFAEdge>>();
    
    Collection<CFAEdge> lVisitedCFAEdges = new HashSet<CFAEdge>();
    
    Stack<CFANode> lWorklist = new Stack<CFANode>();
    
    lWorklist.add(pCFANode);
    
    while (!lWorklist.isEmpty()) {
      CFANode lNode = lWorklist.pop();
      
      for (int lIndex = 0; lIndex < lNode.getNumLeavingEdges(); lIndex++) {
        CFAEdge lEdge = lNode.getLeavingEdge(lIndex);
        
        if (!lVisitedCFAEdges.contains(lEdge)) {
          lVisitedCFAEdges.add(lEdge);
          lWorklist.add(lEdge.getSuccessor());
          
          if (mLabel.matches(lEdge)) {
            // TODO Create matching label
            lResult.add(new CFAEdgeLabel(lEdge));
          }
        }
      }
    }
    
    return lResult;
  }

}
