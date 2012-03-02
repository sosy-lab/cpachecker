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
import java.util.Random;

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
@Options(prefix="cpa.predicate.blk.stochastic")
public class StochasticBlockOperator extends AbstractBlockOperator implements BlockOperator {

  @Option(description="abstract at loop heads with the given probability.")
  private float absAtLoops = 1.0f;

  @Option(description="abstract at function calls/returns with the given probability.")
  private float absAtFunctions = 1.0f;


  private Random rand = new Random();
  private int numBlkFunctions = 0;
  private int numBlkLoops = 0;

  public StochasticBlockOperator(Configuration pConfig, LogManager pLogger, CFA pCFA) throws InvalidConfigurationException {
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

    if ((rand.nextFloat() <= absAtFunctions) && isFunctionCall(succLoc)) {
      numBlkFunctions++;
      return true;
    }

    if ((rand.nextFloat() <= absAtLoops) && isLoopHead(succLoc)) {
      numBlkLoops++;
      return true;
    }

    return false;
  }

  private String toPercent(double val, double full) {
    return String.format("%1.0f", val/full*100) + "%";
  }

  @Override
  public void printStatistics(PrintStream pOut, PredicatePrecisionAdjustment pPrec) {
    pOut.println("  Because of function entry/exit:  " + this.numBlkFunctions + " (" + toPercent(this.numBlkFunctions, pPrec.numAbstractions) + ")");
    pOut.println("  Because of loop head:            " + this.numBlkLoops + " (" + toPercent(this.numBlkLoops, pPrec.numAbstractions) + ")");
  }

}
