package org.sosy_lab.cpachecker.cpa.featurevariables;

import java.util.regex.Pattern;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class FeatureVarsPrecision implements Precision {

  private final Pattern whiteListPattern;

  public FeatureVarsPrecision(String variableWhitelist) {
    whiteListPattern = Pattern.compile(variableWhitelist);
  }

  boolean isDisabled() {
    return whiteListPattern.pattern() == "";
  }

  boolean isOnWhitelist(String variable) {
    return this.whiteListPattern.matcher(variable).matches();
  }

}
