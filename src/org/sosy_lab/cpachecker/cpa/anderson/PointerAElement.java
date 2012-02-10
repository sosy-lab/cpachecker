/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.anderson;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.anderson.util.BaseConstraint;
import org.sosy_lab.cpachecker.cpa.anderson.util.ComplexConstraint;
import org.sosy_lab.cpachecker.cpa.anderson.util.DirectedGraph;
import org.sosy_lab.cpachecker.cpa.anderson.util.SimpleConstraint;
import org.sosy_lab.cpachecker.cpa.pointerA.PointerAElement;
import org.sosy_lab.cpachecker.cpa.pointerA.util.Edge;

public class PointerAElement implements AbstractElement, Cloneable {

  // ------- global constraint system -------
  private static final Set<BaseConstraint> gBaseConstraints = new HashSet<BaseConstraint>();
  private static final Set<SimpleConstraint> gSimpleConstraints = new HashSet<SimpleConstraint>();
  private static final Set<ComplexConstraint> gComplexConstraints = new HashSet<ComplexConstraint>();

  private static final Map<String, String[]> gPointsToSets = new HashMap<String, String[]>();

  private static boolean gChanged = false;

  // ------- local constraint system -------
  private final Set<BaseConstraint> lBaseConstraints = new HashSet<BaseConstraint>();
  private final Set<SimpleConstraint> lSimpleConstraints = new HashSet<SimpleConstraint>();
  private final Set<ComplexConstraint> lComplexConstraints = new HashSet<ComplexConstraint>();

  private final Map<String, String[]> lPointsToSets = new HashMap<String, String[]>();

  private boolean lChanged = false;

  public PointerAElement() {}

  /**
   * Add a (new) {@link BaseConstraint} to this element.
   *
   * @param constr {@link BaseConstraint} that should be added.
   */
  void addConstraint(BaseConstraint constr) {

    lChanged |= lBaseConstraints.add(constr);
    gChanged |= gBaseConstraints.add(constr);
  }

  /**
   * Add a (new) {@link SimpleConstraint} to this element.
   *
   * @param constr {@link SimpleConstraint} that should be added.
   */
  void addConstraint(SimpleConstraint constr) {

    lChanged |= lSimpleConstraints.add(constr);
    gChanged |= gSimpleConstraints.add(constr);
  }

  /**
   * Add a (new) {@link ComplexConstraint} to this element.
   *
   * @param constr {@link ComplexConstraint} that should be added.
   */
  void addConstraint(ComplexConstraint constr) {

    lChanged |= lComplexConstraints.add(constr);
    gChanged |= gComplexConstraints.add(constr);

    //    String subVar = constr.getSubVar();
    //    String superVar = constr.getSuperVar();
    //    HashSet<String> set;
    //
    //    if (constr.isSubDerefed()) {
    //
    //      // local
    //      set = lComplexSub.get(subVar);
    //
    //      if (set == null) {
    //        set = new HashSet<String>();
    //        lComplexSub.put(subVar, set);
    //      }
    //
    //      lChanged |= set.add(superVar);
    //
    //      // global
    //      set = gComplexSub.get(subVar);
    //
    //      if (set == null) {
    //        set = new HashSet<String>();
    //        gComplexSub.put(subVar, set);
    //      }
    //
    //      gChanged |= set.add(superVar);
    //
    //    } else {
    //
    //      // local
    //      set = lComplexSuper.get(superVar);
    //
    //      if (set == null) {
    //        set = new HashSet<String>();
    //        lComplexSuper.put(superVar, set);
    //      }
    //
    //      lChanged |= set.add(subVar);
    //
    //      // global
    //      set = gComplexSuper.get(superVar);
    //
    //      if (set == null) {
    //        set = new HashSet<String>();
    //        gComplexSuper.put(superVar, set);
    //      }
    //
    //      gChanged |= set.add(subVar);
    //
    //    }
  }

  /**
   * Computes and returns the points-to sets for the local constraint system.
   *
   * @return points-to sets for the local constraint system.
   */
  public Map<String, String[]> getLocalPointsToSets() {

    if (lChanged) {
      computeDynTransitiveClosure(lBaseConstraints, lSimpleConstraints, lComplexConstraints, lPointsToSets);
      lChanged = false;
    }

    return lPointsToSets;
  }

  /**
   * Computes and returns the points-to sets for the global constraint system.
   *
   * @return points-to sets for the global constraint system.
   */
  public static Map<String, String[]> getGlobalPointsToSets() {

    if (gChanged) {
      computeDynTransitiveClosure(gBaseConstraints, gSimpleConstraints, gComplexConstraints, gPointsToSets);
      gChanged = false;
    }

    return gPointsToSets;
  }

