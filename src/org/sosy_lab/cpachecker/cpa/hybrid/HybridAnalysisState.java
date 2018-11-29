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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.hybrid.util.*;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class HybridAnalysisState implements
    LatticeAbstractState<HybridAnalysisState>,
    AbstractStateWithAssumptions,
    Graphable {

    private ImmutableSet<CBinaryExpression> assumptions;

    // variable cache
    private ImmutableSet<CIdExpression> trackedVariables;

    public HybridAnalysisState() {
        this(Collections.emptySet());
    }

    public HybridAnalysisState(Set<CExpression> pAssumptions) {
        this.assumptions = ImmutableSet.copyOf(
            CollectionUtils.ofType(pAssumptions, CBinaryExpression.class));
        
        trackedVariables = ImmutableSet.copyOf(
            this.assumptions
                .stream()
                .map(expression -> (CIdExpression) expression.getOperand1())
                .collect(Collectors.toSet()));
    }

    protected HybridAnalysisState(Set<CExpression> pAssumptions, Set<CIdExpression> pVariables) {
      this.assumptions = ImmutableSet.copyOf(
          CollectionUtils.ofType(pAssumptions, CBinaryExpression.class));

      this.trackedVariables = ImmutableSet.copyOf(pVariables);
    }

    private HybridAnalysisState(Collection<CExpression> pAssumptions)
    {
        this(Sets.newHashSet(pAssumptions));
    }

    // creates an exact copy of the given state in terms of assumptions
    public static HybridAnalysisState copyOf(HybridAnalysisState state)
    {
        return new HybridAnalysisState(state.getAssumptions());
    }

    public static HybridAnalysisState copyWithNewAssumptions(HybridAnalysisState pState, CExpression... pExpressions) {
        Set<CExpression> currentAssumptions = Sets.newHashSet(pState.getAssumptions());
        currentAssumptions.addAll(Arrays.asList(pExpressions));
        return new HybridAnalysisState(currentAssumptions);
    }

    @Override
    public HybridAnalysisState join(HybridAnalysisState pOther)
            throws CPAException, InterruptedException {
        
        // for now we simply assume, that an assumption for the same variable is more accurate in pOther
        Set<CBinaryExpression> otherAssumptions = pOther.assumptions;
        Set<CExpression> combinedAssumptions = Sets.newHashSet(otherAssumptions);
        
        for(CBinaryExpression assumption : assumptions)
        {
            // first operand for assumptions must always be CIdExpression!

            assert assumption.getOperand1() instanceof CIdExpression;

            CIdExpression variable = (CIdExpression) assumption.getOperand1();

            // here we calculate the 'intersection'
            if(!pOther.tracksVariable(variable))
            {
                combinedAssumptions.add(assumption);
            }
        }

        return new HybridAnalysisState(combinedAssumptions);
    }

    @Override
    public boolean isLessOrEqual(HybridAnalysisState pOther)
            throws CPAException, InterruptedException {
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

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof HybridAnalysisState)) {
            return false;
        }

        if(obj == this) {
            return true;
        }

        HybridAnalysisState other = (HybridAnalysisState) obj;
        return this.assumptions.equals(other.assumptions);
    }

    @Override
    public int hashCode() {
        return assumptions.hashCode();
    }

    @Override
    public String toDOTLabel() {
        StringBuilder builder = new StringBuilder();
        assumptions.forEach(assumption -> builder.append(assumption).append(System.lineSeparator()));
        return builder.toString();
    }

    @Override
    public boolean shouldBeHighlighted() {
        return false;
    }

    /**
     *
     * @param pCIdExpression
     * @return
     */
    public boolean tracksVariable(CIdExpression pCIdExpression) {
      return trackedVariables.contains(pCIdExpression);
    }
}