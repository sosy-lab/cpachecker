package org.sosy_lab.cpachecker.fllesh.fql2.ast.terms;

public class Variable implements Term {

  private String mName;
  
  public Variable(String pName) {
    mName = pName;
  }
  
  public String getName() {
    return mName;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (!(pOther.getClass().equals(getClass()))) {
      return false;
    }
    
    Variable lVariable = (Variable)pOther;
    
    return mName.equals(lVariable.mName);
  }
  
  @Override
  public int hashCode() {
    return mName.hashCode();
  }
  
  @Override
  public String toString() {
    return mName;
  }
  
  @Override
  public <T> T accept(TermVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

}
