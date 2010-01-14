package exceptions;

import cpa.art.ARTElement;

public class NoNewPredicatesFoundException extends CPAException{

  /**
   * 
   */
  private static final long serialVersionUID = 9109877170260516520L;

  private final ARTElement artLastElement;
  
  public NoNewPredicatesFoundException(ARTElement pArtLastElement){
    artLastElement = pArtLastElement;
  }

  public ARTElement getArtLasttElement() {
    return artLastElement;
  }
  
}
