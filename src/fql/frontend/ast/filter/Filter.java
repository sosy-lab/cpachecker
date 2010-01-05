package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;
import fql.frontend.ast.FQLNode;

public interface Filter extends FQLNode {

  @Override
  public void accept(ASTVisitor pVisitor);
  
}
