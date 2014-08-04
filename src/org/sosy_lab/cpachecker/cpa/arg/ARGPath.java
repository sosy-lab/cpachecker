/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * ARGPath contains a path through the ARG that starts at the root node.
 * It is implemented as a list of pairs of an ARGState and a CFAEdge,
 * where the edge of a pair is the outgoing edge of the element.
 * The first pair contains the root node of the ARG.
 */
public class ARGPath extends LinkedList<Pair<ARGState, CFAEdge>> implements Appender {

  private static final long serialVersionUID = -3223480082103314555L;

  @Override
  public void appendTo(Appendable appendable) throws IOException {
    Joiner.on('\n').skipNulls().appendTo(appendable, asEdgesList());
  }

  @Override
  public String toString() {
    return Joiner.on('\n').skipNulls().join(asEdgesList());
  }

  public void toJSON(Appendable sb) throws IOException {
    List<Map<?, ?>> path = new ArrayList<>(this.size());
    for (Pair<ARGState, CFAEdge> pair : this) {
      Map<String, Object> elem = new HashMap<>();
      ARGState argelem = pair.getFirst();
      CFAEdge edge = pair.getSecond();
      if (edge == null) {
        continue; // in this case we do not need the edge
      }
      elem.put("argelem", argelem.getStateId());
      elem.put("source", edge.getPredecessor().getNodeNumber());
      elem.put("target", edge.getSuccessor().getNodeNumber());
      elem.put("desc", edge.getDescription().replaceAll("\n", " "));
      elem.put("line", edge.getFileLocation().getStartingLineNumber());
      elem.put("file", edge.getFileLocation().getFileName());
      path.add(elem);
    }
    JSON.writeJSONString(path, sb);
  }

  public List<CFAEdge> asEdgesList() {
    return Lists.transform(this, Pair.<CFAEdge>getProjectionToSecond());
  }

  public ImmutableSet<ARGState> getStateSet() {
    List<ARGState> elementList = Lists.transform(this, Pair.<ARGState>getProjectionToFirst());
    return ImmutableSet.copyOf(elementList);
  }
}
