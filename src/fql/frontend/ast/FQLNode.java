package fql.frontend.ast;

public interface FQLNode {

  public void accept(ASTVisitor pVisitor);
  
}
