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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Directed acyclic graph representing ART and env. transitions.
 */
public class InterpolationDag {

  private final List<InterpolationDagNode> roots;
  private final Map<Pair<Integer, Integer>, InterpolationDagNode> nodeMap;

  /**
   * Makes a deep copy of the DAG.
   * @param oldDag
   */
  public InterpolationDag(InterpolationDag oldDag){
    this.roots = new Vector<InterpolationDagNode>(oldDag.roots.size());
    this.nodeMap = new HashMap<Pair<Integer, Integer>, InterpolationDagNode>();

    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    toProcess.addAll(oldDag.roots);

    while(!toProcess.isEmpty()){
      InterpolationDagNode oldNode = toProcess.poll();
      boolean parentsDone = true;
      for (InterpolationDagNode oldParent : oldNode.getParents()){
        if (!this.nodeMap.containsKey(Pair.of(oldParent.tid, oldParent.artElement.getElementId()))){
          parentsDone = false;
          break;
        }
      }

      if (!parentsDone){
        continue;
      }

      InterpolationDagNode newNode = new InterpolationDagNode(oldNode);
      this.nodeMap.put(Pair.of(newNode.tid, newNode.artElement.getElementId()), newNode);

      if (oldNode.getParents().isEmpty()){
        assert oldDag.roots.contains(oldNode);
        this.roots.add(newNode);
      }

      for (InterpolationDagNode oldParent : oldNode.getParents()){
        InterpolationDagNode newParent = this.nodeMap.get(Pair.of(oldParent.tid, oldParent.artElement.getElementId()));
        assert newParent != null;
        newParent.children.add(newNode);
        newNode.parents.add(newParent);
      }

      for (InterpolationDagNode child : oldNode.getChildren()){
        if (!toProcess.contains(child)){
          toProcess.addLast(child);
        }
      }
    }

    dagAssertions();
  }

  public InterpolationDag(){
    this.roots = new Vector<InterpolationDagNode>();
    this.nodeMap = new HashMap<Pair<Integer, Integer>, InterpolationDagNode>();
  }

  public InterpolationDag(List<InterpolationDagNode> roots){
    this.roots = roots;
    this.nodeMap = new HashMap<Pair<Integer, Integer>, InterpolationDagNode>();
    this.getDagFromRoots(roots);
  }

  public void getDagFromRoots(List<InterpolationDagNode> roots){
    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    toProcess.addAll(roots);

    while (!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();
      nodeMap.put(Pair.of(node.getTid(), node.getArtElement().getElementId()), node);
      toProcess.addAll(node.getChildren());
    }
  }

  /**
   * Returns a path from the root to the target element.
   * The path goes only through the thread of the target.
   * @param target
   * @return
   */
  public List<Pair<Integer, Integer>> getModularPathToNode(InterpolationDagNode target){
    assert nodeMap.containsValue(target);

    List<Pair<Integer, Integer>> path = new Vector<Pair<Integer, Integer>>();
    InterpolationDagNode toProcess = target;
    int tid = target.getTid();

    while(toProcess != null){
      InterpolationDagNode node = toProcess;
      toProcess = null;
      path.add(0, Pair.of(node.tid, node.artElement.getElementId()));
      for (InterpolationDagNode parent : node.getParents()){
        if (parent.getTid() == tid){
          toProcess = parent;
          break;
        }
      }
    }

    return path;
  }

  /**
   * In the threadremoves all nodes that are not in the collection.
   * @param retain
   * @return
   */
  public void retainNodesInThread(Collection<Pair<Integer, Integer>> retain, int tid){

    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    for (InterpolationDagNode root : roots){
      if (root.getTid() == tid){
        toProcess.add(root);
      }
    }

    while(!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();

      for (InterpolationDagNode child : node.getChildren()){
        if (child.getTid() == tid){
          toProcess.addLast(child);
        }
      }

      if (!retain.contains(Pair.of(node.tid, node.artElement.getElementId()))){
        removeNode(node);
      }
    }

    dagAssertions();
  }

  /**
   * Removes a non-root node from the DAG.
   * @param node
   */
  public void removeNode(InterpolationDagNode node){
    assert nodeMap.containsValue(node);

    if (roots.contains(node)){
      roots.remove(node);
    }

    for (InterpolationDagNode parent : node.getParents()){
      boolean succ = parent.getChildren().remove(node);
      assert succ;
    }

    for (InterpolationDagNode child : node.getChildren()){
      boolean succ = child.getParents().remove(node);
      if (!succ){
        System.out.println();
      }
      assert succ;
      if (child.getParents().isEmpty()){
        roots.add(child);
      }
    }

    nodeMap.remove(Pair.of(node.getTid(), node.getArtElement().getElementId()));
  }

  public List<InterpolationDagNode> getRoots() {
    return roots;
  }

  public  Map<Pair<Integer, Integer>, InterpolationDagNode> getNodeMap() {
    return nodeMap;
  }

  public InterpolationDagNode getNode(Integer tid, Integer elementId){
    return nodeMap.get(Pair.of(tid, elementId));
  }

  public InterpolationDagNode getNode(Pair<Integer, Integer> key){
    return nodeMap.get(key);
  }

