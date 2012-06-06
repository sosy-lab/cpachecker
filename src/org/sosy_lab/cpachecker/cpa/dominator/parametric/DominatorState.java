/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;

public class DominatorState implements AbstractStateWithLocation, AbstractState {

	private AbstractState dominatedElement;
	private Set<AbstractState> dominators = new HashSet<AbstractState>();

	public DominatorState(AbstractState dominatedElement) {
		if (dominatedElement == null) {
			throw new IllegalArgumentException("dominatedElement is null!");
		}

		this.dominatedElement = dominatedElement;
	}

	public DominatorState(AbstractState dominatedElement, Set<AbstractState> dominators) {
		this(dominatedElement);

		if (dominators == null) {
			throw new IllegalArgumentException("dominators is null!");
		}

		this.dominators.addAll(dominators);
	}

	protected DominatorState() {
	  dominatedElement = null;
	}

	public DominatorState(DominatorState other) {
		this(other.dominatedElement, other.dominators);
	}

	public DominatorState(AbstractState dominatedElement, DominatorState other) {
		this(dominatedElement, other.dominators);
	}

	public void update(AbstractState dominator) {
		if (dominator == null) {
			throw new IllegalArgumentException("dominator is null!");
		}

		dominators.add(dominator);
	}

	public AbstractState getDominatedState() {
		return this.dominatedElement;
	}

	public Iterator<AbstractState> getIterator ()
    {
        return this.dominators.iterator();
    }

	public boolean isDominatedBy(AbstractState dominator) {
		return this.dominators.contains(dominator);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof DominatorState)) {
			return false;
		}

		DominatorState other_element = (DominatorState)other;

		if (!(this.dominatedElement.equals(other_element.dominatedElement))) {
			return false;
		}

		if (dominators.size() != other_element.dominators.size()) {
			return false;
		}

		for (AbstractState dominator : dominators) {
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
        for (AbstractState dominator : this.dominators) {
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

	@Override
  public CFANode getLocationNode() {
		return ((AbstractStateWithLocation)dominatedElement).getLocationNode();
	}

	@Override
	public int hashCode() {
		// TODO: create better hash code?
		return this.dominatedElement.hashCode();
	}
}
