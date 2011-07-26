package org.sosy_lab.cpachecker.cpa.einterpreter.memory;

import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.TypeClass;




public interface Variable {
    TypeClass getTypeClass();
    Type getType();

    String getName();
    Address getAddress();
    int getSize();
    Variable clone();
    void copyVar(String pname,InterpreterElement el) throws Exception;

    boolean isConst();


}
