package org.sosy_lab.cpachecker.util.faultlocalization.appendables;

public class PotentialFix extends FaultInfo {

  protected PotentialFix(InfoType pType, String pDescription) {
    super(pType);
    description = pDescription;
    score = 0;
  }

}
