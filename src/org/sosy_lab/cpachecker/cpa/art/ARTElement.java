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
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ARTElement extends AbstractSingleWrapperElement {

  //private final Set<ARTElement> children;
  //private final Set<ARTElement> parents; // more than one parent if joining elements

  private final HashMap<ARTElement, CFAEdge> parentMap;
  private final HashMap<ARTElement, CFAEdge> childMap;

  private final RGAbstractElement rgElement;

  private ARTElement mCoveredBy = null;
  private Set<ARTElement> mCoveredByThis = null; // lazy initialization because rarely needed

  private boolean mayCover = true;
  private boolean destroyed = false;

  private ARTElement mergedWith = null;

  private final int elementId;

  /**
   * Has a local child been computed.
   */
  private boolean hasLocalChild = false;

  /**
   * Env transition that have been applied to this element. Null means none.
   */
  private Set<RGEnvTransition> envTrApplied;

  /**
   * equivalences class of each program counter
   */
  private final ImmutableList<Integer> locationClasses;

  /**
   * maximum number of env. application on the path to this element.
   */
  private int envAppBefore = 0;

  private static int nextArtElementId = 0;

  public ARTElement(AbstractElement pWrappedElement, Map<ARTElement, CFAEdge> parentEdges, ImmutableList<Integer> locationClasses) {
    super(pWrappedElement);
    assert parentEdges != null;

    this.elementId = ++nextArtElementId;
    this.parentMap = new LinkedHashMap<ARTElement, CFAEdge>(parentEdges);
    this.childMap = new LinkedHashMap<ARTElement, CFAEdge>();
    // add this as a child
    for (ARTElement parent : parentEdges.keySet()){
      CFAEdge edge = parentEdges.get(parent);
      parent.childMap.put(this, edge);
    }

    this.locationClasses = locationClasses;
    this.envTrApplied = null;

    this.rgElement = AbstractElements.extractElementByType(this, RGAbstractElement.class);
    assert this.rgElement != null;
  }

  public ImmutableSet<ARTElement> getParentARTs(){
    return ImmutableSet.copyOf(parentMap.keySet()) ;
  }

  public ImmutableSet<ARTElement> getChildARTs() {
    // note for rely-guarantee analysis we may need the children of a destroyed element
    //assert !destroyed;
    return ImmutableSet.copyOf(childMap.keySet()) ;
  }

  public Collection<CFAEdge> getParentEdges(){
    return parentMap.values();
  }

  public Collection<CFAEdge> getChildrenEdges(){
    return parentMap.values();
  }

  public ImmutableMap<ARTElement, CFAEdge> getParentMap() {
    return ImmutableMap.copyOf(parentMap);
  }

  public ImmutableMap<ARTElement, CFAEdge> getChildMap() {
    return ImmutableMap.copyOf(childMap);
  }

  public void addParents(Map<ARTElement, CFAEdge> parents) {
    parentMap.putAll(parents);
  }

  public void addChildren(Map<ARTElement, CFAEdge> children) {
    childMap.putAll(children);
  }

  public void addParent(ARTElement element, CFAEdge edge) {
    parentMap.put(element, edge);
    element.childMap.put(this, edge);
  }


  public boolean hasLocalChild() {
    return hasLocalChild;
  }

  public void setHasLocalChild(boolean pHasLocalChild) {
    hasLocalChild = pHasLocalChild;
  }

  public ImmutableList<Integer> getLocationClasses() {
    return locationClasses;
  }



  public Set<RGEnvTransition> getEnvTrApplied() {
    return envTrApplied;
  }

  public void setEnvTrApplied(Set<RGEnvTransition> pEnvTrApplied) {
    envTrApplied = pEnvTrApplied;
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
    //assert !destroyed;
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

    CFANode loc = retrieveLocationElement().getLocationNode();

    if (destroyed) {
      sb.append("Destroyed ");
    }
    sb.append("ART Element (Id: ");
    sb.append(elementId);
    sb.append(", Location: "+loc+" "+locationClasses);
    if (!destroyed) {
      sb.append(", Parents: ");
      List<Integer> list = new ArrayList<Integer>();
      for (ARTElement e: parentMap.keySet()) {
        list.add(e.elementId);
      }
      sb.append(list);
      sb.append(", Children: ");
      list.clear();
      for (ARTElement e: childMap.keySet()) {
        list.add(e.elementId);
      }
      sb.append(list);
    }
    sb.append(") ");
    sb.append(rgElement);
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
        workList.addAll(currentElement.childMap.keySet());
      }
    }
    return result;
  }


  public int getEnvAppBefore() {
    return envAppBefore;
  }

  public void setEnvAppBefore(int pEnvAppBefore) {
    envAppBefore = pEnvAppBefore;
  }

  public RGAbstractElement getRgElement() {
    return rgElement;
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
    for (ARTElement child : childMap.keySet()) {
      if (!child.parentMap.containsKey(this)){
        System.out.println(this.getClass().toString());
      }
      assert (child.parentMap.containsKey(this));
      child.parentMap.remove(this);
    }
    childMap.clear();

    // clear parents
    for (ARTElement parent : parentMap.keySet()) {
      if (!parent.childMap.containsKey(this)){
        System.out.println(this.getClass().toString());
      }
      assert (parent.childMap.containsKey(this));
      parent.childMap.remove(this);
    }
    parentMap.clear();

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
    Preconditions.checkArgument(childMap.containsKey(pChild));

    CFANode currentLoc = this.retrieveLocationElement().getLocationNode();
    CFANode childNode = pChild.retrieveLocationElement().getLocationNode();

    return currentLoc.getEdgeTo(childNode);
  }

  public boolean isDestroyed() {
    return destroyed;
  }


}
