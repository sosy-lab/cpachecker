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
package cpa.symbpredabs.explicit;

import java.util.Stack;

import cfa.objectmodel.CFANode;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.symbpredabs.AbstractFormula;
import common.Pair;

/**
 * AbstractElement for explicit-state lazy abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ExplicitAbstractElement
        implements AbstractElementWithLocation {

    private int elemId;
    private CFANode location;
    private AbstractFormula abstraction;
    private ExplicitAbstractElement parent;

    private Stack<Pair<AbstractFormula, CFANode>> context;
    private boolean ownsContext;

    private boolean covered;
    private int mark;

    private static int nextAvailableId = 1;

    public int getId() { return elemId; }
    public CFANode getLocation() { return location; }
    public AbstractFormula getAbstraction() { return abstraction; }

    public void setAbstraction(AbstractFormula a) {
        abstraction = a;
    }

    public ExplicitAbstractElement getParent() { return parent; }
    public void setParent(ExplicitAbstractElement p) { parent = p; }

    public boolean isCovered() { return covered; }
    public void setCovered(boolean yes) { covered = yes; setMark(); }
    
    public boolean isMarked() { return mark > 0; }
    public void setMark() { mark = nextAvailableId++; }
    public int getMark() { return mark; }

    ExplicitAbstractElement(CFANode loc, AbstractFormula a,
            ExplicitAbstractElement p) {
        elemId = nextAvailableId++;
        location = loc;
        abstraction = a;
        parent = p;
        context = null;
        ownsContext = true;
        covered = false;
        mark = 0;
    }

    public ExplicitAbstractElement(CFANode loc) {
        this(loc, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ExplicitAbstractElement)) {
            return false;
        } else {
            return elemId == ((ExplicitAbstractElement)o).elemId;
        }
    }

    @Override
    public int hashCode() {
        return elemId;
    }

    @Override
    public String toString() {
        return "E<" + Integer.toString(
                location.getNodeNumber()) + ">(" +
                (isMarked() ? mark : getId()) + ",P=" +
                (parent != null ? parent.getId() : "NIL") + ")";
    }

    public CFANode getLocationNode() {
        return location;
    }

    public Stack<Pair<AbstractFormula, CFANode>> getContext()
    {
        return context;
    }

    public void setContext(Stack<Pair<AbstractFormula, CFANode>> ctx,
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

    public CFANode topContextLocation() {
        assert(context != null);
        assert(!context.empty());
        return context.peek().getSecond();
    }

    private void cloneContext() {
        // copy-on-write semantics: just duplicate the context and push
        // in the copy
        Stack<Pair<AbstractFormula, CFANode>> copy =
            new Stack<Pair<AbstractFormula, CFANode>>();
        for (Pair<AbstractFormula, CFANode> a : context) {
            copy.add(a);
        }
        context = copy;
        ownsContext = true;
    }

    public void pushContext(AbstractFormula af, CFANode returnLoc) {
        if (!ownsContext) {
            cloneContext();
        }
        context.push(new Pair<AbstractFormula, CFANode>(af, returnLoc));
    }

    public void popContext() {
        if (!ownsContext) {
            cloneContext();
        }
        context.pop();
    }

    public boolean sameContext(ExplicitAbstractElement e2) {
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

    public boolean isDescendant(ExplicitAbstractElement c) {
        ExplicitAbstractElement a = this;
        while (a != null) {
            if (a.equals(c)) return true;
            a = a.getParent();
        }
        return false;
    }
}
