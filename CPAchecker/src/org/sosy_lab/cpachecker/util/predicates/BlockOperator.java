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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

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

  @Option(secure=true,
      description="maximum blocksize before abstraction is forced\n"
        + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
  private int threshold = 0;

  @Option(secure=true, name="functions",
      description="abstractions at function calls/returns if threshold has been reached (no effect if threshold = 0)")
  private boolean absOnFunction = false;

  @Option(secure=true, name="loops",
      description="abstractions at loop heads if threshold has been reached (no effect if threshold = 0)")
  private boolean absOnLoop = false;

  @Option(secure=true, name="join",
      description="abstractions at CFA nodes with more than one incoming edge if threshold has been reached (no effect if threshold = 0)")
  private boolean absOnJoin = false;

  @Option(secure=true, description="force abstractions immediately after threshold is reached (no effect if threshold = 0)")
  private boolean alwaysAfterThreshold = true;

  @Option(secure=true, description="force abstractions at loop heads, regardless of threshold")
  private boolean alwaysAtLoops = true;

  @Option(secure=true, description="force abstractions at each function calls/returns, regardless of threshold")
  private boolean alwaysAtFunctions = true;

  @Option(secure=true, description="force abstractions at each function head (first node in the body), regardless of threshold")
  private boolean alwaysAtFunctionHeads = false;

  @Option(secure=true, description="force abstractions at the head of the analysis-entry function (first node in the body), regardless of threshold")
  private boolean alwaysAtEntryFunctionHead = false;

  @Option(secure=true, description="force abstractions at each function call (node before entering the body), regardless of threshold")
  private boolean alwaysAtFunctionCallNodes = false;

  @Option(secure=true, description="force abstractions at each join node, regardless of threshold")
  private boolean alwaysAtJoin = false;

  @Option(secure=true, description="force abstractions at each branch node, regardless of threshold")
  private boolean alwaysAtBranch = false;

  @Option(secure=true, description="abstraction always and only on explicitly computed abstraction nodes.")
  private boolean alwaysAndOnlyAtExplicitNodes = false;


  private ImmutableSet<CFANode> explicitAbstractionNodes = null;
  private ImmutableSet<CFANode> loopHeads = null;

  public int numBlkEntryFunctionHeads = 0;
  public int numBlkFunctionHeads = 0;
  public int numBlkFunctions = 0;
  public int numBlkLoops = 0;
  public int numBlkJoins = 0;
  public int numBlkBranch = 0;
  public int numBlkThreshold = 0;

  private CFA cfa = null;

  /**
   * Check whether an abstraction should be computed.
   *
   * @param succLoc Successor CFA location (of the analysis).
   * @param predLoc Predecessor CFA location (of the analysis).
   *
   *   ATTENTION: for the backwards analysis the successor/predecessor of the edge do not match succLoc/predLoc.
   *
   * @param edge    The edge between succLoc and predLoc.
   *
   * @return true if succLoc is an abstraction location. For now a location is
   * an abstraction location if it has an incoming loop-back edge, if it is
   * the start node of a function or if it is the call site from a function call.
   */
  public boolean isBlockEnd(CFANode succLoc, CFANode predLoc, CFAEdge edge, PathFormula pf) {
    // If you change this function, make sure to adapt alwaysReturnsFalse(), too!

    if (alwaysAndOnlyAtExplicitNodes) {
      assert (explicitAbstractionNodes != null);
      return explicitAbstractionNodes.contains(predLoc);
    }

    if (threshold == 1) {
      // check SBE case here to avoid need for loop-structure information
      return true;
    }

    if (alwaysAtFunctions && isFunctionCall(succLoc)) {
      numBlkFunctions++;
      return true;
    }

    if (alwaysAtEntryFunctionHead && isFirstLocationInFunctionBody(succLoc)) {
      Preconditions.checkNotNull(cfa);
      if (cfa.getMainFunction().getFunctionName().equals(edge.getPredecessor().getFunctionName())) {
        numBlkEntryFunctionHeads++;
        return true;
      }
    }

    if (alwaysAtFunctionHeads && isFunctionHead(edge)) {
      numBlkFunctionHeads++;
      return true;
    }

    if (alwaysAtFunctionCallNodes && isBeforeFunctionCall(succLoc)) {
      numBlkFunctionHeads++;
      return true;
    }

    if (alwaysAtLoops && isLoopHead(succLoc)) {
      numBlkLoops++;
      return true;
    }

    if (alwaysAtJoin && isJoinNode(succLoc)) {
      numBlkJoins++;
      return true;
    }

    if (alwaysAtBranch && isBranchNode(succLoc)) {
      numBlkBranch++;
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

  /**
   * If this method returns true, {@link #isBlockEnd(CFANode, CFANode, CFAEdge, PathFormula)}
   * is guaranteed to always return false.
   * This can be used to add optimizations.
   */
  public boolean alwaysReturnsFalse() {
    if (alwaysAndOnlyAtExplicitNodes) {
      return explicitAbstractionNodes.isEmpty();
    }
    return !alwaysAtFunctions
        && !alwaysAtEntryFunctionHead
        && !alwaysAtFunctionHeads
        && !alwaysAtFunctionCallNodes
        && !alwaysAtLoops
        && !alwaysAtJoin
        && !alwaysAtBranch
        && (threshold == 0)
        && !absOnFunction
        && !absOnLoop
        && !absOnJoin
        ;
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

  public void setExplicitAbstractionNodes(ImmutableSet<CFANode> pNodes) {
    this.explicitAbstractionNodes = pNodes;
  }

  public void setCFA(CFA pCfa) {
    this.cfa = pCfa;
    if (absOnLoop || alwaysAtLoops) {
      if (cfa.getAllLoopHeads().isPresent()) {
        loopHeads = cfa.getAllLoopHeads().get();
      }
    }
  }

  protected boolean isBranchNode(CFANode pLoc) {
    return pLoc.getNumLeavingEdges() > 1;
  }

  protected boolean isFunctionHead(CFAEdge edge) {
    if (edge instanceof CDeclarationEdge )  {
      CDeclarationEdge e = (CDeclarationEdge) edge;
      if (e.getDeclaration() instanceof CFunctionDeclaration) {
        return true;
      }
    }
    return false;
  }

  private boolean isBeforeFunctionCall(CFANode succLoc) {
    return succLoc.getLeavingSummaryEdge() != null;
  }

  public static boolean isFirstLocationInFunctionBody(CFANode pLoc) {
    Collection<CFAEdge> edges = Lists.newArrayList(CFAUtils.enteringEdges(pLoc));

    for (CFAEdge edge: edges) {
      if (edge instanceof CDeclarationEdge )  {
        CDeclarationEdge e = (CDeclarationEdge) edge;
        if (e.getDeclaration() instanceof CFunctionDeclaration) {
          return true;
        }
      }
    }

    return false;
  }
}
