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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;

import java.util.Map;
import java.util.Set;

public class SMGHveUsePath {

  private final Multimap<PathPosition, SMGHVEOrigin> hveOrigin;
  private final Map<SMGEdgeHasValue, PathPosition> hvePathPosition;

  public SMGHveUsePath(Multimap<PathPosition, SMGHVEOrigin> pHveOrigin,
      Map<SMGEdgeHasValue, PathPosition> pHvePathPosition) {
    super();
    hveOrigin = pHveOrigin;
    hvePathPosition = pHvePathPosition;
  }

  public Multimap<PathPosition, SMGHVEOrigin> getHveOrigin() {
    return hveOrigin;
  }

  public Map<SMGEdgeHasValue, PathPosition> getHvePathPosition() {
    return hvePathPosition;
  }

  public static class SMGHVEOrigin {

    private final SMGEdgeHasValue edge;
    private final Set<SMGEdgeHasValue> originOfEdge;
    public SMGHVEOrigin(SMGEdgeHasValue pEdge, Set<SMGEdgeHasValue> pOriginOfEdge) {
      super();
      edge = pEdge;
      originOfEdge = pOriginOfEdge;
    }

    public SMGEdgeHasValue getEdge() {
      return edge;
    }

    public Set<SMGEdgeHasValue> getOriginOfEdge() {
      return originOfEdge;
    }

    @Override
    public String toString() {
      return "SMGHVEOrigin [edge=" + edge + ", originOfEdge=" + originOfEdge + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((edge == null) ? 0 : edge.hashCode());
      result = prime * result + ((originOfEdge == null) ? 0 : originOfEdge.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SMGHVEOrigin other = (SMGHVEOrigin) obj;
      if (edge == null) {
        if (other.edge != null) {
          return false;
        }
      } else if (!edge.equals(other.edge)) {
        return false;
      }
      if (originOfEdge == null) {
        if (other.originOfEdge != null) {
          return false;
        }
      } else if (!originOfEdge.equals(other.originOfEdge)) {
        return false;
      }
      return true;
    }
  }
}