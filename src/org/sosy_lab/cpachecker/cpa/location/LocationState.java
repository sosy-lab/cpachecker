// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import static org.sosy_lab.cpachecker.util.CFAUtils.allEnteringEdges;
import static org.sosy_lab.cpachecker.util.CFAUtils.allLeavingEdges;
import static org.sosy_lab.cpachecker.util.CFAUtils.enteringEdges;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

public class LocationState
    implements AbstractStateWithLocation, AbstractQueryableState, Partitionable, Serializable {

  private static final long serialVersionUID = -801176497691618779L;

  private static boolean isNoFunctionCall(CFAEdge e) {
    return !(e instanceof FunctionCallEdge || e instanceof FunctionReturnEdge);
  }

  static class BackwardsLocationState extends LocationState {

    private static final long serialVersionUID = 6825257572921009531L;

    BackwardsLocationState(CFANode locationNode, boolean pFollowFunctionCalls) {
      super(locationNode, pFollowFunctionCalls);
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

  private transient CFANode locationNode;
  private boolean followFunctionCalls;

  LocationState(CFANode pLocationNode, boolean pFollowFunctionCalls) {
    locationNode = pLocationNode;
    followFunctionCalls = pFollowFunctionCalls;
  }

  @Override
  public CFANode getLocationNode() {
    return locationNode;
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    if (followFunctionCalls) {
      return leavingEdges(locationNode);

    } else {
      return allLeavingEdges(locationNode).filter(LocationState::isNoFunctionCall);
    }
  }

  @Override
  public Iterable<CFAEdge> getIngoingEdges() {
    if (followFunctionCalls) {
      return enteringEdges(locationNode);

    } else {
      return allEnteringEdges(locationNode).filter(LocationState::isNoFunctionCall);
    }
  }

  @Override
  public String toString() {
    String loc = locationNode.describeFileLocation();
    return locationNode + (loc.isEmpty() ? "" : " (" + loc + ")");
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    List<String> parts = Splitter.on("==").trimResults().splitToList(pProperty);
    if (parts.size() != 2) {
      throw new InvalidQueryException(
          "The Query \""
              + pProperty
              + "\" is invalid. Could not split the property string correctly.");
    } else {
      switch (parts.get(0).toLowerCase()) {
        case "line":
          try {
            int queryLine = Integer.parseInt(parts.get(1));
            for (CFAEdge edge : CFAUtils.enteringEdges(locationNode)) {
              if (edge.getLineNumber() == queryLine) {
                return true;
              }
            }
            return false;
          } catch (NumberFormatException nfe) {
            throw new InvalidQueryException(
                "The Query \""
                    + pProperty
                    + "\" is invalid. Could not parse the integer \""
                    + parts.get(1)
                    + "\"");
          }
        case "functionname":
          return locationNode.getFunctionName().equals(parts.get(1));
        case "label":
          return locationNode instanceof CFALabelNode
              ? ((CFALabelNode) locationNode).getLabel().equals(parts.get(1))
              : false;
        case "nodenumber":
          try {
            int queryNumber = Integer.parseInt(parts.get(1));
            return locationNode.getNodeNumber() == queryNumber;
          } catch (NumberFormatException nfe) {
            throw new InvalidQueryException(
                "The Query \""
                    + pProperty
                    + "\" is invalid. Could not parse the integer \""
                    + parts.get(1)
                    + "\"");
          }
        case "mainentry":
          if (locationNode.getNumEnteringEdges() == 1
              && locationNode.getFunctionName().equals(parts.get(1))) {
            CFAEdge enteringEdge = locationNode.getEnteringEdge(0);
            if (enteringEdge.getDescription().equals("Function start dummy edge")
                && enteringEdge.getEdgeType() == CFAEdgeType.BlankEdge
                && FileLocation.DUMMY.equals(enteringEdge.getFileLocation())) {
              return true;
            }
          }
          return false;
        default:
          throw new InvalidQueryException(
              "The Query \""
                  + pProperty
                  + "\" is invalid. \""
                  + parts.get(0)
                  + "\" is no valid keyword");
      }
    }
  }

  @Override
  public String getCPAName() {
    return "location";
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equalsIgnoreCase("lineno")) {
      if (locationNode.getNumEnteringEdges() > 0) {
        return locationNode.getEnteringEdge(0).getLineNumber();
      }
      return 0; // DUMMY
    } else {
      return checkProperty(pProperty);
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

  /**
   * javadoc to remove unused parameter warning
   *
   * @param in the input stream
   */
  @SuppressWarnings("UnusedVariable") // parameter is required by API
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Proxy required");
  }

  private static class SerialProxy implements Serializable {
    private static final long serialVersionUID = 6889568471468710163L;
    private final int nodeNumber;

    public SerialProxy(int nodeNumber) {
      this.nodeNumber = nodeNumber;
    }

    private Object readResolve() {
      CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().orElseThrow();
      return cfaInfo.getLocationStateFactory().getState(cfaInfo.getNodeByNodeNumber(nodeNumber));
    }
  }
}
