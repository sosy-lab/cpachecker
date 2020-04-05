package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.rankings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultReason;

public class CallHierarchyRanking implements FaultRanking {

  private Map<CFAEdge, Integer> mapEdgeToPosition;
  private int firstErrorEdge;

  public CallHierarchyRanking(List<CFAEdge> pEdgeList, int pNumberErrorEdges) {
    mapEdgeToPosition = new HashMap<>();
    for (int i = 0; i < pEdgeList.size(); i++) {
      mapEdgeToPosition.put(pEdgeList.get(i), i+1);
    }
    firstErrorEdge = pEdgeList.size()-pNumberErrorEdges+1;
  }

  @Override
  public List<Fault> rank(Set<Fault> result) {
    FaultRankingUtils.RankingResults results =
        FaultRankingUtils.rankedListFor(result, f -> f.stream().mapToDouble(fc -> mapEdgeToPosition.get(fc.correspondingEdge())).max().orElse(0));
    double overallSum = results.getLikelihoodMap().values().stream().mapToDouble(Double::doubleValue).sum();
    for (Entry<Fault, Double> faultDoubleEntry : results.getLikelihoodMap().entrySet()) {
      int min = firstErrorEdge - (int)faultDoubleEntry.getValue().doubleValue();
      faultDoubleEntry.getKey().addReason(FaultReason.justify("This fault is " + min + " execution step(s) away from the error location.",
          faultDoubleEntry.getValue() /overallSum));
    }
    return results.getRankedList();
  }
}
