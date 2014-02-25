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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.collect.ImmutableSet;

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

  @Option(name="join",
      description="abstractions at CFA nodes with more than one incoming edge if threshold has been reached (no effect if threshold = 0)")
  private boolean absOnJoin = false;

  @Option(description="force abstractions immediately after threshold is reached (no effect if threshold = 0)")
  private boolean alwaysAfterThreshold = true;

  @Option(description="force abstractions at loop heads, regardless of threshold")
  private boolean alwaysAtLoops = true;

  @Option(description="force abstractions at each function calls/returns, regardless of threshold")
  private boolean alwaysAtFunctions = true;

  @Option(description="force abstractions at each join node, regardless of threshold")
  private boolean alwaysAtJoin = false;

  @Option(description="abstraction always and only on explicitly computed abstraction nodes.")
  private boolean alwaysAndOnlyAtExplicitNodes = false;


  private ImmutableSet<CFANode> explicitAbstractionNodes = null;
  private ImmutableSet<CFANode> loopHeads = null;

  int numBlkFunctions = 0;
  int numBlkLoops = 0;
  int numBlkJoins = 0;
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
  public boolean isBlockEnd(CFAEdge cfaEdge, PathFormula pf) {
    CFANode succLoc = cfaEdge.getSuccessor();
    CFANode predLoc = cfaEdge.getPredecessor();

    if (alwaysAndOnlyAtExplicitNodes) {
      assert (explicitAbstractionNodes != null);
      return explicitAbstractionNodes.contains(predLoc);
    }

    if (alwaysAtFunctions && isFunctionCall(succLoc)) {
      numBlkFunctions++;
      return true;
    }

    if (alwaysAtLoops && isLoopHead(succLoc)) {
      numBlkLoops++;
      return true;
    }

    if(alwaysAtJoin && isJoinNode(succLoc)){
      numBlkJoins++;
      return true;
    }

    if (threshold > 0) {
      if (threshold == 1) {
        return true;
      }

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
        } else if (absOnJoin && isJoinNode(succLoc)) {
          numBlkThreshold++;
          numBlkJoins++;
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

  protected boolean isJoinNode(CFANode pSuccLoc) {
    return pSuccLoc.getNumEnteringEdges()>1;
  }

  protected boolean isThresholdFulfilled(PathFormula pf) {
    return pf.getLength() >= threshold;
  }

  protected boolean isLoopHead(CFANode succLoc) {
    checkState(loopHeads != null, "Missing loop information");
    return loopHeads.contains(succLoc);
  }

  protected boolean isFunctionCall(CFANode succLoc) {
    return (succLoc instanceof FunctionEntryNode) // function call edge
        || (succLoc.getEnteringSummaryEdge() != null); // function return edge
  }

  void setExplicitAbstractionNodes(ImmutableSet<CFANode> pNodes) {
    this.explicitAbstractionNodes = pNodes;
  }

  void setCFA(CFA cfa) {
    if (absOnLoop || alwaysAtLoops) {
      if (cfa.getAllLoopHeads().isPresent()) {
        loopHeads = cfa.getAllLoopHeads().get();
      }
    }
  }
}
