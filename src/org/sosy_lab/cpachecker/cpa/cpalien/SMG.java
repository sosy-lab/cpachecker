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
package org.sosy_lab.cpachecker.cpa.cpalien;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class SMG {
  final private HashSet<SMGObject> objects = new HashSet<>();
  final private HashSet<Integer> values = new HashSet<>();
  final private HashSet<SMGEdgeHasValue> hv_edges = new HashSet<>();
  final private HashSet<SMGEdgePointsTo> pt_edges = new HashSet<>();

  public SMG(){}

  public SMG(SMG pHeap) {
    objects.addAll(pHeap.objects);
    values.addAll(pHeap.values);
    hv_edges.addAll(pHeap.hv_edges);
    pt_edges.addAll(pHeap.pt_edges);
  }

  final public void addObject(SMGObject pObj) {
    this.objects.add(pObj);
  }
  final public void addValue(int pValue) {
    this.values.add(Integer.valueOf(pValue));
  }
  final public void addPointsToEdge(SMGEdgePointsTo pEdge){
    this.pt_edges.add(pEdge);
  }
  final public void addHasValueEdge(SMGEdgeHasValue pNewEdge) {
    this.hv_edges.add(pNewEdge);
  }

  final public String valuesToString(){
    return "values=" + values.toString();
  }

  final public String hvToString(){
    return "hasValue=" + hv_edges.toString();
  }

  final public String ptToString(){
    return "pointsTo=" + pt_edges.toString();
  }

  final public Set<Integer> getValues(){
    return Collections.unmodifiableSet(values);
  }

  final public Set<SMGObject> getObjects(){
    return Collections.unmodifiableSet(objects);
  }

  final public Set<SMGEdgeHasValue> getHVEdges(){
    return Collections.unmodifiableSet(hv_edges);
  }

  final public Set<SMGEdgePointsTo> getPTEdges(){
    return Collections.unmodifiableSet(pt_edges);
  }
}