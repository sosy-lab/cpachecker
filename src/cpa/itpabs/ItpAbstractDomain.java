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
package cpa.itpabs;

import java.util.Collection;

import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.symbpredabs.SymbolicFormulaManager;
import exceptions.CPAException;

/**
 * Abstract domain for interpolation-based lazy abstraction
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpAbstractDomain implements AbstractDomain {

    private final ItpCPA cpa;

    public ItpAbstractDomain(ItpCPA cpa) {
        this.cpa = cpa;
    }

    private final static class ExplicitBottomElement extends ItpAbstractElement {
      public ExplicitBottomElement() {
        super(null);
        // TODO Auto-generated constructor stub
      }

        @Override
    	public String toString() { return "<BOTTOM>"; }

        @Override
        public Collection<CFANode> getLeaves() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public boolean isErrorLocation() {
          // TODO Auto-generated method stub
          return false;
        }
    }
    private final static class ExplicitTopElement extends ItpAbstractElement {

      public ExplicitTopElement() {
        super(null);
        // TODO Auto-generated constructor stub
      }

      @Override
      public Collection<CFANode> getLeaves() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean isErrorLocation() {
        // TODO Auto-generated method stub
        return false;
      }

    }

    private final static class ExplicitJoinOperator implements JoinOperator {
        public AbstractElement join(AbstractElement element1,
                AbstractElement element2) throws CPAException {
            return top;
        }
    }

    private final class ExplicitPartialOrder implements PartialOrder {
        public boolean satisfiesPartialOrder(AbstractElement element1,
                AbstractElement element2) throws CPAException {
            ItpAbstractElement e1 = (ItpAbstractElement)element1;
            ItpAbstractElement e2 = (ItpAbstractElement)element2;
            
            if (e1 == bottom) {
              return true;
            } else if (e2 == top) {
              return true;
            } else if (e2 == bottom) {
              return false;
            } else if (e1 == top) {
              return false;
            }

            assert(e1.getAbstraction() != null);
            assert(e2.getAbstraction() != null);

            if (e1.getLocation().equals(e2.getLocation())) {
                SymbolicFormulaManager mgr = cpa.getFormulaManager();
                return mgr.entails(e1.getAbstraction(), e2.getAbstraction());
            }
            return false;
        }
    }

    private final static ExplicitBottomElement bottom = new ExplicitBottomElement();
    private final static ExplicitTopElement top = new ExplicitTopElement();
    private final static JoinOperator join = new ExplicitJoinOperator();
    private final PartialOrder partial = new ExplicitPartialOrder();

    public ItpAbstractElement getBottomElement() {
        return bottom;
    }

    public JoinOperator getJoinOperator() {
        return join;
    }

    public PartialOrder getPartialOrder() {
        return partial;
    }

    public ItpAbstractElement getTopElement() {
        return top;
    }

    public ItpCPA getCPA() {
        return cpa;
    }

}
