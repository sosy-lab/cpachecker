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
package org.sosy_lab.cpachecker.cpa.art;

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
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeCFAEdge;

import com.google.common.base.Preconditions;

public class ARTElement extends AbstractSingleWrapperElement {

  private final Set<ARTElement> children;
  private final Set<ARTElement> parents; // more than one parent if joining elements

  // TODO for rely-guarantee
  private List<RelyGuaranteeCFAEdge> envEdgesToBeApplied;

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
    envEdgesToBeApplied = null;
  }

  public Set<ARTElement> getParents(){
    return parents;
  }

  public void addParent(ARTElement pOtherParent){
    assert !destroyed;
    if(parents.add(pOtherParent)){
      pOtherParent.children.add(this);
    }
  }

  public Set<ARTElement> getChildren() {
    assert !destroyed;
    return children;
  }

  protected void setCovered(ARTElement pCoveredBy) {
    assert pCoveredBy != null;
    assert pCoveredBy.mayCover;

    mCoveredBy = pCoveredBy;
    if (pCoveredBy.mCoveredByThis == null) {
      // lazy initialization because rarely needed
      pCoveredBy.mCoveredByThis = new HashSet<ARTElement>(2);
    }
    pCoveredBy.mCoveredByThis.add(this);
  }

  public boolean isCovered() {
    assert !destroyed;
    return mCoveredBy != null;
  }

  public ARTElement getCoveringElement() {
    Preconditions.checkState(isCovered());
    return mCoveredBy;
  }

  public Set<ARTElement> getCoveredByThis() {
    assert !destroyed;
    if (mCoveredByThis == null) {
      return Collections.emptySet();
    } else {
      return Collections.unmodifiableSet(mCoveredByThis);
    }
  }

  protected void setMergedWith(ARTElement pMergedWith) {
    assert !destroyed;
    assert mergedWith == null;

    mergedWith = pMergedWith;
  }

  public ARTElement getMergedWith() {
    return mergedWith;
  }

  boolean mayCover() {
    return mayCover;
  }

  public void setNotCovering() {
    assert !isCovered();
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
    assert !destroyed;
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
    assert !destroyed;

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

  public int getElementId() {
    return elementId;
  }

  public CFAEdge getEdgeToChild(ARTElement pChild) {
    Preconditions.checkArgument(children.contains(pChild));

    CFANode currentLoc = this.retrieveLocationElement().getLocationNode();
    CFANode childNode = pChild.retrieveLocationElement().getLocationNode();

    return currentLoc.getEdgeTo(childNode);
  }

  public boolean isDestroyed() {
    return destroyed;
  }

  public List<RelyGuaranteeCFAEdge> getEnvEdgesToBeApplied() {
    return envEdgesToBeApplied;
  }

  public void setEnvEdgesToBeApplied(List<RelyGuaranteeCFAEdge> pEnvEdgesToBeApplied) {
    envEdgesToBeApplied = pEnvEdgesToBeApplied;
  }

}
