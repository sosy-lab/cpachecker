package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;
import org.sosy_lab.solver.visitors.TraversalProcess;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Convert the formula to form *resembling* CNF, but without exponential
 * explosion and without introducing extra existential quantifiers.
 *
 * // TODO: move to a different package.
 */
@Options(prefix="cpa.slicing")
public class RCNFManager implements StatisticsProvider {

  @Option(description="Limit for explicit CNF expansion (potentially exponential otherwise)",
      secure=true)
  private int expansionDepthLimit = 1;

  @Option(description="Limit on the size of the resulting number of lemmas from the explicit "
      + "CNF expansion", secure=true)
  private int expansionResultSizeLimit = 1000;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final RCNFConversionStatistics statistics;

  private final HashMap<BooleanFormula, Set<BooleanFormula>> conversionCache;
  private final HashMap<PathFormula, Set<BooleanFormula>> conversionQeCache;

  public RCNFManager(FormulaManagerView pFmgr, Configuration options)
      throws InvalidConfigurationException{
    options.inject(this);
    bfmgr = pFmgr.getBooleanFormulaManager();
    fmgr = pFmgr;
    statistics = new RCNFConversionStatistics();
    conversionCache = new HashMap<>();
    conversionQeCache = new HashMap<>();
  }

  public Set<BooleanFormula> toLemmas(BooleanFormula input) throws InterruptedException {
    Set<BooleanFormula> out = conversionCache.get(input);
    if (out != null) {
      return out;
    }
    Set<BooleanFormula> conjunctionArgs;
    try {
      statistics.conversion.start();
      conjunctionArgs = bfmgr.toConjunctionArgs(
          convert(fmgr.simplify(input)), true);
    } finally {
      statistics.conversion.stop();
    }
    out = Sets.filter(
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
    conversionCache.put(input, out);
    return out;
  }

  /**
   * Convert the input to RCNF, apply best-effort QE elimination to
   * (implicitly) quantified variables, over-approximate the lemmas which
   * still contain existentials.
   */
  public Set<BooleanFormula> toLemmasRemoveExistentials(PathFormula input)
      throws InterruptedException {
    Set<BooleanFormula> out = conversionQeCache.get(input);
    if (out != null) {
      return out;
    }
    BooleanFormula quantified = fmgr.quantifyDeadVariables(
        input.getFormula(), input.getSsa());
    // OK so if we keep a timer on this thing, how do we return the value
    // for it?
    BooleanFormula qeLightResult;
    try {
      statistics.lightQuantifierElimination.start();
      qeLightResult = fmgr.applyTactic(quantified, Tactic.QE_LIGHT);
    } finally {
      statistics.lightQuantifierElimination.stop();
    }
    out = dropExistentials(qeLightResult);
    conversionQeCache.put(input, out);
    return out;
  }

  private Set<BooleanFormula> dropExistentials(BooleanFormula input)
      throws InterruptedException {
    Optional<BooleanFormula> body = fmgr.visit(quantifiedBodyExtractor, input);
    if (body.isPresent()) {

      // Has quantified variables.
      Set<BooleanFormula> lemmas = toLemmas(body.get());
      return Sets.filter(lemmas, Predicates.not(hasBoundVariables));
    } else {

      // Does not have quantified variables.
      return toLemmas(input);
    }
  }

  /**
   * Convert the formula to RCNF form.
   */
  private BooleanFormula convert(BooleanFormula input) throws
                                                    InterruptedException {
    final AtomicInteger expansionsAllowed = new AtomicInteger(expansionDepthLimit);

    input = fmgr.applyTactic(input, Tactic.NNF);
    return bfmgr.transformRecursively(new BooleanFormulaTransformationVisitor(fmgr) {

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

        BigInteger expectedSizeAfterExpansion = BigInteger.ONE;

        ArrayList<Set<BooleanFormula>> argsAsConjunctionsWithoutIntersection = new ArrayList<>();
        for (Set<BooleanFormula> args : argsAsConjunctions) {
          Set<BooleanFormula> newEl = Sets.difference(args, intersection);
          argsAsConjunctionsWithoutIntersection.add(newEl);
          branches.add(bfmgr.and(newEl));
          expectedSizeAfterExpansion = expectedSizeAfterExpansion.multiply(
              BigInteger.valueOf(newEl.size()));
        }

        // TODO: this is very hacky, e.g. consider a sequence of disjunctions:
        // the first one gets expanded, but not the second one.
        if (expansionsAllowed.get() > 0 &&
            expectedSizeAfterExpansion.compareTo(BigInteger.valueOf(expansionResultSizeLimit)) == -1) {
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

          // TODO: experiment with other conversion formats.
          return bfmgr.and(common, disjunctionToImplication(branches));
        }
      }

    }, input);
  }

  private BooleanFormula disjunctionToImplication(List<BooleanFormula> disjunctionArguments) {
    return bfmgr.implication(
        bfmgr.not(disjunctionArguments.get(0)),
        fmgr.simplify(bfmgr.or(disjunctionArguments.subList(1, disjunctionArguments.size())))
    );
  }

  private final Predicate<BooleanFormula> hasBoundVariables = new Predicate<BooleanFormula>() {
    @Override
    public boolean apply(BooleanFormula input) {
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
  };

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
    Timer conversion = new Timer();

    @Override
    public void printStatistics(
        PrintStream out, Result result, ReachedSet reached) {
      printTimer(out, conversion, "RCNF conversion");
      printTimer(out, lightQuantifierElimination, "quantifier elimination");

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
