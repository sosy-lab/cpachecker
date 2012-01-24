/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.assumptions;


/**
 * Creates a predicate as an UIF for a heuristic using the
 * type of the heuristic and the threshold for it.
 */
public class HeuristicToFormula {

  public enum PreventingHeuristicType {
    PATHLENGTH("PL"),
    SUCCESSORCOMPTIME("SCT"),
    PATHCOMPTIME("PCT"),
    ASSUMEEDGESINPATH("AEIP"),
    REPETITIONSINPATH("RIP"),
    MEMORYUSED("MU"),
    MEMORYOUT("MO"),
    TIMEOUT("TO"),
    LOOPITERATIONS("LI"),
    EDGECOUNT("EC");

  private final String predicateString;

    private PreventingHeuristicType(String predicateStr) {
      predicateString = predicateStr;
    }

    private String getPredicateString(){
      return predicateString;
    }
  }

  public static String getFormulaStringForHeuristic(PreventingHeuristicType type, long thresholdValue) {
    return "VAR " + type.getPredicateString() + ": INTEGER -> BOOLEAN \n\n " + "FORMULA (" + type.getPredicateString() + "(" + thresholdValue + "))";
  }

}
