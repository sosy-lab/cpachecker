package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
  public List<I> rank(ErrorIndicatorSet<I> result) {
    List<I> singleSet =
        new ArrayList<>(FaultLocalizationHeuristicImpl.condenseErrorIndicatorSet(result));
    singleSet.sort(Comparator.comparingInt(l -> edgeList.indexOf(l.correspondingEdge())));
    for (I l : singleSet) {
      FaultLocalizationReason<I> reason =
          new FaultLocalizationReason<>(
              "This location is "
                  + (edgeList.size() - numberErrorEdges - edgeList.indexOf(l.correspondingEdge()))
                  + " execution step(s) away from the error.");
      reason.setLikelihood(
          BigDecimal.valueOf(2)
              .pow(singleSet.indexOf(l))
              .divide(
                  BigDecimal.valueOf(2).pow(singleSet.size()).subtract(BigDecimal.ONE),
                  100,
                  RoundingMode.HALF_UP)
              .doubleValue());
      l.addReason(reason);
    }
    return singleSet;
  }
}
