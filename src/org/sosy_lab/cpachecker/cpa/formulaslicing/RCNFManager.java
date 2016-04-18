package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.google.common.math.LongMath;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;
import org.sosy_lab.solver.visitors.DefaultBooleanFormulaVisitor;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;
import org.sosy_lab.solver.visitors.TraversalProcess;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Convert the formula to form *resembling* CNF, but without exponential
 * explosion and without introducing extra existential quantifiers.
 *
 * // TODO: move to a different package.
 */
@Options(prefix="cpa.slicing")
public class RCNFManager implements StatisticsProvider {

  @Option(description="Limit on the size of the resulting number of lemmas "
      + "from the explicit expansion", secure=true)
  private int expansionResultSizeLimit = 1000;

  @Option(secure=true, description="Quantifier elimination strategy",
      toUppercase=true)
  private BOUND_VARS_HANDLING boundVarsHandling =
      BOUND_VARS_HANDLING.QE_LIGHT_THEN_DROP;

  public enum BOUND_VARS_HANDLING {

    /**
     * Run best-effort quantifier elimination and then over-approximate lemmas
     * which still have quantifiers.
     */
    QE_LIGHT_THEN_DROP,

    /**
     * Run proper quantifier elimination.
     */
    QE,

    /**
     * Over-approximate all lemmas with quantifiers.
      */
    DROP
  }

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final RCNFConversionStatistics statistics;
  private final HashMap<BooleanFormula, Set<BooleanFormula>> conversionCache;

  public RCNFManager(FormulaManagerView pFmgr, Configuration options)
      throws InvalidConfigurationException{
    options.inject(this);
    bfmgr = pFmgr.getBooleanFormulaManager();
    fmgr = pFmgr;
    statistics = new RCNFConversionStatistics();
    conversionCache = new HashMap<>();
  }

  /**
   * @param input Input formula with at most one parent-level existential
   *              quantifier.
   */
  public Set<BooleanFormula> toLemmas(BooleanFormula input) throws InterruptedException {
    Set<BooleanFormula> out = conversionCache.get(input);
    if (out != null) {
      return out;
    }

    BooleanFormula result;
    try {
      switch (boundVarsHandling) {
        case QE_LIGHT_THEN_DROP:
          try {
            statistics.lightQuantifierElimination.start();
            result = fmgr.applyTactic(input, Tactic.QE_LIGHT);
          } finally {
            statistics.lightQuantifierElimination.stop();
          }
          break;
        case QE:
          try {
            statistics.quantifierElimination.start();
            result = fmgr.applyTactic(input, Tactic.QE);
          } finally {
            statistics.quantifierElimination.stop();
          }
          break;
        case DROP:
          result = input;
          break;
        default:
          throw new AssertionError("Unhandled case statement: " + boundVarsHandling);
      }
    } catch (UnsupportedOperationException e) {
      // chosen quantifier elimination tactic didn't work, so fallback to
      // method without elimination
      result = input;
    }
    BooleanFormula noBoundVars = dropBoundVariables(result);

    try {
      statistics.conversion.start();
      out = convert(noBoundVars);
    } finally {
      statistics.conversion.stop();
    }
    conversionCache.put(input, out);
    return out;
  }

  /**
   * @param input Formula with at most one outer-level existential
   *              quantifier, in NNF.
   */
  private BooleanFormula dropBoundVariables(BooleanFormula input)
      throws InterruptedException {

    Optional<BooleanFormula> body = fmgr.visit(quantifiedBodyExtractor, input);
    if (body.isPresent()) {
      return fmgr.filterLiterals(body.get(), new Predicate<BooleanFormula>() {
        @Override
        public boolean apply(BooleanFormula input) {
          return !hasBoundVariables(input);
        }
      });
    } else {

      // Does not have quantified variables.
      return input;
    }
  }

