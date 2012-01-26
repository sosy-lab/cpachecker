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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement;

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


/**
 * Directed acyclic graph representing ARTs linked by env. transitions.
 */
public class InterpolationDag {

  private final List<InterpolationDagNode> roots;
  private final Map<InterpolationDagNodeKey, InterpolationDagNode> nodeMap;


  /**
   * Makes a deep copy of the DAG.
   * @param oldDag
   */
  public InterpolationDag(InterpolationDag oldDag){
    this.roots = new Vector<InterpolationDagNode>(oldDag.roots.size());
    this.nodeMap = new HashMap<InterpolationDagNodeKey, InterpolationDagNode>();

    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    toProcess.addAll(oldDag.roots);

    while(!toProcess.isEmpty()){
      InterpolationDagNode oldNode = toProcess.poll();
      boolean parentsDone = true;
      for (InterpolationDagNode oldParent : oldNode.getParents()){
        if (!this.nodeMap.containsKey(oldParent.getKey())){
          parentsDone = false;
          break;
        }
      }

      if (!parentsDone){
        continue;
      }

      InterpolationDagNode newNode = new InterpolationDagNode(oldNode);
      this.nodeMap.put(newNode.getKey(), newNode);

      if (oldNode.getParents().isEmpty()){
        assert oldDag.roots.contains(oldNode);
        this.roots.add(newNode);
      }

      for (InterpolationDagNode oldParent : oldNode.getParents()){
        InterpolationDagNode newParent = this.nodeMap.get(oldParent.getKey());
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
    this.nodeMap = new HashMap<InterpolationDagNodeKey, InterpolationDagNode>();
  }

  public InterpolationDag(List<InterpolationDagNode> roots){
    this.roots = roots;
    this.nodeMap = new HashMap<InterpolationDagNodeKey, InterpolationDagNode>();
    this.getDagFromRoots(roots);
  }

  public void getDagFromRoots(List<InterpolationDagNode> roots){
    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    toProcess.addAll(roots);

    while (!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();
      nodeMap.put(node.key, node);
      toProcess.addAll(node.getChildren());
    }
  }

  /**
   * Returns a path from the root to the target element.
   * The path goes only through the thread of the target.
   * @param target
   * @return
   */
  public List<InterpolationDagNode> getModularPathToNode(InterpolationDagNode target){
    assert nodeMap.containsValue(target);

    List<InterpolationDagNode> path = new Vector<InterpolationDagNode>();
    InterpolationDagNode toProcess = target;
    int tid = target.getTid();

    while(toProcess != null){
      InterpolationDagNode node = toProcess;
      toProcess = null;
      path.add(0, node);
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
   * Retains given nodes in the DAG.
   * @param retain
   */
  public void retainNodes(Collection<InterpolationDagNode> retain){
    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();

    toProcess.addAll(roots);


    while(!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();

      for (InterpolationDagNode child : node.getChildren()){
        if (!toProcess.contains(child)){
          toProcess.addLast(child);
        }
      }

      if (!retain.contains(node)){
        removeNode(node.key);
      }
    }
  }

  /**
   * In the given thread removes all nodes whose keys are not in the collection.
   * @param retain
   */
  public void retainNodesInThread(Collection<InterpolationDagNodeKey> retain, int tid){

    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    Set<InterpolationDagNode> removed = new HashSet<InterpolationDagNode>();

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

      if (!retain.contains(node.key)){
        removeNode(node.key);
      }
    }

    dagAssertions();
  }

  /**
   * Removes a node from the DAG.
   * @param node
   */
  public void removeNode(InterpolationDagNodeKey key){
    InterpolationDagNode node = nodeMap.get(key);
    assert node != null;

    if (roots.contains(node)){
      roots.remove(node);
    }

    for (InterpolationDagNode parent : node.getParents()){
      boolean succ = parent.getChildren().remove(node);
      assert succ;
    }

    for (InterpolationDagNode child : node.getChildren()){
      boolean succ = child.getParents().remove(node);
      assert succ;
      if (child.getParents().isEmpty()){
        roots.add(child);
      }
    }

    nodeMap.remove(key);
  }

  public List<InterpolationDagNode> getRoots() {
    return roots;
  }

  public  Map<InterpolationDagNodeKey, InterpolationDagNode> getNodeMap() {
    return nodeMap;
  }

  /**
   * Adds the node to the DAG. If it doesn't have any parents, then its added as a root.
   * Method doesn't change parent-child relations.
   * @return
   */
  public void addNode(InterpolationDagNode node){
    nodeMap.put(node.key, node);

    if (node.parents.isEmpty()){
      roots.add(node);
    }
  }

  /**
   * Add all nodes from the other DAG to this one. If any root from the other DAG that doesn't
   * have parents becomes a root in the current DAG. Method doesn't change parent-child relations.
   * @param other
   */
  public void addDag(InterpolationDag other) {
    nodeMap.putAll(other.nodeMap);

    for (InterpolationDagNode root : other.roots){
      if (root.parents.isEmpty()){
        roots.add(root);
      }
    }
  }

  public InterpolationDagNode getNode(Integer tid, Integer elementId){
    InterpolationDagNodeKey key = new InterpolationDagNodeKey(tid, elementId);

    return nodeMap.get(key);
  }


  public InterpolationDagNode getNode(InterpolationDagNodeKey key){
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
  public List<List<InterpolationDagNodeKey>> getBranchesInThread(int tid) {

    InterpolationDagNode root = null;

    // search starting from every root in thread tid
    for (InterpolationDagNode node : roots){
      if (node.getTid() == tid){
        root = node;
        break;
      }
    }

    List<InterpolationDagNodeKey> path = new Vector<InterpolationDagNodeKey>();
    List<List<InterpolationDagNodeKey>>  branches = dfsAddBranch(root, path);

    return branches;
  }

  private List<List<InterpolationDagNodeKey>> dfsAddBranch(InterpolationDagNode node, List<InterpolationDagNodeKey> path){
    List<List<InterpolationDagNodeKey>> branches = new Vector<List<InterpolationDagNodeKey>>();

    if (node == null){
      return branches;
    }

    path.add(node.key);


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
      List<InterpolationDagNodeKey> branch = new Vector<InterpolationDagNodeKey>(path);
      branches.add(branch);
    }

    path.remove(path.size()-1);

    return branches;

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
      assert nodeMap.containsValue(node);


      for (InterpolationDagNode child : node.getChildren()){

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

    if (!nodeMap.containsKey(n1)){
      nodeMap.put(n1.getKey(), n1);
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

  /**
   * Get all env. transitions applied to nodes in thread td
   * @param tid
   * @return
   */
 /* public  ListMultimap<InterpolationDagNode, RelyGuaranteeCFAEdge> getAppliedEnvEdges(int tid) {
    ListMultimap<InterpolationDagNode, RelyGuaranteeCFAEdge> appliedMap = LinkedListMultimap.create();

    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    Set<InterpolationDagNode> visisted    = new HashSet<InterpolationDagNode>();
    InterpolationDagNode root = getRootForThread(tid);
    toProcess.add(root);

    while(!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();
      if (!visisted.contains(node)){
        visisted.add(node);

        if (node.getAppInfo() != null){
          for (RelyGuaranteeCFAEdge edge :  node.getAppInfo().getEnvMap().keySet()){
            appliedMap.put(node, edge);
          }
        }

        for (InterpolationDagNode child : node.children){
          if (child.tid == tid){
            toProcess.addLast(child);
          }
        }
      }
    }

    return appliedMap;
  }*/

  /**
   * List of leaves nodes in thread tid. Leaf is a node without any children in the same thread.
   * @param tid
   * @return
   */
  public List<InterpolationDagNode> getLeavesInThread(int tid) {

    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    Set<InterpolationDagNode> visisted    = new HashSet<InterpolationDagNode>();
    List<InterpolationDagNode> leaves     = new Vector<InterpolationDagNode>();
    InterpolationDagNode root = getRootForThread(tid);
    toProcess.add(root);

    while(!toProcess.isEmpty()){
      InterpolationDagNode node = toProcess.poll();
      if (!visisted.contains(node)){
        visisted.add(node);

        boolean noTidChild = true;
        for (InterpolationDagNode child : node.children){
          if (child.tid == tid){
            toProcess.addLast(child);
            noTidChild = false;
          }
        }

        if (noTidChild){
          leaves.add(node);
        }
      }
    }

    return leaves;
  }

  /**
   * Returns all nodes that belong to thread tid.
   * @param tid
   * @return
   */
  public Set<InterpolationDagNode> getNodesInThread(int tid) {

    Set<InterpolationDagNode> nodes = new HashSet<InterpolationDagNode>();

    for (InterpolationDagNode node : nodeMap.values()){
      if (node.tid == tid){
        nodes.add(node);
      }
    }

    return nodes;
  }


  /**
   * Return the node keys of the DAG in topogical order.
   */
  public List<InterpolationDagNode> topSort() {

    Set<InterpolationDagNode> visited = new HashSet<InterpolationDagNode>();
    List<InterpolationDagNode> topList = new Vector<InterpolationDagNode>();

    topList.addAll(roots);

    int i=0;
    while(i<topList.size()){
      InterpolationDagNode node = topList.get(i);
      for (InterpolationDagNode child : node.getChildren()){
        if (!visited.contains(child) && topList.containsAll(child.getParents())){
          visited.add(child);
          topList.add(child);
        }
      }
      i++;
    }


    return topList;
  }

  @Deprecated
  public List<InterpolationDagNodeKey> topSort2() {
    List<InterpolationDagNode> topList = topSort();
    List<InterpolationDagNodeKey> topKeyList = new Vector<InterpolationDagNodeKey>(topList.size());
    for (InterpolationDagNode node : topList){
      topKeyList.add(node.key);
    }

    return topKeyList;
  }

  public int size() {
    return nodeMap.size();
  }

  /**
   * Returns all ancestors of the node.
   * @param node
   * @return
   */
  public Set<InterpolationDagNode> getAncestorsOf(InterpolationDagNode node) {

    Deque<InterpolationDagNode> toProcess = new LinkedList<InterpolationDagNode>();
    Set<InterpolationDagNode> ancestors = new HashSet<InterpolationDagNode>();
    toProcess.addAll(node.parents);

    while (!toProcess.isEmpty()){
      InterpolationDagNode nd = toProcess.poll();
      ancestors.add(nd);

      for (InterpolationDagNode child : nd.parents){
        if (!ancestors.contains(child)){
          toProcess.addLast(child);
        }
      }
    }

    return ancestors;
  }






}
