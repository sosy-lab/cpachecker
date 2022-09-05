package org.sosy_lab.cpachecker.cpa.cer.io;

import java.util.Collection;
import java.util.Optional;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.cer.CERCPAStatistics;
import org.sosy_lab.cpachecker.cpa.cer.cex.Cex;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexPathTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexState;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexRootTransition;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexTransition;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;

public class CexMapper {

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
            CexState currentCexState = cex.getRootState();
            CFANode currentCfaNode = cfa.getMainFunction();

            boolean cexUnmapped = false;
            while (currentCexState.getLeavingTransition().isPresent()) {
                CexTransition currentTransition = currentCexState.getLeavingTransition().get();
                boolean transitionMapped = false;
                for (int i = 0; i < currentCfaNode.getNumLeavingEdges(); ++i) {
                    CFAEdge edge = currentCfaNode.getLeavingEdge(i);
                    Pair<CexState, CFANode> res = evaluate(currentTransition, edge);
                    if (!res.getFirst().equals(currentCexState)) {
                        transitionMapped = true;
                        currentCexState = res.getFirst();
                        currentCfaNode = res.getSecond();
                        currentCexState.setMappedNode(currentCfaNode);
                        break;
                    }
                }

                if (!transitionMapped) {
                    if (currentCfaNode.getLeavingSummaryEdge() != null) {
                        currentCfaNode = currentCfaNode.getLeavingSummaryEdge().getSuccessor();
                    } else if (currentCfaNode.getNumLeavingEdges() == 1) {
                        currentCfaNode = currentCfaNode.getLeavingEdge(0).getSuccessor();
                    } else {
                        cexUnmapped = true;
                        break;
                    }
                }
            }

            if (cexUnmapped) {
                unmappedCounter.inc();
            } else {
                report.putCex(cex);
                mappedCounter.inc();
            }
        }

        return report;
    }

    private Pair<CexState, CFANode> evaluate(CexTransition pTransition, CFAEdge pEdge) {
        if (pTransition instanceof CexRootTransition) {
            CexRootTransition transition = (CexRootTransition) pTransition;
            Optional<CFANode> node = transition.getRootFromCFA(cfa);
            if (node.isPresent()) {
                return Pair.of(pTransition.getEndState(), node.get());
            }
        } else if (pTransition instanceof CexPathTransition) {
            CexPathTransition transition = (CexPathTransition) pTransition;
            CexState newState = transition.evaluate(transition.getStartState(), pEdge);
            if (newState.equals(transition.getEndState())) {
                return Pair.of(pTransition.getEndState(), pEdge.getSuccessor());
            }
        }

        return Pair.of(pTransition.getStartState(), pEdge.getPredecessor());
    }
}
