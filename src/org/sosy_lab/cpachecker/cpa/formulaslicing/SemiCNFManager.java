package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Convert the formula to form *resembling* CNF, but without exponential
 * explosion and without introducing extra existential quantifiers.
 */
@Options(prefix="cpa.slicing")
public class SemiCNFManager {

  @Option(description="Limit for explicit CNF expansion (potentially exponential otherwise)",
      secure=true)
  private int expansionDepthLimit = 1;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;

  private final HashMap<BooleanFormula, BooleanFormula> conversionCache;

  public SemiCNFManager(FormulaManagerView pFmgr, Configuration options)
      throws InvalidConfigurationException{
    options.inject(this);
    bfmgr = pFmgr.getBooleanFormulaManager();
    fmgr = pFmgr;
    conversionCache = new HashMap<>();
  }

  public Set<BooleanFormula> toClauses(BooleanFormula input) throws InterruptedException {
    Set<BooleanFormula> conjunctionArgs = bfmgr.toConjunctionArgs(convert(fmgr.simplify(input)), true);
    return Sets.filter(
        conjunctionArgs,
        new Predicate<BooleanFormula>() {
          @Override
          public boolean apply(BooleanFormula input) {
            // Remove redundant constraints.
            assert bfmgr.toConjunctionArgs(input, true).size() == 1;
            return !bfmgr.isTrue(fmgr.simplify(input));
          }
        }
    );
  }

  /**
   * Convert the formula to semi-CNF form.
   */
  public BooleanFormula convert(BooleanFormula input) throws InterruptedException {
    BooleanFormula out = conversionCache.get(input);
    if (out != null) {
      return out;
    }
    final AtomicInteger expansionsAllowed = new AtomicInteger(expansionDepthLimit);

    input = fmgr.applyTactic(input, Tactic.NNF);
    out = bfmgr.transformRecursively(new BooleanFormulaTransformationVisitor(fmgr) {

      /**
       * Flatten AND-.
       */
      @Override
      public BooleanFormula visitAnd(List<BooleanFormula> processed) {
        return bfmgr.and(bfmgr.toConjunctionArgs(bfmgr.and(processed), true));
      }

      @Override
      public BooleanFormula visitOr(List<BooleanFormula> processed) {

        Set<BooleanFormula> intersection = null;
        ArrayList<Set<BooleanFormula>> argsAsConjunctions = new ArrayList<>();
        for (BooleanFormula op : processed) {
          Set<BooleanFormula> args = bfmgr.toConjunctionArgs(op, true);

          argsAsConjunctions.add(args);

          // Factor out the common term.
          if (intersection == null) {
            intersection = args;
          } else {
            intersection = Sets.intersection(intersection, args);
          }
        }

        assert intersection != null : "Should not be null for a non-zero number of operands.";

        BooleanFormula common = bfmgr.and(intersection);
        List<BooleanFormula> branches = new ArrayList<>();

        ArrayList<Set<BooleanFormula>> argsAsConjunctionsWithoutIntersection = new ArrayList<>();
        for (Set<BooleanFormula> args : argsAsConjunctions) {
          Set<BooleanFormula> newEl = Sets.difference(args, intersection);
          argsAsConjunctionsWithoutIntersection.add(newEl);
          branches.add(bfmgr.and(newEl));
        }

        if (expansionsAllowed.get() > 0) {
          expansionsAllowed.decrementAndGet();

          // Perform recursive expansion.
          Set<List<BooleanFormula>> product = Sets.cartesianProduct(argsAsConjunctionsWithoutIntersection);
          List<BooleanFormula> newArgs = new ArrayList<>(product.size() + 1);
          newArgs.add(common);
          for (List<BooleanFormula> l : product) {
            newArgs.add(disjunctionToImplication(l));
          }
          return bfmgr.and(newArgs);
        } else {
          return bfmgr.and(common, disjunctionToImplication(branches));
        }
      }

    }, input);
    conversionCache.put(input, out);
    return out;
  }

  private BooleanFormula disjunctionToImplication(List<BooleanFormula> disjunctionArguments) {
    return bfmgr.implication(
        bfmgr.not(disjunctionArguments.get(0)),
        fmgr.simplify(bfmgr.or(disjunctionArguments.subList(1, disjunctionArguments.size())))
    );
  }
}
