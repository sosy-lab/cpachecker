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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdge;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ARTElement extends AbstractSingleWrapperElement {

  /** Parent ARTElement in the same thread and the edge between them. */
  private final HashMap<ARTElement, CFAEdge> localParentMap;
  /** Child ARTElement in the same thread and the edge between them. */
  private final HashMap<ARTElement, CFAEdge> localChildMap;

  /** Parent ARTElement in another thread that created the env. edge */
  private final HashMap<ARTElement, RGCFAEdge> envParentMap;
  /** Child ARTElement in another thread that was created by the env. edge*/
  private final HashMap<ARTElement, RGCFAEdge> envChildMap;
  /** Thread id. */
  private final int tid;

  private final RGAbstractElement rgElement;

  private ARTElement mCoveredBy = null;
  private Set<ARTElement> mCoveredByThis = null; // lazy initialization because rarely needed

  private boolean mayCover = true;
  private boolean destroyed = false;

  private ARTElement mergedWith = null;

  private final int elementId;

  /**
   * equivalences class of each program counter
   */
  private final ImmutableMap<Integer, Integer> locationClasses;

  private ImmutableList<Pair<ARTElement, RGEnvTransition>> envApplied = ImmutableList.of();

  private static int nextArtElementId = 0;

  public ARTElement(AbstractElement pWrappedElement, Map<ARTElement, CFAEdge> parentEdges, Map<ARTElement, RGCFAEdge> parentEnvEdges, ImmutableMap<Integer, Integer> locCl, int tid) {
    super(pWrappedElement);
    assert parentEdges != null;
    assert !locCl.containsKey(tid);

    this.elementId = ++nextArtElementId;
    this.localParentMap = new LinkedHashMap<ARTElement, CFAEdge>(parentEdges);
    this.localChildMap = new LinkedHashMap<ARTElement, CFAEdge>();
    this.envParentMap = new LinkedHashMap<ARTElement, RGCFAEdge>(parentEnvEdges);
    this.envChildMap = new LinkedHashMap<ARTElement, RGCFAEdge>();
    this.tid = tid;

    // TODO move it somewhere
    // add this as a child
    for (ARTElement parent : parentEdges.keySet()){
      CFAEdge edge = parentEdges.get(parent);
      parent.localChildMap.put(this, edge);
    }

    for (ARTElement parent : this.envParentMap.keySet()){
      RGCFAEdge edge = this.envParentMap.get(parent);
      assert edge != null;
      parent.envChildMap.put(this, edge);
    }

    this.locationClasses = locCl;

    this.rgElement = AbstractElements.extractElementByType(this, RGAbstractElement.class);
    assert this.rgElement != null;
  }

  public ImmutableSet<ARTElement> getLocalParents(){
    return ImmutableSet.copyOf(localParentMap.keySet()) ;
  }

  public ImmutableSet<ARTElement> getLocalChildren() {
    // note for rely-guarantee analysis we may need the children of a destroyed element
    //assert !destroyed;
    return ImmutableSet.copyOf(localChildMap.keySet()) ;
  }


  public Map<ARTElement, CFAEdge> getLocalParentMap() {
    return Collections.unmodifiableMap(localParentMap);
  }

  public Map<ARTElement, CFAEdge> getLocalChildMap() {
    return Collections.unmodifiableMap(localChildMap);
  }

  public Map<ARTElement, RGCFAEdge> getEnvParentMap() {
    return Collections.unmodifiableMap(envParentMap);
  }

  public Map<ARTElement, RGCFAEdge> getEnvChildMap() {
    return Collections.unmodifiableMap(envChildMap);
  }

  public void addLocalParent(ARTElement element, CFAEdge edge) {
    localParentMap.put(element, edge);
    element.localChildMap.put(this, edge);
  }

  public void addLocalChildren(Map<ARTElement, CFAEdge> children) {
    localChildMap.putAll(children);
  }

  public void addEnvParent(ARTElement element, RGCFAEdge edge) {
    envParentMap.put(element, edge);
    element.envChildMap.put(this, edge);
  }

  public void addEnvChildren(Map<ARTElement, RGCFAEdge> children){
    envChildMap.putAll(children);
  }


  public boolean hasLocalChild() {
    for (CFAEdge edge : this.localChildMap.values()){
      if (edge.getEdgeType() != CFAEdgeType.RelyGuaranteeCFAEdge){
        return true;
      }
    }

    return false;
  }

  public ImmutableMap<Integer, Integer> getLocationClasses() {
    return locationClasses;
  }

  public int getTid() {
    return tid;
  }

  protected void (ARTElement pCoveredBy) {
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



  public ImmutableList<Pair<ARTElement, RGEnvTransition>> getEnvApplied() {
    return envApplied;
  }


  public void setEnvApplied(ImmutableList<Pair<ARTElement, RGEnvTransition>> pEnvApplied) {
    envApplied = pEnvApplied;
  }

  public void addEnvApplied(Pair<ARTElement, RGEnvTransition> application) {
    assert application != null;
    envApplied.add(application);
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
    sb.append(", tid: ");
    sb.append(tid);
    sb.append(", Location: "+loc+" "+locationClasses);
    sb.append(", env. applied: "+envAppliedToString());
    if (!destroyed) {
      sb.append(", Local parents: ");
      List<Integer> list = new ArrayList<Integer>();
      for (ARTElement e: localParentMap.keySet()) {
        list.add(e.elementId);
      }
      sb.append(list);

      sb.append(", Env parents: ");
      list.clear();
      for (ARTElement e : envParentMap.keySet()){
        list.add(e.elementId);
      }
      sb.append(list);

      sb.append(", Local children: ");
      list.clear();
      for (ARTElement e: localChildMap.keySet()) {
        list.add(e.elementId);
      }
      sb.append(list);

      sb.append(", Env children: ");
      list.clear();
      for (ARTElement e: envChildMap.keySet()) {
        list.add(e.elementId);
      }
      sb.append(list);
    }
    sb.append(") ");
    sb.append(rgElement);
    return sb.toString();
  }


  private String envAppliedToString(){
    List<Pair<Integer, Integer>> repr = new Vector<Pair<Integer, Integer>>(envApplied.size());

    for (Pair<ARTElement, RGEnvTransition> pair: envApplied){
      int elemId = pair.getFirst().getElementId();
      int envId = pair.getSecond().getSourceARTElement().getElementId();
      repr.add(Pair.of(elemId, envId));
    }

    return repr.toString();
  }

  // TODO check
  public Set<ARTElement> getLocalSubtree() {
    assert !destroyed;
    Set<ARTElement> result = new HashSet<ARTElement>();
    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();

    workList.add(this);

    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (result.add(currentElement)) {
        // currentElement was not in result
        workList.addAll(currentElement.localChildMap.keySet());
      }
    }
    return result;
  }

  public Set<ARTElement> getAllSubtree() {
    assert !destroyed;
    Set<ARTElement> result = new HashSet<ARTElement>();
    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();

    workList.add(this);

    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (result.add(currentElement)) {
        // currentElement was not in result
        workList.addAll(currentElement.localChildMap.keySet());
        workList.addAll(currentElement.envChildMap.keySet());
      }
    }
    return result;
  }


  public RGAbstractElement getRgElement() {
    return rgElement;
  }

  public int getElementId() {
    return elementId;
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
    for (ARTElement child : localChildMap.keySet()) {
      assert child.localParentMap.containsKey(this);
      child.localParentMap.remove(this);
    }
    localChildMap.clear();

    for (ARTElement child : envChildMap.keySet()){
      if (!child.envParentMap.containsKey(this)){
        System.out.println(this.getClass());
      }
      assert child.envParentMap.containsKey(this);
      child.envParentMap.remove(this);
    }
    envChildMap.clear();

    // clear parents
    for (ARTElement parent : localParentMap.keySet()) {
      assert parent.localChildMap.containsKey(this);
      parent.localChildMap.remove(this);
    }
    localParentMap.clear();

    for (ARTElement parent : envParentMap.keySet()) {
      assert parent.envChildMap.containsKey(this);
      parent.envChildMap.remove(this);
    }
    envParentMap.clear();

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



  public CFAEdge getEdgeToChild(ARTElement pChild) {
    Preconditions.checkArgument(localChildMap.containsKey(pChild));

    CFANode currentLoc = this.retrieveLocationElement().getLocationNode();
    CFANode childNode = pChild.retrieveLocationElement().getLocationNode();

    return currentLoc.getEdgeTo(childNode);
  }

  public boolean isDestroyed() {
    return destroyed;
  }

  /**
   * Returns env transitions that created a child of this element.
   * @return
   */
  public Set<RGEnvTransition> getEnvTransitionsApplied() {
    HashSet<RGEnvTransition> envTr = new HashSet<RGEnvTransition>();

    for (CFAEdge edge : this.localChildMap.values()){
      if (edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge){
        RGCFAEdge rgEdge = (RGCFAEdge) edge;
        envTr.add(rgEdge.getRgEnvTransition());
      }
    }

    return envTr;
  }




}
