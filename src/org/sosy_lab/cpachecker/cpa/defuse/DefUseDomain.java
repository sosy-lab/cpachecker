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
package org.sosy_lab.cpachecker.cpa.defuse;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.cpa.defuse.DefUseDefinition;
import org.sosy_lab.cpachecker.cpa.defuse.DefUseElement;

public class DefUseDomain implements AbstractDomain
{
    private static class DefUseTopElement extends DefUseElement
    {
      public DefUseTopElement ()
      {
          super (null);
      }
    }

    private static class DefUsePartialOrder implements PartialOrder
    {
        @Override
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
        @Override
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

    private final static DefUseTopElement topElement = new DefUseTopElement ();
    private final static PartialOrder partialOrder = new DefUsePartialOrder ();
    private final static JoinOperator joinOperator = new DefUseJoinOperator ();

    @Override
    public AbstractElement getTopElement ()
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
