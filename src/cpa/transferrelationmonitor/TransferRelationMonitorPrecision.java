package cpa.transferrelationmonitor;

import cpa.common.interfaces.Precision;

public class TransferRelationMonitorPrecision implements Precision {
  
  private final Precision wrappedPrec;
  
  public TransferRelationMonitorPrecision(Precision pWrappedPrec) {
//    assert pWrappedPrec != null;
    wrappedPrec = pWrappedPrec;
  }
  
  public Precision getPrecision(){
    return wrappedPrec;
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    } else if (pObj == null || !(pObj instanceof TransferRelationMonitorPrecision)) {
      return false;
    } else {
      return wrappedPrec.equals(((TransferRelationMonitorPrecision)pObj).wrappedPrec);
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
