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

import java.util.Iterator;
import java.util.List;

import cpa.common.interfaces.AbstractElement;

public class CompositeBottomElement extends CompositeElement
{

    public CompositeBottomElement (List<AbstractElement> bottoms)
    {
        super(bottoms,null);
    }

    @Override
    public boolean equals (Object o)
    {
        if (!(o instanceof CompositeBottomElement))
            return false;

        CompositeBottomElement otherComposite = (CompositeBottomElement) o;
        if (getElements().size () != otherComposite.getElements().size ())
            return false;

        Iterator<AbstractElement> iter = otherComposite.getElements().iterator();
        for (AbstractElement e : getElements()) {
          if (!e.equals(iter.next())) return false;
        }

        return true;
    }
    
    @Override
    public int hashCode() {
      return Integer.MIN_VALUE;
    }
}
