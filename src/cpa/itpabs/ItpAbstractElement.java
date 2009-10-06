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
package cpa.itpabs;

import java.util.Collection;
import java.util.Stack;

import cfa.objectmodel.CFANode;

import common.Pair;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.symbpredabs.SymbolicFormula;

/**
 * Abstract element for interpolation-based lazy abstraction
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public abstract class ItpAbstractElement
        implements AbstractElementWithLocation,
        Comparable<ItpAbstractElement> {

    private final int elemId;
    private final CFANode location;
    private SymbolicFormula abstraction;
    private ItpAbstractElement parent;
    private ItpAbstractElement coveredBy;

    private Stack<Pair<SymbolicFormula, CFANode>> context;
    private boolean ownsContext;

    private static int nextAvailableId = 1;

    public int getId() { return elemId; }
    public CFANode getLocation() { return location; }
    public SymbolicFormula getAbstraction() { return abstraction; }

    public void setAbstraction(SymbolicFormula a) {
        abstraction = a;
    }

    public ItpAbstractElement getParent() { return parent; }
    public void setParent(ItpAbstractElement p) { parent = p; }

    private ItpAbstractElement(CFANode loc, SymbolicFormula a,
            ItpAbstractElement p) {
        elemId = nextAvailableId++;
        location = loc;
        abstraction = a;
        parent = p;
        context = null;
        ownsContext = true;
        coveredBy = null;
    }

    public ItpAbstractElement(CFANode loc) {
        this(loc, null, null);
    }

    public boolean isCovered() { return coveredBy != null; }
    public ItpAbstractElement getCoveredBy() { return coveredBy; }
    public void setCoveredBy(ItpAbstractElement e) { coveredBy = e; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ItpAbstractElement)) {
            return false;
        } else {
            return elemId == ((ItpAbstractElement)o).elemId;
        }
    }

    @Override
    public int hashCode() {
        return elemId;
    }

    public CFANode getLocationNode() {
        return location;
    }

    public Stack<Pair<SymbolicFormula, CFANode>> getContext()
    {
        return context;
    }

    public void setContext(Stack<Pair<SymbolicFormula, CFANode>> ctx,
                           boolean owns)
    {
        context = ctx;
        ownsContext = owns;
    }

    public SymbolicFormula topContextAbstraction() {
        assert(context != null);
        assert(!context.empty());
        return context.peek().getFirst();
    }

    public CFANode topContextLocation() {
        assert(context != null);
        assert(!context.empty());
        return context.peek().getSecond();
    }

    private void cloneContext() {
        // copy-on-write semantics: just duplicate the context and push
        // in the copy
        Stack<Pair<SymbolicFormula, CFANode>> copy =
            new Stack<Pair<SymbolicFormula, CFANode>>();
        for (Pair<SymbolicFormula, CFANode> a : context) {
            copy.add(a);
        }
        context = copy;
        ownsContext = true;
    }

    public void pushContext(SymbolicFormula af, CFANode returnLoc) {
        if (!ownsContext) {
            cloneContext();
        }
        context.push(new Pair<SymbolicFormula, CFANode>(af, returnLoc));
    }

    public void popContext() {
        if (!ownsContext) {
            cloneContext();
        }
        context.pop();
    }

    public boolean sameContext(ItpAbstractElement e2) {
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

    public boolean isDescendant(ItpAbstractElement c) {
        ItpAbstractElement a = this;
        while (a != null) {
            if (a.equals(c)) return true;
            a = a.getParent();
        }
        return false;
    }

    @Override
    public int compareTo(ItpAbstractElement o) {
        return getId() - o.getId();
    }

    public abstract Collection<CFANode> getLeaves();

}
