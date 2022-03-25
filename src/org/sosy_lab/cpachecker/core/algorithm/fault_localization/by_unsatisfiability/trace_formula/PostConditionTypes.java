// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public enum PostConditionTypes {
  /**
   * <p>Finds the last assume edge in the counterexample and uses it as post-condition.</p>
   * <br>
   * <p>Example 1:
   * <pre>
   * {@code
   * if (x == 0 && y == 10) {
   *   //...
   * }}
   * </pre>
   * Results in the post-condition: y == 10.
   * </p>
   * <br>
   * <p>Example 2:
   * <pre>
   * {@code
   * if (x == 0) {
   *   if (y == 10) {
   *     //...
   *   }
   * }
   * </pre>
   * Results in the post-condition: y == 10.
   * </p>
   */
  ONLY_LAST_ASSUME_EDGE {
    @Override
    public PostCondition createPostConditionFromCounterexample(
        List<CFAEdge> pCounterexample, FormulaContext pContext)
        throws SolverException, InterruptedException, CPATransferException {
      List<CFAEdge> irrelevant = new ArrayList<>();
      int i;
      for (i = pCounterexample.size() - 1; i >= 0; i--) {
        CFAEdge currentEdge = pCounterexample.get(i);
        if (currentEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
          List<CFAEdge> postConditionEdges = ImmutableList.of(currentEdge);
          postConditionEdges.forEach(
              edge ->
                  pContext
                      .getLogger()
                      .log(
                          Level.FINEST,
                          "tfpostcondition=" + edge.getFileLocation().getStartingLineInOrigin()));
          return new PostCondition(
              postConditionEdges,
              Lists.reverse(irrelevant),
              pCounterexample.subList(0, i + 1),
              formulaForList(pContext, postConditionEdges));
        } else {
          irrelevant.add(currentEdge);
        }
      }
      throw new IllegalArgumentException(
          "Cannot find post-condition for provided counterexample: " + pCounterexample);
    }
  },
  /**
   * <p>Finds the last assume edge in the counterexample and uses it as post-condition and additionally
   * conjuncts all directly succeeding assume-edges that are on the same line.</p>
   * <br>
   * <p>Example 1:
   * <pre>
   * {@code
   * if (x == 0 && y == 10) {
   *   //...
   * }}
   * </pre>
   * Results in the post-condition: x == 10 && y == 10.
   * </p>
   * <br>
   * <p>Example 2:
   * <pre>
   * {@code
   * if (x == 0) {
   *   if (y == 10) {
   *     //...
   *   }
   * }
   * </pre>
   * Results in the post-condition: y == 10.
   * </p>
   */
  LAST_ASSUME_EDGES_ON_SAME_LINE {
    @Override
    public PostCondition createPostConditionFromCounterexample(
        List<CFAEdge> pCounterexample, FormulaContext pContext)
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
              pContext
                  .getLogger()
                  .log(
                      Level.FINEST,
                      "tfpostcondition=" + edge.getFileLocation().getStartingLineInOrigin()));
      return new PostCondition(
          postConditionEdges,
          Lists.reverse(irrelevant),
          pCounterexample.subList(0, i + 1),
          formulaForList(pContext, postConditionEdges));
    }
  },
  /**
   * <p>Finds the last assume edge in the counterexample and uses it as post-condition and additionally
   * conjuncts all directly succeeding assume-edges.</p>
   * <br>
   * <p>Example 1:
   * <pre>
   * {@code
   * if (x == 0 && y == 10) {
   *   //...
   * }}
   * </pre>
   * Results in the post-condition: x == 10 && y == 10.
   * </p>
   * <br>
   * <p>Example 2:
   * <pre>
   * {@code
   * if (x == 0) {
   *   if (y == 10) {
   *     //...
   *   }
   * }
   * </pre>
   * Results in the post-condition: x == 10 && y == 10.
   * </p>
   */
  LAST_ASSUME_EDGE_CLUSTER {
    @Override
    public PostCondition createPostConditionFromCounterexample(
        List<CFAEdge> pCounterexample, FormulaContext pContext)
        throws SolverException, InterruptedException, CPATransferException {
      CFAEdge lastAssumeEdge = null;
      List<CFAEdge> irrelevant = new ArrayList<>();
      List<CFAEdge> postConditionEdges = new ArrayList<>();
      int i;
      for (i = pCounterexample.size() - 1; i >= 0; i--) {
        CFAEdge currentEdge = pCounterexample.get(i);
        if (currentEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
          lastAssumeEdge = currentEdge;
          postConditionEdges.add(currentEdge);
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
              pContext
                  .getLogger()
                  .log(
                      Level.FINEST,
                      "tfpostcondition=" + edge.getFileLocation().getStartingLineInOrigin()));
      return new PostCondition(
          postConditionEdges,
          Lists.reverse(irrelevant),
          pCounterexample.subList(0, i + 1),
          formulaForList(pContext, postConditionEdges));
    }
  };

  public abstract PostCondition createPostConditionFromCounterexample(
      List<CFAEdge> pCounterexample, FormulaContext pContext)
      throws SolverException, InterruptedException, CPATransferException;

  private static BooleanFormula formulaForList(FormulaContext pContext, List<CFAEdge> pEdges)
      throws CPATransferException, InterruptedException {
    return pContext
        .getSolver()
        .getFormulaManager()
        .uninstantiate(pContext.getManager().makeFormulaForPath(pEdges).getFormula());
  }
}
