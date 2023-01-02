// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapParser;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.Converter.PrecisionConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.KeyValueStatistics;
import org.sosy_lab.java_smt.api.BooleanFormula;

@Options(prefix = "cpa.predicate")
public class PredicatePrecisionBootstrapper implements StatisticsProvider {

  @Option(
      secure = true,
      name = "abstraction.initialPredicates",
      description =
          "get an initial map of predicates from a list of files (see source"
              + " doc/examples/predmap.txt for an example)")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> predicatesFiles = ImmutableList.of();

  @Option(
      secure = true,
      description = "always check satisfiability at end of block, even if precision is empty")
  private boolean checkBlockFeasibility = false;

  @Options(prefix = "cpa.predicate.abstraction.initialPredicates")
  public static class InitialPredicatesOptions {

    @Option(
        secure = true,
        description = "Apply location-specific predicates to all locations in their function")
    private boolean applyFunctionWide = false;

    @Option(
        secure = true,
        description =
            "Apply location- and function-specific predicates globally (to all locations in the"
                + " program)")
    private boolean applyGlobally = false;

    @Option(
        secure = true,
        description =
            "when reading predicates from file, convert them from Integer- to BV-theory or"
                + " reverse.")
    private PrecisionConverter encodePredicates = PrecisionConverter.DISABLE;

    @Option(secure = true, description = "initial predicates are added as atomic predicates")
    private boolean splitIntoAtoms = false;

    public boolean applyFunctionWide() {
      return applyFunctionWide;
    }

    public boolean applyGlobally() {
      return applyGlobally;
    }

    public PrecisionConverter getPrecisionConverter() {
      return encodePredicates;
    }
  }

  private final FormulaManagerView formulaManagerView;
  private final AbstractionManager abstractionManager;

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;

  private final ShutdownNotifier shutdownNotifier;
  private final PathFormulaManager pathFormulaManager;
  private final PredicateAbstractionManager predicateAbstractionManager;

  private final KeyValueStatistics statistics = new KeyValueStatistics();

  private final InitialPredicatesOptions options;

  public PredicatePrecisionBootstrapper(
      Configuration config,
      LogManager logger,
      CFA cfa,
      AbstractionManager abstractionManager,
      FormulaManagerView formulaManagerView,
      ShutdownNotifier shutdownNotifier,
      PathFormulaManager pathFormulaManager,
      PredicateAbstractionManager predicateAbstractionManager)
      throws InvalidConfigurationException {
    this.config = config;
    this.logger = logger;
    this.cfa = cfa;

    this.abstractionManager = abstractionManager;
    this.formulaManagerView = formulaManagerView;

    this.shutdownNotifier = shutdownNotifier;
    this.pathFormulaManager = pathFormulaManager;
    this.predicateAbstractionManager = predicateAbstractionManager;

    config.inject(this);

    options = new InitialPredicatesOptions();
    config.inject(options);
  }

  private PredicatePrecision internalPrepareInitialPredicates()
      throws InvalidConfigurationException, InterruptedException {

    PredicatePrecision result = PredicatePrecision.empty();

    if (checkBlockFeasibility) {
      result =
          result.addGlobalPredicates(
              Collections.singleton(abstractionManager.makeFalsePredicate()));
    }

    if (!predicatesFiles.isEmpty()) {
      PredicateMapParser parser =
          new PredicateMapParser(cfa, logger, formulaManagerView, abstractionManager, options);

      for (Path predicatesFile : predicatesFiles) {
        try {
          IO.checkReadableFile(predicatesFile);
          if (AutomatonGraphmlParser.isGraphmlAutomatonFromConfiguration(predicatesFile)) {
            switch (AutomatonGraphmlParser.getWitnessType(predicatesFile)) {
              case CORRECTNESS_WITNESS:
                result =
                    result.mergeWith(
                        parseInvariantsFromCorrectnessWitnessAsPredicates(predicatesFile));
                break;
              case VIOLATION_WITNESS:
                logger.log(Level.WARNING, "Invariants do not exist in a violaton witness");
                break;
            }
          } else {
            result = result.mergeWith(parser.parsePredicates(predicatesFile));
          }

        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not read predicate precision from file");

        } catch (PredicateParsingFailedException e) {
          logger.logUserException(Level.WARNING, e, "Could not read predicate map");
        }
      }
    }

    return result;
  }

