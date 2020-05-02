package org.sosy_lab.cpachecker.util.faultlocalization.appendables;

public class FaultReason extends FaultInfo {

  protected FaultReason(InfoType pType, String pDescription){
    super(pType);
    description = pDescription;
  }

}
