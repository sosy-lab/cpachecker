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

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Algorithm for Computing the Control Dependencies given a Flow Graph and its DominanceFrontier
 */
public class ControlDependenceComputer {

  private CFA cfa;

  private Map<CFANode,TreeSet<CFANode>> df=new TreeMap<>();
  private Map<CFANode,TreeSet<CFANode>> cd=new TreeMap<>();
  private Map<CFANode,TreeSet<CFANode>> rcd=new TreeMap<>();

  public ControlDependenceComputer(CFA pCfa, Map<CFANode,TreeSet<CFANode>> pDf){
    this.df=pDf;
    this.cfa=pCfa;
  }

  public void execute(){
    //Control Dependence
    for(CFANode m: cfa.getAllNodes()){
      TreeSet<CFANode> cdset=new TreeSet<>();
      cd.put(m,cdset);
    }
    for(CFANode n: cfa.getAllNodes()){
      for(CFANode m: df.get(n)){
        TreeSet<CFANode> cdset=cd.get(m);
        cdset.add(n);
      }
    }

    for(CFANode m: cfa.getAllNodes()){
      TreeSet<CFANode> cdset=new TreeSet<>();
      rcd.put(m,cdset);
    }

    for(CFANode n: cfa.getAllNodes()){
      for(CFANode m: cd.get(n)){
        TreeSet<CFANode> rcdset=rcd.get(m);
        rcdset.add(n);
      }
    }

    boolean changed=true;

    while(changed){
      changed=false;
      for(CFANode n: cfa.getAllNodes()){
        TreeSet<CFANode> rcdsetn=rcd.get(n);
        for(CFANode m: new TreeSet<>(rcdsetn)){
          TreeSet<CFANode> rcdsetm=rcd.get(m);
          for(CFANode l: rcdsetm){
            if(!(rcdsetn.contains(l))){
              changed=true;
              rcdsetn.add(l);
            }
          }
        }
      }
    }
  }

  public Map<CFANode,TreeSet<CFANode>> getControlDependency(){
    return cd;
  }

  public Map<CFANode,TreeSet<CFANode>> getReversedControlDependency(){
    return rcd;
  }
}
