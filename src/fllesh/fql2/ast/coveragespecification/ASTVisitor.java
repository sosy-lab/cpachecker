package fllesh.fql2.ast.coveragespecification;

import fllesh.fql2.ast.Edges;

public interface ASTVisitor<T> {
  
  public <T> T visit(Concatenation pConcatenation);
  public <T> T visit(Quotation pQuotation);
  public <T> T visit(Union pUnion);
  public <T> T visit(Edges pEdges);
  
}
