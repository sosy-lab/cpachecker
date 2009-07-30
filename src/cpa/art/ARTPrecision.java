package cpa.art;

import cpa.common.interfaces.Precision;

public class ARTPrecision implements Precision {
  
  private Precision wrappedPrec;
  
  public ARTPrecision(Precision pWrappedPrec) {
    wrappedPrec = pWrappedPrec;
  }
  
  public Precision getPrecision(){
    return wrappedPrec;
  }

}
