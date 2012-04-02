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

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGLocationClass;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ARTMergeJoin implements MergeOperator {

  private final MergeOperator wrappedMerge;

  public ARTMergeJoin(MergeOperator pWrappedMerge)  {
    wrappedMerge = pWrappedMerge;
  }

  @Override
  public AbstractElement merge(AbstractElement pElement1, AbstractElement pElement2, Precision pPrecision) throws CPAException {

    ARTElement artElement1 = (ARTElement)pElement1;
    ARTElement artElement2 = (ARTElement)pElement2;
    ARTPrecision precision = (ARTPrecision) pPrecision;

    // covered elements are not in the reached set
    assert !artElement1.isCovered();
    assert !artElement2.isCovered();

    // can't merge element from different threads
    assert artElement1.getTid() == artElement2.getTid();

    if (!artElement2.mayCover()) {
      // elements that may not cover should also not be used for merge
      return pElement2;
    }

    // merged elements that have matching location classes
     ImmutableMap<Integer, RGLocationClass> locations1 = artElement1.getLocationClasses();
     ImmutableMap<Integer, RGLocationClass> locations2 = artElement2.getLocationClasses();

    if (!locations1.equals(locations2)){
      return pElement2;
    }


    // merge only if elements have the same application points
    ImmutableSet<ARTElement> eapp1 = artElement1.getEnvAppliedPoints();
    ImmutableSet<ARTElement> eapp2 = artElement2.getEnvAppliedPoints();
    if (!eapp1.equals(eapp2)){
      return pElement2;
    }

    AbstractElement wrappedElement1 = artElement1.getWrappedElement();
    AbstractElement wrapppedElement2 = artElement2.getWrappedElement();
    CompositePrecision wP = precision.getWrappedPrecision();
    AbstractElement retElement = wrappedMerge.merge(wrappedElement1, wrapppedElement2, wP);
    if(retElement.equals(wrapppedElement2)){
      return pElement2;
    }

    ARTElement mergedElement = new ARTElement(retElement,
        Collections.<ARTElement, CFAEdge> emptyMap(),
        Collections.<ARTElement, RGEnvTransition>emptyMap(),
        locations1,
        artElement1.getTid());

    // add local and environmental parents
    for ( Entry<ARTElement, CFAEdge> entry : artElement1.getLocalParentMap().entrySet()){
      mergedElement.addLocalParent(entry.getKey(), entry.getValue());
    }

    for ( Entry<ARTElement, CFAEdge> entry : artElement2.getLocalParentMap().entrySet()){
      mergedElement.addLocalParent(entry.getKey(), entry.getValue());
    }

    for ( Entry<ARTElement, RGEnvTransition> entry : artElement1.getEnvParentMap().entrySet()){
      mergedElement.addEnvParent(entry.getKey(), entry.getValue());
    }

    for ( Entry<ARTElement, RGEnvTransition> entry : artElement2.getEnvParentMap().entrySet()){
      mergedElement.addEnvParent(entry.getKey(), entry.getValue());
    }

    // artElement1 is the current successor, it does not have any children yet
    assert artElement1.getLocalChildren().isEmpty();
    assert artElement1.getEnvChildMap().isEmpty();

    // add children
    // TODO check if correct - can get a loop in ARTby the two lines below
    for (Entry<ARTElement, CFAEdge> entry : artElement2.getLocalChildMap().entrySet()){
      entry.getKey().addLocalParent(mergedElement, entry.getValue());
    }

    for (Entry<ARTElement, RGEnvTransition> entry : artElement2.getEnvChildMap().entrySet()){
      entry.getKey().addEnvParent(mergedElement, entry.getValue());
    }

    // artElement1 will only be removed from ART if stop(e1, reached) returns true
    artElement2.removeFromART();

    artElement1.setMergedWith(mergedElement);

    // in rely-guarantee analysis, we need this info
    artElement2.setMergedWith(mergedElement);

    // set the number of env. applications
    mergedElement.setDistanceFromRoot(Math.max(artElement1.getDistanceFromRoot(), artElement2.getDistanceFromRoot()));
    mergedElement.setEnvApplied(artElement1.getEnvApplied());
    mergedElement.localChildrenNotComputed();

    return mergedElement;
  }


  private void checkParentChildCheck(ARTElement elem){

    Map<ARTElement, CFAEdge> pMap = elem.getLocalParentMap();
    for (ARTElement parent : pMap.keySet()){
      assert !parent.isDestroyed();
      CFAEdge edge = pMap.get(parent);
      CFAEdge pEdge = parent.getLocalChildMap().get(elem);
      assert pEdge != null && edge != null && pEdge.equals(edge);
    }

    Map<ARTElement, CFAEdge> cMap = elem.getLocalChildMap();
    for (ARTElement child : cMap.keySet()){
      assert !child.isDestroyed();
      CFAEdge edge = pMap.get(child);
      CFAEdge cEdge = child.getLocalParentMap().get(elem);
      assert cEdge != null && edge != null && cEdge.equals(edge);
    }

  }
}
