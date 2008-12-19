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
/**
 *
 */
package cpa.dominator.parametric;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.dominator.parametric.DominatorElement;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import cfa.objectmodel.CFANode;

/**
 * @author holzera
 *
 */
public class DominatorElement implements AbstractElementWithLocation {

	private AbstractElementWithLocation dominatedElement;
	private Set<AbstractElementWithLocation> dominators = new HashSet<AbstractElementWithLocation>();

	public DominatorElement(AbstractElementWithLocation dominatedElement) {
		if (dominatedElement == null) {
			throw new IllegalArgumentException("dominatedElement is null!");
		}

		this.dominatedElement = dominatedElement;
	}

	public DominatorElement(AbstractElementWithLocation dominatedElement, Set<AbstractElementWithLocation> dominators) {
		this(dominatedElement);

		if (dominators == null) {
			throw new IllegalArgumentException("dominators is null!");
		}

		this.dominators.addAll(dominators);
	}

	public DominatorElement(DominatorElement other) {
		this(other.dominatedElement, other.dominators);
	}

	public DominatorElement(AbstractElementWithLocation dominatedElement, DominatorElement other) {
		this(dominatedElement, other.dominators);
	}

	@Override
	public DominatorElement clone()
    {
        return new DominatorElement(this);
    }

	public void update(AbstractElementWithLocation dominator) {
		if (dominator == null) {
			throw new IllegalArgumentException("dominator is null!");
		}

		dominators.add(dominator);
	}

	public AbstractElementWithLocation getDominatedElement() {
		return this.dominatedElement;
	}

	public Iterator<AbstractElementWithLocation> getIterator ()
    {
        return this.dominators.iterator();
    }

	public boolean isDominatedBy(AbstractElementWithLocation dominator) {
		return this.dominators.contains(dominator);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof DominatorElement)) {
			return false;
		}

		DominatorElement other_element = (DominatorElement)other;

		if (!(this.dominatedElement.equals(other_element.dominatedElement))) {
			return false;
		}

		if (dominators.size() != other_element.dominators.size()) {
			return false;
		}

		for (AbstractElementWithLocation dominator : dominators) {
			if (!other_element.isDominatedBy(dominator)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder ();
        builder.append ("( " + this.dominatedElement.toString() + ", {");

        boolean first = true;
        for (AbstractElementWithLocation dominator : this.dominators) {
        	if (first)  {
        		first = false;
        	}
        	else {
        		builder.append(", ");
        	}

        	builder.append(dominator.toString());
        }

        builder.append ("})");

        return builder.toString ();
	}

	public CFANode getLocationNode() {
		return this.dominatedElement.getLocationNode();
	}

	@Override
	public int hashCode() {
		// TODO: create better hash code?
		return this.dominatedElement.hashCode();
	}
}
