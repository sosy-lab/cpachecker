package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;
import fql.frontend.ast.pathmonitor.PathMonitor;

public interface Filter extends PathMonitor {

  @Override
  public void accept(ASTVisitor pVisitor);
  
}
