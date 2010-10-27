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
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CompositePartialOrder implements PartialOrder
{
  private final ImmutableList<AbstractDomain> domains;

    public CompositePartialOrder(ImmutableList<AbstractDomain> domains)
    {
      this.domains = domains;
    }

    @Override
    public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2) throws CPAException
    {
        CompositeElement comp1 = (CompositeElement) element1;
        CompositeElement comp2 = (CompositeElement) element2;

        List<AbstractElement> comp1Elements = comp1.getElements ();
        List<AbstractElement> comp2Elements = comp2.getElements ();

        if (comp1Elements.size () != comp2Elements.size ())
            throw new CPAException ("Must check pre-order satisfaction of composite elements of the same size");
        if (comp1Elements.size () != domains.size ())
            throw new CPAException ("Wrong number of pre-orders");

        for (int idx = 0; idx < comp1Elements.size (); idx++)
        {
            AbstractDomain domain = domains.get(idx);
            if (!domain.satisfiesPartialOrder (comp1Elements.get (idx), comp2Elements.get (idx)))
                return false;
        }

        return true;
    }
}
