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
package cpa.itpabs.explicit;

import java.util.Collection;
import java.util.Collections;

import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFANode;

import cpa.itpabs.ItpAbstractElement;

/**
 * AbstractElement for the Explicit-state version of the interpolation-based
 * lazy abstraction analysis
 * 
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN 
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpExplicitAbstractElement extends ItpAbstractElement {

    public ItpExplicitAbstractElement(CFANode loc) {
        super(loc);
    }

    @Override
    public String toString() {
        return "E<" + Integer.toString(
                getLocation().getNodeNumber()) + ">(" +
                Integer.toString(getId()) + ",P=" +
                (getParent() != null ? getParent().getId() : "NIL") + ")" + 
                (isCovered() ? "[C " + getCoveredBy() + "]" : "") + "\\n" +
                getAbstraction();
    }

    @Override
    public boolean isError() {
        return (getLocation() instanceof CFAErrorNode);
    }

    @Override
    public Collection<CFANode> getLeaves() {
        return Collections.singleton(getLocation());
    }

}
