/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.blocking.container;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemTree<N, L> {
  private ItemTree<N,L> parentTree = null;
  private Set<L> leafs = new HashSet<L>();
  private Map<N, ItemTree<N, L>> childTrees = new HashMap<N, ItemTree<N, L>>();

  public ItemTree() {
  }

  private ItemTree (ItemTree<N,L> pParentTree) {
    this.parentTree = pParentTree;
  }

  private boolean containsLeaf(N[] pStackLevel, int pStackOffset, L pLeaf) {
    if (pStackLevel.length == pStackOffset) {
      return leafs.contains(pLeaf);
    } else if (childTrees.containsKey(pStackLevel[pStackOffset])) {
      return childTrees.get(pStackLevel[pStackOffset]).containsLeaf(pStackLevel, pStackOffset + 1, pLeaf);
    } else {
      return false;
    }
  }

  public boolean containsLeaf(N[] pStackLevel, L pLeaf) {
    return containsLeaf(pStackLevel, 0, pLeaf);
  }

  public ItemTree<N, L> put(N[] pOnLevel) {
    ItemTree<N, L> result = this;
    for (N l: pOnLevel) {
      result = result.put(l);
    }
    return result;
  }

  public void addOnRoot(L pLeaf) {
    if (this.parentTree == null) {
      this.leafs.add(pLeaf);
    } else {
      this.parentTree.addOnRoot(pLeaf);
    }
  }

  public ItemTree<N, L> put(N pChildLevelObject) {
    ItemTree<N, L> result = childTrees.get(pChildLevelObject);
    if (result == null) {
      result = new ItemTree<N, L>(this);
      childTrees.put(pChildLevelObject, result);
    }
    return result;
  }

  public void addLeaf(L pLeaf) {
    addLeaf(pLeaf, false);
  }

  public void addLeaf(L pLeaf, boolean pAddToRootToo) {
    this.leafs.add(pLeaf);
    if (pAddToRootToo) {
      addOnRoot(pLeaf);
    }
  }

  public int getNumberOfLeafs(boolean pIncludeNodesOnRoot) {
    int result = (!pIncludeNodesOnRoot && this.parentTree == null) ? 0 : this.leafs.size();
    for (ItemTree<N,L> child: this.childTrees.values()) {
      result += child.getNumberOfLeafs(pIncludeNodesOnRoot);
    }
    return result;
  }

  public void print() {
    for (N childKey: this.childTrees.keySet()) {
      System.out.print(childKey + ":");
      for (L leave : this.leafs) {
        System.out.println(leave);
      }
      childTrees.get(childKey).print();
    }
  }

}
