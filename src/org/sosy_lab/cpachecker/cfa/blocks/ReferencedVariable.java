package org.sosy_lab.cpachecker.cfa.blocks;

/**
 * Represents a reference to a variable in the CFA.
 * @author dwonisch
 *
 */
public class ReferencedVariable {
  private String ident;
  private boolean occursInCondition;
  private boolean occursOnLhs;
  private ReferencedVariable lhsVariable;
  
  public ReferencedVariable(String pIdent, boolean pOccursInCondition,
      boolean pOccursOnLhs, ReferencedVariable pLhsVariable) {
    super();
    ident = pIdent;
    occursInCondition = pOccursInCondition;
    occursOnLhs = pOccursOnLhs;
    lhsVariable = pLhsVariable;
  }

  public boolean occursInCondition() {
    return occursInCondition;
  }
  
  public String getName() {
    return ident;
  }
  
  public boolean occursOnLhs() {
    return occursOnLhs;
  }
  
  public ReferencedVariable getLhsVariable() {
    return lhsVariable;
  }
  
  @Override
  public boolean equals(Object o) {
    if(!(o instanceof ReferencedVariable)) {
      return false;
    }
    
    ReferencedVariable rhs = (ReferencedVariable)o;    
    return ident.equals(rhs.ident) && occursInCondition == rhs.occursInCondition && occursOnLhs == rhs.occursOnLhs && (lhsVariable == null && rhs.lhsVariable == null || lhsVariable.equals(rhs.lhsVariable));
  }
  
  @Override
  public int hashCode() {
    return ident.hashCode() + (occursInCondition?7:0) + (occursOnLhs?42:3) + (lhsVariable==null?0:lhsVariable.hashCode());
  }
  
  @Override
  public String toString() {
    return ident;
  }
}
