// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.ForwardingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class ExpressionCollector extends ForwardingCFAVisitor {
  private final Set<CExpression> expressions = Sets.newHashSet();

  public ExpressionCollector() {
    super(new EdgeCollectingCFAVisitor());
  }

  @Override
  public TraversalProcess visitEdge(CFAEdge edge) {
    ImmutableSet<? extends CExpression> currentExpressions =
        FluentIterable.from(CFAUtils.getAstNodesFromCfaEdge(edge))
            .transformAndConcat(CFAUtils::traverseRecursively)
            .filter(CExpression.class)
            .toSet();

    expressions.addAll(currentExpressions);

    return super.visitEdge(edge);
  }

  private Set<CExpression> getExpressions() {
    return expressions;
  }

  /** Returns all expressions found within the given CFA. */
  public static Set<CExpression> collectExpressions(CFA cfa) {
    final ExpressionCollector expressionCollector = new ExpressionCollector();

    CFATraversal.dfs()
        .ignoreSummaryEdges()
        .traverseOnce(cfa.getMainFunction(), expressionCollector);

    return expressionCollector.getExpressions();
  }
}
