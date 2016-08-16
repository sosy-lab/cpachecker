/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.HashSet;
import java.util.Set;


public class SMGHasValueEdgeSet extends HashSet<SMGEdgeHasValue> implements SMGHasValueEdges {

  private static final long serialVersionUID = 2898673244970871322L;

  @Override
  public SMGHasValueEdges copy() {
    SMGHasValueEdgeSet copy = new SMGHasValueEdgeSet();
    copy.addAll(this);
    return copy;
  }

  @Override
  public void removeAllEdgesOfObject(SMGObject pObj) {
    Set<SMGEdgeHasValue> toRemove = SMGEdgeHasValueFilter.objectFilter(pObj).filterSet(this);

    for (SMGEdgeHasValue edge : toRemove) {
      remove(edge);
    }
  }

  @Override
  public void addEdge(SMGEdgeHasValue pEdge) {
    add(pEdge);
  }

  @Override
  public void removeEdge(SMGEdgeHasValue pEdge) {
    remove(pEdge);
  }

  @Override
  public void replaceHvEdges(Set<SMGEdgeHasValue> pNewHV) {
    clear();
    addAll(pNewHV);
  }

  @Override
  public Set<SMGEdgeHasValue> getHvEdges() {
    return ImmutableSet.copyOf(this);
  }

  @Override
  public Set<SMGEdgeHasValue> filter(SMGEdgeHasValueFilter pFilter) {
    return pFilter.filterSet(this);
  }
}