package cpa.art;

import cpa.common.interfaces.Precision;

public class ArtPrecision implements Precision {
  
  private Precision prec;
  
  public ArtPrecision(Precision precision) {
    prec = precision;
  }
  
  public Precision getPrecision(){
    return prec;
  }

}
