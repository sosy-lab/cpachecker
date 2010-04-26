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
package org.sosy_lab.cpachecker.fllesh.fql.fllesh.util;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;

public abstract class CFATraversal {
  
  public static void traverse(CFANode pInitialNode, CFAVisitor pVisitor) {
    assert(pVisitor != null);
    assert(pInitialNode != null);
    
    Set<CFANode> lWorklist = new LinkedHashSet<CFANode>();
    Set<CFANode> lVisitedNodes = new HashSet<CFANode>();
    
    lWorklist.add(pInitialNode);
    
    pVisitor.init(pInitialNode);
    
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
        
        pVisitor.visit(lCallToReturnEdge);
        
        CFANode lSuccessor = lCallToReturnEdge.getSuccessor();
        lWorklist.add(lSuccessor);
      }
      
      int lNumberOfLeavingEdges = lCFANode.getNumLeavingEdges();
      
      for (int lEdgeIndex = 0; lEdgeIndex < lNumberOfLeavingEdges; lEdgeIndex++) {
        CFAEdge lEdge = lCFANode.getLeavingEdge(lEdgeIndex);
        
        pVisitor.visit(lEdge);
        
        CFANode lSuccessor = lEdge.getSuccessor();
        lWorklist.add(lSuccessor);
      }
    }
  }
  
}
