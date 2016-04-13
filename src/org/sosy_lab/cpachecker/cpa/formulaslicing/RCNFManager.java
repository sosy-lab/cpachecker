package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Optional;
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
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FunctionDeclaration;
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

  @Option(secure=true, description="Quantifier elimination strategy",
      toUppercase=true)
  private QUANTIFIER_HANDLING quantifiedHandling =
      QUANTIFIER_HANDLING.QE_LIGHT_THEN_DROP;

  enum QUANTIFIER_HANDLING {

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
  private final BooleanFormulaTransformationVisitor
        dropQuantifiedLiteralsVisitor;

  private final HashMap<BooleanFormula, Set<BooleanFormula>> conversionCache;

  public RCNFManager(FormulaManagerView pFmgr, Configuration options)
      throws InvalidConfigurationException{
    options.inject(this);
    bfmgr = pFmgr.getBooleanFormulaManager();
    fmgr = pFmgr;
    statistics = new RCNFConversionStatistics();
    conversionCache = new HashMap<>();
    dropQuantifiedLiteralsVisitor = new BooleanFormulaTransformationVisitor(fmgr) {
      @Override
      public BooleanFormula visitAtom(
          BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> decl) {
        if (hasBoundVariables(pAtom)) {
          return bfmgr.makeBoolean(true);
        }
        return super.visitAtom(pAtom, decl);
      }

      @Override
      public BooleanFormula visitNot(BooleanFormula pOperand) {
        if (hasBoundVariables(pOperand)) {
          return bfmgr.makeBoolean(true);
        }
        return super.visitNot(pOperand);
      }
    };
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
    switch (quantifiedHandling) {
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
        throw new UnsupportedOperationException("Unexpected state");
    }

    // TODO: NNF does not work nicely with quantified variables.
    BooleanFormula noBoundVars = dropBoundVariables(result);

    try {
      statistics.conversion.start();
      out = bfmgr.toConjunctionArgs(convert(noBoundVars), true);
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
      BooleanFormula nnf = fmgr.applyTactic(body.get(), Tactic.NNF);
      return bfmgr.transformRecursively(dropQuantifiedLiteralsVisitor, nnf);
    } else {

      // Does not have quantified variables.
      return input;
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
