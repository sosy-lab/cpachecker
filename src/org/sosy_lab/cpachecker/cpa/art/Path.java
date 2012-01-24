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
package org.sosy_lab.cpachecker.cpa.art;

import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Path contains a path through the ART that starts at the root node.
 * It is implemented as a list of pairs of an ARTElement and a CFAEdge,
 * where the edge of a pair is the outgoing edge of the element.
 * The first pair contains the root node of the ART.
 */
public class Path extends LinkedList<Pair<ARTElement, CFAEdge>> {

  private static final long serialVersionUID = -3223480082103314555L;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (CFAEdge edge : asEdgesList()) {
      sb.append("Line ");
      sb.append(edge.getLineNumber());
      sb.append(": ");
      sb.append(edge);
      sb.append("\n");
    }

    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  public JSONArray toJSON() {
    JSONArray path = new JSONArray();
    for (Pair<ARTElement, CFAEdge> pair : this) {
      JSONObject elem = new JSONObject();
      ARTElement artelem = pair.getFirst();
      CFAEdge edge = pair.getSecond();
      elem.put("artelem", artelem.getElementId());
      elem.put("source", edge.getPredecessor().getNodeNumber());
      elem.put("target", edge.getSuccessor().getNodeNumber());
      elem.put("desc", edge.getRawStatement().replaceAll("\n", " "));
      elem.put("line", edge.getLineNumber());
      path.add(elem);
    }
    return path;
  }

  public List<CFAEdge> asEdgesList() {
    return Lists.transform(this, Pair.<CFAEdge>getProjectionToSecond());
  }

  public ImmutableSet<ARTElement> getElementSet() {
    List<ARTElement> elementList = Lists.transform(this, Pair.<ARTElement>getProjectionToFirst());
    return ImmutableSet.copyOf(elementList);
  }
}
