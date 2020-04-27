package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class TCFANode extends CFANode {

    private final String name;
    private final TaVariableCondition invariant;
    private final boolean isInitialState;
    
    private static final long serialVersionUID = -7796108813615096804L;
    
    public TCFANode(String pName, TaVariableCondition pInvariant, TaDeclaration pDeclaration, boolean pIsInitialState) {
        super(pDeclaration);
        name = pName;
        invariant = pInvariant;
        isInitialState = pIsInitialState;
    }

    public String getName() {
        return name;
    }

    public boolean isInitialState() {
        return isInitialState;
    }

    public TaVariableCondition getInvariant() {
        return invariant;
    }


}