/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Algorithm for computing the DominanceFrontier given a FlowGraph and its DominatorTree
 */
public class DominanceFrontier {

  private CFA cfa;
  private Map<CFANode, CFANode> dominators;
  private CFANode entry;
  private CFANode exit;
// @SuppressWarnings("unused")
// private int mode;
// @SuppressWarnings("unused")
// private Collection<CFANode> nodes;
  private Map<CFANode,TreeSet<CFANode>> df=new TreeMap<>();

  @SuppressWarnings("unused")
  public DominanceFrontier(CFA pCfa, Map<CFANode, CFANode> pDom){
    this.dominators=pDom;
    this.cfa=pCfa;
    this.entry=pCfa.getMainFunction();
    this.exit=((FunctionEntryNode) entry).getExitNode();
//    this.mode=pMode;
//    this.nodes = pCfa.getAllNodes();
    this.df=new TreeMap<>();
  }


  public void execute(){
    //Compute Dominator Frontier

    for(CFANode m: cfa.getAllNodes()){
      TreeSet<CFANode> dfset=new TreeSet<>();
      for(CFANode r: dominators.keySet()){
        if(!(r.equals(exit))){
          int n=r.getNumLeavingEdges();
          FunctionSummaryEdge e=r.getLeavingSummaryEdge();
          if(e!=null){
            n++;
          }
          for(int i=0; i<n; i++){
            CFANode p;
            if((e!=null) && (i==n-1)){
              p=e.getSuccessor();
            }
            else{
              p=r.getLeavingEdge(i).getSuccessor();
            }
            boolean containp=false;
            CFANode domp=p;
            while(domp!=null){
               if(domp.equals(m)){
                 containp=true;
               }
               domp=dominators.get(domp);
            }
            for(int j=i+1;j<n; j++){
              CFANode q;
              if((e!=null) && (j==n-1)){
                q=e.getSuccessor();
              }
              else{
                q=r.getLeavingEdge(j).getSuccessor();
              }
              boolean containq=false;
              CFANode domq=q;
              while(domq!=null){
                 if(domq.equals(m)){
                   containq=true;
                 }
                 domq=dominators.get(domq);
              }
              if((containp && !containq) || (!containp && containq)){
                  dfset.add(r);
              }
            }
          }
        }
      }
      df.put(m, dfset);
    }
  }

  public Map<CFANode,TreeSet<CFANode>> getDominanceFrontier(){
    return df;
  }

}
