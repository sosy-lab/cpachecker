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

import logging.LazyLogger;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.ReturnEdge;
import cpa.itpabs.ItpAbstractElement;
import cpa.itpabs.ItpAbstractElementManager;
import cpa.itpabs.ItpCPA;
import cpa.itpabs.ItpCPAStatistics;
import cpaplugin.CPAStatistics;
import cpa.itpabs.explicit.ItpExplicitAbstractElement;

/**
 * Explicit-state version of the interpolation-based lazy abstraction
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpExplicitCPA extends ItpCPA {

    class ItpExplicitAbstractElementCreator
        implements ItpAbstractElementManager {
        @Override
        public ItpAbstractElement create(CFANode location) {
            return new ItpExplicitAbstractElement(location);
        }

        @Override
        public boolean isFunctionEnd(ItpAbstractElement e,
                                     ItpAbstractElement succ) {
            CFANode n = e.getLocation();
            return (n.getNumLeavingEdges() > 0 &&
                    n.getLeavingEdge(0) instanceof ReturnEdge);
        }

        @Override
        public boolean isFunctionStart(ItpAbstractElement e) {
            return (e.getLocation() instanceof FunctionDefinitionNode);
        }

        @Override
        public boolean isRightEdge(ItpAbstractElement e, CFAEdge edge,
                ItpAbstractElement succ) {
            if (isFunctionEnd(e, succ)) {
                CFANode retNode = e.topContextLocation();
                if (!succ.getLocation().equals(retNode)) {
                    LazyLogger.log(LazyLogger.DEBUG_1,
                            "Return node for this call is: ", retNode,
                            ", but edge leads to: ", succ.getLocation());
                    return false;
                }
            }
            return true;
        }

        @Override
        public void pushContextFindRetNode(ItpAbstractElement e,
                ItpAbstractElement succ) {
            assert(e.getLocation().getLeavingSummaryEdge() != null);
            CFANode retNode =
                e.getLocation().getLeavingSummaryEdge().getSuccessor();
            succ.pushContext(succ.getAbstraction(), retNode);
        }
    }

    private ItpExplicitAbstractElementCreator elemCreator;
    private ItpCPAStatistics stats;

    private ItpExplicitCPA() {
        super();
        elemCreator = new ItpExplicitAbstractElementCreator();
        stats = new ItpCPAStatistics(this,
                "Explicit-State Interpolation-based Lazy Abstraction");
    }

    /**
     * Constructor conforming to the "contract" in CompositeCPA. The two
     * arguments are ignored
     * @param s1
     * @param s2
     */
    public ItpExplicitCPA(String s1, String s2) {
        this();
    }

    public CPAStatistics getStatistics() {
        return stats;
    }

    @Override
    public ItpAbstractElementManager getElementCreator() {
        return elemCreator;
    }
}
