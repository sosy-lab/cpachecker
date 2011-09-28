/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class ExplicitPrecision implements Precision {
  private final Pattern blackListPattern;

  private Map<CFAEdge, Set<String>> whiteList = null;

  CFAEdge currentEdge = null;

  public ExplicitPrecision(String variableBlacklist, Map<CFAEdge, Set<String>> whiteList) {

    blackListPattern = Pattern.compile(variableBlacklist);

    if(whiteList != null)
      this.whiteList = new HashMap<CFAEdge, Set<String>>(whiteList);
  }

  public ExplicitPrecision(ExplicitPrecision precision, Map<CFAEdge, Set<String>> whiteList) {

    blackListPattern = precision.blackListPattern;

    if(whiteList != null)
      this.whiteList = new HashMap<CFAEdge, Set<String>>(whiteList);
  }

  public void setEdge(CFAEdge node)
  {
    currentEdge = node;
  }

  boolean isOnBlacklist(String variable)
  {
    return this.blackListPattern.matcher(variable).matches();
  }

  boolean isOnWhitelist(String variable)
  {
    return whiteList == null || (whiteList.containsKey(currentEdge) && whiteList.get(currentEdge).contains(variable));
  }

  public boolean isTracking(String variable)
  {
    return isOnWhitelist(variable) && !blackListPattern.matcher(variable).matches();
  }

  public boolean isNotTracking(String variable)
  {
    return !isTracking(variable);
  }

  public Map<CFAEdge, Set<String>> getWhiteList()
  {
    return whiteList;
  }

  public String getBlackListPattern()
  {
    return blackListPattern.pattern();
  }
}
