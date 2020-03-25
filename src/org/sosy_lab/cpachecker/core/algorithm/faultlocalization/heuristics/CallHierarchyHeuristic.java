package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristicImpl;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationOutput;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationReason;

public class CallHierarchyHeuristic<I extends FaultLocalizationOutput>
    implements FaultLocalizationHeuristic<I> {

  private final List<CFAEdge> edgeList;
  private int numberErrorEdges;

  public CallHierarchyHeuristic(List<CFAEdge> pEdgeList, int pNumberErrorEdges) {
    edgeList = pEdgeList;
    numberErrorEdges = pNumberErrorEdges;
  }

  @Override
  public Map<I, Integer> rank(ErrorIndicatorSet<I> result) {
    List<I> singleSet =
        new ArrayList<>(FaultLocalizationHeuristicImpl.condenseErrorIndicatorSet(result));
    singleSet.sort(Comparator.comparingInt(l -> edgeList.indexOf(l.correspondingEdge())));
    Map<I, Double> scoreMap = new HashMap<>();
    for (I l : singleSet) {
      FaultLocalizationReason reason =
          new FaultLocalizationReason(
              "This location is "
                  + (edgeList.size() - numberErrorEdges - edgeList.indexOf(l.correspondingEdge()))
                  + " execution step(s) away from the error.");
      double likelihood = BigDecimal.valueOf(2)
          .pow(singleSet.indexOf(l))
          .divide(
              BigDecimal.valueOf(2).pow(singleSet.size()).subtract(BigDecimal.ONE),
              100,
              RoundingMode.HALF_UP)
          .doubleValue();
      reason.setLikelihood(likelihood);
      scoreMap.put(l, likelihood);
      l.addReason(reason);
    }
    return FaultLocalizationHeuristicImpl.scoreToRankMap(scoreMap);
  }
}