  private static void computeDynTransitiveClosure(Set<BaseConstraint> bConstr, Set<SimpleConstraint> sConstr,
      Set<ComplexConstraint> cConstr, Map<String, String[]> ptSets) {

    Set<DirectedGraph.Node> workset = new HashSet<DirectedGraph.Node>();
    DirectedGraph g = new DirectedGraph();

    // HCD offline - build offline graph

    HashMap<DirectedGraph.Node, String> nodeStrMap = new HashMap<DirectedGraph.Node, String>();

    for (SimpleConstraint sc : sConstr) {

      String srcStr = sc.getSubVar();
      String destStr = sc.getSuperVar();

      DirectedGraph.Node src = g.getNode(srcStr);
      DirectedGraph.Node dest = g.getNode(destStr);
      g.addEdge(src, dest);

      workset.add(src);
      workset.add(dest);

      nodeStrMap.put(src, srcStr);
      nodeStrMap.put(dest, destStr);
    }

    for (ComplexConstraint cc : cConstr) {

      String srcStr, destStr;

      if (cc.isSubDerefed()) {

        srcStr = '*' + cc.getSubVar();
        destStr = cc.getSuperVar();

      } else {

        srcStr = cc.getSubVar();
        destStr = '*' + cc.getSuperVar();

      }

      DirectedGraph.Node src = g.getNode(srcStr);
      DirectedGraph.Node dest = g.getNode(destStr);
      g.addEdge(src, dest);

      workset.add(src);
      workset.add(dest);

      nodeStrMap.put(src, srcStr);
      nodeStrMap.put(dest, destStr);
    }

    // find strongly-connected components (using tarjans linear algorithm)
    int maxdfs = 1;
    LinkedList<DirectedGraph.Node> stack = new LinkedList<DirectedGraph.Node>();
    LinkedList<LinkedList<DirectedGraph.Node>> sccs = new LinkedList<LinkedList<DirectedGraph.Node>>();
    while (!workset.isEmpty()) {

      DirectedGraph.Node n = workset.iterator().next();

      maxdfs = tarjan(maxdfs, n, workset, stack, sccs);
    }

    // clean up part 1
    stack.clear(); // maybe helps GC
    stack = null;



    // build initial graph
    g.clear();
    workset.clear();

    for (BaseConstraint bc : bConstr) {

      DirectedGraph.Node n = g.getNode(bc.getSuperVar());
      n.addPointerTarget(bc.getSubVar());

      workset.add(n);
      workset.add(g.getNode(bc.getSuperVar()));
    }

    for (SimpleConstraint sc : sConstr) {

      DirectedGraph.Node src = g.getNode(sc.getSubVar());
      DirectedGraph.Node dest = g.getNode(sc.getSuperVar());
      g.addEdge(src, dest);

      workset.add(src);
      workset.add(dest);
    }

    for (ComplexConstraint cc : cConstr) {

      DirectedGraph.Node n;

      if (cc.isSubDerefed()) {

        n = g.getNode(cc.getSubVar());
        n.complexConstrMeSub.add(cc.getSuperVar());

      } else {

        n = g.getNode(cc.getSuperVar());
        n.complexConstrMeSuper.add(cc.getSubVar());

      }

      workset.add(n);
    }


    // for HCD...

    for (LinkedList<DirectedGraph.Node> scc : sccs) {

      LinkedList<DirectedGraph.Node> refNodes = new LinkedList<DirectedGraph.Node>();
      LinkedList<DirectedGraph.Node> normNodes = new LinkedList<DirectedGraph.Node>();

      for (DirectedGraph.Node n : scc) {

        // translate to new node
        String str = nodeStrMap.get(n);
        if (str.charAt(0) == '*')
          refNodes.add(g.getNode(str.substring(1)));
        else
          normNodes.add(g.getNode(str));
      }

      DirectedGraph.Node merged = g.mergeNodes(normNodes.poll(), normNodes);

      for (DirectedGraph.Node n : refNodes)
        n.mergePts = merged;

      scc.clear();
    }

    // clean up part 2
    sccs.clear();
    nodeStrMap.clear();
    sccs = null;
    nodeStrMap = null;



    HashSet<DirectedGraph.Edge> tested = new HashSet<DirectedGraph.Edge>();

    // dynamic transitive closure
    while (!workset.isEmpty()) {

      DirectedGraph.Node n = workset.iterator().next();
      workset.remove(n);
      if (!n.isValid())
        continue;

      // last two lines of HCD code
      if (n.mergePts != null)
        g.mergeNodes(n.mergePts, n.getPointsToNodesSet());

      for (DirectedGraph.Node v : n.getPointsToNodesSet()) {

        for (String aStr : n.complexConstrMeSub) {
          DirectedGraph.Node a = g.getNode(aStr);

          if (!v.isSuccessor(a)) {
            g.addEdge(v, a);
            workset.add(v);
          }
        }

        for (String bStr : n.complexConstrMeSuper) {
          DirectedGraph.Node b = g.getNode(bStr);

          if (!b.isSuccessor(v)) {
            g.addEdge(b, v);
            workset.add(b);
          }
        }

      } // for (String vStr : n.pointsToSet)

      for (DirectedGraph.Node z : n.getSuccessors()) {

        // LCD code
        DirectedGraph.Edge edge = g.new Edge(n, z);
        if (z.getPointsToSet().equals(n.getPointsToSet()) && !tested.contains(edge)) {
          tested.add(edge);
          DirectedGraph.Node merged = g.detectAndCollapseCycleContainingEdge(edge);

          if (merged != null) {
            workset.add(merged);
            break;
          }

        } else if (n.propagatePointerTargetsTo(z))
          workset.add(z);

      }

    } // while (!workset.isEmpty())

    ptSets.clear();

    for (Map.Entry<String, DirectedGraph.Node> e : g.getNameMappings()) {

      Collection<String> ptSetNode = e.getValue().getPointsToSet();
      ptSets.put(e.getKey(), ptSetNode.toArray(new String[ptSetNode.size()]));
    }
  }

