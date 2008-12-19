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
package cpa.defuse;

import java.util.ArrayList;
import java.util.List;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;
import cpa.defuse.DefUseDefinition;
import cpa.defuse.DefUseElement;

public class DefUseDomain implements AbstractDomain
{
    private static class DefUseBottomElement extends DefUseElement implements BottomElement
    {
        public DefUseBottomElement ()
        {
            super (null);
        }
    }

    private static class DefUseTopElement implements TopElement
    {

    }

    private static class DefUsePartialOrder implements PartialOrder
    {
        public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
        {
            DefUseElement defUseElement1 = (DefUseElement) element1;
            DefUseElement defUseElement2 = (DefUseElement) element2;

            int numDefs = defUseElement1.getNumDefinitions ();
            for (int idx = 0; idx < numDefs; idx++)
            {
                DefUseDefinition definition = defUseElement1.getDefinition (idx);
                if (!defUseElement2.containsDefinition (definition))
                    return false;
            }

            return true;
        }
    }

    private static class DefUseJoinOperator implements JoinOperator
    {
        public AbstractElement join (AbstractElement element1, AbstractElement element2)
        {
            // Useless code, but helps to catch bugs by causing cast exceptions
            DefUseElement defUseElement1 = (DefUseElement) element1;
            DefUseElement defUseElement2 = (DefUseElement) element2;

            List<DefUseDefinition> joined = new ArrayList<DefUseDefinition> ();
            for (int idx = 0; idx < defUseElement1.getNumDefinitions (); idx++)
                joined.add (defUseElement1.getDefinition (idx));

            for (int idx = 0; idx < defUseElement2.getNumDefinitions (); idx++)
            {
                DefUseDefinition def = defUseElement2.getDefinition (idx);
                if (!joined.contains (def))
                    joined.add (def);
            }

            return new DefUseElement (joined);
        }
    }

    private final static BottomElement bottomElement = new DefUseBottomElement ();
    private final static TopElement topElement = new DefUseTopElement ();
    private final static PartialOrder partialOrder = new DefUsePartialOrder ();
    private final static JoinOperator joinOperator = new DefUseJoinOperator ();

    public DefUseDomain ()
    {

    }

    public AbstractElement getBottomElement ()
    {
        return bottomElement;
    }

	public boolean isBottomElement(AbstractElement element) {
		// TODO Auto-generated method stub
		return false;
	}

    public AbstractElement getTopElement ()
    {
        return topElement;
    }

    public JoinOperator getJoinOperator ()
    {
        return joinOperator;
    }

    public PartialOrder getPartialOrder ()
    {
        return partialOrder;
    }
}
