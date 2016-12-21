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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/**
 * Algorithm after Lengauer and Tarjan
 * "A Fast Algorithm for Finding Dominators in a Flow Graph"
 * 1977
 * including small modification for functions.
 */
public class Dominators {

  private List<CFANode> vertex = new ArrayList<>();
  private Map<CFANode, NodeInfo> map = new TreeMap<>();
  private Map<CFANode, CFANode> dom = new TreeMap<>();

  private CFANode entry;
  private CFANode exit;
  private int mode;
  private int n;
  private Collection<CFANode> nodes;

  @SuppressWarnings("unused")
  public Dominators(CFA pCfa, int pMode) {
    this.entry = pCfa.getMainFunction();
    this.exit = ((FunctionEntryNode) entry).getExitNode();
    this.mode = pMode;
    this.nodes = pCfa.getAllNodes();
    this.vertex = new ArrayList<>(pCfa.getAllNodes().size());
    for (int i = 0; i < pCfa.getAllNodes().size() + 1; i++) {
      vertex.add(null);
    }
  }

  private void dfs(CFANode pNode) {
    NodeInfo infov = map.get(pNode);
    n = n + 1;
    infov.semi = n;
    vertex.set(n, pNode);
    infov.label = pNode;
    infov.ancestor = null;
    int m;
    if (mode == 0) {
      m = pNode.getNumLeavingEdges();
    } else {
      m = pNode.getNumEnteringEdges();
    }
    FunctionSummaryEdge e;
    CFANode w;
    if (mode == 0) {
      e = pNode.getLeavingSummaryEdge();
    } else {
      e = pNode.getEnteringSummaryEdge();
    }

    if (e != null) {
      if (mode == 0) {
        w = e.getSuccessor();
      } else {
        w = e.getPredecessor();
      }
      NodeInfo infow = map.get(w);
      if (infow.semi == 0) {
        infow.parent = pNode;
        dfs(w);
      }
      infow.pred.add(pNode);

    }
    // else{
    for (int i = 0; i < m; i++) {
      if (mode == 0) {
        w = pNode.getLeavingEdge(i).getSuccessor();
      } else {
        w = pNode.getEnteringEdge(i).getPredecessor();
      }
      NodeInfo infow = map.get(w);
      if (infow.semi == 0) {
        infow.parent = pNode;
        dfs(w);
      }
      infow.pred.add(pNode);
    }
    //   }
  }

  private void compress(CFANode pNode) {
    NodeInfo infov = map.get(pNode);
    CFANode av = infov.ancestor;
    NodeInfo infoav = map.get(av);
    if (infoav.ancestor != null) {
      compress(av);
      NodeInfo infovl = map.get(infov.label);
      NodeInfo infoavl = map.get(infoav.label);
      if (infoavl.semi < infovl.semi) {
        infov.label = infoav.label;
      }
      infov.ancestor = infoav.ancestor;
    }
  }

  private CFANode eval(CFANode pNode) {
    NodeInfo infov = map.get(pNode);
    if (infov.ancestor == null) {
      return pNode;
    } else {
      compress(pNode);
      return infov.label;
    }
  }

  private void link(CFANode pNodeV, CFANode pNodeW) {
    NodeInfo infow = map.get(pNodeW);
    infow.ancestor = pNodeV;
  }

  /**
   * Execute the specified Dominators computation.
   */
  public void execute() {
    //step0();
    step1();
    for (int i = n; i > 1; i--) {
      CFANode w = vertex.get(i);
      step2(w);
      step3(w);
    }
    step4();
  }

  private void step1() {
    n = 0;
    for (CFANode v : nodes) {
      NodeInfo infov = new NodeInfo();
      map.put(v, infov);
      infov.pred = new TreeSet<>();
      infov.bucket = new TreeSet<>();
      infov.semi = 0;
    }
    if (mode == 0) {
      dfs(entry);
    } else {
      dfs(exit);
    }
  }

  private void step2(CFANode pNodeW) {
    NodeInfo infow = map.get(pNodeW);
    for (CFANode v : infow.pred) {
      CFANode u = eval(v);
      NodeInfo infou = map.get(u);
      if (infou.semi < infow.semi) {
        infow.semi = infou.semi;
      }
      CFANode t = vertex.get(infow.semi);
      NodeInfo infot = map.get(t);
      infot.bucket.add(pNodeW);
      link(infow.parent, pNodeW);
    }
  }

  private void step3(CFANode pNodeW) {
    NodeInfo infow = map.get(pNodeW);
    NodeInfo infopw = map.get(infow.parent);
    for (CFANode v : infopw.bucket) {
      NodeInfo infov = map.get(v);
      CFANode u = eval(v);
      NodeInfo infou = map.get(u);
      if (infou.semi < infov.semi) {
        dom.put(v, u);
      } else {
        dom.put(v, infow.parent);
      }
    }
    infopw.bucket = new TreeSet<>();
  }

  private void step4() {
    for (int i = 2; i < n; i++) {
      CFANode w = vertex.get(i);
      NodeInfo infow = map.get(w);
      CFANode d = dom.get(w);
      if (!(d.equals(vertex.get(infow.semi)))) {
        CFANode dd = dom.get(d);
        dom.put(d, dd);
      }
    }
    if (mode == 0) {
      dom.put(entry, null);
    } else {
      dom.put(exit, null);
    }
  }

  /**
   * Returns the computed Dominators
   * @return the computed map of Dominators
   */
  public Map<CFANode, CFANode> getDom() {
    return dom;
  }

  static class NodeInfo {
    //CFANode node;
    private CFANode parent;
    private CFANode ancestor;
    private CFANode label;
    private Integer semi;
    private Collection<CFANode> pred;
    private Collection<CFANode> bucket;
  }

}
