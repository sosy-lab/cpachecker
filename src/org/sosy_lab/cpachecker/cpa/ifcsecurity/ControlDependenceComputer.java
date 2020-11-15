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

/**
 * Algorithm for Computing the Control Dependencies given a Flow Graph and its DominanceFrontier
 */
public class ControlDependenceComputer {

  private CFA cfa;

  private Map<CFANode, NavigableSet<CFANode>> df = new TreeMap<>();
  private Map<CFANode, NavigableSet<CFANode>> cd = new TreeMap<>();
  private Map<CFANode, NavigableSet<CFANode>> rcd = new TreeMap<>();

  public ControlDependenceComputer(CFA pCfa, Map<CFANode, NavigableSet<CFANode>> pDf) {
    this.df=pDf;
    this.cfa=pCfa;
  }

  public void execute(){
    //Control Dependence
    for(CFANode m: cfa.getAllNodes()){
      NavigableSet<CFANode> cdset = new TreeSet<>();
      cd.put(m,cdset);
    }
    for(CFANode n: cfa.getAllNodes()){
      for(CFANode m: df.get(n)){
        NavigableSet<CFANode> cdset = cd.get(m);
        cdset.add(n);
      }
    }

    for(CFANode m: cfa.getAllNodes()){
      NavigableSet<CFANode> cdset = new TreeSet<>();
      rcd.put(m,cdset);
    }

    for(CFANode n: cfa.getAllNodes()){
      for(CFANode m: cd.get(n)){
        NavigableSet<CFANode> rcdset = rcd.get(m);
        rcdset.add(n);
      }
    }

    boolean changed=true;

    while(changed){
      changed=false;
      for(CFANode n: cfa.getAllNodes()){
        NavigableSet<CFANode> rcdsetn = rcd.get(n);
        for(CFANode m: new TreeSet<>(rcdsetn)){
          NavigableSet<CFANode> rcdsetm = rcd.get(m);
          for(CFANode l: rcdsetm){
            if (!rcdsetn.contains(l)) {
              changed=true;
              rcdsetn.add(l);
            }
          }
        }
      }
    }
  }

  public Map<CFANode, NavigableSet<CFANode>> getControlDependency() {
    return cd;
  }

  public Map<CFANode, NavigableSet<CFANode>> getReversedControlDependency() {
    return rcd;
  }
}
