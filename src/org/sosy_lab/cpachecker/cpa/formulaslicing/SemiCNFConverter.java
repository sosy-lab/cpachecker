package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.visitors.DefaultBooleanFormulaVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Convert the formula to form *resembling* CNF, but without exponential
 * explosion and without introducing extra existential quantifiers.
 */
public class SemiCNFConverter {
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;

  public SemiCNFConverter(FormulaManagerView pFmgr) {
    bfmgr = pFmgr.getBooleanFormulaManager();
    fmgr = pFmgr;
  }

  public BooleanFormula toSemiCNF(BooleanFormula input) {

    return bfmgr.visit(new BooleanFormulaTransformationVisitor(fmgr) {

      /**
       * Flatten AND-.
       */
      @Override
      public BooleanFormula visitAnd(List<BooleanFormula> pOperands) {
        List<BooleanFormula> processed = visitIfNotSeen(pOperands);

        List<BooleanFormula> allArgs = new ArrayList<>();
        for (BooleanFormula op : processed) {
          Set<BooleanFormula> args = getConjunctionArgs(op);
          if (args.isEmpty()) {
            return bfmgr.and(processed);
          } else {
            allArgs.addAll(args);
          }
        }
        return bfmgr.and(allArgs);
      }

      /**
       * Factor out the common term in OR-.
       */
      @Override
      public BooleanFormula visitOr(List<BooleanFormula> pOperands) {
        List<BooleanFormula> processed = visitIfNotSeen(pOperands);

        Set<BooleanFormula> intersection = null;
        ArrayList<Set<BooleanFormula>> argsReceived = new ArrayList<>();
        for (BooleanFormula op : processed) {
          Set<BooleanFormula> args = getConjunctionArgs(op);

          if (args.isEmpty()) {
            // Fail fast.
            return bfmgr.or(processed);
          }

          argsReceived.add(args);
          if (intersection == null) {
            intersection = args;
          } else {
            intersection = Sets.intersection(intersection, args);
          }
        }

        if (intersection != null && !intersection.isEmpty()) {
          BooleanFormula head = bfmgr.and(intersection);
          List<BooleanFormula> options = new ArrayList<>();
          for (Set<BooleanFormula> args : argsReceived) {
            options.add(bfmgr.and(Sets.difference(args, intersection)));
          }
          return bfmgr.and(
              head,
              bfmgr.or(options)
          );
        } else {
          return bfmgr.or(processed);
        }
      }

    }, input);
  }

  private Set<BooleanFormula> getConjunctionArgs(BooleanFormula f) {
    return bfmgr.visit(new DefaultBooleanFormulaVisitor<Set<BooleanFormula>>() {
      @Override
      protected Set<BooleanFormula> visitDefault() {
        return ImmutableSet.of();
      }

      @Override
      public Set<BooleanFormula> visitAnd(List<BooleanFormula> operands) {
        return ImmutableSet.copyOf(operands);
      }
    }, f);
  }
}
