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
package org.sosy_lab.cpachecker.cpa.art;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

public class ARTElement extends AbstractSingleWrapperElement implements Comparable<ARTElement> {

  private static final long serialVersionUID = 2608287648397165040L;
  private final Set<ARTElement> children;
  private final Set<ARTElement> parents; // more than one parent if joining elements

  private ARTElement mCoveredBy = null;
  private Set<ARTElement> mCoveredByThis = null; // lazy initialization because rarely needed

  private boolean mayCover = true;
  private boolean destroyed = false;

  private ARTElement mergedWith = null;

  private final int elementId;

  private static int nextArtElementId = 0;

  public ARTElement(AbstractElement pWrappedElement, ARTElement pParentElement) {
    super(pWrappedElement);
    elementId = ++nextArtElementId;
    parents = new LinkedHashSet<ARTElement>();
    if(pParentElement != null){
      addParent(pParentElement);
    }
    children = new LinkedHashSet<ARTElement>();
  }

  public Set<ARTElement> getParents(){
    return parents;
  }

  public void addParent(ARTElement pOtherParent){
    assert !destroyed : "Don't use destroyed ARTElements!";
    if(parents.add(pOtherParent)){
      pOtherParent.children.add(this);
    }
  }

  public Set<ARTElement> getChildren() {
    assert !destroyed : "Don't use destroyed ARTElements!";
    return children;
  }

  public void setCovered(ARTElement pCoveredBy) {
    assert !isCovered();
    assert pCoveredBy != null;
    assert pCoveredBy.mayCover;

    mCoveredBy = pCoveredBy;
    if (pCoveredBy.mCoveredByThis == null) {
      // lazy initialization because rarely needed
      pCoveredBy.mCoveredByThis = new HashSet<ARTElement>(2);
    }
    pCoveredBy.mCoveredByThis.add(this);
  }

  /**
   * Uncover all nodes covered by this node.
   * @return a list of all nodes that were previously covered by this node
   */
  public Set<ARTElement> clearCoverage() {
    if (mCoveredByThis == null || mCoveredByThis.isEmpty()) {
      return Collections.emptySet();
    }
    assert !isCovered();

    Set<ARTElement> result = mCoveredByThis;
    mCoveredByThis = null;

    for (ARTElement element : result) {
      assert element.mCoveredBy == this;
      element.mCoveredBy = null; // uncover
    }

    return result;
  }

  public boolean isCovered() {
    assert !destroyed : "Don't use destroyed ARTElements!";
    return mCoveredBy != null;
  }

  public ARTElement getCoveringElement() {
    Preconditions.checkState(isCovered());
    return mCoveredBy;
  }

  public Set<ARTElement> getCoveredByThis() {
    assert !destroyed : "Don't use destroyed ARTElements!";
    if (mCoveredByThis == null) {
      return Collections.emptySet();
    } else {
      return Collections.unmodifiableSet(mCoveredByThis);
    }
  }

  protected void setMergedWith(ARTElement pMergedWith) {
    assert !destroyed : "Don't use destroyed ARTElements!";
    assert mergedWith == null;

    mergedWith = pMergedWith;
  }

  protected ARTElement getMergedWith() {
    return mergedWith;
  }

  boolean mayCover() {
    return mayCover;
  }

  public void setNotCovering() {
    assert !destroyed : "Don't use destroyed ARTElements!";
    mayCover = false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (destroyed) {
      sb.append("Destroyed ");
    }
    sb.append("ART Element (Id: ");
    sb.append(elementId);
    if (!destroyed) {
      sb.append(", Parents: ");
      List<Integer> list = new ArrayList<Integer>();
      for (ARTElement e: parents) {
        list.add(e.elementId);
      }
      sb.append(list);
      sb.append(", Children: ");
      list.clear();
      for (ARTElement e: children) {
        list.add(e.elementId);
      }
      sb.append(list);
    }
    sb.append(") ");
    sb.append(getWrappedElement());
    return sb.toString();
  }

