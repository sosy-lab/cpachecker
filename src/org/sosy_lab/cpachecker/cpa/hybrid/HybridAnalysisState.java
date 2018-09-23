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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.cpa.hybrid.value.HybridValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class HybridAnalysisState implements 
    AbstractQueryableState, 
    LatticeAbstractState<HybridAnalysisState>,
    AbstractStateWithAssumptions
{

    private Set<HybridValue> definitiveVars; 
    private Set<CExpression> assumptions;

    public HybridAnalysisState()
    {
        this(Collections.emptySet());
    }

    public HybridAnalysisState(Set<CExpression> assumptions)
    {
        this.assumptions = assumptions;
    }

    @Override
    public String getCPAName() 
    {
        return HybridAnalysisCPA.class.getSimpleName();
    }

    @Override
    public boolean checkProperty(String property) throws InvalidQueryException
    {
        // TODO: define dsl for properties
        return true;
    }

    @Override
    public HybridAnalysisState join(HybridAnalysisState pOther)
            throws CPAException, InterruptedException {
        return null;
    }

    @Override
    public boolean isLessOrEqual(HybridAnalysisState pOther)
            throws CPAException, InterruptedException {
        return false;
    }

    @Override
    public List<CExpression> getAssumptions() 
    {
      return ImmutableList.copyOf(assumptions);
    }



}