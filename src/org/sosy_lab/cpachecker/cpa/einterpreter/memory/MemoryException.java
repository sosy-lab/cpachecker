package org.sosy_lab.cpachecker.cpa.einterpreter.memory;

public class MemoryException extends Exception {

  /**
   * 
   */
  private  String message;
  private static final long serialVersionUID = -8347896258634487921L;

  
  public MemoryException(String pmessage){
    message = pmessage;
  }
  
  @Override
  public String getMessage(){
    return message;
  }
  
   @Override
  public String toString(){
     return message;
   }
  

}
