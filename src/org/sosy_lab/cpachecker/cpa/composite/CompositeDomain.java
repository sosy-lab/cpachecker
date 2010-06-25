/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite;

import java.util.List;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

public class CompositeDomain implements AbstractDomain
{
    private final ImmutableList<AbstractDomain> domains;

    private final CompositeElement bottomElement;
    private final CompositeElement topElement;
    private final CompositeJoinOperator joinOperator;
    private final CompositePartialOrder partialOrder;

    public CompositeDomain(ImmutableList<AbstractDomain> domains)
    {
        this.domains = domains;

        ImmutableList.Builder<AbstractElement> bottoms = ImmutableList.builder();
        ImmutableList.Builder<AbstractElement> tops = ImmutableList.builder();
        ImmutableList.Builder<JoinOperator> joinOperators = ImmutableList.builder();
        ImmutableList.Builder<PartialOrder> partialOrders = ImmutableList.builder();

        for (AbstractDomain domain : domains)
        {
            bottoms.add (domain.getBottomElement ());
            tops.add (domain.getTopElement ());
            joinOperators.add (domain.getJoinOperator ());
            partialOrders.add (domain.getPartialOrder ());
        }

        this.bottomElement = new CompositeElement(bottoms.build(), null);
        this.topElement = new CompositeElement(tops.build(), null);
        this.joinOperator = new CompositeJoinOperator(joinOperators.build());
        this.partialOrder = new CompositePartialOrder(partialOrders.build());
    }

    public List<AbstractDomain> getDomains ()
    {
        return domains;
    }

    @Override
    public CompositeElement getBottomElement ()
    {
        return bottomElement;
    }

    @Override
    public CompositeElement getTopElement ()
    {
        return topElement;
    }

    @Override
    public JoinOperator getJoinOperator ()
    {
        return joinOperator;
    }

    @Override
    public PartialOrder getPartialOrder ()
    {
        return partialOrder;
    }
}
