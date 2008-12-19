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
import cpa.common.interfaces.BottomElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.TopElement;

public class OctDomain implements AbstractDomain{

	private static class OctBottomElement extends OctElement implements BottomElement
    {
        public OctBottomElement ()
        {
        	super ();
        }
    }

    private static class OctTopElement extends OctElement implements TopElement
    {
    	public OctTopElement ()
        {
            //super (LibraryAccess.universe(Variables.numOfVars));
        }
    }

    private static class OctPartialOrder implements PartialOrder
    {
        public boolean satisfiesPartialOrder (AbstractElement element1, AbstractElement element2)
        {
            OctElement octElement1 = (OctElement) element1;
            OctElement octElement2 = (OctElement) element2;

            boolean b = LibraryAccess.isIn(octElement1, octElement2);
            return b;
        }
    }

    private static class OctJoinOperator implements JoinOperator
    {
        public AbstractElement join (AbstractElement element1, AbstractElement element2)
        {
        	// TODO fix
        	OctElement octEl1 = (OctElement) element1;
    		OctElement octEl2 = (OctElement) element2;
    		return LibraryAccess.widening(octEl1, octEl2);
        }
    }

    private final static BottomElement bottomElement = new OctBottomElement ();
    private final static TopElement topElement = new OctTopElement ();
    private final static PartialOrder partialOrder = new OctPartialOrder ();
    private final static JoinOperator joinOperator = new OctJoinOperator ();

    public OctDomain ()
    {

    }

    public AbstractElement getBottomElement ()
    {
        return bottomElement;
    }

    //TODO test this
	public boolean isBottomElement(AbstractElement element) {
		OctElement octElem = (OctElement) element;
		return octElem.isEmpty();
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
