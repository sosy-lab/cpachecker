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
package cpa.predicateabstraction;

import java.io.IOException;

import predicateabstraction.MathSatWrapper;
import predicateabstraction.Predicate;
import predicateabstraction.ThreeValuedBoolean;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;

public class PredicateAbstractionDomain implements AbstractDomain{

    private static class PredicateAbstractionBottomElement extends PredicateAbstractionElement
    {
        public PredicateAbstractionBottomElement ()
        {
            super (null, true);
        }
    }

    private static class PredicateAbstractionTopElement extends PredicateAbstractionElement
    {
    	public PredicateAbstractionTopElement ()
        {
    	  super (null, false);
        }
    }

    private static class PredicateAbstractionPartialOrder implements PartialOrder
    {
    	// returns true if element1 < element2 on lattice
        public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
        {
        	PredicateAbstractionElement predicateAbstractionElement1 = (PredicateAbstractionElement) element1;
        	PredicateAbstractionElement predicateAbstractionElement2 = (PredicateAbstractionElement) element2;

        	ThreeValuedBoolean tbv = ThreeValuedBoolean.DONTKNOW;
			try {
				tbv = MathSatWrapper.implies(predicateAbstractionElement1.getRegion(), predicateAbstractionElement2.getRegion());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            if(tbv == ThreeValuedBoolean.TRUE){
            	return true;
            }
            else{
            	return false;
            }
        }
    }

    private static class PredicateAbstractionJoinOperator implements JoinOperator
    {
        public AbstractElement join (AbstractElement element1, AbstractElement element2)
        {
            PredicateAbstractionElement predAbstElement1 = (PredicateAbstractionElement) element1;
            PredicateAbstractionElement predAbstElement2 = (PredicateAbstractionElement) element2;

            if(predAbstElement1.getPredicateList().size() !=
            	predAbstElement2.getPredicateList().size()){
            	// TODO handle this case
            }

            PredicateAbstractionElement joined = new PredicateAbstractionElement(predAbstElement1.getPredicateList(), false);

            for(Predicate pred1:predAbstElement1.getPredicateList().getPredicates()){
            	Predicate pred2 = predAbstElement2.getPredicateList().getPredicate(pred1);
            	Predicate joinedPred = joined.getPredicateList().getPredicate(pred1);
            	if(pred1.getTruthValue() == pred2.getTruthValue()){
            		joinedPred.setTruthValue(pred1.getTruthValue());
            	}
            	else{
            		joinedPred.setTruthValue(ThreeValuedBoolean.DONTKNOW);
            	}
            }
            return joined;
        }
    }

    private final static PredicateAbstractionBottomElement bottomElement = new PredicateAbstractionBottomElement ();
    private final static PredicateAbstractionTopElement topElement = new PredicateAbstractionTopElement ();
    private final static PartialOrder partialOrder = new PredicateAbstractionPartialOrder ();
    private final static JoinOperator joinOperator = new PredicateAbstractionJoinOperator ();

    public PredicateAbstractionDomain ()
    {

    }

    public AbstractElement getBottomElement ()
    {
        return bottomElement;
    }

	public boolean isBottomElement(AbstractElement element) {

		PredicateAbstractionElement predAbsElem = (PredicateAbstractionElement) element;

		if(predAbsElem.equals(bottomElement)){
			return true;
		}

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
