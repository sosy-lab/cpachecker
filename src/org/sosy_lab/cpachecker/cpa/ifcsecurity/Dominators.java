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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Algorithm after Lengauer and Tarjan
 * "A Fast Algorithm for Finding Dominators in a Flow Graph"
 * 1977
 * including small modification for functions.
 */
public class Dominators {

  private ArrayList<CFANode> vertex = new ArrayList<>();
  private TreeMap<CFANode, NodeInfo> map = new TreeMap<>();
  private TreeMap<CFANode, CFANode> dom = new TreeMap<>();

  private CFANode entry;
  private CFANode exit;
  private int mode;
  private int n;
  private Collection<CFANode> nodes;
  private CFA cfa;

  public Dominators(CFA cfa, int mode) {
    this.cfa = cfa;
    this.entry = cfa.getMainFunction();
    this.exit = ((FunctionEntryNode) entry).getExitNode();
    this.mode = mode;
    this.nodes = cfa.getAllNodes();
    this.vertex = new ArrayList<>(cfa.getAllNodes().size());
    for (int i = 0; i < cfa.getAllNodes().size() + 1; i++) {
      vertex.add(null);
    }
    //this.n=cfa.getAllNodes().size();
  }

  private void dfs(CFANode v) {
    NodeInfo infov = map.get(v);
    n = n + 1;
    infov.semi = n;
    vertex.set(n, v);
    infov.label = v;
    infov.ancestor = null;
    int m;
    if (mode == 0) {
      m = v.getNumLeavingEdges();
    }
    else {
      m = v.getNumEnteringEdges();
    }
    FunctionSummaryEdge e;
    CFANode w;
    if (mode == 0) {
      e = v.getLeavingSummaryEdge();
    }
    else {
      e = v.getEnteringSummaryEdge();
    }

    if (e != null) {
      if (mode == 0) {
        w = e.getSuccessor();
      }
      else {
        w = e.getPredecessor();
      }
      NodeInfo infow = map.get(w);
      if (infow.semi == 0) {
        infow.parent = v;
        dfs(w);
      }
      infow.pred.add(v);

    }
    // else{
    for (int i = 0; i < m; i++) {
      if (mode == 0) {
        w = v.getLeavingEdge(i).getSuccessor();
      }
      else {
        w = v.getEnteringEdge(i).getPredecessor();
      }
      NodeInfo infow = map.get(w);
      if (infow.semi == 0) {
        infow.parent = v;
        dfs(w);
      }
      infow.pred.add(v);
    }
    //   }
  }

  private void compress(CFANode v) {
    NodeInfo infov = map.get(v);
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

  private CFANode eval(CFANode v) {
    NodeInfo infov = map.get(v);
    if (infov.ancestor == null) {
      return v;
    }
    else {
      compress(v);
      return infov.label;
    }
  }

  private void link(CFANode v, CFANode w) {
    NodeInfo infow = map.get(w);
    infow.ancestor = v;
  }

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

  public TreeMap<CFANode, TreeSet<CFANode>> functionedge = new TreeMap<>();
  public TreeMap<CFANode, TreeSet<CFANode>> reversedfunctionedge = new TreeMap<>();

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
    }
    else {
      dfs(exit);
    }
  }

  private void step2(CFANode w) {
    NodeInfo infow = map.get(w);
    for (CFANode v : infow.pred) {
      CFANode u = eval(v);
      NodeInfo infou = map.get(u);
      if (infou.semi < infow.semi) {
        infow.semi = infou.semi;
      }
      CFANode t = vertex.get(infow.semi);
      NodeInfo infot = map.get(t);
      infot.bucket.add(w);
      link(infow.parent, w);
    }
  }

  private void step3(CFANode w) {
    NodeInfo infow = map.get(w);
    NodeInfo infopw = map.get(infow.parent);
    Iterator<CFANode> iterator = infopw.bucket.iterator();
    while (iterator.hasNext()) {
      CFANode v = iterator.next();
      NodeInfo infov = map.get(v);
      CFANode u = eval(v);
      NodeInfo infou = map.get(u);
      if (infou.semi < infov.semi) {
        dom.put(v, u);
      }
      else {
        dom.put(v, infow.parent);
      }
    }
    infopw.bucket = new TreeSet<>();
    ;
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
    }
    else {
      dom.put(exit, null);
    }
  }

  public TreeMap<CFANode, CFANode> getDom() {
    return dom;
  }

  class NodeInfo {

    CFANode node;
    CFANode parent;
    CFANode ancestor;
    CFANode label;
    Integer semi;
    Collection<CFANode> pred;
    Collection<CFANode> bucket;
  }

}
