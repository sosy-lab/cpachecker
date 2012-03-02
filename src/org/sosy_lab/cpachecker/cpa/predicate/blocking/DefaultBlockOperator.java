/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate.blocking;

import java.io.PrintStream;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.predicate.interfaces.BlockOperator;
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
public class DefaultBlockOperator extends AbstractBlockOperator implements BlockOperator {

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


  int numBlkFunctions = 0;
  int numBlkLoops = 0;
  int numBlkThreshold = 0;

  public DefaultBlockOperator(Configuration pConfig, LogManager pLogger, CFA pCFA) throws InvalidConfigurationException {
    super(pConfig, pLogger, pCFA);
  }

  /**
   * Check whether an abstraction should be computed.
   *
   * @param succLoc successor CFA location.
   * @param thresholdReached if the maximum block size has been reached
   * @return true if succLoc is an abstraction location. For now a location is
   * an abstraction location if it has an incoming loop-back edge, if it is
   * the start node of a function or if it is the call site from a function call.
   */
  @Override
  public boolean isBlockEnd(AbstractElement element, CFAEdge cfaEdge, PathFormula pf) {
    CFANode succLoc = cfaEdge.getSuccessor();

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

  protected boolean isThresholdFulfilled(PathFormula pf) {
    return pf.getLength() >= threshold;
  }

  private String toPercent(double val, double full) {
    return String.format("%1.0f", val/full*100) + "%";
  }

  @Override
  public void printStatistics(PrintStream pOut, PredicatePrecisionAdjustment pPrec) {
    pOut.println("  Because of function entry/exit:  " + this.numBlkFunctions + " (" + toPercent(this.numBlkFunctions, pPrec.numAbstractions) + ")");
    pOut.println("  Because of loop head:            " + this.numBlkLoops + " (" + toPercent(this.numBlkLoops, pPrec.numAbstractions) + ")");
    pOut.println("  Because of threshold:            " + this.numBlkThreshold + " (" + toPercent(this.numBlkThreshold, pPrec.numAbstractions) + ")");
  }

}
