package cpa.art;

import cpa.common.interfaces.Precision;

public class ARTPrecision implements Precision {
  
  private final Precision wrappedPrec;
  
  public ARTPrecision(Precision pWrappedPrec) {
    assert pWrappedPrec != null;
    wrappedPrec = pWrappedPrec;
  }
  
  public Precision getPrecision(){
    return wrappedPrec;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (pObj == null || !(pObj instanceof ARTPrecision)) {
      return false;
    } else {
      return wrappedPrec.equals(((ARTPrecision)pObj).wrappedPrec);
    }
  }
  
  @Override
  public int hashCode() {
    return wrappedPrec.hashCode();
  }
  
  @Override
  public String toString() {
    return String.valueOf(wrappedPrec);
  }
}
