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
package org.sosy_lab.cpachecker.cpa.location;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.CFAUtils.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

public class LocationState implements AbstractStateWithLocation, AbstractQueryableState, Partitionable, Serializable {

  private static final long serialVersionUID = -801176497691618779L;

  @Options(prefix="cpa.location")
  public static class LocationStateFactory {

    private final LocationState[] states;

    enum LocationStateType {FORWARD, BACKWARD, BACKWARDNOTARGET}

    @Option(secure=true, description="With this option enabled, unction calls that occur"
        + " in the CFA are followed. By disabling this option one can traverse a function"
        + " without following function calls (in this case FunctionSummaryEdges are used)")
    private boolean followFunctionCalls = true;

    public LocationStateFactory(CFA pCfa, LocationStateType locationType, Configuration config) throws InvalidConfigurationException {
      config.inject(this);

      SortedSet<CFANode> allNodes = from(pCfa.getAllNodes())
          // First, we collect all CFANodes in between the inner edges of all MultiEdges.
          // This is necessary for cpa.composite.splitMultiEdges
          .transformAndConcat(new Function<CFANode, Iterable<CFAEdge>>() {
                @Override
                public Iterable<CFAEdge> apply(CFANode pInput) {
                  return CFAUtils.leavingEdges(pInput);
                }
              })
          .filter(MultiEdge.class)
          .transformAndConcat(new Function<MultiEdge, Iterable<CFAEdge>>() {
                @Override
                public Iterable<CFAEdge> apply(MultiEdge pInput) {
                  return pInput.getEdges();
                }
              })
          .transform(CFAUtils.TO_SUCCESSOR)
          // Second, we collect all normal CFANodes
          .append(pCfa.getAllNodes())
          // Third, sort and remove duplicates
          .toSortedSet(Ordering.natural());

      int maxNodeNumber = allNodes.last().getNodeNumber();
      states = new LocationState[maxNodeNumber+1];
      for (CFANode node : allNodes) {
        LocationState state = locationType == LocationStateType.BACKWARD
            ? new BackwardsLocationState(node, pCfa, followFunctionCalls)
            : locationType == LocationStateType.BACKWARDNOTARGET
                ? new BackwardsLocationStateNoTarget(node, pCfa, followFunctionCalls)
                : new LocationState(node, followFunctionCalls);

        states[node.getNodeNumber()] = state;
      }
    }

    public LocationState getState(CFANode node) {
      return Preconditions.checkNotNull(states[checkNotNull(node).getNodeNumber()],
          "LocationState for CFANode %s in function %s requested,"
          + " but this node is not part of the current CFA.",
          node, node.getFunctionName());
    }
  }

  private static class BackwardsLocationState extends LocationState implements Targetable {

    private static final long serialVersionUID = 6825257572921009531L;

    private final CFA cfa;
    private boolean followFunctionCalls;

    protected BackwardsLocationState(CFANode locationNode, CFA pCfa, boolean pFollowFunctionCalls) {
      super(locationNode, pFollowFunctionCalls);
      cfa = pCfa;
      followFunctionCalls = pFollowFunctionCalls;
    }

    @Override
    public Iterable<CFAEdge> getOutgoingEdges() {
      if (followFunctionCalls) {
        return enteringEdges(getLocationNode());

      } else {
        return allEnteringEdges(getLocationNode()).filter(not(or(instanceOf(FunctionReturnEdge.class), instanceOf(FunctionCallEdge.class))));
      }
    }

    @Override
    public boolean isTarget() {
      return cfa.getMainFunction() == getLocationNode();
    }

    @Override
    public Set<Property> getViolatedProperties() throws IllegalStateException {
      return ImmutableSet.<Property>of(NamedProperty.create("Entry node reached backwards."));
    }

  }

  private static class BackwardsLocationStateNoTarget extends BackwardsLocationState {

    private static final long serialVersionUID = -2918748452708606128L;

    protected BackwardsLocationStateNoTarget(CFANode pLocationNode, CFA pCfa, boolean pFollowFunctionCalls) {
      super(pLocationNode, pCfa, pFollowFunctionCalls);
    }

    @Override
    public boolean isTarget() {
      return false;
    }
  }

  private transient CFANode locationNode;
  private boolean followFunctionCalls;

  private LocationState(CFANode pLocationNode, boolean pFollowFunctionCalls) {
    locationNode = pLocationNode;
    followFunctionCalls = pFollowFunctionCalls;
  }

  @Override
  public CFANode getLocationNode() {
      return locationNode;
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
      return Collections.singleton(locationNode);
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    if (followFunctionCalls) {
      return leavingEdges(locationNode);

    } else {
      return allLeavingEdges(locationNode).filter(not(or(instanceOf(FunctionReturnEdge.class), instanceOf(FunctionCallEdge.class))));
    }
  }

  @Override
  public String toString() {
    String loc = locationNode.describeFileLocation();
    return locationNode
        + (loc.isEmpty() ? "" : " (" + loc + ")");
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    String[] parts = pProperty.split("==");
    if (parts.length != 2) {
      throw new InvalidQueryException("The Query \"" + pProperty
          + "\" is invalid. Could not split the property string correctly.");
    } else {
      if (parts[0].toLowerCase().equals("line")) {
        try {
          int queryLine = Integer.parseInt(parts[1]);
          for (CFAEdge edge : CFAUtils.enteringEdges(this.locationNode)) {
            if (edge.getLineNumber()  == queryLine) {
              return true;
            }
          }
          return false;
        } catch (NumberFormatException nfe) {
          throw new InvalidQueryException("The Query \"" + pProperty
              + "\" is invalid. Could not parse the integer \"" + parts[1] + "\"");
        }
      } else if (parts[0].toLowerCase().equals("functionname")) {
        return this.locationNode.getFunctionName().equals(parts[1]);
      } else {
        throw new InvalidQueryException("The Query \"" + pProperty
            + "\" is invalid. \"" + parts[0] + "\" is no valid keyword");
      }
    }
  }

  @Override
  public void modifyProperty(String pModification)
      throws InvalidQueryException {
    throw new InvalidQueryException("The location CPA does not support modification.");
  }

  @Override
  public String getCPAName() {
    return "location";
  }

  @Override
  public Object evaluateProperty(String pProperty)
      throws InvalidQueryException {
    if (pProperty.equalsIgnoreCase("lineno")) {
      if (this.locationNode.getNumEnteringEdges() > 0) {
        return this.locationNode.getEnteringEdge(0).getLineNumber();
      }
      return 0; // DUMMY
    } else {
      return Boolean.valueOf(checkProperty(pProperty));
    }
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

  // no equals and hashCode because there is always only one element per CFANode

  private Object writeReplace() {
    return new SerialProxy(locationNode.getNodeNumber());
  }

  private static class SerialProxy implements Serializable {
    private static final long serialVersionUID = 6889568471468710163L;
    private final int nodeNumber;

    public SerialProxy(int nodeNumber) {
      this.nodeNumber = nodeNumber;
    }

    private Object readResolve() {
      CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().get();
      return cfaInfo.getLocationStateFactory().getState(cfaInfo.getNodeByNodeNumber(nodeNumber));
    }
  }
}