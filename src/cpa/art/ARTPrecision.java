package cpa.art;

import cpa.common.interfaces.Precision;

public class ARTPrecision implements Precision {
  
  private Precision prec;
  
  public ARTPrecision(Precision precision) {
    prec = precision;
  }
  
  public Precision getPrecision(){
    return prec;
  }

}