  // TODO check
  public Set<ARTElement> getSubtree() {
    assert !destroyed : "Don't use destroyed ARTElements!";
    Set<ARTElement> result = new HashSet<ARTElement>();
    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();

    workList.add(this);

    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (result.add(currentElement)) {
        // currentElement was not in result
        workList.addAll(currentElement.children);
      }
    }
    return result;
  }

  /**
   * This method removes this element from the ART by removing it from its
   * parents' children list and from its children's parents list.
   *
   * This method also removes the element from the covered set of the other
   * element covering this element, if it is covered.
   *
   * This means, if its children do not have any other parents, they will be not
   * reachable any more, i.e. they do not belong to the ART any more. But those
   * elements will not be removed from the covered set.
   */
  public void removeFromART() {
    assert !destroyed : "Don't use destroyed ARTElements!";

    // clear children
    for (ARTElement child : children) {
      assert (child.parents.contains(this));
      child.parents.remove(this);
    }
    children.clear();

    // clear parents
    for (ARTElement parent : parents) {
      assert (parent.children.contains(this));
      parent.children.remove(this);
    }
    parents.clear();

    // clear coverage relation
    if (isCovered()) {
      assert mCoveredBy.mCoveredByThis.contains(this);

      mCoveredBy.mCoveredByThis.remove(this);
      mCoveredBy = null;
    }

    if (mCoveredByThis != null) {
      for (ARTElement covered : mCoveredByThis) {
        covered.mCoveredBy = null;
      }
      mCoveredByThis.clear();
      mCoveredByThis = null;
    }

    destroyed = true;
  }

  /**
   * This method does basically the same as removeFromART for this element, but
   * before destroying it, it will copy all relationships to other elements to
   * a new element. I.e., the replacement element will receive all parents and
   * children of this element, and it will also cover all elements which are
   * currently covered by this element.
   *
   * @param replacement
   */
  protected void replaceInARTWith(ARTElement replacement) {
    assert !destroyed : "Don't use destroyed ARTElements!";
    assert !replacement.destroyed : "Don't use destroyed ARTElements!";
    assert !isCovered();
    assert !replacement.isCovered();

    // copy children
    for (ARTElement child : children) {
      assert (child.parents.contains(this));
      child.parents.remove(this);
      child.addParent(replacement);
    }
    children.clear();

    for (ARTElement parent : parents) {
      assert (parent.children.contains(this));
      parent.children.remove(this);
      replacement.addParent(parent);
    }
    parents.clear();

    if (mCoveredByThis != null) {
      if (replacement.mCoveredByThis == null) {
        // lazy initialization because rarely needed
        replacement.mCoveredByThis = new HashSet<ARTElement>(mCoveredByThis.size());
      }

      for (ARTElement covered : mCoveredByThis) {
        assert covered.mCoveredBy == this;
        covered.mCoveredBy = replacement;
        replacement.mCoveredByThis.add(covered);
      }

      mCoveredByThis.clear();
      mCoveredByThis = null;
    }

    destroyed = true;
  }

  public int getElementId() {
    return elementId;
  }

  public CFAEdge getEdgeToChild(ARTElement pChild) {
    Preconditions.checkArgument(children.contains(pChild));

    CFANode currentLoc = extractLocation(this);
    CFANode childNode = extractLocation(pChild);

    return currentLoc.getEdgeTo(childNode);
  }

  public boolean isDestroyed() {
    return destroyed;
  }

  /**
   * The ordering of this class is the chronological creation order.
   *
   * Note: Although equals() is not overwritten, this ordering is consistent
   * with equals() as the elementId field is unique.
   */
  @Override
  public int compareTo(ARTElement pO) {
    return Ints.compare(this.elementId, pO.elementId);
  }

  public boolean isOlderThan(ARTElement other) {
    return (elementId < other.elementId);
  }
}
