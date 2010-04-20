package fllesh.fql.fllesh.util;

import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.MultiDeclarationEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import fllesh.fql.fllesh.cpa.InternalSelfLoop;

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
