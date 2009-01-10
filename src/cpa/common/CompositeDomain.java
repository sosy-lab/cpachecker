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

import java.util.ArrayList;
import java.util.List;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;

public class CompositeDomain implements AbstractDomain
{
    private final List<AbstractDomain> domains;

    private final CompositeBottomElement bottomElement;
    private final CompositeTopElement topElement;
    private final CompositeJoinOperator joinOperator;
    private final CompositePartialOrder partialOrder;

    public CompositeDomain (List<AbstractDomain> domains)
    {
        this.domains = domains;

        List<AbstractElement> bottoms = new ArrayList<AbstractElement> ();
        List<AbstractElement> tops = new ArrayList<AbstractElement> ();
        List<JoinOperator> joinOperators = new ArrayList<JoinOperator> ();
        List<PartialOrder> partialOrders = new ArrayList<PartialOrder> ();

        for (AbstractDomain domain : domains)
        {
            bottoms.add (domain.getBottomElement ());
            tops.add (domain.getTopElement ());
            joinOperators.add (domain.getJoinOperator ());
            partialOrders.add (domain.getPartialOrder ());
        }

        this.bottomElement = new CompositeBottomElement (bottoms);
        this.topElement = new CompositeTopElement (tops);
        this.joinOperator = new CompositeJoinOperator (joinOperators);
        this.partialOrder = new CompositePartialOrder (partialOrders);
    }
    
    public boolean isBottomElement(AbstractElement element){
      CompositeElement compositeElem = (CompositeElement)element;
      
      if(compositeElem instanceof CompositeBottomElement){
        return true;
      }

      for(int i=0; i<domains.size(); i++){
        AbstractDomain absDomain = domains.get(i);
        AbstractElement absElem1 = compositeElem.get(i);
        if(absDomain.isBottomElement(absElem1)){
          return true;
        }
      }
      return false;
      
    }

    public List<AbstractDomain> getDomains ()
    {
        return domains;
    }

    public AbstractElement getBottomElement ()
    {
        return bottomElement;
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
