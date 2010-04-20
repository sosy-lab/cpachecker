package fllesh.fql.fllesh.util;

import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.MultiDeclarationEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;

public interface CFAEdgeVisitor<T> {

  public T visit(CFAEdge pEdge);
  
  public T visit(BlankEdge pEdge);
  public T visit(AssumeEdge pEdge);
  public T visit(CallToReturnEdge pEdge);
  public T visit(DeclarationEdge pEdge);
  public T visit(FunctionCallEdge pEdge);
  public T visit(GlobalDeclarationEdge pEdge);
  public T visit(MultiDeclarationEdge pEdge);
  public T visit(MultiStatementEdge pEdge);
  public T visit(ReturnEdge pEdge);
  public T visit(StatementEdge pEdge);
  
}
