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
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

public class DefUseMergeJoin implements MergeOperator
{
    private final DefUseDomain defUseDomain;

    public DefUseMergeJoin (DefUseDomain defUseDomain)
    {
        this.defUseDomain = defUseDomain;
    }

    public AbstractDomain getAbstractDomain ()
    {
        return defUseDomain;
    }

    public AbstractElement merge (AbstractElement element1, AbstractElement element2, Precision prec)
    {
        DefUseElement defUseElement1 = (DefUseElement) element1;
        DefUseElement defUseElement2 = (DefUseElement) element2;

        List<DefUseDefinition> mergedDefinitions = new ArrayList<DefUseDefinition> ();
        for (int defIdx = 0; defIdx < defUseElement1.getNumDefinitions (); defIdx++)
            mergedDefinitions.add (defUseElement1.getDefinition (defIdx));

        for (int defIdx = 0; defIdx < defUseElement2.getNumDefinitions (); defIdx++)
        {
            DefUseDefinition def = defUseElement2.getDefinition (defIdx);
            if (!mergedDefinitions.contains (def))
                mergedDefinitions.add (def);
        }

        return new DefUseElement (mergedDefinitions);
    }

    public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                             AbstractElementWithLocation pElement2,
                                             Precision prec) throws CPAException {
      throw new CPAException ("Cannot return element with location information");
    }
}
