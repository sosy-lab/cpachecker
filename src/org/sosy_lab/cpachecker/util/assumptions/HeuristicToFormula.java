package org.sosy_lab.cpachecker.util.assumptions;

import org.sosy_lab.common.Pair;

/**
 * Creates a predicate as an UIF for a heuristic using the
 * type of the heuristic and the threshold for it.
 *
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

  public static String getFormulaStringForHeuristic(Pair<PreventingHeuristicType, Long> preventingCondition){
    PreventingHeuristicType type = preventingCondition.getFirst();
    Long thresholdValue = preventingCondition.getSecond();
    return "VAR " + type.getPredicateString() + ": INTEGER -> BOOLEAN \n\n " + "FORMULA (" + type.getPredicateString() + "(" + thresholdValue + "))";
  }

}
