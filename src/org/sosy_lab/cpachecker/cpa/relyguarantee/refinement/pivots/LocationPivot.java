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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.pivots;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.Path;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * A pivot for location refinement.
 */
public class LocationPivot implements Pivot {

  private final ARTElement element;
  private final SetMultimap<Path, Pair<CFANode, CFANode>> mismatchesPerPath;

  public LocationPivot(ARTElement element){
    this.element = element;
    this.mismatchesPerPath = LinkedHashMultimap.create();
  }

  @Override
  public ARTElement getElement() {
    return element;
  }

  @Override
  public int getTid() {
    return element.getTid();
  }


  @Override
  public void addPrecisionOf(Pivot pivot){
    Preconditions.checkArgument(pivot instanceof LocationPivot);
    LocationPivot locPiv = (LocationPivot) pivot;

    mismatchesPerPath.putAll(locPiv.mismatchesPerPath);
  }

  public boolean addMismatchesPerPath(ImmutableSetMultimap<Path, Pair<CFANode, CFANode>> map) {
    return mismatchesPerPath.putAll(map);
  }

  public boolean addMismatchPerPath(Path pPi, Pair<CFANode, CFANode> pMismatch) {
    return mismatchesPerPath.put(pPi, pMismatch);
  }

  public SetMultimap<Path, Pair<CFANode, CFANode>> getMismatchesPerPath() {
    return mismatchesPerPath;
  }

  @Override
  public String toString(){
    String str = "LocationPivot element id: "+element.getElementId()+", mismatches: "+mismatchesPerPath.values();
    return str;
  }

  @Override
  public boolean equals(Object ob){
    if (ob instanceof LocationPivot){
      LocationPivot locpiv = (LocationPivot) ob;
      return locpiv.element.equals(element) &&
          locpiv.mismatchesPerPath.equals(mismatchesPerPath);
    }

    return false;
  }

  @Override
  public int hashCode(){
    return mismatchesPerPath.hashCode() + 13 * element.hashCode();
  }
}
