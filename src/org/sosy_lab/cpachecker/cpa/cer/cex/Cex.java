package org.sosy_lab.cpachecker.cpa.cer.cex;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.cer.CERUtils;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.CounterexampleInformation;
import org.sosy_lab.cpachecker.cpa.cer.cexInfos.PrecisionInformation;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Preconditions;

public class Cex {
    private final CexNode rootNode;

    public Cex(@Nonnull CexNode pRootNode) {
        Preconditions.checkNotNull(pRootNode, "The given root node is null");
        rootNode = pRootNode;
    }

    public Cex(@Nonnull List<CFAEdgeWithAdditionalInfo> edgeWithInfos) {
        Preconditions.checkNotNull(
                edgeWithInfos,
                "Can not create a counterexample for an empty edge list");

        CexNode lastNode = new CexNode();
        this.rootNode = lastNode;

        for (CFAEdgeWithAdditionalInfo edgeWithInfo : edgeWithInfos) {
            CFAEdge edge = edgeWithInfo.getCFAEdge();
            CexNode nextNode = new CexNode();
            nextNode.setMappedNode(edge.getSuccessor());

            CexTransition transition;
            if (edge.getPredecessor() instanceof FunctionEntryNode) {
                transition =
                        CexFunctionHeadTransition.create(
                                lastNode,
                                (FunctionEntryNode) edge.getPredecessor(),
                                nextNode);
            } else if (edge instanceof FunctionReturnEdge) {
                transition =
                        CexFunctionReturnTransition
                                .create(lastNode, (FunctionReturnEdge) edge, nextNode);
            } else {
                transition =
                        CexStatementTransition
                                .create(lastNode, CERUtils.CFAEdgeToString(edge), nextNode);
            }
            lastNode.setLeavingTransition(transition);
            Set<CounterexampleInformation> cexInfo = new HashSet<>();
            Set<MemoryLocation> valuePrecision = CERUtils.extractPrecisionInfo(edgeWithInfo);
            if (!valuePrecision.isEmpty()) {
                PrecisionInformation precisionInfo = new PrecisionInformation(valuePrecision);
                cexInfo.add(precisionInfo);
            }

            lastNode = nextNode;
            lastNode.setCexInfos(cexInfo);
        }
    }

    public CexNode getRootNode() {
        return rootNode;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        CexNode currentNode = rootNode;
        while (currentNode.getLeavingTransition().isPresent()) {
            builder.append(" -> ");
            builder.append(currentNode.getLeavingTransition().get().toString());
            currentNode = currentNode.getLeavingTransition().get().getEndNode();
        }
        return builder.toString();
    }
}
