/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;


/**
 * Perform a (very) simple cone-of-influence reduction on the given CFA.
 * That is, get rid of all the nodes/edges that are not reachable from the
 * error location(s) and assert(s).
 *
 * In fact, this should probably *not* be called ConeOfInfluenceCFAReduction,
 * since it is *much* more trivial (and less powerful) than that.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
@Options(prefix="cfa.pruning")
public class CFAReduction {
  
  @Option
  private boolean markOnly = false;
  
  public CFAReduction(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  private static final String ASSERT_FUNCTION = "__assert_fail";
  private static final String ERROR_LABEL = "error";
  
  public void removeIrrelevantForErrorLocations(final CFAFunctionDefinitionNode cfa) {
    Set<CFANode> allNodes = new HashSet<CFANode>();
    Set<CFANode> relevantNodes = new HashSet<CFANode>();
    Set<CFANode> errorNodes = new HashSet<CFANode>();
    
    dfs(cfa, allNodes, false);
    for (CFANode n : allNodes) {
      if (relevantNodes.contains(n)) {
        // this node has already been determined to be necessary
        continue;
      }
      
      boolean errorLocation = false;
      // first, check for label "ERROR"
      if (n instanceof CFALabelNode) {
        CFALabelNode l = (CFALabelNode)n;
        errorLocation = l.getLabel().toLowerCase().startsWith(ERROR_LABEL);
      }
      
      // second, check for assert
      if (!errorLocation) {
        for (int i = 0; i < n.getNumEnteringEdges(); i++) {
          CFAEdge e = n.getEnteringEdge(i);
          if ((e.getEdgeType() == CFAEdgeType.StatementEdge)
              && (e.getRawStatement().trim().startsWith(ASSERT_FUNCTION))) {
            errorLocation = true;
            break;
          }
        }
      }
      
      if (errorLocation) {
        dfs(n, relevantNodes, true);
        errorNodes.add(n);
      }
    }

    // now detach all the nodes not visited
    for (CFANode n : allNodes) {
      if (!relevantNodes.contains(n)) {
        boolean irrelevant = true;
        int edgeIndex = 0;
        while (n.getNumEnteringEdges() > edgeIndex) {
          CFAEdge removedEdge = n.getEnteringEdge(edgeIndex);
          CFANode prevNode = removedEdge.getPredecessor();
          if(!(errorNodes.contains(prevNode))) {
            // do not remove the direct successors of error nodes
            irrelevant = false;
            if (!markOnly) {
              prevNode.removeLeavingEdge(removedEdge);
              n.removeEnteringEdge(removedEdge);
            } else {
              ++edgeIndex;
            }
          } else {
            ++edgeIndex;
          }
        }
        if (markOnly) {
          if (irrelevant) {
            n.setIrrelevant();
          }
        } else {
          while (n.getNumLeavingEdges() > 0) {
            CFAEdge removedEdge = n.getLeavingEdge(0);
            n.removeLeavingEdge(removedEdge);
            CFANode succNode = removedEdge.getSuccessor();
            succNode.removeEnteringEdge(removedEdge);
          }
          n.addEnteringSummaryEdge(null);
          n.addLeavingSummaryEdge(null);
        }
      }
    }
  }

  private void dfs(CFANode start, Set<CFANode> seen, boolean reverse) {
    Deque<CFANode> toProcess = new ArrayDeque<CFANode>();
    toProcess.push(start);
    while (!toProcess.isEmpty()) {
      CFANode n = toProcess.pop();
      seen.add(n);
      if (reverse) {
        for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
          CFAEdge e = n.getEnteringEdge(i);
          CFANode s = e.getPredecessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
          }
        }
        if (n.getEnteringSummaryEdge() != null) {
          CFANode s = n.getEnteringSummaryEdge().getPredecessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
          }
        }
      } else {
        for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
          CFAEdge e = n.getLeavingEdge(i);
          CFANode s = e.getSuccessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
          }
        }
        if (n.getLeavingSummaryEdge() != null) {
          CFANode s = n.getLeavingSummaryEdge().getSuccessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
          }
        }
      }
    }
  }
}
