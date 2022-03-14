// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.Tactic;
import org.sosy_lab.java_smt.api.visitors.DefaultBooleanFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

/**
 * Convert the formula to a quantifier-free form *resembling* CNF (relaxed conjunctive normal form),
 * but without exponential explosion and without introducing extra existential quantifiers.
 */
@Options(prefix = "rcnf")
public class RCNFManager implements StatisticsProvider {

  @Option(
      description =
          "Limit on the size of the resulting number of lemmas " + "from the explicit expansion",
      secure = true)
  private int expansionResultSizeLimit = 100;

  @Option(secure = true, description = "Quantifier elimination strategy", toUppercase = true)
  private BOUND_VARS_HANDLING boundVarsHandling = BOUND_VARS_HANDLING.QE_LIGHT_THEN_DROP;

  @Option(
      secure = true,
      description =
          "Expand equality atoms. E.g. 'x=a' gets "
              + "expanded into 'x >= a AND x <= a'. Can lead to stronger weakenings.")
  private boolean expandEquality = false;

  public enum BOUND_VARS_HANDLING {

    /**
     * Run best-effort quantifier elimination and then over-approximate lemmas which still have
     * quantifiers.
     */
    QE_LIGHT_THEN_DROP,

    /** Run proper quantifier elimination. */
    QE,

    /** Over-approximate all lemmas with quantifiers. */
    DROP
  }

  private FormulaManagerView fmgr = null;
  private BooleanFormulaManager bfmgr = null;
  private final RCNFConversionStatistics statistics;
  private final Map<BooleanFormula, ImmutableSet<BooleanFormula>> conversionCache;

  public RCNFManager(Configuration options) throws InvalidConfigurationException {
    options.inject(this);
    statistics = new RCNFConversionStatistics();
    conversionCache = new HashMap<>();
  }

  /**
   * Existentially quantify dead variables, and apply RCNF conversion.
   *
   * @return Set of lemmas, only have variables with latest SSA index.
   */
  public Set<BooleanFormula> toLemmasInstantiated(PathFormula pf, FormulaManagerView pFmgr)
      throws InterruptedException {
    BooleanFormula transition = pf.getFormula();
    SSAMap ssa = pf.getSsa();
    transition = pFmgr.filterLiterals(transition, input -> !hasDeadUf(input, ssa, pFmgr));
    BooleanFormula quantified = pFmgr.quantifyDeadVariables(transition, ssa);

    return toLemmas(quantified, pFmgr);
  }

