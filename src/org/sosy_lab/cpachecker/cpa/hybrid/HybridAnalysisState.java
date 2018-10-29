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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.cpa.hybrid.util.*;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class HybridAnalysisState implements 
    AbstractQueryableState, 
    LatticeAbstractState<HybridAnalysisState>,
    AbstractStateWithAssumptions {

    // private Set<HybridValue> definitiveVars; 
    private ImmutableSet<CBinaryExpression> assumptions;

    // TODO: define varibale cache 

    public HybridAnalysisState() {
        this(Collections.emptySet());
    }

    public HybridAnalysisState(Set<CExpression> assumptions) {
        this.assumptions = ImmutableSet.copyOf(
            CollectionUtils.ofType(assumptions, CBinaryExpression.class));
    }

    private HybridAnalysisState(Collection<CExpression> assumptions)
    {
        this(Sets.newHashSet(assumptions));
    }

    // creates an exact copy of the given state in terms of assumptions
    public static HybridAnalysisState copyOf(HybridAnalysisState state)
    {
        return new HybridAnalysisState(state.getAssumptions());
    }

    @Override
    public String getCPAName() {
        return HybridAnalysisCPA.class.getSimpleName();
    }

    @Override
    public boolean checkProperty(String property) throws InvalidQueryException {
    // TODO: define dsl for properties
    return true;
    }

    @Override
    public HybridAnalysisState join(HybridAnalysisState pOther)
            throws CPAException, InterruptedException {
        
        // for now we simply assume, that an assumption for the same variable is more accurate in pOther
        Set<CBinaryExpression> otherAssumptions = pOther.assumptions;
        Set<CExpression> combinedAssumptions = Sets.newHashSet(otherAssumptions);
        
        for(CBinaryExpression assumption : assumptions)
        {
            // first operand for assumptions must always be CIdExpression! -> if the cast fails, something is wrong
            CIdExpression localVariable = (CIdExpression) assumption.getOperand1();

            // here we calculate the 'intersection'
            if(!CollectionUtils.appliesToAtLeastOne(otherAssumptions, exp -> localVariable.equals(exp.getOperand1())))
            {
                combinedAssumptions.add(assumption);
            }
        }

        return new HybridAnalysisState(combinedAssumptions);
    }

    @Override
    public boolean isLessOrEqual(HybridAnalysisState pOther)
            throws CPAException, InterruptedException {
        // TODO: check for TOP/BOTTOM element
        List<CExpression> otherAssumptions = pOther.getAssumptions();
        // avoid copying the state inside the lamda 
        return CollectionUtils.appliesToAll(assumptions, a -> otherAssumptions.contains(a));
    }

    @Override
    public List<CExpression> getAssumptions() {
      return ImmutableList.copyOf(assumptions);
    }

    /**
     * Creates a copy of the assumptions held by this state
     * @return the assumptions with explicit expression type
     */
    public ImmutableSet<CBinaryExpression> getExplicitAssumptions() {
        return ImmutableSet.copyOf(assumptions);
    }

}