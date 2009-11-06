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
package cpa.itpabs.symbolic;

import java.util.Collection;
import java.util.Map;

import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.SymbolicFormula;

import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFANode;

import cpa.itpabs.ItpAbstractElement;
import common.Pair;
import cpa.symbpredabs.summary.SummaryCFANode;

/**
 * AbstractElement for the symbolic version (with summary locations) of the
 * interpolation-based lazy abstraction analysis
 * 
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN 
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpSymbolicAbstractElement extends ItpAbstractElement {
    // for each "leaf" node in the inner CFA of this summary, we keep the
    // symbolic representation of all the paths leading to the leaf
    private Map<CFANode, Pair<SymbolicFormula, SSAMap>> pathFormulas;

    public ItpSymbolicAbstractElement(CFANode loc) {
        super(loc);
    }

    @Override
    public String toString() {
        return "SE<" + Integer.toString(
                ((SummaryCFANode)getLocation()).getInnerNode().getNodeNumber())
                + ">(" + Integer.toString(getId()) + ",P=" +
                (getParent() != null ? getParent().getId() : "NIL") + ")";
    }

    @Override
    public boolean isError() {
        return (((SummaryCFANode)getLocation()).getInnerNode() instanceof
                CFAErrorNode);
    }

    public Pair<SymbolicFormula, SSAMap> getPathFormula(CFANode leaf) {
        return pathFormulas.get(leaf);
    }

    public void setPathFormulas(Map<CFANode, Pair<SymbolicFormula, SSAMap>> pf){
        pathFormulas = pf;
    }

    @Override
    public Collection<CFANode> getLeaves() {
        assert(pathFormulas != null);
        return pathFormulas.keySet();
    }

}
