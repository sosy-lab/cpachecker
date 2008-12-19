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

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;

/**
 * @author holzera
 *
 */
public class DominatorMerge implements MergeOperator {
	private DominatorDomain domain = null;

	public DominatorMerge(DominatorDomain domain) {
		if (domain == null) {
			throw new IllegalArgumentException("domain is null!");
		}

		this.domain = domain;
	}

	/* (non-Javadoc)
	 * @see cpa.common.interfaces.MergeOperator#merge(cpa.common.interfaces.AbstractElement, cpa.common.interfaces.AbstractElement)
	 */
	public AbstractElement merge(AbstractElement element1,
			AbstractElement element2, Precision prec) {
			AbstractElement joinedElement = this.domain.join(element1, element2);

			if (joinedElement.equals(this.domain.getTopElement())) {
				return element2;
			}
			else {
				return joinedElement;
			}
	}

	public AbstractElementWithLocation merge(AbstractElementWithLocation element1,
	                             AbstractElementWithLocation element2, Precision prec) {
	    AbstractElementWithLocation joinedElement = this.domain.join(element1, element2);

	    if (joinedElement.equals(this.domain.getTopElement())) {
	      return element2;
	    }
	    else {
	      return joinedElement;
	    }
	}

}
