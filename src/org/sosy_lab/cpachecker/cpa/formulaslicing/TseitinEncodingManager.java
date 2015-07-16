package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView.RecursiveBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * TODO: Class Description
 */
public class TseitinEncodingManager extends RecursiveBooleanFormulaVisitor {
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final List<BooleanFormula> clauses;
  private final UniqueIdGenerator idGenerator;

  static class BooleanFormulaRef {
    BooleanFormula ref;
  }

  protected TseitinEncodingManager(
      FormulaManagerView pFmgr) {
    super(pFmgr);
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    clauses = new ArrayList<>();
    idGenerator = new UniqueIdGenerator();
  }

  private BooleanFormula mkFresh() {
    // todo: keep meta-DAG to express dependencies.
    // todo: factor string out in private static final var.
    return bfmgr.makeVariable("TSEITIN_" + idGenerator.getFreshId());
  }

  @Override
  protected Void visitAnd(BooleanFormula... operands) {
    return null;
  }


  @Override
  protected Void visitTrue() {
    return null;
  }

  @Override
  protected Void visitFalse() {
    return null;
  }

  @Override
  protected Void visitAtom(BooleanFormula atom) {
    return null;
  }

  @Override
  protected Void visitNot(BooleanFormula operand) {
    // todo: in this mode every expression is not visited more than once.
    // sometimes we do apparently need to visit the same expression multiple
    // times.
    // Can't it potentially lead to exponential explosion?
    // (e.g. == without "let" terms)
    return null;
  }


  @Override
  protected Void visitOr(BooleanFormula... operand) {
    return null;
  }

  @Override
  protected Void visitEquivalence(BooleanFormula operand1,
      BooleanFormula operand2) {
    return null;
  }

  @Override
  protected Void visitImplication(BooleanFormula operand1,
      BooleanFormula operand2) {
    return null;
  }

  @Override
  protected Void visitIfThenElse(BooleanFormula condition,
      BooleanFormula thenFormula, BooleanFormula elseFormula) {
    return null;
  }
}
