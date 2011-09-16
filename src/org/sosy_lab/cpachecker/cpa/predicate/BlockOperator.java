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

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * This class implements the blk operator from the paper
 * "Adjustable Block-Encoding" [Beyer/Keremoglu/Wendler FMCAD'10],
 * i.e., an operator that determines when a block ends and an abstraction step
 * should be done.
 *
 * This operator is configurable by the user.
 */
@Options(prefix="cpa.predicate")
public class BlockOperator {

  @Option(name="blk.threshold",
      description="maximum blocksize before abstraction is forced\n"
        + "(non-negative number, special values: 0 = don't check threshold, 1 = SBE)")
  private int absBlockSize = 0;

  @Option(name="blk.functions",
      description="force abstractions on function call/return")
  private boolean absOnFunction = true;

  @Option(name="blk.loops",
      description="force abstractions for each loop iteration")
  private boolean absOnLoop = true;

  @Option(name="blk.requireThresholdAndLBE",
      description="require that both the threshold and (functions or loops) "
        + "have to be fulfilled to compute an abstraction")
  private boolean absOnlyIfBoth = false;


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
  public boolean isBlockEnd(CFANode succLoc, PathFormula pf) {
    boolean result = false;

    if (absOnLoop) {
      result = succLoc.isLoopStart();
      if (result) {
        numBlkLoops++;
      }
    }
    if (absOnFunction) {
      boolean function =
               (succLoc instanceof CFAFunctionDefinitionNode) // function call edge
            || (succLoc.getEnteringSummaryEdge() != null); // function return edge
      if (function) {
        result = true;
        numBlkFunctions++;
      }
    }

    if (absBlockSize > 0) {
      boolean threshold = (pf.getLength() >= absBlockSize);
      if (threshold) {
        numBlkThreshold++;
      }

      if (absOnlyIfBoth) {
        result = result && threshold;
      } else {
        result = result || threshold;
      }
    }

    return result;
  }
}
