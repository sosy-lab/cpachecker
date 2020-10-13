/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.defaults.AnyCFAEdge;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;

public class LocationStateWithEdge extends LocationState implements AbstractStateWithEdge {

  private static final long serialVersionUID = 2798558767388783223L;

  private AbstractEdge edge;
  private final static CFANode dummy =
      new CFANode(
          new CFunctionDeclaration(
              FileLocation.DUMMY,
              CFunctionType.NO_ARGS_VOID_FUNCTION,
              "Projection",
              ImmutableList.of()));

  public LocationStateWithEdge(
      CFANode pLocationNode,
      boolean pFollowFunctionCalls,
      AbstractEdge pEdge) {
    super(pLocationNode, pFollowFunctionCalls);
    edge = pEdge;
  }

  static class ProjectedLocationStateWithEdge extends LocationStateWithEdge {

    private static final long serialVersionUID = 6825257572921009531L;
    private final static ProjectedLocationStateWithEdge instance =
        new ProjectedLocationStateWithEdge();

    private ProjectedLocationStateWithEdge() {
      super(dummy, true, AnyCFAEdge.getInstance());
    }

    @Override
    public Iterable<CFAEdge> getOutgoingEdges() {
      return ImmutableSet.of();
    }

    @Override
    public Iterable<CFAEdge> getIngoingEdges() {
      return ImmutableSet.of();
    }

    public static ProjectedLocationStateWithEdge getInstance() {
      return instance;
    }
  }

  static class BackwardsLocationStateWithEdge extends LocationStateWithEdge {

    private static final long serialVersionUID = 6825257572921009531L;

    public BackwardsLocationStateWithEdge(
        CFANode locationNode,
        boolean pFollowFunctionCalls,
        AbstractEdge pEdge) {
      super(locationNode, pFollowFunctionCalls, pEdge);
    }

    @Override
    public Iterable<CFAEdge> getOutgoingEdges() {
      return super.getIngoingEdges();
    }

    @Override
    public Iterable<CFAEdge> getIngoingEdges() {
      return super.getOutgoingEdges();
    }

  }

  @Override
  public AbstractEdge getAbstractEdge() {
    return edge;
  }

  @Override
  public int hashCode() {
    return Objects.hash(edge, locationNode);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof LocationStateWithEdge)) {
      return false;
    }
    LocationStateWithEdge other = (LocationStateWithEdge) obj;

    return Objects.equals(edge, other.edge) && locationNode == other.locationNode;
  }

  public LocationStateWithEdge updateEdge(AbstractEdge pEdge) {
    return new LocationStateWithEdge(
        locationNode,
        this.followFunctionCalls,
        pEdge);
  }

  @Override
  public Object getPartitionKey() {
    if (edge == EmptyEdge.getInstance() || edge == AnyCFAEdge.getInstance()) {
      return edge;
    } else {
      return super.getPartitionKey();
    }
  }

  @Override
  public boolean hasEmptyEffect() {
    return true;
  }

  @Override
  public boolean isProjection() {
    return edge == AnyCFAEdge.getInstance();
  }
}
