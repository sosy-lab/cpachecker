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
