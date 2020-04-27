package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import java.util.List;

import com.google.common.base.Optional;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class TCFAEntryNode extends FunctionEntryNode {

    public TCFAEntryNode(
            FileLocation pFileLocation,
            FunctionExitNode pExitNode,
            AFunctionDeclaration pFunctionDefinition) {
        super(pFileLocation, pExitNode, pFunctionDefinition, Optional.absent());
    }

    private static final long serialVersionUID = 1L;


    @Override
    public List<? extends AParameterDeclaration> getFunctionParameters() {
        
        return null;
    }

}