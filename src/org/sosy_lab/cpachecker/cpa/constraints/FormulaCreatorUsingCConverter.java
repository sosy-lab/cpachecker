// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.SymbolicExpressionToCExpressionTransformer;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Creator for {@link Formula}s using a given {@link CtoFormulaConverter} for creating {@link
 * BooleanFormula}s out of {@link Constraint}s.
 *
 * <p>The properties responsible for the behaviour of PredicateCPA's formula handling influence the
 * behaviour of this class, too. A number of important properties can be found in the classes {@link
 * FormulaEncodingOptions} and {@link FormulaManagerView}.
 */
public class FormulaCreatorUsingCConverter implements FormulaCreator {

  private final CtoFormulaConverter toFormulaTransformer;

  private final String functionName;

  public FormulaCreatorUsingCConverter(
      final CtoFormulaConverter pConverter, final String pFunctionName) {
    toFormulaTransformer = pConverter;
    functionName = pFunctionName;
  }

  @Override
  public BooleanFormula createFormula(final Constraint pConstraint)
      throws UnrecognizedCodeException, InterruptedException {

    final SymbolicExpressionToCExpressionTransformer toExpressionTransformer =
        new SymbolicExpressionToCExpressionTransformer();

    CExpression constraintExpression = pConstraint.accept(toExpressionTransformer);

    return toFormulaTransformer.makePredicate(
        constraintExpression, getDummyEdge(), functionName, getSsaMapBuilder());
  }

  private CFAEdge getDummyEdge() {
    return DummyEdge.getInstance(functionName);
  }

  private SSAMap.SSAMapBuilder getSsaMapBuilder() {
    return SSAMap.emptySSAMap().builder();
  }

  private static class DummyEdge implements CFAEdge {

    private static final long serialVersionUID = -8457186174249491758L;

    private static final String UNKNOWN = "unknown";

    private static Map<String, DummyEdge> existingEdges = new HashMap<>();

    private final CFANode dummyNode;

    private DummyEdge(String pFunctionName) {
      dummyNode = CFANode.newDummyCFANode(pFunctionName);
    }

    public static DummyEdge getInstance(String pFunctionName) {
      DummyEdge edge = existingEdges.get(pFunctionName);

      if (edge == null) {
        edge = new DummyEdge(pFunctionName);
        existingEdges.put(pFunctionName, edge);
      }

      return edge;
    }

    @Override
    public CFAEdgeType getEdgeType() {
      return CFAEdgeType.BlankEdge;
    }

    @Override
    public CFANode getPredecessor() {
      return dummyNode;
    }

    @Override
    public CFANode getSuccessor() {
      return dummyNode;
    }

    @Override
    public Optional<AAstNode> getRawAST() {
      return Optional.empty();
    }

    @Override
    public int getLineNumber() {
      return 0;
    }

    @Override
    public FileLocation getFileLocation() {
      return FileLocation.DUMMY;
    }

    @Override
    public String getRawStatement() {
      return UNKNOWN;
    }

    @Override
    public String getCode() {
      return UNKNOWN;
    }

    @Override
    public String getDescription() {
      return UNKNOWN;
    }
  }
}
