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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.predicate.interfaces.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;


public abstract class AbstractBlockOperator implements BlockOperator {

  protected final Configuration config;
  protected final LogManager logger;
  protected final CFA cfa;

  public AbstractBlockOperator(Configuration pConfig, LogManager pLogger, CFA pCFA) throws InvalidConfigurationException {
    pConfig.inject(this);

    this.config = pConfig;
    this.logger = pLogger;
    this.cfa = pCFA;
  }

  @Override
  public boolean isBlockEnd(AbstractElement pElement, CFAEdge pCfaEdge, PathFormula pPf) {
    return false;
  }

  @Override
  public boolean isBlockEndStrengthened(AbstractElement pElement, CFAEdge pCfaEdge, PathFormula pPf, CallstackElement pCallstackElement) {
    return false;
  }

  protected boolean isLoopHead(CFANode succLoc) {
    return succLoc.isLoopStart();
  }

  protected boolean isFunctionCall(CFANode succLoc) {
    return (succLoc instanceof CFAFunctionDefinitionNode) // function call edge
        || (succLoc.getEnteringSummaryEdge() != null); // function return edge
  }

  @Override
  public void printStatistics(PrintStream pOut, PredicatePrecisionAdjustment pPrec) {
  }
}
