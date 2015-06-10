package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView.BooleanFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.collect.ImmutableList;

/**
 * TODO: Class Description
 */
class OverApproximationVisitor
    extends BooleanFormulaVisitor<BooleanFormula> {
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final SSAMap ssa;

  private OverApproximationVisitor(
      FormulaManagerView pFmgr, SSAMap pInitialSSA) {
    super(pFmgr);
    ssa = pInitialSSA;
    bfmgr = pFmgr.getBooleanFormulaManager();
    fmgr = pFmgr;
  }

  static OverApproximationVisitor bind(FormulaManagerView fmgr, SSAMap initialSSA) {
    return new OverApproximationVisitor(fmgr, initialSSA);
  }

  @Override
  protected BooleanFormula visitTrue() {
    return bfmgr.makeBoolean(true);
  }

  @Override
  protected BooleanFormula visitFalse() {
    return bfmgr.makeBoolean(false);
  }

  @Override
  protected BooleanFormula visitAtom(BooleanFormula atom) {
    Set<String> vars = fmgr.extractFunctionNames(atom, true);
    // todo: converting into NNF vs. tracking NOT-s manually.
    // might be worth it to try to profile the difference.
    boolean isAtomFinal = true;
    for (String s : vars) {
      boolean isFinal;
      Pair<String, Integer> p = FormulaManagerView.parseName(s);
      if (p.getSecond() == null) {
        isFinal = true; // Constant;
      } else {
        int idx = p.getSecondNotNull();
        String varName = p.getFirstNotNull();
        if (ssa.getIndex(varName) == idx) {
          isFinal = true;
        } else {
          isFinal = false;
        }
      }
      if (!isFinal) {
        isAtomFinal = false;
      }
    }
    if (!isAtomFinal) {
      // todo: make sure we are in NNF.
      return bfmgr.makeBoolean(true);
    }
    return atom;
  }

  @Override
  protected BooleanFormula visitNot(BooleanFormula operand) {
    // In NNF the not- part is treated atomically with the atom.
    return visitAtom(operand);
  }

  @Override
  protected BooleanFormula visitAnd(BooleanFormula... operands) {
    return bfmgr.and(ImmutableList.copyOf(operands));
  }

  @Override
  protected BooleanFormula visitOr(BooleanFormula... operands) {
    return bfmgr.or(ImmutableList.copyOf(operands));
  }

  @Override
  protected BooleanFormula visitEquivalence(BooleanFormula operand1,
      BooleanFormula operand2) {
    // todo: check?
    throw new UnsupportedOperationException();
  }

  @Override
  protected BooleanFormula visitImplication(BooleanFormula operand1,
      BooleanFormula operand2) {
    // todo: check.
    throw new UnsupportedOperationException();
  }

  @Override
  protected BooleanFormula visitIfThenElse(BooleanFormula condition,
      BooleanFormula thenFormula, BooleanFormula elseFormula) {
    // todo: check.
    throw new UnsupportedOperationException();
  }
}
