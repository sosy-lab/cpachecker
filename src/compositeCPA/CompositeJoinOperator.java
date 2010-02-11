/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package compositeCPA;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import exceptions.CPAException;

public class CompositeJoinOperator implements JoinOperator
{
    private final ImmutableList<JoinOperator> joinOperators;

    public CompositeJoinOperator (ImmutableList<JoinOperator> joinOperators)
    {
        this.joinOperators = joinOperators;
    }

    public AbstractElement join (AbstractElement element1, AbstractElement element2) throws CPAException
    {
        CompositeElement comp1 = (CompositeElement) element1;
        CompositeElement comp2 = (CompositeElement) element2;

        List<AbstractElement> comp1Elements = comp1.getElements ();
        List<AbstractElement> comp2Elements = comp2.getElements ();

        if (comp1Elements.size () != comp2Elements.size ())
            throw new CPAException ("Must join composite elements of the same size");
        if (comp1Elements.size () != joinOperators.size ())
            throw new CPAException ("Wrong number of join operators");

        List<AbstractElement> results = new ArrayList<AbstractElement> ();

        for (int idx = 0; idx < comp1Elements.size (); idx++)
        {
            JoinOperator joinOperator = joinOperators.get (idx);
            AbstractElement result = joinOperator.join (comp1Elements.get (idx), comp2Elements.get (idx));
            results.add (result);
        }

        // TODO do we ever use this function?
        return new CompositeElement (results, null);
    }

}
