package org.sosy_lab.cpachecker.cpa.cer.cex;

import java.util.Optional;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public abstract class CexRootTransition extends CexTransition {

    public CexRootTransition(CexState pstartNode, CexState pEndNode) {
        super(pstartNode, pEndNode);
    }

    public abstract Optional<CFANode> getRootFromCFA(CFA cfa);
}
