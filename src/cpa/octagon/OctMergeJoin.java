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
package cpa.octagon;

import octagon.LibraryAccess;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

public class OctMergeJoin implements MergeOperator{

	private final OctDomain octDomain;

	public OctMergeJoin (OctDomain octDomain)
	{
		this.octDomain = octDomain;
	}

	public AbstractDomain getAbstractDomain() {
		return octDomain;
	}

	public AbstractElement merge(AbstractElement element1, AbstractElement element2, Precision prec) {
		OctElement octEl1 = (OctElement) element1;
		OctElement octEl2 = (OctElement) element2;

		int dim1 = LibraryAccess.getDim(octEl1);
		int dim2 = LibraryAccess.getDim(octEl2);

		// TODO recursive join should be handled gracefully here
		//octEl2.addVariablesFrom(octEl1);
		//System.out.println(octEl1.getNumberOfVars() + "{ }" + octEl2.getNumberOfVars());

		assert(dim1 == dim2);

		if(OctConstants.useWidening){
			OctConstants.useWidening = false;
			return LibraryAccess.widening(octEl2, octEl1);
		}
		else{
			System.out.println("Using UNION");
			return LibraryAccess.union(octEl2, octEl1);
		}
	}

  public AbstractElementWithLocation merge(AbstractElementWithLocation pElement1,
                                           AbstractElementWithLocation pElement2,
                                           Precision prec) throws CPAException {
    throw new CPAException ("Cannot return element with location information");
  }
}
