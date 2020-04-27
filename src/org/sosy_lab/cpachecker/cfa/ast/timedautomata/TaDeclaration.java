package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import java.util.ArrayList;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

public class TaDeclaration extends AFunctionDeclaration {

    private static final long serialVersionUID = 1L;

    public TaDeclaration(
            FileLocation pFileLocation,
            String pName) {
        super(pFileLocation, CFunctionType.NO_ARGS_VOID_FUNCTION, pName, pName, new ArrayList<>());
    }

    @Override
    public <R, R1 extends R, R2 extends R, X1 extends Exception, X2 extends Exception, V extends CAstNodeVisitor<R1, X1> & JAstNodeVisitor<R2, X2>>
            R accept_(V pV) throws X1, X2 {
        return null;
    }

}