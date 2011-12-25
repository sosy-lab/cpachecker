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

      if (node.children.size() <=1 || topList.containsAll(node.children)){
        // all children haven been visited
        topList.add(node);

        if (node.parent != null){
          queue.addLast(node.parent);
        }
      } else {
        // find the fist missing child
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
          if (leaf.uniqueId == mChild.uniqueId){
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

  public Integer size() {
    return nodeMap.size();
  }





}
