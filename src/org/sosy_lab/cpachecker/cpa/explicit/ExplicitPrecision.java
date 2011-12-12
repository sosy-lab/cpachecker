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

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

public class ExplicitPrecision implements Precision {

  private final Pattern blackListPattern;

  private SetMultimap<CFANode, String> whiteList = null;

  CFANode currentLocation = null;

  public ExplicitPrecision(String variableBlacklist, SetMultimap<CFANode, String> whiteList) {
    blackListPattern = Pattern.compile(variableBlacklist);

    if (whiteList != null) {
      this.whiteList = HashMultimap.create(whiteList);
    }
  }

  public ExplicitPrecision(ExplicitPrecision precision, Multimap<CFANode, String> predicateInfo,
      Multimap<CFANode, String> pathInfo) {
    blackListPattern = precision.blackListPattern;

    this.whiteList = HashMultimap.create(precision.whiteList);

    this.whiteList.putAll(predicateInfo);
    this.whiteList.putAll(pathInfo);
  }

  @Override
  public String toString() {
    return whiteList != null ? whiteList.toString() : "whitelist disabled";
  }

  public void setLocation(CFANode node) {
    currentLocation = node;
  }

  boolean isOnBlacklist(String variable) {
    return this.blackListPattern.matcher(variable).matches();
  }

  boolean isOnWhitelist(String variable) {
    return whiteList == null
        || whiteList.containsEntry(currentLocation, variable);
  }

  public boolean isTracking(String variable) {
    return isOnWhitelist(variable) && !blackListPattern.matcher(variable).matches();
  }

  public boolean isNotTracking(String variable) {
    return !isTracking(variable);
  }

  public String getBlackListPattern() {
    return blackListPattern.pattern();
  }

  private void addToWhitelist(Map<CFANode, Set<String>> additionalInfo) {
    for (Map.Entry<CFANode, Set<String>> entry : additionalInfo.entrySet()) {
      whiteList.putAll(entry.getKey(), entry.getValue());
    }
  }
}
