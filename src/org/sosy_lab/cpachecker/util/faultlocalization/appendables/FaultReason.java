package org.sosy_lab.cpachecker.util.faultlocalization.appendables;

public class FaultReason extends FaultInfo {

  protected FaultReason(InfoType pType, String pDescription, double pScore){
    super(pType);
    description = pDescription;
    score = pScore;
  }

}
