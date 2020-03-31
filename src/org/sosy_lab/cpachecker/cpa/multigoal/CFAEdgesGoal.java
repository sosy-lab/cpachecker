/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.multigoal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Property;

public class CFAEdgesGoal implements Property {

  private PartialPath path;
  private Set<PartialPath> negatedPaths;


  public CFAEdgesGoal(List<CFAEdge> pEdges) {
    path = new PartialPath(pEdges);
  }

  // returns true, if a change of state happened due to processing edge
  public boolean acceptsEdge(CFAEdge edge, int index) {
    return path.acceptsEdge(edge, index);
  }

  public void addNegatedPath(List<CFAEdge> pEdges) {
    if (negatedPaths == null) {
      negatedPaths = new HashSet<>();
    }
    negatedPaths.add(new PartialPath(pEdges));
  }

  public Set<PartialPath> getNegatedPaths() {
    if (negatedPaths == null) {
      return Collections.emptySet();
    }
    return negatedPaths;
  }

  public PartialPath getPath() {
    return path;
  }

  public boolean coveredByPath(List<CFAEdge> pPath) {
    return path.isCoveredBy(pPath);
  }

  public boolean containsNegatedEdge(CFAEdge pCfaEdge) {
    if (negatedPaths != null) {
      for (PartialPath negPath : negatedPaths) {
        if (negPath.contains(pCfaEdge)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Edges:\n");
    builder.append(path.toString());
    builder.append("\n");

    if (negatedPaths != null) {
      builder.append("Negated Paths:\n");
      for (PartialPath negPath : negatedPaths) {
        builder.append(negPath.toString());
        builder.append("\n");
      }
    }
    return builder.toString();
  }

}
