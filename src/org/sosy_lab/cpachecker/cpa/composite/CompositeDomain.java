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
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CompositeDomain implements AbstractDomain
{
    private final ImmutableList<AbstractDomain> domains;

    private final CompositeJoinOperator joinOperator;
    private final CompositePartialOrder partialOrder;

    public CompositeDomain(ImmutableList<AbstractDomain> domains)
    {
        this.domains = domains;

        this.joinOperator = new CompositeJoinOperator(domains);
        this.partialOrder = new CompositePartialOrder(domains);
    }

    public List<AbstractDomain> getDomains ()
    {
        return domains;
    }

    @Override
    public AbstractElement join(AbstractElement pElement1,
        AbstractElement pElement2) throws CPAException {
      return joinOperator.join(pElement1, pElement2);
    }

    @Override
    public boolean satisfiesPartialOrder(AbstractElement pElement1,
        AbstractElement pElement2) throws CPAException {
      return partialOrder.satisfiesPartialOrder(pElement1, pElement2);
    }
}
