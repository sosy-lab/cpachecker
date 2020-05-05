package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;

public interface TaVariableConditionVisitor<R, X extends Exception>
    extends CRightHandSideVisitor<R, X> {

  R visit(TaVariableCondition pTaVariableCondition) throws X;
}
