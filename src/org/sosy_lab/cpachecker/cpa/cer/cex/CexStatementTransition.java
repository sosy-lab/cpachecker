package org.sosy_lab.cpachecker.cpa.cer.cex;

import java.util.Optional;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.cer.CERUtils;

public class CexStatementTransition extends CexPathTransition {
    private final String statement;

    private CexStatementTransition(CexNode pstartNode, String pStatement, CexNode pEndNode) {
        super(pstartNode, pEndNode);
        statement = pStatement;
    }

    public static CexStatementTransition
            create(CexNode pstartNode, String pStatement, CexNode pEndNode) {
        return new CexStatementTransition(pstartNode, pStatement, pEndNode);
    }

    public String getStatement() {
        return statement;
    }

    @Override
    public CexNode evaluate(CexNode node, CFAEdge pEdge) {
        if (!node.equals(startNode)) {
            return node;
        }
        if (statement.equals(CERUtils.CFAEdgeToString(pEdge))) {
            return endNode;
        }
        return node;
    }

    /**
     * Tries to match the transition to a statement in the cfa. Explores the cfa from the given
     * rootNode and stops if the statement was mapped or an unexpected assume operation occurs.
     * 
     * @param rootNode the cfa node from which the exploration starts
     * @return the cfa node that matches the end node of this transition
     */
    public Optional<CFANode> findNextMatchInCFA(CFANode rootNode) {
        // TODO split in assign and assume statements and mark relevant variables which are not
        // allowed
        // to be overwritten by unknown assigns

        // prevent infinite exploration of loop edges
        CFANode currentNode = rootNode;
        while (true) {
            int numEdges = currentNode.getNumLeavingEdges();
            if (numEdges == 1) {
                CFAEdge edge = currentNode.getLeavingEdge(0);
                if (statement.equals(CERUtils.CFAEdgeToString(edge))) {
                    return Optional.of(edge.getSuccessor());
                } else {
                    currentNode = edge.getSuccessor();
                    continue;
                }
            } else if (numEdges > 1) {
                for (int i = 0; i < numEdges; ++i) {
                    CFAEdge edge = currentNode.getLeavingEdge(i);
                    if (statement.equals(CERUtils.CFAEdgeToString(edge))) {
                        return Optional.of(edge.getSuccessor());
                    }
                }
                break;
            }
            break;
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return statement;
    }
}
