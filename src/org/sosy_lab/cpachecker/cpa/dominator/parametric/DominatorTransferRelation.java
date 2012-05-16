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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class DominatorTransferRelation implements TransferRelation {

	private final ConfigurableProgramAnalysis cpa;

	public DominatorTransferRelation(ConfigurableProgramAnalysis cpa) {
		if (cpa == null) {
			throw new IllegalArgumentException("cpa is null!");
		}

		this.cpa = cpa;
	}

	@Override
	public Collection<DominatorElement> getAbstractSuccessors(
	    AbstractElement element, Precision prec, CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

	  assert element instanceof DominatorElement;

    DominatorElement dominatorElement = (DominatorElement)element;

    Collection<? extends AbstractElement> successorsOfDominatedElement = this.cpa.getTransferRelation().getAbstractSuccessors(dominatorElement.getDominatedElement(), prec, cfaEdge);

    Collection<DominatorElement> successors = new ArrayList<DominatorElement>(successorsOfDominatedElement.size());
    for (AbstractElement successorOfDominatedElement : successorsOfDominatedElement) {
      DominatorElement successor = new DominatorElement(successorOfDominatedElement, dominatorElement);
      successor.update(successorOfDominatedElement);
      successors.add(successor);
    }

    return successors;
	}

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {
    return null;
  }
}
