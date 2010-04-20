package fql.frontend.ast;

public interface FQLNode {

  public <T> T accept(ASTVisitor<T> pVisitor);
  
}
