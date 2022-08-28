package org.sosy_lab.cpachecker.cpa.cer.io;

import java.util.Collection;
import java.util.Optional;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.cer.CERCPAStatistics;
import org.sosy_lab.cpachecker.cpa.cer.cex.Cex;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexPathTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexNode;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexRootTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexTransition;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;

public class CexMapper {

    private CexNode currentCexNode;
    private CexTransition currentTransition;
    private CFANode currentCfaNode;

    private final CFA cfa;
    private final CERCPAStatistics statistics;

    private final StatCounter mappedCounter;
    private final StatCounter unmappedCounter;

    public CexMapper(CFA pCfa, CERCPAStatistics pStatistics) {
        cfa = pCfa;
        statistics = pStatistics;

        mappedCounter = statistics.getMappedCexCounter();
        unmappedCounter = statistics.getUnmappedCexCounter();
    }

    public CexMapperReport mapCexs(Collection<Cex> cexs) {
        CexMapperReport report = new CexMapperReport();

        for (Cex cex : cexs) {
            currentCexNode = cex.getRootNode();
            currentTransition = currentCexNode.getLeavingTransition().get();
            currentCfaNode = null;

            // Retrieve the mapping root
            if (currentTransition instanceof CexRootTransition) {
                boolean hasNoRoot = handleCexRootTransition();
                if (hasNoRoot) {
                    unmappedCounter.inc();
                    continue;
                }
            } else {
                // No root found.
                unmappedCounter.inc();
                continue;
            }

            // Map the other transitions
            boolean mappingFailed = false;

            while (currentCexNode.getLeavingTransition().isPresent()) {
                currentTransition = currentCexNode.getLeavingTransition().get();

                if (currentTransition instanceof CexPathTransition) {
                    mappingFailed = handleForwardTransition();
                } else if (currentTransition instanceof CexRootTransition) {
                    mappingFailed = handleCexRootTransition();
                }

                if (mappingFailed) {
                    break;
                }
            }

            if (mappingFailed) {
                unmappedCounter.inc();
            } else {
                // Fill the report
                report.putCex(cex);
                mappedCounter.inc();
            }
        }

        currentCexNode = null;
        currentCfaNode = null;
        currentTransition = null;
        return report;
    }

    private boolean handleForwardTransition() {
        CexPathTransition transition = (CexPathTransition) currentTransition;
        if (currentCfaNode.getNumLeavingEdges() == 1) {
            CFAEdge cfaEdge = currentCfaNode.getLeavingEdge(0);
            CexNode edgeResultNode = transition.evaluate(currentCexNode, cfaEdge);
            currentCfaNode = cfaEdge.getSuccessor();
            if (!edgeResultNode.equals(currentCexNode)) {
                currentCexNode = edgeResultNode;
                currentCexNode.setMappedNode(currentCfaNode);
            }
            return false;
        } else if (currentCfaNode.getNumLeavingEdges() > 1) {
            for (int i = 0; i < currentCfaNode.getNumLeavingEdges(); ++i) {
                CFAEdge cfaEdge = currentCfaNode.getLeavingEdge(i);
                CexNode edgeResultNode = transition.evaluate(currentCexNode, cfaEdge);
                if (!edgeResultNode.equals(currentCexNode)) {
                    currentCfaNode = cfaEdge.getSuccessor();
                    currentCexNode = edgeResultNode;
                    currentCexNode.setMappedNode(currentCfaNode);
                    return false;
                }
            }
            // Stop mapping if no leaving edge matches
            return true;

        } else {
            // No leaving edge but still remaining transitions
            return true;
        }
    }

    private boolean handleCexRootTransition() {
        CexRootTransition transition = (CexRootTransition) currentTransition;
        Optional<CFANode> nextNode = transition.getRootFromCFA(cfa);
        if (nextNode.isPresent()) {
            currentCfaNode = nextNode.get();
            currentCexNode = currentTransition.getEndNode();
            currentCexNode.setMappedNode(currentCfaNode);
            return false;
        }
        return true;
    }
}
