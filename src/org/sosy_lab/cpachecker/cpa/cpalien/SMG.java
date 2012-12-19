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

import java.util.HashSet;
import java.util.Set;


public class SMG {
  protected HashSet<SMGObject> objects;
  protected Set<Integer> values;
  protected Set<SMGEdgeHasValue> hv_edges;
  protected Set<SMGEdgePointsTo> pt_edges;

  public SMG(){
    objects = new HashSet<>();
    values = new HashSet<>();
    hv_edges = new HashSet<>();
    pt_edges = new HashSet<>();
  }

  public SMG(SMG pHeap) {
    objects = new HashSet<>();
    values = new HashSet<>();
    hv_edges = new HashSet<>();
    pt_edges = new HashSet<>();

    objects.addAll(pHeap.objects);
    values.addAll(pHeap.values);
    hv_edges.addAll(pHeap.hv_edges);
    pt_edges.addAll(pHeap.pt_edges);
  }

  public void addObject(SMGObject pObj) {
    this.objects.add(pObj);
  }
  public void addValue(Integer pValue) {
    this.values.add(pValue);
  }
  public void addPointsToEdge(SMGEdgePointsTo pEdge){
    this.pt_edges.add(pEdge);
  }
  public void addHasValueEdge(SMGEdgeHasValue pNewEdge) {
    this.hv_edges.add(pNewEdge);
  }
}