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
package cpa.symbpredabs.summary;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.symbpredabs.AbstractFormulaManager;
import exceptions.CPAException;


/**
 * Abstract domain for Symbolic lazy abstraction with summaries.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryAbstractDomain implements AbstractDomain {

    private final SummaryCPA cpa;

    public SummaryAbstractDomain(SummaryCPA cpa) {
        this.cpa = cpa;
    }

    private final static class SummaryBottomElement extends SummaryAbstractElement {
      public SummaryBottomElement() {
        super(null);
        // TODO Auto-generated constructor stub
      }

        @Override
    	public String toString() { return "<BOTTOM>"; }
    }
    private final static class SummaryTopElement extends SummaryAbstractElement {
      public SummaryTopElement() {
        super(null);
        // TODO Auto-generated constructor stub
      }
    }

    private final static class SummaryJoinOperator implements JoinOperator {
        public AbstractElement join(AbstractElement element1,
                AbstractElement element2) throws CPAException {
            throw new CPAException("Can't join summaries!");
        }
    }

    private final class SummaryPartialOrder implements PartialOrder {
        public boolean satisfiesPartialOrder(AbstractElement element1,
                AbstractElement element2) throws CPAException {
            SummaryAbstractElement e1 = (SummaryAbstractElement)element1;
            SummaryAbstractElement e2 = (SummaryAbstractElement)element2;
            
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
                AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
                return amgr.entails(e1.getAbstraction(), e2.getAbstraction());
            }
            return false;
        }
    }

    private final static SummaryBottomElement bottom = new SummaryBottomElement();
    private final static SummaryTopElement top = new SummaryTopElement();
    private final static JoinOperator join = new SummaryJoinOperator();
    private final PartialOrder partial = new SummaryPartialOrder();

    public SummaryAbstractElement getBottomElement() {
        return bottom;
    }

    public JoinOperator getJoinOperator() {
        return join;
    }

    public PartialOrder getPartialOrder() {
        return partial;
    }

    public SummaryAbstractElement getTopElement() {
        return top;
    }

    public SummaryCPA getCPA() {
        return cpa;
    }

}
