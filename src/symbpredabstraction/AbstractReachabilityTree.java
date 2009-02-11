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
package symbpredabstraction;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;

import cfa.objectmodel.CFANode;

import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.symbpredabsCPA.SymbPredAbsAbstractElement;

public class AbstractReachabilityTree {

  private Map<SymbPredAbsAbstractElement, 
  Collection<SymbPredAbsAbstractElement>> tree;
  private SymbPredAbsAbstractElement root;

  public AbstractReachabilityTree() {
    tree = new HashMap<SymbPredAbsAbstractElement, 
    Collection<SymbPredAbsAbstractElement>>();
    root = null;
  }

  public void addChild(SymbPredAbsAbstractElement parent, 
                       SymbPredAbsAbstractElement child) {
    if (root == null) {
      root = parent;
    }
    if (!tree.containsKey(parent)) {
      tree.put(parent, new Vector<SymbPredAbsAbstractElement>());
    }
    Collection<SymbPredAbsAbstractElement> c = tree.get(parent);
    c.add(child);
  }

  public Collection<SymbPredAbsAbstractElement> getChildren(SymbPredAbsAbstractElement e) {
    if (tree.containsKey(e)) {
      return tree.get(e);
    }
    return Collections.emptySet();
  }

  public Collection<SymbPredAbsAbstractElement> getSubtree(
      AbstractElement root,
      boolean remove, boolean includeRoot) {

    Vector<SymbPredAbsAbstractElement> ret = 
      new Vector<SymbPredAbsAbstractElement>();

    Stack<SymbPredAbsAbstractElement> toProcess = 
      new Stack<SymbPredAbsAbstractElement>();
    toProcess.push((SymbPredAbsAbstractElement)root);

    while (!toProcess.empty()) {
      SymbPredAbsAbstractElement cur = toProcess.pop();
      ret.add(cur);
      if (tree.containsKey(cur)) {
        toProcess.addAll(remove ? tree.remove(cur) : tree.get(cur));
      }
    }
    if (!includeRoot) {
      SymbPredAbsAbstractElement tmp = ret.lastElement();
      assert(ret.firstElement() == root);
      ret.setElementAt(tmp, 0);
      ret.remove(ret.size()-1);
    }
    return ret;
  }

  public SymbPredAbsAbstractElement findHighest(CFANode loc) {
    if (root == null) return null;

    Queue<SymbPredAbsAbstractElement> toProcess =
      new ArrayDeque<SymbPredAbsAbstractElement>();
    toProcess.add(root);

    while (!toProcess.isEmpty()) {
      SymbPredAbsAbstractElement e = toProcess.remove();
      if (e.getAbstractionLocation().equals(loc)) {
        return e;
      }
      if (tree.containsKey(e)) {
        toProcess.addAll(tree.get(e));
      }
    }
    System.out.println("ERROR, NOT FOUND: " + loc);
    //assert(false);
    //return null;
    return root;
  }

  public boolean inTree(SymbPredAbsAbstractElement n) {
    Stack<SymbPredAbsAbstractElement> toProcess = 
      new Stack<SymbPredAbsAbstractElement>();
    toProcess.push(root);
    while (!toProcess.empty()) {
      SymbPredAbsAbstractElement e = toProcess.pop();
      if (e == n) return true;
      toProcess.addAll(getChildren(e));
    }
    return false;
  }

  public SymbPredAbsAbstractElement getRoot() { return root; }

  public boolean contains(SymbPredAbsAbstractElement n) {
    return tree.containsKey(n);
  }

  public void dump(String outfile) throws IOException {
    PrintWriter out = new PrintWriter(new File(outfile));
    out.println("digraph ART {");
    Stack<Pair<SymbPredAbsAbstractElement, Integer>> toProcess = 
      new Stack<Pair<SymbPredAbsAbstractElement, Integer>>();
    int i = 0;
    if (root != null) {
      toProcess.push(
          new Pair<SymbPredAbsAbstractElement, Integer>(root, i));
      out.println("" + (i++) + " [label=\"" + root + "\"];");
    }

    while (!toProcess.empty()) {
      Pair<SymbPredAbsAbstractElement, Integer> e = toProcess.pop();
      for (SymbPredAbsAbstractElement c : getChildren(e.getFirst())) {
        int cur = i;
        out.println("" + cur + " [label=\"" + c + "\"];");
        out.println("" + e.getSecond() + " -> " + cur);
        toProcess.push(
            new Pair<SymbPredAbsAbstractElement, Integer>(c, i));
        ++i;
      }
    }
    out.println("}");
    out.flush();
    out.close();
  }

  public void clear() {
    root = null;
    tree.clear();
  }

}
