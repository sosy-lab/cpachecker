package org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.Edges;

public interface ASTVisitor<T> {
  
  public T visit(Concatenation pConcatenation);
  public T visit(Quotation pQuotation);
  public T visit(Union pUnion);
  public T visit(Edges pEdges);
  
}
