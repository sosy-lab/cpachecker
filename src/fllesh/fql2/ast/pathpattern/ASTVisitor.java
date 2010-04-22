package fllesh.fql2.ast.pathpattern;

import fllesh.fql2.ast.Edges;

public interface ASTVisitor<T> {

  public T visit(Concatenation pConcatenation);
  public T visit(Repetition pRepetition);
  public T visit(Union pUnion);
  public T visit(Edges pEdges);
  
}
