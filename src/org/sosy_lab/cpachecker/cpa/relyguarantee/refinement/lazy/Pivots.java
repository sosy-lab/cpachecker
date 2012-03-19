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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.lazy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class Pivots {

  private final Multimap<Integer, Pivot> pivotsPerThread;
  /** ARTElement in the subtrees of pivots */
  private final Set<ARTElement> elemsInSubtrees;

  public Pivots(){
    this.pivotsPerThread  = LinkedHashMultimap.create();
    this.elemsInSubtrees          = new HashSet<ARTElement>();
  }


  public Pivots(Pivots pivs){
    this.pivotsPerThread = LinkedHashMultimap.create(pivs.pivotsPerThread);
    this.elemsInSubtrees         = new HashSet<ARTElement>(pivs.elemsInSubtrees);
  }

  public Multimap<Integer, Pivot> getPivotMap() {
    return pivotsPerThread;
  }

  /*
  public boolean addAll(Multimap<Integer, Pivot> pPivotsPerThread) {
    return this.pivotsPerThread.putAll(pPivotsPerThread);
  }*/

  public void addAll(Pivots pivs) {
    this.pivotsPerThread.putAll(pivs.pivotsPerThread);
    this.elemsInSubtrees.addAll(pivs.elemsInSubtrees);
  }

  public boolean isEmpty() {
    return pivotsPerThread.isEmpty();
  }

  public Set<ARTElement> getElemsInSubtrees() {
    return elemsInSubtrees;
  }

  public  Set<Integer> getTids() {
    return pivotsPerThread.keySet();
  }

  public Collection<Pivot> getPivotsForThread(int tid) {
    return pivotsPerThread.get(tid);
  }

  public boolean containsPivotsWithElement(ARTElement element) {
    return elemsInSubtrees.contains(element);
  }

  public boolean addPivot(Pivot pivot) {
    int tid = pivot.getTid();
    elemsInSubtrees.addAll(pivot.getLocalSubtree());
    return pivotsPerThread.put(tid, pivot);
  }

  @Override
  public String toString(){
    StringBuilder bldr = new StringBuilder();

    for (Integer tid : pivotsPerThread.keySet()){
      bldr.append("Pivots for thread "+tid+"\n");

      for (Pivot piv : pivotsPerThread.get(tid)){
        bldr.append("\t-"+piv+"\n");
      }
    }

    return bldr.toString();
  }



}
