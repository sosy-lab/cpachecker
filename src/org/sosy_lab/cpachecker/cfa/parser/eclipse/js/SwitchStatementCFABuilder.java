/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import static org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge.assume;

import java.util.List;
import javax.annotation.Nonnull;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.SwitchStatement;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.CFARemoveUnreachable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;
import org.sosy_lab.cpachecker.cfa.types.js.JSType;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CFAUtils.AssumeEdges;

@SuppressWarnings("ResultOfMethodCallIgnored")
class SwitchStatementCFABuilder implements SwitchStatementAppendable {

  @Override
  public void append(final JavaScriptCFABuilder pBuilder, final SwitchStatement pSwitchStatement) {
    @SuppressWarnings("unused")
    final Builder builder = new Builder(pBuilder, pSwitchStatement);
  }

  private static final class Builder {
    /** The node after the switch statement. */
    @Nonnull final CFANode exitNode;

    /** Builder of the path that is taken when all cases are not matched. */
    @Nonnull private final JavaScriptCFABuilder elseCaseBuilder;

    /**
     * Builder of the path that is taken when (the statements of) all cases are walked through (as
     * if no continue-, break- and return-statements would exist). The fall-through-edges that are
     * never take are removed later by {@link CFARemoveUnreachable}.
     */
    private JavaScriptCFABuilder currentCaseBuilder;

    /**
     * The value of the switch statement that is matched with the expressions of the switch-cases.
     */
    @Nonnull private final JSExpression value;

    /**
     * Node that is added to {@link #elseCaseBuilder} and has a leaving (dummy assume) edge to the
     * default case statements and a leaving (dummy assume) edge to continue the {@link
     * #elseCaseBuilder}. This node and its leaving edges are later removed, when all non matching
     * paths have been added to the {@link #elseCaseBuilder}. Only then is the real node known from
     * which the real edge leads to the standard case.
     */
    private CFANode dummyEntryToDefaultCase = null;

    @SuppressWarnings("unchecked")
    private Builder(final JavaScriptCFABuilder pBuilder, final SwitchStatement pSwitchStatement) {
      exitNode = pBuilder.createNode();
      elseCaseBuilder = pBuilder.copyWith(new SwitchScope(pBuilder.getScope(), exitNode));
      value = pBuilder.append(pSwitchStatement.getExpression());
      visitSwitchStatements(pSwitchStatement.statements());
      if (currentCaseBuilder == null) {
        // switch has no cases
        appendEdgeToExitNode(pBuilder);
      } else {
        if (dummyEntryToDefaultCase == null) {
          // switch statement has no default case
          appendEdgeToExitNode(elseCaseBuilder);
        } else {
          replaceEntryToDefaultCase();
        }
        appendEdgeToExitNode(currentCaseBuilder);
        pBuilder.append(elseCaseBuilder).append(currentCaseBuilder);
      }
    }

    private void appendEdgeToExitNode(final JavaScriptCFABuilder pBuilder) {
      pBuilder.appendEdge(exitNode, DummyEdge.withDescription("END switch"));
    }

    private void replaceEntryToDefaultCase() {
      assert dummyEntryToDefaultCase.getNumLeavingEdges() == 2;
      final AssumeEdges assumeEdges = CFAUtils.leavingAssumeEdges(dummyEntryToDefaultCase);
      CFACreationUtils.removeEdgeFromNodes(assumeEdges.falseEdge);
      CFACreationUtils.removeEdgeFromNodes(assumeEdges.trueEdge);
      addBlankEdge(dummyEntryToDefaultCase, assumeEdges.falseEdge.getSuccessor());
      addBlankEdge(elseCaseBuilder.getExitNode(), assumeEdges.trueEdge.getSuccessor());
    }

    private void addBlankEdge(final CFANode pPredecessor, final CFANode pSuccessor) {
      CFACreationUtils.addEdgeToCFA(
          new BlankEdge("", FileLocation.DUMMY, pPredecessor, pSuccessor, ""),
          elseCaseBuilder.getLogger());
    }

    void visitSwitchStatements(final List<Statement> pStatements) {
      for (final Statement current : pStatements) {
        if (current instanceof SwitchCase) {
          visitSwitchCase((SwitchCase) current);
        } else {
          assert currentCaseBuilder != null;
          currentCaseBuilder.append(current);
        }
      }
    }

    private void visitSwitchCase(final SwitchCase pNextCase) {
      if (pNextCase.isDefault()) {
        dummyEntryToDefaultCase = elseCaseBuilder.getExitNode();
      }
      final JSExpression valueMatchesCase = getBranchConditionOf(pNextCase);
      if (currentCaseBuilder == null) {
        // next case is the first case
        currentCaseBuilder = elseCaseBuilder.copy().appendEdge(assume(valueMatchesCase, true));
      } else {
        addFallThroughEdge();
        elseCaseBuilder.addParseResult(
            elseCaseBuilder
                .copy()
                .appendEdge(currentCaseBuilder.getExitNode(), assume(valueMatchesCase, true))
                .getParseResult());
      }
      elseCaseBuilder.appendEdge(assume(valueMatchesCase, false));
    }

    @Nonnull
    private JSExpression getBranchConditionOf(final SwitchCase pNextCase) {
      if (pNextCase.isDefault()) {
        return SwitchDefaultCaseDummyCondition.instance;
      }
      final JSExpression caseExpression = elseCaseBuilder.append(pNextCase.getExpression());
      return new JSBinaryExpression(
          FileLocation.DUMMY, value, caseExpression, BinaryOperator.EQUAL_EQUAL_EQUAL);
    }

    /**
     * Always add a fall-through-edge from the current case to the next case. This edge might be
     * removed later by {@link CFARemoveUnreachable}.
     */
    private void addFallThroughEdge() {
      assert currentCaseBuilder != null;
      currentCaseBuilder.appendEdge(DummyEdge.withDescription("SWITCH CASE: fall through"));
    }

    /**
     * The condition used in the dummy assume edge that is appended to {@link
     * #dummyEntryToDefaultCase}.
     */
    private static final class SwitchDefaultCaseDummyCondition implements JSExpression {

      static final SwitchDefaultCaseDummyCondition instance = new SwitchDefaultCaseDummyCondition();

      private static final long serialVersionUID = -4566031217467905622L;

      private SwitchDefaultCaseDummyCondition() {}

      @Override
      public <R, X extends Exception> R accept(final JSExpressionVisitor<R, X> v) {
        throw new CFAGenerationRuntimeException(
            "Dummy condition should never been visited"
                + " since it should have been removed from the CFA");
      }

      @Override
      public JSType getExpressionType() {
        return JSAnyType.ANY;
      }

      @Override
      public FileLocation getFileLocation() {
        return FileLocation.DUMMY;
      }

      @Override
      public String toASTString(final boolean pQualified) {
        return "switch default case dummy condition";
      }

      @Override
      public String toParenthesizedASTString(final boolean pQualified) {
        return "(" + toASTString() + ")";
      }
    }
  }
}
