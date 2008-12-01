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
