package fql.fllesh.util;

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

public class DefaultCFAEdgeVisitor<T> extends AbstractCFAEdgeVisitor<T> {

  @Override
  public T visit(BlankEdge pEdge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T visit(AssumeEdge pEdge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T visit(CallToReturnEdge pEdge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T visit(DeclarationEdge pEdge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T visit(FunctionCallEdge pEdge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T visit(GlobalDeclarationEdge pEdge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T visit(MultiDeclarationEdge pEdge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T visit(MultiStatementEdge pEdge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T visit(ReturnEdge pEdge) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T visit(StatementEdge pEdge) {
    throw new UnsupportedOperationException();
  }

}