  private PredicatePrecision parseInvariantsFromCorrectnessWitnessAsPredicates(Path pWitnessFile)
      throws InterruptedException {
    PredicatePrecision result = PredicatePrecision.empty();
    try {
      WitnessInvariantsExtractor extractor =
          new WitnessInvariantsExtractor(config, logger, cfa, shutdownNotifier, pWitnessFile);
      final Set<ExpressionTreeLocationInvariant> invariants =
          extractor.extractInvariantsFromReachedSet();

      boolean wereAnyPredicatesLoaded = false;

      for (ExpressionTreeLocationInvariant invariant : invariants) {

        ListMultimap<CFANode, AbstractionPredicate> localPredicates =
            MultimapBuilder.treeKeys().arrayListValues().build();
        Set<AbstractionPredicate> globalPredicates = new HashSet<>();
        ListMultimap<String, AbstractionPredicate> functionPredicates =
            MultimapBuilder.treeKeys().arrayListValues().build();

        List<AbstractionPredicate> predicates = new ArrayList<>();
        // get atom predicates from invariant
        if (options.splitIntoAtoms) {
          predicates.addAll(splitToPredicates(invariant));
        }
        // get predicate from invariant
        else {
          predicates.add(
              abstractionManager.makePredicate(
                  invariant.getFormula(formulaManagerView, pathFormulaManager, null)));
        }
        for (AbstractionPredicate predicate : predicates) {
          localPredicates.put(invariant.getLocation(), predicate);
          globalPredicates.add(predicate);
          functionPredicates.put(invariant.getLocation().getFunctionName(), predicate);
        }

        wereAnyPredicatesLoaded |= !predicates.isEmpty();

        // add predicates according to the scope
        // location scope is chosen if neither function or global scope is specified or both are
        // specified which would be a conflict here
        boolean applyLocally = options.applyFunctionWide == options.applyGlobally;
        if (applyLocally) {
          result = result.addLocalPredicates(localPredicates.entries());
        } else if (options.applyFunctionWide) {
          result = result.addFunctionPredicates(functionPredicates.entries());
        } else if (options.applyGlobally) {
          result = result.addGlobalPredicates(globalPredicates);
        }
      }
      if (!invariants.isEmpty() && !wereAnyPredicatesLoaded) {
        logger.log(
            Level.WARNING,
            "Predicate from correctness witness invariants could not be computed."
                + "They are present, but are not correctly loaded");
      }

    } catch (CPAException | InvalidConfigurationException e) {
      logger.logUserException(
          Level.WARNING, e, "Predicate from correctness witness invariants could not be computed");
    }
    return result;
  }

  private ImmutableSet<AbstractionPredicate> splitToPredicates(
      ExpressionTreeLocationInvariant pInvariant)
      throws CPATransferException, InterruptedException {
    Collection<ExpressionTree<AExpression>> atoms = splitTree(pInvariant);
    ImmutableSet.Builder<AbstractionPredicate> predicates = ImmutableSet.builder();

    for (ExpressionTree<AExpression> atom : atoms) {
      predicates.addAll(predicateAbstractionManager.getPredicatesForAtomsOf(toFormula(atom)));
    }
    return predicates.build();
  }

  private BooleanFormula toFormula(ExpressionTree<AExpression> expressionTree)
      throws CPATransferException, InterruptedException {
    ToFormulaVisitor toFormulaVisitor =
        new ToFormulaVisitor(formulaManagerView, pathFormulaManager, null);
    try {
      return expressionTree.accept(toFormulaVisitor);
    } catch (ToFormulaException e) {
      Throwables.propagateIfPossible(
          e.getCause(), CPATransferException.class, InterruptedException.class);
      throw new UnexpectedCheckedException("expression tree to formula", e);
    }
  }

  private Collection<ExpressionTree<AExpression>> splitTree(
      ExpressionTreeLocationInvariant pInvariant) {
    // Break the decision tree into its atoms before transforming it to SMT
    // formulae. This avoids simplifications like '(x>0)||(x<=0)' to 'true'
    // which would immediately happen when building the BooleanFormula.
    return split(cast(pInvariant.asExpressionTree()));
  }

  @SuppressWarnings("unchecked")
  // we keep this method locally and do not put it into utils, because it is dangerous to use
  // if the real type of pInvariant is not known.
  private <LeafType> ExpressionTree<LeafType> cast(ExpressionTree<Object> toCast) {
    return (ExpressionTree<LeafType>) toCast;
  }

  private Collection<ExpressionTree<AExpression>> split(ExpressionTree<AExpression> pExpr) {
    ImmutableSet.Builder<ExpressionTree<AExpression>> builder = ImmutableSet.builder();
    split0(pExpr, builder);
    return builder.build();
  }

  /** Extracts the given {@link ExpressionTree}'s leaf nodes and adds them to the given builder. */
  private void split0(
      ExpressionTree<AExpression> pExpr,
      ImmutableSet.Builder<ExpressionTree<AExpression>> pSetBuilder) {
    if (pExpr instanceof And) {
      ((And<AExpression>) pExpr).forEach(conj -> split0(conj, pSetBuilder));
    } else if (pExpr instanceof Or) {
      ((Or<AExpression>) pExpr).forEach(conj -> split0(conj, pSetBuilder));
    } else if (pExpr instanceof LeafExpression) {
      pSetBuilder.add(pExpr);
    } else {
      throw new AssertionError("Unhandled expression type: " + pExpr.getClass());
    }
  }

  /** Read the (initial) precision (predicates to track) from a file. */
  public PredicatePrecision prepareInitialPredicates()
      throws InvalidConfigurationException, InterruptedException {
    PredicatePrecision result = internalPrepareInitialPredicates();

    statistics.addKeyValueStatistic("Init. global predicates", result.getGlobalPredicates().size());
    statistics.addKeyValueStatistic(
        "Init. location predicates", result.getLocalPredicates().size());
    statistics.addKeyValueStatistic(
        "Init. function predicates", result.getFunctionPredicates().size());

    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