  public BooleanFormula factorize(BooleanFormula input) {
    return bfmgr.transformRecursively(new BooleanFormulaTransformationVisitor(fmgr) {

      /**
       * Flatten AND-.
       */
      @Override
      public BooleanFormula visitAnd(List<BooleanFormula> processed) {
        return bfmgr.and(bfmgr.toConjunctionArgs(bfmgr.and(processed), true));
      }

      /**
       * Factorize OR-.
       */
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

        assert intersection != null
            : "Should not be null for a non-zero number of operands.";

        BooleanFormula common = bfmgr.and(intersection);
        List<BooleanFormula> branches = new ArrayList<>();

        for (Set<BooleanFormula> args : argsAsConjunctions) {
          Set<BooleanFormula> newEl = Sets.difference(args, intersection);
          branches.add(bfmgr.and(newEl));
        }

        return bfmgr.and(common, bfmgr.or(branches));
      }
    }, input);
  }

  private BooleanFormula expandClause(final BooleanFormula input) {
    return bfmgr.visit(new DefaultBooleanFormulaVisitor<BooleanFormula>() {
      @Override
      protected BooleanFormula visitDefault() {
        return input;
      }

      @Override
      public BooleanFormula visitOr(List<BooleanFormula> operands) {
        long sizeAfterExpansion = 1;

        List<Set<BooleanFormula>> asConjunctions = new ArrayList<>();
        for (BooleanFormula op : operands) {
          Set<BooleanFormula> out = bfmgr.toConjunctionArgs(op, true);
          try {
            sizeAfterExpansion = LongMath.checkedMultiply(
                sizeAfterExpansion, out.size()
            );
          } catch (ArithmeticException ex) {
            sizeAfterExpansion = expansionResultSizeLimit + 1;
            break;
          }
          asConjunctions.add(out);
        }

        if (sizeAfterExpansion <= expansionResultSizeLimit) {
          // Perform recursive expansion.
          Set<List<BooleanFormula>> product = Sets.cartesianProduct(asConjunctions);
          Set<BooleanFormula> newArgs = new HashSet<>(product.size());
          for (List<BooleanFormula> l : product) {
            newArgs.add(bfmgr.or(l));
          }
          return bfmgr.and(newArgs);
        } else {
          return bfmgr.or(operands);
        }
      }
    }, input);
  }

  private Set<BooleanFormula> convert(BooleanFormula input) {
    BooleanFormula factorized = factorize(input);
    Set<BooleanFormula> factorizedLemmas =
        bfmgr.toConjunctionArgs(factorized, true);
    Set<BooleanFormula> out = new HashSet<>();
    for (BooleanFormula lemma : factorizedLemmas) {
      BooleanFormula expanded = expandClause(lemma);
      Set<BooleanFormula> expandedLemmas =
          bfmgr.toConjunctionArgs(expanded, true);
      out.addAll(expandedLemmas);
    }
    return out;
  }

  private boolean hasBoundVariables(BooleanFormula input) {
    final AtomicBoolean hasBound = new AtomicBoolean(false);
    fmgr.visitRecursively(new DefaultFormulaVisitor<TraversalProcess>() {
      @Override
      protected TraversalProcess visitDefault(Formula f) {
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitBoundVariable(Formula f, int deBruijnIdx) {
        hasBound.set(true);
        return TraversalProcess.ABORT;
      }
    }, input);
    return hasBound.get();
  }

  private final DefaultFormulaVisitor<Optional<BooleanFormula>>
      quantifiedBodyExtractor = new
      DefaultFormulaVisitor<Optional<BooleanFormula>> () {
        @Override
        protected Optional<BooleanFormula> visitDefault(Formula f) {
          return Optional.absent();
        }

        @Override
        public Optional<BooleanFormula> visitQuantifier(
            BooleanFormula f,
            Quantifier quantifier,
            List<Formula> boundVariables,
            BooleanFormula body) {
          return Optional.of(body);
        }
      };

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }

  private static class RCNFConversionStatistics implements Statistics {
    Timer lightQuantifierElimination = new Timer();
    Timer quantifierElimination = new Timer();
    Timer conversion = new Timer();

    @Override
    public void printStatistics(
        PrintStream out, Result result, ReachedSet reached) {
      printTimer(out, conversion, "RCNF conversion");
      printTimer(out, lightQuantifierElimination, "light quantifier "
          + "elimination");
      printTimer(out, quantifierElimination, "quantifier elimination");

    }

    @Override
    public String getName() {
      return "RCNF Conversion";
    }

    private void printTimer(PrintStream out, Timer t, String name) {
      out.printf("Time spent in %s: %s (Max: %s), (Avg: %s), (#intervals = %s)%n",
          name,
          t.getSumTime().formatAs(TimeUnit.SECONDS),
          t.getMaxTime().formatAs(TimeUnit.SECONDS),
          t.getAvgTime().formatAs(TimeUnit.SECONDS),
          t.getNumberOfIntervals());
    }
  }
}
