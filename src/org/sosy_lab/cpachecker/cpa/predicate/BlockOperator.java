/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Random;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.clustering.interfaces.Clusterer;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * This class implements the blk operator from the paper
 * "Adjustable Block-Encoding" [Beyer/Keremoglu/Wendler FMCAD'10],
 * i.e., an operator that determines when a block ends and an abstraction step
 * should be done.
 *
 * This operator is configurable by the user.
 */
@Options(prefix="cpa.predicate.blk")
public class BlockOperator {

  private CFA cfa = null;
  private Clusterer clusterer = null;
  private Random rand = new Random();

  @Option(
      description="maximum blocksize before abstraction is forced\n"
        + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
  private int threshold = 0;

  @Option(name="functions",
      description="abstractions at function calls/returns if threshold has been reached (no effect if threshold = 0)")
  private boolean absOnFunction = false;

  @Option(name="loops",
      description="abstractions at loop heads if threshold has been reached (no effect if threshold = 0)")
  private boolean absOnLoop = false;

  @Option(description="force abstractions immediately after threshold is reached (no effect if threshold = 0)")
  private boolean alwaysAfterThreshold = true;

  @Option(description="force abstractions at loop heads, regardless of threshold")
  private boolean alwaysAtLoops = true;

  @Option(description="force abstractions at each function calls/returns, regardless of threshold")
  private boolean alwaysAtFunctions = true;

  @Option(description="force abstractions at each function return, regardless of threshold")
  private boolean alwaysAtFunctionReturns = false;

  @Option(description="abstraction between clusters of the call-graph.")
  private boolean alwaysAtClusterBorder = false;

  @Option(description="propability of a random abstraction in any position of the CFA?")
  private float alwaysWithRandomProp = 0.0f;

  int numBlkFunctions = 0;
  int numBlkLoops = 0;
  int numBlkThreshold = 0;

  /**
   * Check whether an abstraction should be computed.
   *
   * @param succLoc successor CFA location.
   * @param thresholdReached if the maximum block size has been reached
   * @return true if succLoc is an abstraction location. For now a location is
   * an abstraction location if it has an incoming loop-back edge, if it is
   * the start node of a function or if it is the call site from a function call.
   */
  public boolean isBlockEnd(CFANode loc, CFANode succLoc, PathFormula pf) {
    if (alwaysWithRandomProp > 0) {
      if (rand.nextFloat() > alwaysWithRandomProp) {
        return true;
      }
    }

    if (alwaysAtClusterBorder) {
      String predCluster = clusterer.getClusterOfNode(loc);
      String succCluster = clusterer.getClusterOfNode(succLoc);

      if (predCluster == null) {
        System.out.println(String.format("No valid cluster (null) for %s!", loc.getFunctionName()));
        if (succCluster != null) {
          return true;
        }
      } else if (succCluster == null) {
        System.out.println(String.format("No valid cluster (null) for %s!", succLoc.getFunctionName()));
        return true;
      } else if (!succCluster.equals(predCluster)) {
        System.out.println(String.format("Abstraction because of changed cluster: %s --> %s", predCluster, succCluster));
        return true;
      }
    }

    if (alwaysAtFunctionReturns && isFunctionReturn(succLoc)) {
      return true;
    }

    if (alwaysAtFunctions && isFunctionCall(succLoc)) {
      numBlkFunctions++;
      return true;
    }

    if (alwaysAtLoops && isLoopHead(succLoc)) {
      numBlkLoops++;
      return true;
    }

    if (threshold > 0) {

      if (isThresholdFulfilled(pf)) {

        if (alwaysAfterThreshold) {
          numBlkThreshold++;
          return true;

        } else if (absOnFunction && isFunctionCall(succLoc)) {
          numBlkThreshold++;
          numBlkFunctions++;
          return true;

        } else if (absOnLoop && isLoopHead(succLoc)) {
          numBlkThreshold++;
          numBlkLoops++;
          return true;
        }
      }

    } else {
      assert threshold == 0;

      // Specifying blk.functions and blk.loops does not make sense with threshold=0.
      // For compatibility reasons, act as if blk.alwaysAtFunctions / blk.alwaysAtLoops
      // was instead specified.
      if (absOnFunction && isFunctionCall(succLoc)) {
        numBlkFunctions++;
        return true;
      }

      if (absOnLoop && isLoopHead(succLoc)) {
        numBlkLoops++;
        return true;
      }
    }

    return false;
  }

  public void setCfa(CFA pCfa) {
    this.cfa = pCfa;
  }

  public void setClusterer(Clusterer pClusterer) {
    this.clusterer = pClusterer;
  }

  protected boolean isThresholdFulfilled(PathFormula pf) {
    return pf.getLength() >= threshold;
  }

  protected boolean isLoopHead(CFANode succLoc) {
    return succLoc.isLoopStart();
  }

  protected boolean isFunctionReturn(CFANode succLoc) {
    return  (succLoc.getEnteringSummaryEdge() != null); // function return edge
  }

  protected boolean isFunctionCall(CFANode succLoc) {
    return (succLoc instanceof CFAFunctionDefinitionNode) // function call edge
        || (succLoc.getEnteringSummaryEdge() != null); // function return edge
  }
}
