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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


/**
 * Tree of abstraction points.
 */
public class InterpolationTree {

  private  InterpolationTreeNode root;
  private final Set<InterpolationTreeNode> leafs;
  private final Map<InterpolationTreeNodeKey, InterpolationTreeNode> nodeMap;

  /**
   * Create an empty tree.
   */
  public InterpolationTree(){
    this.root = null;
    this.leafs = new HashSet<InterpolationTreeNode>();
    this.nodeMap = new HashMap<InterpolationTreeNodeKey, InterpolationTreeNode>();
  }

  public void addNode(InterpolationTreeNode node) {
    if (node.parent == null){
      this.root = node;
    } else {
      this.leafs.remove(node.parent);
    }

    if (node.children.isEmpty()){
      this.leafs.add(node);

    }

    this.nodeMap.put(node.getKey(), node);
  }

  public InterpolationTreeNode getNode(Integer tid, Integer ARTElementId, Integer uniqueId) {
    InterpolationTreeNodeKey key = new InterpolationTreeNodeKey(tid, ARTElementId, uniqueId);
    return this.nodeMap.get(key);
  }

  /**
   * Removes node from the tree.
   * @param node
   */
  public void removeNode(InterpolationTreeNode node) {
    nodeMap.remove(node.key);
    if (root.equals(node)){
      root = null;
    }
    leafs.remove(node);
  }

  /**
   * Attaches a non-empty subtree. Parent-child relationships should be set before.
   * @param sTree
   */
  public void addSubTree(InterpolationTree sTree) {
    assert sTree.root != null && sTree.root.parent != null;
    InterpolationTreeNodeKey key = sTree.root.parent.key;
    InterpolationTreeNode node = this.nodeMap.get(key);
    assert node != null;
    leafs.remove(node);
    leafs.addAll(sTree.leafs);
    nodeMap.putAll(sTree.nodeMap);
  }

