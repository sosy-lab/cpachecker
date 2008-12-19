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

import java.util.Collection;
import java.util.List;

import cpa.common.CompositeDomain;
import cpa.common.CompositeElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class CompositeStopOperator implements StopOperator{

	private final CompositeDomain compositeDomain;
	private final List<StopOperator> stopOperators;

	public CompositeStopOperator (CompositeDomain compositeDomain, List<StopOperator> stopOperators)
	{
		this.compositeDomain = compositeDomain;
		this.stopOperators = stopOperators;
	}

	public <AE extends AbstractElement> boolean stop (AE element, Collection<AE> reached, Precision precision) throws CPAException
	{
		if (element.equals(compositeDomain.getBottomElement())) {
			return true;
		}

		CompositeElement comp1 = (CompositeElement) element;
		List<AbstractElement> comp1Elements = comp1.getElements ();

		if (comp1Elements.size () != stopOperators.size ())
			throw new CPAException ("Wrong number of stop operator");

		for(AbstractElement reachedElement:reached){
			if (stop (element, reachedElement)){
				return true;
			}
		}
		return false;
	}

	public boolean stop(AbstractElement element, AbstractElement reachedElement)
	throws CPAException {
		CompositeElement compositeElement1 = (CompositeElement) element;
		CompositeElement compositeElement2 = (CompositeElement) reachedElement;

		List<AbstractElement> compositeElements1 = compositeElement1.getElements ();
		List<AbstractElement> compositeElements2 = compositeElement2.getElements ();

		for (int idx = 0; idx < compositeElements1.size (); idx++)
		{
			StopOperator stopOp = stopOperators.get(idx);
			AbstractElement absElem1 = compositeElements1.get(idx);
			AbstractElement absElem2 = compositeElements2.get(idx);
			if (!stopOp.stop(absElem1, absElem2))
				return false;
		}
		return true;
	}
}
