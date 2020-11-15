// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

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
  private Map<CFANode, NavigableSet<CFANode>> df = new TreeMap<>();

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
      NavigableSet<CFANode> dfset = new TreeSet<>();
      for(CFANode r: dominators.keySet()){
        if (!r.equals(exit)) {
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

  public Map<CFANode, NavigableSet<CFANode>> getDominanceFrontier() {
    return df;
  }

}
