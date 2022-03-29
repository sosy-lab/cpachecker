// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.postcondition;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.SolverException;

public class FinalAssumeEdgesOnSameLinePostConditionComposer implements PostConditionComposer {

  private final FormulaContext context;

  public FinalAssumeEdgesOnSameLinePostConditionComposer(FormulaContext pContext) {
    context = pContext;
  }

  /**
   * Finds the last assume edge in the counterexample and uses it as post-condition and additionally
   * conjuncts all directly preceding assume edges that are on the same line. <br>
   *
   * <p>Example 1:
   *
   * <pre>{@code
   * if (x == 0 && y == 10) {
   *   //...
   * }
   * }</pre>
   *
   * Results in the post-condition: x == 0 && y == 10. <br>
   *
   * <p>Example 2:
   *
   * <pre>{@code
   * if (x == 0) {
   *   if (y == 10) {
   *     //...
   *   }
   * }
   * }</pre>
   *
   * Results in the post-condition: y == 10.
   *
   * @param pCounterexample full counterexample from program entry to violation
   * @return post-condition based on the last assume edges on the same line
   * @throws SolverException if solver runs into an error
   * @throws InterruptedException if solver interrupted unexpectedly
   * @throws CPATransferException if PathFormulaManager cannot create path formula for edge
   */
  @Override
  public PostCondition extractPostCondition(List<CFAEdge> pCounterexample)
      throws SolverException, InterruptedException, CPATransferException {
    List<CFAEdge> irrelevant = new ArrayList<>();
    List<CFAEdge> postConditionEdges = new ArrayList<>();
    CFAEdge lastAssumeEdge = null;
    int i;
    for (i = pCounterexample.size() - 1; i >= 0; i--) {
      CFAEdge currentEdge = pCounterexample.get(i);
      if (currentEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        if (lastAssumeEdge == null) {
          lastAssumeEdge = currentEdge;
          postConditionEdges.add(currentEdge);
        } else {
          if (currentEdge.getFileLocation().getStartingLineInOrigin()
              == lastAssumeEdge.getFileLocation().getStartingLineInOrigin()) {
            postConditionEdges.add(currentEdge);
            lastAssumeEdge = currentEdge;
          } else {
            break;
          }
        }
      } else {
        if (lastAssumeEdge == null) {
          irrelevant.add(currentEdge);
        } else {
          break;
        }
      }
    }
    if (postConditionEdges.isEmpty()) {
      throw new AssertionError(
          "Cannot extract post-condition from counterexample: " + pCounterexample);
    }
    postConditionEdges = Lists.reverse(postConditionEdges);
    postConditionEdges.forEach(
        edge ->
            context
                .getLogger()
                .log(
                    Level.FINEST,
                    "tfpostcondition=" + edge.getFileLocation().getStartingLineInOrigin()));
    return new PostCondition(
        postConditionEdges,
        Lists.reverse(irrelevant),
        pCounterexample.subList(0, i + 1),
        PostConditionComposer.postConditionFromList(context, postConditionEdges));
  }
}
