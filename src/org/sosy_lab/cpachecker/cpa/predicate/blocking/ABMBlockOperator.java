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

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

@Options
public class ABMBlockOperator extends DefaultBlockOperator {

  public ABMBlockOperator(Configuration pConfig, LogManager pLogger, CFA pCFA) throws InvalidConfigurationException {
    super(pConfig, pLogger, pCFA);
    pConfig.inject(this);
  }

  private BlockPartitioning partitioning = null;

  public void setPartitioning(BlockPartitioning pPartitioning) {
    checkState(partitioning == null);
    partitioning = pPartitioning;
  }

  @Override
  public boolean isBlockEnd(AbstractElement pElement, CFAEdge pEdge, PathFormula pPf) {
    CFANode succLoc = pEdge.getSuccessor();

    return super.isBlockEnd(pElement, pEdge, pPf) || partitioning.isCallNode(succLoc) || partitioning.isReturnNode(succLoc);
  }

  public BlockPartitioning getPartitioning() {
    checkState(partitioning != null);
    return partitioning;
  }
}
