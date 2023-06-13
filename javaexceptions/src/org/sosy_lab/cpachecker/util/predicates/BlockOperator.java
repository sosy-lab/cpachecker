// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;

/**
 * This class implements the blk operator from the paper "Adjustable Block-Encoding"
 * [Beyer/Keremoglu/Wendler FMCAD'10], i.e., an operator that determines when a block ends and an
 * abstraction step should be done.
 *
 * <p>This operator is configurable by the user.
 */
@Options(prefix = "cpa.predicate.blk")
public class BlockOperator {

  @Option(
      secure = true,
      description =
          "maximum blocksize before abstraction is forced\n"
              + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
  @IntegerOption(min = 0)
  private int threshold = 0;

  @Option(
      secure = true,
      name = "functions",
      description =
          "abstractions at function calls/returns if threshold has been reached (no effect if"
              + " threshold = 0)")
  private boolean absOnFunction = false;

  @Option(
      secure = true,
      name = "loops",
      description =
          "abstractions at loop heads if threshold has been reached (no effect if threshold = 0)")
  private boolean absOnLoop = false;

  @Option(
      secure = true,
      name = "join",
      description =
          "abstractions at CFA nodes with more than one incoming edge if threshold has been reached"
              + " (no effect if threshold = 0)")
  private boolean absOnJoin = false;

  @Option(
      secure = true,
      description =
          "force abstractions immediately after threshold is reached (no effect if threshold = 0)")
  private boolean alwaysAfterThreshold = true;

  @Option(secure = true, description = "force abstractions at loop heads, regardless of threshold")
  private boolean alwaysAtLoops = true;

  @Option(
      secure = true,
      description = "force abstractions at each function calls/returns, regardless of threshold")
  private boolean alwaysAtFunctions = true;

  @Option(
      secure = true,
      description =
          "force abstractions at each function head (first node in the body), regardless of"
              + " threshold")
  private boolean alwaysAtFunctionHeads = false;

  @Option(
      secure = true,
      description =
          "force abstractions at the head of the analysis-entry function (first node in the body),"
              + " regardless of threshold")
  private boolean alwaysAtEntryFunctionHead = false;

  @Option(
      secure = true,
      description =
          "force abstractions at each function call (node before entering the body), regardless of"
              + " threshold")
  private boolean alwaysAtFunctionCallNodes = false;

  @Option(
      secure = true,
      description = "force abstractions at each join node, regardless of threshold")
  private boolean alwaysAtJoin = false;

  @Option(
      secure = true,
      description = "force abstractions at each branch node, regardless of threshold")
  private boolean alwaysAtBranch = false;

  @Option(
      secure = true,
      description =
          "force abstractions at program exit (program end, abort, etc.), regardless of threshold")
  private boolean alwaysAtProgramExit = false;

  @Option(
      secure = true,
      description = "abstraction always and only on explicitly computed abstraction nodes.")
  private boolean alwaysAndOnlyAtExplicitNodes = false;

  @Option(
      secure = true,
      description = "abstraction always at explicitly computed abstraction nodes.")
  private boolean alwaysAtExplicitNodes = false;

  private ImmutableSet<CFANode> explicitAbstractionNodes = null;
  private ImmutableSet<CFANode> loopHeads = null;

  public StatCounter numBlkEntryFunctionHeads = new StatCounter("");
  public StatCounter numBlkFunctionHeads = new StatCounter("");
  public StatCounter numBlkFunctions = new StatCounter("");
  public StatCounter numBlkLoops = new StatCounter("");
  public StatCounter numBlkJoins = new StatCounter("");
  public StatCounter numBlkBranch = new StatCounter("");
  public StatCounter numBlkThreshold = new StatCounter("");
  public StatCounter numBlkExit = new StatCounter("");

  /**
   * Check whether an abstraction should be computed.
   *
   * @param loc Current CFA location (of the analysis).
   * @return true if loc is an abstraction location according to the configuration.
   */
  public boolean isBlockEnd(final CFANode loc, final int thresholdValue) {
    // If you change this function, make sure to adapt alwaysReturnsFalse(), too!

    if (alwaysAndOnlyAtExplicitNodes) {
      assert explicitAbstractionNodes != null;
      return explicitAbstractionNodes.contains(loc);
    }

    if (alwaysAtExplicitNodes
        && explicitAbstractionNodes != null
        && explicitAbstractionNodes.contains(loc)) {
      return true;
    }

    if (threshold == 1) {
      // check SBE case here to avoid need for loop-structure information
      return true;
    }

    if (alwaysAtFunctions && isFunctionCall(loc)) {
      numBlkFunctions.inc();
      return true;
    }

    if (alwaysAtEntryFunctionHead && isFirstLocationInMainFunctionBody(loc)) {
      numBlkEntryFunctionHeads.inc();
      return true;
    }

    if (alwaysAtFunctionHeads && isFunctionHead(loc)) {
      numBlkFunctionHeads.inc();
      return true;
    }

    if (alwaysAtFunctionCallNodes && isBeforeFunctionCall(loc)) {
      numBlkFunctionHeads.inc();
      return true;
    }

    if (alwaysAtLoops && isLoopHead(loc)) {
      numBlkLoops.inc();
      return true;
    }

    if (alwaysAtJoin && isJoinNode(loc)) {
      numBlkJoins.inc();
      return true;
    }

    if (alwaysAtBranch && isBranchNode(loc)) {
      numBlkBranch.inc();
      return true;
    }

    if (alwaysAtProgramExit && isProgramExit(loc)) {
      numBlkExit.inc();
      return true;
    }

    if (threshold > 0) {
      if (isThresholdFulfilled(thresholdValue)) {

        if (alwaysAfterThreshold) {
          numBlkThreshold.inc();
          return true;

        } else if (absOnFunction && isFunctionCall(loc)) {
          numBlkThreshold.inc();
          numBlkFunctions.inc();
          return true;

        } else if (absOnLoop && isLoopHead(loc)) {
          numBlkThreshold.inc();
          numBlkLoops.inc();
          return true;
        } else if (absOnJoin && isJoinNode(loc)) {
          numBlkThreshold.inc();
          numBlkJoins.inc();
          return true;
        }
      }

    } else {
      assert threshold == 0;

      // Specifying blk.functions and blk.loops does not make sense with threshold=0.
      // For compatibility reasons, act as if blk.alwaysAtFunctions / blk.alwaysAtLoops
      // was instead specified.
      if (absOnFunction && isFunctionCall(loc)) {
        numBlkFunctions.inc();
        return true;
      }

      if (absOnLoop && isLoopHead(loc)) {
        numBlkLoops.inc();
        return true;
      }
    }

    return false;
  }

  /**
   * If this method returns true, {@link #isBlockEnd(CFANode, int)} is guaranteed to always return
   * false. This can be used to add optimizations.
   */
  public boolean alwaysReturnsFalse() {
    if (alwaysAndOnlyAtExplicitNodes) {
      return explicitAbstractionNodes.isEmpty();
    }
    return !alwaysAtFunctions
        && !alwaysAtEntryFunctionHead
        && !alwaysAtFunctionCallNodes
        && !alwaysAtLoops
        && !alwaysAtJoin
        && !alwaysAtBranch
        && !alwaysAtProgramExit
        && (!alwaysAtExplicitNodes
            || explicitAbstractionNodes == null
            || explicitAbstractionNodes.isEmpty())
        && (threshold == 0)
        && !absOnFunction
        && !absOnLoop
        && !absOnJoin;
  }

  protected boolean isJoinNode(CFANode pSuccLoc) {
    return pSuccLoc.getNumEnteringEdges() > 1;
  }

  protected boolean isThresholdFulfilled(int thresholdValue) {
    return thresholdValue >= threshold;
  }

  protected boolean isLoopHead(CFANode succLoc) {
    checkState(loopHeads != null, "Missing loop information");
    return loopHeads.contains(succLoc);
  }

  protected boolean isFunctionCall(CFANode succLoc) {
    return isFunctionHead(succLoc)
        || (succLoc.getEnteringSummaryEdge() != null); // function return edge
  }

  public void setExplicitAbstractionNodes(ImmutableSet<CFANode> pNodes) {
    explicitAbstractionNodes = pNodes;
  }

  public void setCFA(CFA cfa) {
    if (absOnLoop || alwaysAtLoops) {
      if (cfa.getAllLoopHeads().isPresent()) {
        loopHeads = cfa.getAllLoopHeads().orElseThrow();
      }
    }
  }

  protected boolean isBranchNode(CFANode pLoc) {
    return pLoc.getNumLeavingEdges() > 1;
  }

  protected boolean isFunctionHead(CFANode succLoc) {
    return succLoc instanceof FunctionEntryNode;
  }

  protected boolean isProgramExit(CFANode pLoc) {
    return pLoc.getNumLeavingEdges() == 0;
  }

  private boolean isBeforeFunctionCall(CFANode succLoc) {
    return succLoc.getLeavingSummaryEdge() != null;
  }

  private static boolean isFirstLocationInMainFunctionBody(CFANode pLoc) {
    for (CFAEdge edge : CFAUtils.leavingEdges(pLoc)) {
      if (edge instanceof BlankEdge) {
        if (edge.getDescription().equals("Function start dummy edge")) {
          return true;
        }
      }
    }

    return false;
  }
}
