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

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFAEdge;

import exceptions.CPATransferException;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;

/**
 * @author holzera
 *
 */
public class DominatorTransferRelation implements TransferRelation {

	private final DominatorDomain domain;
	private final ConfigurableProgramAnalysis cpa;

	public DominatorTransferRelation(DominatorDomain domain, ConfigurableProgramAnalysis cpa) {
		if (domain == null) {
			throw new IllegalArgumentException("domain is null!");
		}

		if (cpa == null) {
			throw new IllegalArgumentException("cpa is null!");
		}

		this.domain = domain;
		this.cpa = cpa;
	}

	/* (non-Javadoc)
	 * @see cpa.common.interfaces.TransferRelation#getAbstractSuccessor(cpa.common.interfaces.AbstractElement, cfa.objectmodel.CFAEdge)
	 */
	public AbstractElement getAbstractSuccessor(AbstractElement element, CFAEdge cfaEdge, Precision prec) throws CPATransferException {
		if (!(element instanceof DominatorElement)) {
			return this.domain.getBottomElement();
		}

		DominatorElement dominatorElement = (DominatorElement)element;

		AbstractElement successorOfDominatedElement_tmp = this.cpa.getTransferRelation().getAbstractSuccessor(dominatorElement.getDominatedElement(), cfaEdge, prec);

		// TODO: make this nicer
		AbstractElementWithLocation successorOfDominatedElement = (AbstractElementWithLocation)successorOfDominatedElement_tmp;

		if (successorOfDominatedElement.equals(this.cpa.getAbstractDomain().getBottomElement())) {
			return this.domain.getBottomElement();
		}

		if (successorOfDominatedElement.equals(this.cpa.getAbstractDomain().getTopElement())) {
			return this.domain.getTopElement();
		}

		DominatorElement successor = new DominatorElement(successorOfDominatedElement, dominatorElement);

		successor.update(successorOfDominatedElement);

		return successor;
	}

	/* (non-Javadoc)
	 * @see cpa.common.interfaces.TransferRelation#getAllAbstractSuccessors(cpa.common.interfaces.AbstractElement)
	 */
	public List<AbstractElementWithLocation> getAllAbstractSuccessors(
	    AbstractElementWithLocation element, Precision prec) throws CPAException, CPATransferException {
		List<AbstractElementWithLocation> successors = new ArrayList<AbstractElementWithLocation>();

		if (element instanceof DominatorElement) {
			DominatorElement dominatorElement = (DominatorElement)element;

			List<AbstractElementWithLocation> successorsOfDominatedElement = this.cpa.getTransferRelation().getAllAbstractSuccessors(dominatorElement.getDominatedElement(), prec);

			for (AbstractElementWithLocation successorOfDominatedElement : successorsOfDominatedElement) {

				if (successorOfDominatedElement.equals(this.cpa.getAbstractDomain().getBottomElement())) {
					successors.add(this.domain.getBottomElement());

					continue;
				}

				if (successorOfDominatedElement.equals(this.cpa.getAbstractDomain().getTopElement())) {
					successors.add(this.domain.getTopElement());

					continue;
				}

				DominatorElement successor = new DominatorElement(successorOfDominatedElement, dominatorElement);

				successor.update(successorOfDominatedElement);

				successors.add(successor);
			}
		}

		return successors;
	}

  @Override
  public void strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {    
  }
}