  /**
   * Convert an input formula to RCNF form. A formula in RCNF form is a conjunction over
   * quantifier-free formulas.
   *
   * @param input Formula over-approximation with at most one parent-level existential quantifier.
   *     Contains only latest SSA indexes.
   * @param pFmgr Formula manager which performs the conversion.
   */
  public ImmutableSet<BooleanFormula> toLemmas(BooleanFormula input, FormulaManagerView pFmgr)
      throws InterruptedException {
    Preconditions.checkNotNull(pFmgr);
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();

    ImmutableSet<BooleanFormula> out = conversionCache.get(input);
    if (out != null) {
      statistics.conversionCacheHits++;
      return out;
    }

    BooleanFormula result;
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
          result = fmgr.getQuantifiedFormulaManager().eliminateQuantifiers(input);
        } catch (SolverException pE) {
          throw new UnsupportedOperationException("Unexpected solver error", pE);
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
   * @param input Formula with at most one outer-level existential quantifier, in NNF.
   */
  private BooleanFormula dropBoundVariables(BooleanFormula input) throws InterruptedException {

    Optional<BooleanFormula> body = fmgr.visit(input, quantifiedBodyExtractor);
    if (body.isPresent()) {
      return fmgr.filterLiterals(body.orElseThrow(), input1 -> !hasBoundVariables(input1));
    } else {

      // Does not have quantified variables.
      return input;
    }
  }

  private BooleanFormula factorize(BooleanFormula input) {
    return bfmgr.transformRecursively(
        input,
        new BooleanFormulaTransformationVisitor(fmgr) {

          /** Flatten AND-. */
          @Override
          public BooleanFormula visitAnd(List<BooleanFormula> processed) {
            return bfmgr.and(bfmgr.toConjunctionArgs(bfmgr.and(processed), false));
          }

          /** Factorize OR-. */
          @Override
          public BooleanFormula visitOr(List<BooleanFormula> processed) {

            Set<BooleanFormula> intersection = null;
            List<Set<BooleanFormula>> argsAsConjunctions = new ArrayList<>();
            for (BooleanFormula op : processed) {
              Set<BooleanFormula> args = bfmgr.toConjunctionArgs(op, false);

              argsAsConjunctions.add(args);

              // Factor out the common term.
              if (intersection == null) {
                intersection = args;
              } else {
                intersection = Sets.intersection(intersection, args);
              }
            }

            assert intersection != null : "Should not be null for a non-zero number of operands.";
            Set<BooleanFormula> commonTerms = intersection;

            BooleanFormula common = bfmgr.and(commonTerms);
            BooleanFormula branches =
                argsAsConjunctions.stream()
                    .map(args -> bfmgr.and(Sets.difference(args, commonTerms)))
                    .collect(bfmgr.toDisjunction());

            return bfmgr.and(common, branches);
          }
        });
  }

  private Iterable<BooleanFormula> expandClause(final BooleanFormula input) {
    return bfmgr.visit(
        input,
        new DefaultBooleanFormulaVisitor<Iterable<BooleanFormula>>() {
          @Override
          protected Iterable<BooleanFormula> visitDefault() {
            return ImmutableList.of(input);
          }

          @Override
          public Iterable<BooleanFormula> visitOr(List<BooleanFormula> operands) {
            long sizeAfterExpansion = 1;

            List<Set<BooleanFormula>> asConjunctions = new ArrayList<>();
            for (BooleanFormula op : operands) {
              Set<BooleanFormula> out = bfmgr.toConjunctionArgs(op, true);
              try {
                sizeAfterExpansion = Math.multiplyExact(sizeAfterExpansion, out.size());
              } catch (ArithmeticException ex) {
                sizeAfterExpansion = expansionResultSizeLimit + 1L;
                break;
              }
              asConjunctions.add(out);
            }

            if (sizeAfterExpansion <= expansionResultSizeLimit) {
              // Perform recursive expansion.
              Set<List<BooleanFormula>> product = Sets.cartesianProduct(asConjunctions);
              return from(product).transform(bfmgr::or);
            } else {
              return ImmutableList.of(bfmgr.or(operands));
            }
          }
        });
  }

  private ImmutableSet<BooleanFormula> convert(BooleanFormula input) {
    BooleanFormula factorized = factorize(input);
    Set<BooleanFormula> factorizedLemmas = bfmgr.toConjunctionArgs(factorized, true);
    ImmutableSet.Builder<BooleanFormula> out = ImmutableSet.builder();
    for (BooleanFormula lemma : factorizedLemmas) {
      Iterable<BooleanFormula> expandedLemmas = expandClause(lemma);
      for (BooleanFormula l : expandedLemmas) {
        if (expandEquality) {
          out.addAll(bfmgr.toConjunctionArgs(transformEquality(l), false));
        } else {
          out.add(l);
        }
      }
    }
    return out.build();
  }

  /** Transform {@code a = b} to {@code a >= b /\ a <= b}. */
  private BooleanFormula transformEquality(BooleanFormula input) {
    return fmgr.visit(
        input,
        new DefaultFormulaVisitor<BooleanFormula>() {
          @Override
          protected BooleanFormula visitDefault(Formula f) {
            return (BooleanFormula) f;
          }

          @Override
          public BooleanFormula visitFunction(
              Formula f, List<Formula> newArgs, FunctionDeclaration<?> functionDeclaration) {
            if (functionDeclaration.getKind() == FunctionDeclarationKind.EQ
                && fmgr.getFormulaType(newArgs.get(0)).isNumeralType()) {
              Preconditions.checkState(newArgs.size() == 2);
              Formula a = newArgs.get(0);
              Formula b = newArgs.get(1);
              return bfmgr.and(
                  fmgr.makeGreaterOrEqual(a, b, true), fmgr.makeLessOrEqual(a, b, true));
            } else {
              return (BooleanFormula) f;
            }
          }
        });
  }

  private boolean hasBoundVariables(BooleanFormula input) {
    final AtomicBoolean hasBound = new AtomicBoolean(false);
    fmgr.visitRecursively(
        input,
        new DefaultFormulaVisitor<TraversalProcess>() {
          @Override
          protected TraversalProcess visitDefault(Formula f) {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitBoundVariable(Formula f, int deBruijnIdx) {
            hasBound.set(true);
            return TraversalProcess.ABORT;
          }
        });
    return hasBound.get();
  }

  private final DefaultFormulaVisitor<Optional<BooleanFormula>> quantifiedBodyExtractor =
      new DefaultFormulaVisitor<>() {
        @Override
        protected Optional<BooleanFormula> visitDefault(Formula f) {
          return Optional.empty();
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
    int conversionCacheHits = 0;

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      printTimer(out, conversion, "RCNF conversion");
      printTimer(out, lightQuantifierElimination, "light quantifier " + "elimination");
      printTimer(out, quantifierElimination, "quantifier elimination");
    }

    @Override
    public String getName() {
      return "RCNF Conversion";
    }

    private void printTimer(PrintStream out, Timer t, String name) {
      out.printf(
          "Time spent in %s: %s (Max: %s), (Avg: %s), (#calls = %s), " + "(#cached = %d) %n",
          name,
          t.getSumTime().formatAs(TimeUnit.SECONDS),
          t.getMaxTime().formatAs(TimeUnit.SECONDS),
          t.getAvgTime().formatAs(TimeUnit.SECONDS),
          t.getNumberOfIntervals(),
          conversionCacheHits);
    }
  }

  private boolean hasDeadUf(BooleanFormula atom, final SSAMap pSSAMap, FormulaManagerView pFmgr) {
    final AtomicBoolean out = new AtomicBoolean(false);
    pFmgr.visitRecursively(
        atom,
        new DefaultFormulaVisitor<TraversalProcess>() {
          @Override
          protected TraversalProcess visitDefault(Formula f) {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitFunction(
              Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
            if (functionDeclaration.getKind() == FunctionDeclarationKind.UF) {
              if (pFmgr.isIntermediate(functionDeclaration.getName(), pSSAMap)) {
                out.set(true);
                return TraversalProcess.ABORT;
              }
            }
            return TraversalProcess.CONTINUE;
          }
        });
    return out.get();
  }
}
