package cpaplugin.cpa.cpas.itpabs.explicit;

import cpaplugin.CPAStatistics;
import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cpa.cpas.itpabs.ItpAbstractElement;
import cpaplugin.cpa.cpas.itpabs.ItpAbstractElementManager;
import cpaplugin.cpa.cpas.itpabs.ItpCPA;
import cpaplugin.cpa.cpas.itpabs.ItpCPAStatistics;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;

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
        public boolean isFunctionEnd(ItpAbstractElement e) {
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
            if (isFunctionEnd(e)) {
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
