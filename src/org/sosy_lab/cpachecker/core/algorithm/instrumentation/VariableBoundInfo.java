package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class VariableBoundInfo {
    protected NormalLoopType loopType;
    protected final CIdExpression leftVariable;
    protected final CIdExpression rightVariable;

    public VariableBoundInfo(
        CIdExpression leftInfo,CIdExpression rightInfo
        ){
            this.leftVariable = leftInfo;
            this.rightVariable = rightInfo;
    }


    public CIdExpression getRightInfo(){
        return this.rightVariable;
    }
    
    public CIdExpression getLeftInfo(){
        return this.leftVariable;
    }

    
}