  /**
   * Writes a DOT file for a tree representation of ARTs and environmental transitions.
   */
  public void writeToDOT(String file) {
    FileWriter fstream;
    try {
      fstream = new FileWriter(file);
      BufferedWriter out = new BufferedWriter(fstream);;
      String s = DOTTreeBuilder.generateDOT(root);
      out.write(s);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns nodes in topological order.
   * @return
   */
  public List<InterpolationTreeNode> topSort(){

    Deque<InterpolationTreeNode> queue = new LinkedList<InterpolationTreeNode>();
    List<InterpolationTreeNode> topList = new Vector<InterpolationTreeNode>(this.nodeMap.size());

    // find leaf that with the same id as the root
    InterpolationTreeNode lLeaf = null;
    for (InterpolationTreeNode leaf : this.leafs){
      if (leaf.uniqueId == this.root.uniqueId){
        lLeaf = leaf;
        break;
      }
    }
    assert lLeaf != null;

    queue.add(lLeaf);

    while(!queue.isEmpty()){
      InterpolationTreeNode node = queue.pop();

      if (topList.containsAll(node.children)){
        // all children haven been visited
        topList.add(node);

        if (node.parent != null){
          queue.addLast(node.parent);
        }
      } else {
        // find the first missing child
        InterpolationTreeNode mChild = null;
        for(InterpolationTreeNode child : node.children){
          if (!topList.contains(child)){
            mChild = child;
            break;
          }
        }

        assert mChild != null;

        // find the leaf for the missing child
        InterpolationTreeNode mLeaf = null;
        for (InterpolationTreeNode leaf : this.leafs){
          if (leaf.uniqueId.equals(mChild.uniqueId)){
            mLeaf = leaf;
            break;
          }
        }

        assert mLeaf != null;

        queue.addLast(mLeaf);
      }

    }

    return topList;
  }


  /**
   * Returns a list of all ancestors of the node.
   * @param node
   * @return
   */
  public List<InterpolationTreeNode> getAncestorsOf(InterpolationTreeNode node) {

    Deque<InterpolationTreeNode> queue = new LinkedList<InterpolationTreeNode>();
    List<InterpolationTreeNode>  cList = new Vector<InterpolationTreeNode>();

    queue.addAll(node.children);
    while(!queue.isEmpty()){
      InterpolationTreeNode cNode = queue.pop();
      cList.add(cNode);
      queue.addAll(cNode.children);
    }

    return cList;
  }

  /**
   * Get node by key.
   * @param key
   * @return
   */
  public InterpolationTreeNode getNode(InterpolationTreeNodeKey key) {
    return this.nodeMap.get(key);
  }


  /**
   * Removes subtree root at the node.
   * @param node
   */
  public void removeSubtree(InterpolationTreeNode node){
    if (node.equals(root)){
      root = null;
    } else {
      node.parent.children.remove(node);

      boolean hasSucc = false;
      for (InterpolationTreeNode child : node.parent.children){
        if (child.uniqueId.equals(node.parent.uniqueId)){
          hasSucc = true;
          break;
        }
      }
      if (!hasSucc){
        leafs.add(node.parent);
      }


      node.parent = null;
    }

    leafs.remove(node);
    nodeMap.remove(node.key);

    Deque<InterpolationTreeNode> queue = new LinkedList<InterpolationTreeNode>();

    queue.addAll(node.children);
    while(!queue.isEmpty()){
      InterpolationTreeNode cNode = queue.pop();
      nodeMap.remove(cNode.key);
      leafs.remove(cNode);
      queue.addAll(cNode.children);
    }
  }

  /**
   * Removes all ancestors of the node.
   * @param node
   */
  public void removeAncestorsOf(InterpolationTreeNode node) {
    Deque<InterpolationTreeNode> queue = new LinkedList<InterpolationTreeNode>();

    queue.addAll(node.children);
    while(!queue.isEmpty()){
      InterpolationTreeNode cNode = queue.pop();
      nodeMap.remove(cNode.key);
      leafs.remove(cNode);
      queue.addAll(cNode.children);
    }

    leafs.add(node);
    node.children.clear();

  }

  /**
   * Returns the length of the branch that has the same id as the root.
   * If there are many branches with the same id, it picks a random one.
   * @return
   */
  public int lengthOfTheTrunk() {
    if (root == null){
      return 0;
    }

    InterpolationTreeNode node = root;
    Integer uid = root.uniqueId;
    int length = 0;
    while (node != null){
      InterpolationTreeNode newNode = null;
      for (InterpolationTreeNode child : node.children){
        if (child.uniqueId.equals(uid)){
          length++;
          newNode = child;
          break;
        }
      }
      node = newNode;
    }

    return length;
  }

  /**
   * Returns a branch that has the same id as the root. We assume that there is only
   * one such branch.
   * @return
   */
  public List<InterpolationTreeNode> getTrunk() {
    List<InterpolationTreeNode> trunk = new Vector<InterpolationTreeNode>();
    InterpolationTreeNode toProcess = root;

    while(toProcess != null){
      trunk.add(toProcess);
      InterpolationTreeNode newNode = null;
      for (InterpolationTreeNode child : toProcess.children){
        if (child.uniqueId.equals(toProcess.uniqueId)){
          newNode = child;
          break;
        }
      }
      toProcess = newNode;
    }

    return trunk;
  }

  /**
   * Performs a bfs search and returns the required number of nodes.
   * @param start
   * @param limit
   * @return
   */
  public Set<InterpolationTreeNode> bfs(InterpolationTreeNode start, int limit) {

    Deque<InterpolationTreeNode> toProcess = new LinkedList<InterpolationTreeNode>();
    Set<InterpolationTreeNode> result = new HashSet<InterpolationTreeNode>(limit);
    if (start != null){
      toProcess.addLast(start);
    }

    int count = 0;
    while (!toProcess.isEmpty() && count < limit){
      InterpolationTreeNode node = toProcess.pop();
      result.add(node);
      count++;

      for (InterpolationTreeNode child : node.children){
        toProcess.addLast(child);
      }
    }

    return result;
  }


  public Integer size() {
    return nodeMap.size();
  }

  public InterpolationTreeNode getRoot() {
    return root;
  }

  public void setRoot(InterpolationTreeNode pRoot) {
    root = pRoot;
  }

  public Set<InterpolationTreeNode> getLeafs() {
    return leafs;
  }

  public Map<InterpolationTreeNodeKey, InterpolationTreeNode> getNodeMap() {
    return nodeMap;
  }








}
