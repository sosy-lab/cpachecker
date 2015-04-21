package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;

public class FormulaLinearizationManager {
  private final UnsafeFormulaManager ufmgr;
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifmgr;

  public static final String CHOICE_VAR_NAME = "__POLICY_CHOICE_";
  private final UniqueIdGenerator choiceVarCounter = new UniqueIdGenerator();

  public FormulaLinearizationManager(UnsafeFormulaManager pUfmgr,
      BooleanFormulaManager pBfmgr, FormulaManagerView pFmgr,
      NumeralFormulaManagerView<IntegerFormula, IntegerFormula> pIfmgr) {
    ufmgr = pUfmgr;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    ifmgr = pIfmgr;
  }

  /**
   * Convert non-concave statements into disjunctions.
   *
   * At the moment handles:
   *
   *  x NOT(EQ(A, B)) => (A > B) \/ (A < B)
   */
  public BooleanFormula linearize(BooleanFormula input) {
    return new LinearizationManager(
        fmgr, new HashMap<BooleanFormula, BooleanFormula>()
    ).visit(input);
  }

  /**
   * Annotate disjunctions with choice variables.
   */
  public BooleanFormula annotateDisjunctions(BooleanFormula input) {
    return new DisjunctionAnnotationVisitor(fmgr,
        new HashMap<BooleanFormula, BooleanFormula>()).visit(input);
  }

  private class LinearizationManager extends BooleanFormulaTransformationVisitor {

    protected LinearizationManager(
        FormulaManagerView pFmgr, Map<BooleanFormula, BooleanFormula> pCache) {
      super(pFmgr, pCache);
    }

    @Override
    protected BooleanFormula visitNot(BooleanFormula pOperand) {
      List<BooleanFormula> split = fmgr.splitNumeralEqualityIfPossible(pOperand);

      // Pattern matching on (NOT (= A B)).
      if (split.size() == 2) {
        return bfmgr.or(
            bfmgr.not(split.get(0)), bfmgr.not(split.get(1))
        );
      }
      return super.visitNot(pOperand);
    }
  }

  private class DisjunctionAnnotationVisitor extends BooleanFormulaTransformationVisitor {
    // todo: fail fast if the disjunction is inside NOT operator.

    protected DisjunctionAnnotationVisitor(
        FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache) {
      super(pFmgr, pCache);
    }

    @Override
    protected BooleanFormula visitOr(BooleanFormula... pOperands) {
      String freshVarName = CHOICE_VAR_NAME + choiceVarCounter.getFreshId();
      IntegerFormula var = ifmgr.makeVariable(freshVarName);
      List<BooleanFormula> newArgs = new ArrayList<>();
      for (int i=0; i < pOperands.length; i++) {
        newArgs.add(
            bfmgr.and(
                visitIfNotSeen(pOperands[i]),
                fmgr.makeEqual(var, ifmgr.makeNumber(i))
            )
        );
      }
      return bfmgr.or(newArgs);
    }
  }

  /**
   * Removes disjunctions from the {@code input} formula, by replacing them
   * with arguments which were used to generate the {@code model}.
   */
  public BooleanFormula enforceChoice(
      final BooleanFormula input,
      final Iterable<Map.Entry<Model.AssignableTerm, Object>> model
  ) {
    Map<Formula, Formula> mapping = new HashMap<>();
    for (Map.Entry<Model.AssignableTerm, Object> entry : model) {
      String termName = entry.getKey().getName();
      if (termName.contains(CHOICE_VAR_NAME)) {
        BigInteger value = (BigInteger) entry.getValue();
        mapping.put(ifmgr.makeVariable(termName), ifmgr.makeNumber(value));
      }
    }
    BooleanFormula pathSelected = ufmgr.substitute(input, mapping);
    pathSelected = fmgr.simplify(pathSelected);
    return pathSelected;
  }
}
