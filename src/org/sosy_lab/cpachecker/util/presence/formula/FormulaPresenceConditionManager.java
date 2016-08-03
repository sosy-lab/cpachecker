package org.sosy_lab.cpachecker.util.presence.formula;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceConditionManager;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;

import java.io.IOException;

public class FormulaPresenceConditionManager implements PresenceConditionManager {

  private final Solver solver;
  private final FormulaManagerView fmv;
  private final PathFormulaManager pfm;
  private final BooleanFormulaManagerView bfmgr;

  private final FormulaPresenceCondition truePresenceCondition;
  private final FormulaPresenceCondition falsePresenceCondition;

  public FormulaPresenceConditionManager(PathFormulaManager pPfm, Solver pSolver) {

    pfm = Preconditions.checkNotNull(pPfm);
    solver = Preconditions.checkNotNull(pSolver);
    fmv = solver.getFormulaManager();
    bfmgr = fmv.getBooleanFormulaManager();

    truePresenceCondition = new FormulaPresenceCondition(bfmgr.makeBoolean(true));
    falsePresenceCondition = new FormulaPresenceCondition(bfmgr.makeBoolean(false));
  }

  @Override
  public PresenceCondition makeTrue() {
    return truePresenceCondition;
  }

  @Override
  public PresenceCondition makeFalse() {
    return falsePresenceCondition;
  }

  @Override
  public PresenceCondition makeNegation(PresenceCondition pNegationOf) {
    FormulaPresenceCondition negationOf = (FormulaPresenceCondition) pNegationOf;

    if (pNegationOf.equals(truePresenceCondition)) {
      return falsePresenceCondition;
    } else if (pNegationOf.equals(falsePresenceCondition)) {
      return truePresenceCondition;
    }

    return new FormulaPresenceCondition(bfmgr.not(negationOf.getFormula()));
  }

  @Override
  public PresenceCondition makeOr(PresenceCondition pCond1, PresenceCondition pCond2) {
    FormulaPresenceCondition cond1 = (FormulaPresenceCondition) pCond1;
    FormulaPresenceCondition cond2 = (FormulaPresenceCondition) pCond2;

    return new FormulaPresenceCondition(bfmgr.or(cond1.getFormula(), cond2.getFormula()));
  }

  @Override
  public PresenceCondition makeAnd(PresenceCondition pCond1, PresenceCondition pCond2) {
    FormulaPresenceCondition cond1 = (FormulaPresenceCondition) pCond1;
    FormulaPresenceCondition cond2 = (FormulaPresenceCondition) pCond2;

    return new FormulaPresenceCondition(bfmgr.and(cond1.getFormula(), cond2.getFormula()));
  }

  @Override
  public PresenceCondition makeAnd(PresenceCondition pCond1, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {

    FormulaPresenceCondition cond1 = (FormulaPresenceCondition) pCond1;
    PathFormula cond1Pf = new PathFormula(cond1.getFormula(),
        SSAMap.emptySSAMap(), PointerTargetSet.emptyPointerTargetSet(), 0);

    PathFormula pf = pfm.makeAnd(cond1Pf, pEdge);

    return new FormulaPresenceCondition(fmv.uninstantiate(pf.getFormula()));
  }

  @Override
  public Appender dump(PresenceCondition pCond) {
    final FormulaPresenceCondition cond = (FormulaPresenceCondition) pCond;

    return new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        BooleanFormula f = fmv.simplify(cond.getFormula());
        fmv.dumpFormula(f).appendTo(pAppendable);
      }
    };

  }

  @Override
  public boolean checkEntails(PresenceCondition pCond1, PresenceCondition pCond2)
      throws InterruptedException {
    FormulaPresenceCondition cond1 = (FormulaPresenceCondition) pCond1;
    FormulaPresenceCondition cond2 = (FormulaPresenceCondition) pCond2;

    try {
      return solver.implies(cond1.getFormula(), cond2.getFormula());
    } catch (SolverException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public boolean checkConjunction(PresenceCondition pCond1, PresenceCondition pCond2)
      throws InterruptedException {
    FormulaPresenceCondition cond1 = (FormulaPresenceCondition) pCond1;
    FormulaPresenceCondition cond2 = (FormulaPresenceCondition) pCond2;

    BooleanFormula conjunction = bfmgr.and(cond1.getFormula(), cond2.getFormula());

    try {
      return !solver.isUnsat(conjunction);
    } catch (SolverException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public boolean checkSat(PresenceCondition pCond) throws InterruptedException {
    FormulaPresenceCondition cond = (FormulaPresenceCondition) pCond;

    if (pCond.equals(truePresenceCondition)) {
      return true;
    } else if (pCond.equals(falsePresenceCondition)) {
      return false;
    }

    try {
      return !solver.isUnsat(cond.getFormula());
    } catch (SolverException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public boolean checkEqualsTrue(PresenceCondition pCond)
      throws InterruptedException {

    FormulaPresenceCondition cond = (FormulaPresenceCondition) pCond;
    if (pCond.equals(truePresenceCondition)) {
      return true;
    }

    try {
      return solver.isUnsat(bfmgr.not(cond.getFormula()));
    } catch (SolverException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public boolean checkEqualsFalse(PresenceCondition pCond)
      throws InterruptedException {

    FormulaPresenceCondition cond = (FormulaPresenceCondition) pCond;
    if (pCond.equals(falsePresenceCondition)) {
      return true;
    }

    try {
      return solver.isUnsat(cond.getFormula());
    } catch (SolverException pE) {
      throw new RuntimeException(pE);
    }
  }

  @Override
  public PresenceCondition removeMarkerVariables(PresenceCondition pCond) {
    return null;
  }
}
