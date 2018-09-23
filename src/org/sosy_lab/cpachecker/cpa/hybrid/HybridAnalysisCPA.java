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
package org.sosy_lab.cpachecker.cpa.hybrid;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.hybrid.util.AssumptionParser;

@Options(prefix = "cpa.hybrid")
public class HybridAnalysisCPA implements ConfigurableProgramAnalysis 
{
    @Option(secure = true, 
            name = "initialAssumptions",
            description = "The initial assumptions for a given program.")
    private String initialAssumptionsStringEncoded = "";

    @Option(secure = true,
            name = "ignnoreInvalidAssumptions",
            description = "Per default, invalid assumptions (e.g. declared variable does not exist) will be ignored." +
                          "If set to true, invalid assumptions will cause an error.")
    private boolean ignoreInvalidAssumptions = true;

    @Option(secure = true,
            name = "delimiter",
            description = "The delimiter for different assumptions in 'initialAssumptions' (e.g. x = 10.0; y = -5).")
    private String delimiter = ";";

    private static final String cfaErrorMessage = "CFA must be present for HybridAnalysis";

    private final AbstractDomain abstractDomain;
    private final CFA cfa;
    private final LogManager logger;

    protected HybridAnalysisCPA( 
        CFA cfa, 
        LogManager logger)
    {
        this.abstractDomain = DelegateAbstractDomain.<HybridAnalysisState>getInstance();
        this.cfa = Preconditions.checkNotNull(cfa, cfaErrorMessage);
        this.logger = logger;
    }

    @Override
    public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
            throws InterruptedException {
        return new HybridAnalysisState(new AssumptionParser(delimiter).parseMany(initialAssumptionsStringEncoded));
    }

    @Override
    public TransferRelation getTransferRelation()
    {
        return new HybridAnalysisTransferRelation();
    }

    @Override
    public AbstractDomain getAbstractDomain() {
        return abstractDomain;
    }

    @Override
    public MergeOperator getMergeOperator() {
        return null;
    }

    @Override
    public StopOperator getStopOperator() {
      return null;
    }


}