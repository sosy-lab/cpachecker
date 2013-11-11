/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.sign;

import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import com.google.common.collect.Sets;



public class SignState implements AbstractState {

  public static enum SIGN {
    PLUS, MINUS, ZERO;
  }

  private Set<Map<String, SIGN>> possibleSigns;

  public SignState(Set<Map<String, SIGN>> pPossibleSigns) {
    possibleSigns = pPossibleSigns;
  }

  public SignState union(SignState pToJoin) {
    if(pToJoin == this) {
      return this;
    }
    Set<Map<String, SIGN>> resultSetOfPossStates;
    resultSetOfPossStates = Sets.union(possibleSigns, pToJoin.possibleSigns);
    return new SignState(resultSetOfPossStates);
  }

  public boolean isSubsetOf(SignState pSuperset) {
    if(pSuperset == this) {
      return true;
    }
    return pSuperset.possibleSigns.containsAll(possibleSigns);
  }

  public Set<Map<String, SIGN>> getPossibleSigns() {
    return possibleSigns;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    String delim = ", ";
    builder.append("[");
    for(Map<String, SIGN> varMap : possibleSigns) {
      builder.append("{");
      String loopDelim = "";
      for(String key : varMap.keySet()) {
        builder.append(loopDelim);
        builder.append(key + "->" + varMap.get(key));
        loopDelim = delim;
      }
      builder.append("}");
    }
    builder.append("]");
    return builder.toString();
  }

}
