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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackElement;
import org.sosy_lab.cpachecker.cpa.predicate.interfaces.BlockOperator;
import org.sosy_lab.cpachecker.util.blocking.BlockedCFAReducer;
import org.sosy_lab.cpachecker.util.blocking.container.ItemTree;
import org.sosy_lab.cpachecker.util.blocking.interfaces.BlockComputer;
import org.sosy_lab.cpachecker.util.clustering.ReducedCfaClusterer;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

@Options(prefix="cpa.predicate.blk.cfareduction2")
public class CfaReductionBlockOperator extends AbstractBlockOperator implements BlockOperator  {

  @Option(description="Consider the callstack for the explicitly computed abstraction nodes.")
  private boolean considerCallstack = true;

  @Option(description="Instead of reducing the CFA by applying LBE rules; comute a clustering of the inlined CFA.")
  private boolean clusterInlinedCfa = false;

  private final ItemTree<String, CFANode> abstractionNodes;

  public CfaReductionBlockOperator(Configuration pConfig, LogManager pLogger, CFA pCFA) throws InvalidConfigurationException {
    super(pConfig, pLogger, pCFA);
    pConfig.inject(this);

    if (clusterInlinedCfa) {
      ReducedCfaClusterer cfaClusterer = new ReducedCfaClusterer(config, logger);
      this.abstractionNodes = cfaClusterer.computeAbstractionNodes(cfa);
    } else {
      BlockComputer blockComputer = new BlockedCFAReducer(config, logger);
      this.abstractionNodes = blockComputer.computeAbstractionNodes(cfa);
    }
  }

  @Override
  public boolean isBlockEnd(AbstractElement pElement, CFAEdge pCfaEdge, PathFormula pPf) {
    CFANode succLoc = pCfaEdge.getSuccessor();
    if (isLoopHead(succLoc)) {
      return true;
    } else if (!considerCallstack) {
      final String[] noCallstack = {};
      return abstractionNodes.containsLeaf(noCallstack, succLoc);
    }

    return false;
  }

  @Override
  public boolean isBlockEndStrengthened(AbstractElement pElement, CFAEdge pCfaEdge, PathFormula pPf, CallstackElement pCallstackElement) {
    CFANode succLoc = pCfaEdge.getSuccessor();

    if (considerCallstack) {
      String[] callstackFnc = pCallstackElement.getCallstackFunctions(succLoc.getFunctionName());
      if (isLoopHead(succLoc)) {
        return true;
      } else {
        return abstractionNodes.containsLeaf(callstackFnc, succLoc);
      }
    }

    return false;
  }

}
