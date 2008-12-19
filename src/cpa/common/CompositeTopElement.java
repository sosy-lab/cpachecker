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
    package cpa.common;

import java.util.List;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.TopElement;

public class CompositeTopElement implements TopElement
{
    private final List<AbstractElement> tops;

    public CompositeTopElement (List<AbstractElement> tops)
    {
        this.tops = tops;
    }

    @Override
    public boolean equals (Object o)
    {
        if (!(o instanceof CompositeBottomElement))
            return false;

        CompositeTopElement otherComposite = (CompositeTopElement) o;
        if (tops.size () != otherComposite.tops.size ())
            return false;

        for (int idx = 0; idx < tops.size (); idx++)
        {
            AbstractElement top1 = tops.get (idx);
            AbstractElement top2 = otherComposite.tops.get (idx);

            if (!top1.equals (top2))
                return false;
        }

        return true;
    }
    
    @Override
    public int hashCode() {
      return Integer.MAX_VALUE;
    }
}
