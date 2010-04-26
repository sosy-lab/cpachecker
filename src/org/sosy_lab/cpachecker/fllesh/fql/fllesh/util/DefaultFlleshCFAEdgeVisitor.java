package org.sosy_lab.cpachecker.fllesh.fql.fllesh.util;

import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.MultiDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.MultiStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa.InternalSelfLoop;

public class DefaultFlleshCFAEdgeVisitor<T> extends AbstractFlleshCFAEdgeVisitor<T> {

  @Override
  public T visit(InternalSelfLoop pEdge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T visit(BlankEdge pEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public T visit(AssumeEdge pEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public T visit(CallToReturnEdge pEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public T visit(DeclarationEdge pEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public T visit(FunctionCallEdge pEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public T visit(GlobalDeclarationEdge pEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public T visit(MultiDeclarationEdge pEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public T visit(MultiStatementEdge pEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public T visit(ReturnEdge pEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public T visit(StatementEdge pEdge) {
    // TODO Auto-generated method stub
    return null;
  }

}