  private static int tarjan(int maxdfs, DirectedGraph.Node v, Set<DirectedGraph.Node> workset,
      LinkedList<DirectedGraph.Node> stack, LinkedList<LinkedList<DirectedGraph.Node>> sccs) {

    v.dfs = maxdfs;
    v.lowlink = maxdfs;
    maxdfs++;
    stack.push(v);
    workset.remove(v);

    for (DirectedGraph.Node succ : v.getSuccessors()) {
      if (workset.contains(succ)) {
        maxdfs = tarjan(maxdfs, succ, workset, stack, sccs);
        v.lowlink = Math.min(v.lowlink, succ.lowlink);
      } else if (succ.dfs > 0) // <==> stack.contains(succ)
        v.lowlink = Math.min(v.lowlink, succ.dfs);
    }

    if (v.lowlink == v.dfs) {

      DirectedGraph.Node succ;
      LinkedList<DirectedGraph.Node> scc = new LinkedList<DirectedGraph.Node>();

      do {
        succ = stack.pop();
        succ.dfs = -succ.dfs;
        scc.add(succ);
      } while (!succ.equals(v));

      if (scc.size() > 1)
        sccs.add(scc);
    }

    return maxdfs;
  }

  //  /**
  //   * This element joins this element with another element.
  //   *
  //   * @param other the other element to join with this element
  //   * @return a new element representing the join of this element and the other element
  //   */
  //  public PointerAElement join(PointerAElement other) {
  //    return null;
  //  }
  //
  //  /**
  //   * This method decides if this element is less or equal than the other element, based on the order imposed by the lattice.
  //   *
  //   * @param other the other element
  //   * @return true, if this element is less or equal than the other element, based on the order imposed by the lattice
  //   */
  //  public boolean isLessOrEqual(PointerAElement other) {
  //    return false;
  //  }

  @Override
  public boolean equals(Object other) {

    if (this == other)
      return true;

    if (other == null || !this.getClass().equals(other.getClass()))
      return false;

    PointerAElement oEl = (PointerAElement) other;

    return (this.lBaseConstraints.equals(oEl.lBaseConstraints)
        && this.lSimpleConstraints.equals(oEl.lSimpleConstraints)
        && this.lComplexConstraints.equals(oEl.lComplexConstraints));
  }

  @Override
  public PointerAElement clone() {

    // super.clone() is not possible for final attributes... and the list interface also doesn't
    // provide a clone() method
    PointerAElement clone = new PointerAElement();

    clone.lBaseConstraints.addAll(this.lBaseConstraints);
    clone.lSimpleConstraints.addAll(this.lSimpleConstraints);
    clone.lComplexConstraints.addAll(this.lComplexConstraints);

    clone.lPointsToSets.putAll(this.lPointsToSets);

    clone.lChanged = this.lChanged;

    return clone;
  }

  @Override
  public int hashCode() {

    int hash = 51;
    hash = 31 * hash + this.lBaseConstraints.hashCode();
    hash = 31 * hash + this.lSimpleConstraints.hashCode();
    hash = 31 * hash + this.lComplexConstraints.hashCode();

    return hash;
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    sb.append('[').append('\n');

    for (BaseConstraint bc : this.lBaseConstraints) {
      sb.append('{').append(bc.getSubVar()).append("} \u2286 ");
      sb.append(bc.getSuperVar()).append('\n');
    }

    for (SimpleConstraint bc : this.lSimpleConstraints) {
      sb.append(bc.getSubVar()).append(" \u2286 ");
      sb.append(bc.getSuperVar()).append('\n');
    }

    for (ComplexConstraint bc : this.lComplexConstraints) {
      sb.append(bc.getSubVar()).append(" \u2286 ");
      sb.append(bc.getSuperVar()).append('\n');
    }

    int size = this.lBaseConstraints.size() + this.lSimpleConstraints.size() + this.lComplexConstraints.size();

    sb.append("] size->  ").append(size);

    // points-to sets
    sb.append('\n');
    sb.append('[').append('\n');

    Map<String, String[]> ptSet = getLocalPointsToSets();
    for (String key : ptSet.keySet()) {

      sb.append(key).append(" -> {");
      String[] vals = ptSet.get(key);

      for (String val : vals)
        sb.append(val).append(',');

      if (vals.length > 0)
        sb.setLength(sb.length() - 1);

      sb.append('}').append('\n');
    }

    sb.append(']').append('\n');

    return sb.toString();
  }
}