  /**
   * Writes a DOT file for DAG representation of ARTs and environmental transitions.
   */
  public void writeToDOT(String file) {
    FileWriter fstream;
    try {
      fstream = new FileWriter(file);
      BufferedWriter out = new BufferedWriter(fstream);;
      String s = DOTDagBuilder.generateDOT(roots);
      out.write(s);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get a list of branches (lists of nodes keys) in the thread tid.
   * @param tid
   * @return
   */
  public List<List<Pair<Integer, Integer>>> getBranchesInThread(int tid) {

    InterpolationDagNode root = null;

    // search starting from every root in thread tid
    for (InterpolationDagNode node : roots){
      if (node.getTid() == tid){
        root = node;
        break;
      }
    }

    List<Pair<Integer, Integer>> path = new Vector<Pair<Integer, Integer>>();
    List<List<Pair<Integer, Integer>>>  branches = dfsAddBranch(root, path);

    return branches;
  }

  private List<List<Pair<Integer, Integer>>> dfsAddBranch(InterpolationDagNode node, List<Pair<Integer, Integer>> path){
    List<List<Pair<Integer, Integer>>> branches = new Vector<List<Pair<Integer, Integer>>>();

    if (node == null){
      return branches;
    }

    path.add(Pair.of(node.tid, node.artElement.getElementId()));


    boolean hasChildren = false;
    for (InterpolationDagNode child : node.getChildren()){
      if (child.getTid() == node.getTid()){
        branches.addAll(dfsAddBranch(child, path));
        hasChildren = true;
      }
    }

    if (!hasChildren){
      /*System.out.print("Added branch: ");
      for (InterpolationDagNode bn : path){
        System.out.println(bn.getArtElement().getElementId()+" ");
      }*/
      System.out.println();
      List<Pair<Integer, Integer>> branch = new Vector<Pair<Integer, Integer>>(path);
      branches.add(branch);
    }

    path.remove(path.size()-1);

    return branches;

  }

  /**
   * Replaces a node with a new one with the given path formula. Returns the new node.
   * @param node
   * @param newPf
   */
  public InterpolationDagNode replacePathFormulaInNode(InterpolationDagNode node, PathFormula newPf) {
    assert nodeMap.values().contains(node);
    InterpolationDagNode newNode = new InterpolationDagNode(newPf, node.traceNo, node.artElement, node.children, node.parents, node.tid, node.envPrimes);

    for (InterpolationDagNode parent : node.parents){
      boolean succ = parent.children.remove(node);
      assert succ;
      parent.children.add(newNode);
    }

    for (InterpolationDagNode child : node.children){
      boolean succ = child.parents.remove(node);
      assert succ;
      child.parents.add(newNode);
    }

    boolean succ = roots.remove(node);
    if (succ){
      roots.add(newNode);
    }

    Pair<Integer, Integer> key = null;
    for (Pair<Integer, Integer> pair: nodeMap.keySet()){
      if (nodeMap.get(pair) == node){
        key = pair;
        break;
      }
    }
    assert key != null;
    nodeMap.put(key, newNode);
    // TODO remove
    dagAssertions();

    return newNode;
  }


  /**
   * Check the correctness of the DAG.
   * @param roots
   */
  public void dagAssertions(){

    for (InterpolationDagNode root : roots){
      assert root.parents.isEmpty();
    }

    dagAssertions(roots);
  }


  private void dagAssertions(List<InterpolationDagNode> nodes){
    for (InterpolationDagNode node : nodes){

      if (!nodeMap.containsValue(node)){
        System.out.println(node);
      }
      assert nodeMap.containsValue(node);


      for (InterpolationDagNode child : node.getChildren()){
        if (!child.getParents().contains(node)){
          System.out.println();
        }

        assert child.getParents().contains(node);
      }

      dagAssertions(node.getChildren());
    }
  }



  /**
   * Make n1 parent of n2. n2 should be in the DAG.
   * @param parent
   * @param child
   */
  public void makeParentOf(InterpolationDagNode n1, InterpolationDagNode n2) {
    assert !n1.children.contains(n2);
    assert !n2.parents.contains(n1);
    assert nodeMap.containsValue(n2);
    n1.children.add(n2);
    n2.parents.add(n1);

    if (!nodeMap.containsKey(Pair.of(n1.tid, n1.artElement.getElementId()))){
      nodeMap.put(Pair.of(n1.tid, n1.artElement.getElementId()), n1);
    }

    if (roots.contains(n2)){
      roots.remove(n2);
      roots.add(n1);
    }
  }

  /**
   * Return the first root of thread tid.
   * @param tid
   * @return
   */
  public InterpolationDagNode getRootForThread(int tid){
    InterpolationDagNode root = null;
    for (InterpolationDagNode node : roots){
      if (node.tid == tid){
        root = node;
        break;
      }
    }
    return root;
  }

  public  ListMultimap<InterpolationDagNode, RelyGuaranteeCFAEdge> getAppliedEnvEdges(int td) {
    ListMultimap<InterpolationDagNode, RelyGuaranteeCFAEdge> appliedMap = LinkedListMultimap.create();

    // get all env. transitions applied to nodes in thread td
    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    Set<InterpolationDagNode> visisted    = new HashSet<InterpolationDagNode>();
    InterpolationDagNode root = getRootForThread(td);
    toProcess.add(root);

    while(!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();
      if (!visisted.contains(node)){
        visisted.add(node);

        for (RelyGuaranteeCFAEdge edge :  node.getEnvPrimes().keySet()){
          appliedMap.put(node, edge);
        }

        toProcess.addAll(node.children);
      }
    }

    return appliedMap;
  }





}
