package org.sosy_lab.cpachecker.core.algorithm.instrumentation;


public class VariableBoundInfo {
    protected NormalLoopType loopType;
    protected final VariableInfo leftVariable;
    protected final VariableInfo rightVariable;

    public VariableBoundInfo(
        VariableInfo leftInfo,VariableInfo rightInfo, NormalLoopType loopType
        ){
            this.leftVariable = leftInfo;
            this.rightVariable = rightInfo;
            this.loopType = loopType;
    }

    public void setLoopType(NormalLoopType loopType){
        this.loopType = loopType;
    }

    public VariableInfo getRightInfo(){
        return this.rightVariable;
    }
    
    public VariableInfo getLeftInfo(){
        return this.leftVariable;
    }

    public NormalLoopType getLoopType(){
        return this.loopType;
    }

    public boolean isBinary(){
        return !((this.leftVariable == null) || (this.rightVariable == null));
    }
    
}

