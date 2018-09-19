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

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class HybridAnalysisCPA extends AbstractCPA implements ConfigurableProgramAnalysis 
{

    private static final String cfaErrorMessage = "CFA must be present for HybridAnalysis";

    private final AbstractDomain abstractDomain;
    private final CFA cfa;

    protected HybridAnalysisCPA(String mergeType, String stopType, @Nullable TransferRelation transfer, CFA cfa)
    {
        super(mergeType, stopType, transfer);
        abstractDomain = super.getAbstractDomain();
        this.cfa = Preconditions.checkNotNull(cfa, cfaErroMessage);
    }

    protected HybridAnalysisCPA(AbstractDomain domain, TransferRelation transfer, CFA cfa)
    {
        super(domain, transfer);
        abstractDomain = domain;
        this.cfa = Preconditions.checkNotNull(cfa, cfaErroMessage);
    }

    protected HybridAnalysisCPA(String mergeType, String stopType, AbstractDomain domain, @Nullable TransferRelation transfer, CFA cfa)
    {
        super(mergeType, stopType, domain, transfer);
        abstractDomain = domain;
        this.cfa = Preconditions.checkNotNull(cfa, cfaErroMessage);
    }

    @Override
    public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
            throws InterruptedException {
        return null;
    }

    @Override
    public TransferRelation getTransferRelation()
    {
        return new HybridAnalysisTransferRelation();
    }


}