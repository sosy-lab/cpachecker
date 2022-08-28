package org.sosy_lab.cpachecker.cpa.cer.cexInfos;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.cer.cex.Cex;
import org.sosy_lab.cpachecker.cpa.cer.cex.CexNode;

public class PrecisionStore {

    // For reuse
    private final Map<CFANode, PrecisionInformation> precisions;

    public PrecisionStore() {
        precisions = new HashMap<>();
    }

    public PrecisionInformation getPrecisionInfoForNode(CFANode node) {
        return precisions.get(node);
    }

    public void updateWithCexs(Collection<Cex> pCexs) {
        for (Cex cex : pCexs) {
            CexNode currentNode = cex.getRootNode();

            while (true) {
                if (currentNode.getMappedNode().isPresent()) {
                    CFANode node = currentNode.getMappedNode().get();
                    for (CounterexampleInformation cexInfo : currentNode.getCexInfos()) {
                        if (cexInfo instanceof PrecisionInformation) {
                            PrecisionInformation precInfo = (PrecisionInformation) cexInfo;
                            PrecisionInformation storeInfo = precisions.get(node);
                            precisions.put(node, PrecisionInformation.merge(precInfo, storeInfo));
                        }
                    }
                }
                if (currentNode.getLeavingTransition().isPresent()) {
                    currentNode = currentNode.getLeavingTransition().get().getEndNode();
                } else {
                    break;
                }
            }
        }
    }
}
