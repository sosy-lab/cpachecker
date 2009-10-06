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
package cpa.symbpredabs.summary;

import java.util.Collection;
import java.util.Map;
import java.util.Stack;

import cfa.objectmodel.CFANode;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.symbpredabs.AbstractFormula;
import common.Pair;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;

/**
 * AbstractElement for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryAbstractElement
        implements AbstractElementWithLocation {

    private int elemId;
    private SummaryCFANode summaryLocation;
    // for each "leaf" node in the inner CFA of this summary, we keep the
    // symbolic representation of all the paths leading to the leaf
    private Map<CFANode, Pair<SymbolicFormula, SSAMap>> pathFormulas;
    private AbstractFormula abstraction;
    private SummaryAbstractElement parent;

    // context is used to deal with function calls/returns
    private Stack<Pair<AbstractFormula, SummaryCFANode>> context;
    private boolean ownsContext;

    private boolean covered;
    private int mark;    
    
    private static int nextAvailableId = 1;

    public int getId() { return elemId; }
    public SummaryCFANode getLocation() { return summaryLocation; }
    public Pair<SymbolicFormula, SSAMap> getPathFormula(CFANode leaf) {
        return pathFormulas.get(leaf);
    }
    public AbstractFormula getAbstraction() { return abstraction; }

    public void setAbstraction(AbstractFormula a) {
        abstraction = a;
    }
    public void setPathFormulas(Map<CFANode, Pair<SymbolicFormula, SSAMap>> pf){
        pathFormulas = pf;
    }
    
    public boolean isCovered() { return covered; }
    public void setCovered(boolean yes) { covered = yes; setMark(); }
    
    public boolean isMarked() { return mark > 0; }
    public void setMark() { mark = nextAvailableId++; }
    public int getMark() { return mark; }    

    public SummaryAbstractElement getParent() { return parent; }
    public void setParent(SummaryAbstractElement p) { parent = p; }

    private SummaryAbstractElement(SummaryCFANode loc, AbstractFormula a,
            Map<CFANode, Pair<SymbolicFormula, SSAMap>> pf,
            SummaryAbstractElement p) {
        elemId = nextAvailableId++;
        summaryLocation = loc;
        abstraction = a;
        pathFormulas = pf;
        parent = p;
        context = null;
        ownsContext = true;
        covered = false;
        mark = 0;
    }

    public SummaryAbstractElement(SummaryCFANode loc) {
        this(loc, null, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof SummaryAbstractElement)) {
            return false;
        } else {
            return elemId == ((SummaryAbstractElement)o).elemId;
        }
    }

    @Override
    public int hashCode() {
        return elemId;
    }

    @Override
    public String toString() {
        return "SE<" + Integer.toString(
                summaryLocation.getInnerNode().getNodeNumber()) + ">";//">(" +
                //Integer.toString(getId()) + ")";
    }

    public CFANode getLocationNode() {
        return (CFANode)summaryLocation;
    }

    public Collection<CFANode> getLeaves() {
        assert(pathFormulas != null);

        return pathFormulas.keySet();
    }

    public Stack<Pair<AbstractFormula, SummaryCFANode>> getContext()
    {
        return context;
    }

    public void setContext(Stack<Pair<AbstractFormula, SummaryCFANode>> ctx,
                           boolean owns)
    {
        context = ctx;
        ownsContext = owns;
    }

    public AbstractFormula topContextAbstraction() {
        assert(context != null);
        assert(!context.empty());
        return context.peek().getFirst();
    }

    public SummaryCFANode topContextLocation() {
        assert(context != null);
        assert(!context.empty());
        return context.peek().getSecond();
    }

    private void cloneContext() {
        // copy-on-write semantics: just duplicate the context and push
        // in the copy
        Stack<Pair<AbstractFormula, SummaryCFANode>> copy =
            new Stack<Pair<AbstractFormula, SummaryCFANode>>();
        for (Pair<AbstractFormula, SummaryCFANode> a : context) {
            copy.add(a);
        }
        context = copy;
        ownsContext = true;
    }

    public void pushContext(AbstractFormula af, SummaryCFANode returnLoc) {
        if (!ownsContext) {
            cloneContext();
        }
        context.push(new Pair<AbstractFormula, SummaryCFANode>(af, returnLoc));
    }

    public void popContext() {
        if (!ownsContext) {
            cloneContext();
        }
        context.pop();
    }

    public boolean sameContext(SummaryAbstractElement e2) {
        assert(context != null && e2.context != null);

        if (context == e2.context) {
            return true;
        } else if (context.size() != e2.context.size()) {
            return false;
        } else {
            for (int i = 0; i < context.size(); ++i) {
                if (!context.elementAt(i).equals(e2.context.elementAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isDescendant(SummaryAbstractElement c) {
        SummaryAbstractElement a = this;
        while (a != null) {
            if (a.equals(c)) return true;
            a = a.getParent();
        }
        return false;
    }

    @Override
    public boolean isError() {
      return false;
    }
}
