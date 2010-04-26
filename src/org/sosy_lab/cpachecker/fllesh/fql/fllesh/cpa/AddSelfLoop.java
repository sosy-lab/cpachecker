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
package org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.CFATraversal;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.util.CFAVisitor;

public class AddSelfLoop {
  private static class AddSelfLoopCFAVisitor implements CFAVisitor {

    private static AddSelfLoopCFAVisitor mInstance = new AddSelfLoopCFAVisitor();
    private Set<CFAEdge> mSelfLoops = new HashSet<CFAEdge>(); 
    
    @Override
    public void init(CFANode pInitialNode) {
      mSelfLoops.add(InternalSelfLoop.getOrCreate(pInitialNode));
    }

    @Override
    public void visit(CFAEdge pEdge) {
      CFANode lSuccessor = pEdge.getSuccessor();
      
      mSelfLoops.add(InternalSelfLoop.getOrCreate(lSuccessor));
    }
    
  }
  
  public static Set<CFAEdge> addSelfLoops(CFANode pInitialNode) {
    CFATraversal.traverse(pInitialNode, AddSelfLoopCFAVisitor.mInstance);
    
    return AddSelfLoopCFAVisitor.mInstance.mSelfLoops;
  }
}
