/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2009  Dirk Beyer and Erkan Keremoglu.
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
package programtesting;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Andreas Holzer <holzer@forsyte.de>
 *
 */
public class ParametricAbstractReachabilityTree<TreeElement> {

  private TreeElement mRoot;
  private Map<TreeElement, Collection<TreeElement>> mChildren;

  public ParametricAbstractReachabilityTree() {
    mRoot = null;
    mChildren = new HashMap<TreeElement, Collection<TreeElement>>();
  }

  public void clear() {
    mChildren.clear();
    mRoot = null;
  }

  public void setRoot(TreeElement pRoot) {
    assert (pRoot != null);
    assert (mRoot == null);

    mRoot = pRoot;

    createEntry(mRoot);
  }

  public TreeElement getRoot() {
    assert (mRoot != null);

    return mRoot;
  }

  public boolean hasRoot() {
    return (mRoot != null);
  }

  private void createEntry(TreeElement pElement) {
    assert (pElement != null);
    assert (!contains(pElement));

    mChildren.put(pElement, new HashSet<TreeElement>());
  }

  public void add(TreeElement pParent, TreeElement pChild) {
    assert (pParent != null);
    assert (pChild != null);

    // pChild has to be a new element in the tree
    assert (!contains(pChild));
    // pParent has to be an element in the tree
    assert (contains(pParent));

    Collection<TreeElement> lParentEntry = getChildren(pParent);
    lParentEntry.add(pChild);

    createEntry(pChild);
  }

  public boolean contains(TreeElement pElement) {
    assert (pElement != null);

    return mChildren.containsKey(pElement);
  }

  public Collection<TreeElement> getChildren(TreeElement pElement) {
    assert (pElement != null);
    assert (contains(pElement));

    return mChildren.get(pElement);
  }
}
