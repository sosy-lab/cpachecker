package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.regex.Pattern;

import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class ExplicitPrecision implements Precision {
  final Pattern blackListPattern;

  public ExplicitPrecision(String variableBlacklist) {
    blackListPattern = Pattern.compile(variableBlacklist);
  }

  boolean isOnBlacklist(String variable) {
    return this.blackListPattern.matcher(variable).matches();
  }

}
