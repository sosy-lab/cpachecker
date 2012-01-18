/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.dominator.parametric;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class DominatorDomain implements AbstractDomain {

	private final ConfigurableProgramAnalysis cpa;

	public DominatorDomain(ConfigurableProgramAnalysis cpa) {
		this.cpa = cpa;
	}

	private static class DominatorTopElement extends DominatorElement
    {

        @Override
        public String toString() {
        	return "\\bot";
        }

        @Override
        public boolean equals(Object o) {
        	return (o instanceof DominatorTopElement);
        }

        @Override
        public int hashCode() {
        	return Integer.MIN_VALUE;
        }

        @Override
        public CFANode getLocationNode() {
          // TODO Auto-generated method stub
          return null;
        }
    }

    private final static DominatorTopElement topElement = new DominatorTopElement();

    @Override
    public boolean isLessOrEqual(AbstractElement element1, AbstractElement element2) throws CPAException
    {
        if (element1.equals(element2)) {
          return true;
        }

        if (element2.equals(topElement)) {
          return true;
        }

        if (element1 instanceof DominatorElement && element2 instanceof DominatorElement) {
        	DominatorElement dominatorElement1 = (DominatorElement)element1;
        	DominatorElement dominatorElement2 = (DominatorElement)element2;

        	if (this.cpa.getAbstractDomain().isLessOrEqual(dominatorElement1.getDominatedElement(), dominatorElement2.getDominatedElement())) {
        		Iterator<AbstractElement> dominatorIterator = dominatorElement2.getIterator();

        		while (dominatorIterator.hasNext()) {
        		  AbstractElement dominator = dominatorIterator.next();

        			if (!dominatorElement1.isDominatedBy(dominator)) {
        				return false;
        			}
        		}

        		return true;
        	}
        }

        return false;
    }

    @Override
    public AbstractElement join(AbstractElement element1, AbstractElement element2) {
      if (!(element1 instanceof DominatorElement)) {
        throw new IllegalArgumentException(
            "element1 is not a DominatorElement!");
      }

      if (!(element2 instanceof DominatorElement)) {
        throw new IllegalArgumentException(
            "element2 is not a DominatorElement!");
      }

      DominatorElement dominatorElement1 = (DominatorElement) element1;
      DominatorElement dominatorElement2 = (DominatorElement) element2;

		if (element1.equals(topElement)) {
			return dominatorElement1;
		}

		if (element2.equals(topElement)) {
			return dominatorElement2;
		}

		if (!dominatorElement1.getDominatedElement().equals(dominatorElement2.getDominatedElement())) {
			return topElement;
		}

		Set<AbstractElement> intersectingDominators = new HashSet<AbstractElement>();

		Iterator<AbstractElement> dominatorIterator = dominatorElement1.getIterator();

		while (dominatorIterator.hasNext()) {
		  AbstractElement dominator = dominatorIterator.next();

			if (dominatorElement2.isDominatedBy(dominator)) {
				intersectingDominators.add(dominator);
			}
		}

		DominatorElement result = new DominatorElement(dominatorElement1.getDominatedElement(), intersectingDominators);

		result.update(dominatorElement1.getDominatedElement());

		return result;
	}
}
