package org.sosy_lab.cpachecker.fllesh.fql.fllesh.util;

import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.MultiDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.MultiStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;

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
