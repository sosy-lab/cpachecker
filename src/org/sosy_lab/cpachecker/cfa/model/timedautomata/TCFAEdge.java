package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;

public class TCFAEdge extends BlankEdge {

    private static final long serialVersionUID = 5472749446453717391L;

    private final TaVariableCondition guard;
    private final Set<TaIdExpression> variablesToReset;

    public TCFAEdge(
            FileLocation pFileLocation,
            TCFANode pPredecessor,
            TCFANode pSuccessor,
            TaVariableCondition pGuard,
            Set<TaIdExpression> pVariablesToReset) {
        super(getEdgeLabel(pGuard), pFileLocation, pPredecessor, pSuccessor, getEdgeLabel(pGuard));

        guard = pGuard;
        variablesToReset = pVariablesToReset;
    }

    private static String getEdgeLabel(TaVariableCondition guard) {
        return guard.toASTString();
    }


    public TaVariableCondition getGuard() {
        return guard;
    }

    public Set<TaIdExpression> getVariablesToReset() {
        return variablesToReset;
    }
}