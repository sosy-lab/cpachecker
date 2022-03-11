// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * This class provides a flexible way of building path formulas. The initial {@link SSAMap} / {@link
 * PathFormula} and the {@link PathFormulaManager} do not have to be known before the corresponding
 * build-method is called. This allows e.g. to easily reorder parts of path formulas and much more.
 */
public class DefaultPathFormulaBuilder extends CachingPathFormulaBuilder {

  private abstract static class PathFormulaAndBuilder extends DefaultPathFormulaBuilder {

    protected final DefaultPathFormulaBuilder previousPathFormula;

    private PathFormulaAndBuilder(DefaultPathFormulaBuilder pPathFormulaAndBuilder) {
      previousPathFormula = pPathFormulaAndBuilder;
    }
  }

  private static class EdgePathFormulaAndBuilder extends PathFormulaAndBuilder {

    private CFAEdge edge;

    protected EdgePathFormulaAndBuilder(
        DefaultPathFormulaBuilder pPathFormulaAndBuilder, CFAEdge pEdge) {
      super(pPathFormulaAndBuilder);
      edge = pEdge;
    }

    @Override
    protected PathFormula buildImplementation(PathFormulaManager pPfmgr, PathFormula pathFormula)
        throws CPATransferException, InterruptedException {
      return pPfmgr.makeAnd(previousPathFormula.build(pPfmgr, pathFormula), edge);
    }
  }

  private static class ExpressionPathFormulaAndBuilder extends PathFormulaAndBuilder {

    private CExpression assumption;

    protected ExpressionPathFormulaAndBuilder(
        DefaultPathFormulaBuilder pPathFormulaAndBuilder, CExpression pAssumption) {
      super(pPathFormulaAndBuilder);
      assumption = pAssumption;
    }

    @Override
    protected PathFormula buildImplementation(PathFormulaManager pPfmgr, PathFormula pathFormula)
        throws CPATransferException, InterruptedException {
      return pPfmgr.makeAnd(previousPathFormula.build(pPfmgr, pathFormula), assumption);
    }
  }

  private static class PathFormulaOrBuilder extends DefaultPathFormulaBuilder {

    private PathFormulaBuilder first;
    private PathFormulaBuilder second;

    protected PathFormulaOrBuilder(PathFormulaBuilder first, PathFormulaBuilder second) {
      this.first = first;
      this.second = second;
    }

    @Override
    protected PathFormula buildImplementation(PathFormulaManager pPfmgr, PathFormula pathFormula)
        throws CPATransferException, InterruptedException {
      return pPfmgr.makeOr(first.build(pPfmgr, pathFormula), second.build(pPfmgr, pathFormula));
    }
  }

  @Override
  public PathFormulaBuilder makeOr(PathFormulaBuilder other) {
    return new PathFormulaOrBuilder(this, other);
  }

  @Override
  public PathFormulaBuilder makeAnd(CFAEdge pEdge) {
    return new EdgePathFormulaAndBuilder(this, pEdge);
  }

  @Override
  public PathFormulaBuilder makeAnd(CExpression pAssumption) {
    return new ExpressionPathFormulaAndBuilder(this, pAssumption);
  }

  @Override
  protected PathFormula buildImplementation(PathFormulaManager pPfmgr, PathFormula pathFormula)
      throws CPATransferException, InterruptedException {
    return pathFormula;
  }

  public static class Factory implements PathFormulaBuilderFactory {
    @Override
    public DefaultPathFormulaBuilder create() {
      return new DefaultPathFormulaBuilder();
    }
  }
}
