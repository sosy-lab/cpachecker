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

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.Map;
import java.util.Set;

public interface SMGPointsToEdges {

  public SMGPointsToEdges copy();

  public void add(SMGEdgePointsTo pEdge);

  public void remove(SMGEdgePointsTo pEdge);

  public void removeAllEdgesOfObject(SMGObject pObj);

  public void removeEdgeWithValue(int pValue);

  public Map<Integer, SMGEdgePointsTo> asMap();

  public boolean containsEdgeWithValue(Integer pValue);

  public SMGEdgePointsTo getEdgeWithValue(Integer pValue);

  public void clear();

  public Set<SMGEdgePointsTo> filter(SMGEdgePointsToFilter pFilter);

  public Set<SMGEdgePointsTo> asSet();

}
