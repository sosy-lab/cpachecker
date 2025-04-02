// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.cfa.Language.C;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.io.InputStream;
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
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapParser;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.ACSLParserUtils;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor.InvalidWitnessException;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.Converter.PrecisionConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.Invariant;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.InvariantExchangeFormatTransformer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LemmaEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LemmaSetEntry;
import org.sosy_lab.java_smt.api.BooleanFormula;

@Options(prefix = "cpa.predicate")
public final class PredicatePrecisionBootstrapper {

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
  public static final class InitialPredicatesOptions {

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
            "when reading invariants from YML witness ignore the location context and only consider"
                + " the function context of the program")
    private boolean ignoreLocationInfoInYMLWitness = false;

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

  public PredicatePrecision prepareInitialPredicates()
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
          } else if (AutomatonWitnessV2ParserUtils.isYAMLWitness(predicatesFile)) {
            if (!AutomatonWitnessV2ParserUtils.getWitnessTypeIfYAML(predicatesFile)
                .orElseThrow()
                .equals(WitnessType.CORRECTNESS_WITNESS)) {
              logger.log(
                  Level.WARNING, "For witnesses V2 invariants only exist in correctness witnesses");
              continue;
            }
            if (options.ignoreLocationInfoInYMLWitness) {
              if (!(options.applyFunctionWide || options.applyGlobally)) {
                logger.log(Level.WARNING, "Invariants of witness will be applied function wide.");
              }
              result =
                  result.mergeWith(
                      parseInvariantFromYMLCorrectnessWitnessNonLocally(predicatesFile));
            } else {
              result =
                  result.mergeWith(
                      parseInvariantsFromCorrectnessWitnessAsPredicates(predicatesFile));
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

  public ImmutableSet<PathFormula> prepareInitialLemmas()
      throws InterruptedException, InvalidConfigurationException {
    ImmutableList.Builder<LemmaSetEntry> lemmaSetEntriesBuilder = ImmutableList.builder();
    ImmutableList.Builder<LemmaEntry> lemmaSetBuilder = ImmutableList.builder();
    ImmutableList.Builder<String> declarationEntriesBuilder = ImmutableList.builder();

    if (!predicatesFiles.isEmpty()) {
      for (Path lemmaFile : predicatesFiles) {
        try {
          IO.checkReadableFile(lemmaFile);
          if (!AutomatonWitnessV2ParserUtils.isYAMLWitness(lemmaFile)) {
            logger.log(Level.WARNING, "Lemmas can only be supplied via a YAML file.");
            continue;
          }
          ImmutableList<LemmaSetEntry> lemmaSetEntries =
              lemmaSetEntriesBuilder
                  .addAll(AutomatonWitnessV2ParserUtils.readLemmaFile(lemmaFile))
                  .build();
          declarationEntriesBuilder.addAll(
              AutomatonWitnessV2ParserUtils.parseDeclarationsFromFile(lemmaSetEntries));
          lemmaSetBuilder.addAll(
              AutomatonWitnessV2ParserUtils.parseLemmasFromFile(lemmaSetEntries));
        } catch (IOException e) {
          logger.logfUserException(Level.WARNING, e, "Could not read lemma file");
        }
      }
    }
    ImmutableList<LemmaEntry> lemmaSet = lemmaSetBuilder.build();
    ImmutableList<String> declarationEntries = declarationEntriesBuilder.build();

    InvariantExchangeFormatTransformer transformer =
        new InvariantExchangeFormatTransformer(config, logger, shutdownNotifier, cfa);
    PathFormulaManagerImpl pathFormulaManagerImpl =
        new PathFormulaManagerImpl(
            formulaManagerView, config, logger, shutdownNotifier, cfa, AnalysisDirection.FORWARD);

    List<CDeclaration> declarations = ACSLParserUtils.parseDeclarations(declarationEntries);

    if (cfa.getLanguage() != C) {
      throw new InvalidYAMLWitnessException("CFA language must be C.");
    }
    CProgramScope scope = new CProgramScope(cfa, logger);
    for (CDeclaration declaration : declarations) {
      scope.addDeclarationToScope(declaration);
    }
    ImmutableSet.Builder<PathFormula> lemmas = new ImmutableSet.Builder<>();
    for (LemmaEntry e : lemmaSet) {
      lemmas.add(transformer.parseLemmaEntry(e, pathFormulaManagerImpl, scope));
    }
    return lemmas.build();
  }

  private PredicatePrecision parseInvariantFromYMLCorrectnessWitnessNonLocally(
      final Path pWitnessFile) throws IOException, InterruptedException {
    PredicatePrecision result = PredicatePrecision.empty();
    try (InputStream witness = MoreFiles.asByteSource(pWitnessFile).openStream()) {
      List<AbstractEntry> entries = AutomatonWitnessV2ParserUtils.parseYAML(witness);

      InvariantExchangeFormatTransformer transformer =
          new InvariantExchangeFormatTransformer(config, logger, shutdownNotifier, cfa);

      Set<AbstractionPredicate> globalPredicates = new HashSet<>();
      Multimap<String, AbstractionPredicate> functionPredicates =
          MultimapBuilder.treeKeys().arrayListValues().build();
      BooleanFormula atomFormula;
      String function;
      boolean containsFalsePredicate = false;
      for (Invariant invariant : transformer.generateInvariantsFromEntries(entries)) {
        function = invariant.getFunction();

        if (!ExpressionTrees.isConstant(invariant.getFormula())) {
          Collection<ExpressionTree<AExpression>> atoms;
          if (options.splitIntoAtoms) {
            atoms = split(invariant.getFormula());

          } else {
            atoms = ImmutableList.of(invariant.getFormula());
          }
          for (ExpressionTree<AExpression> atom : atoms) {
            atomFormula = toFormula(atom);
            // only continue if formula is not true
            if (options.applyGlobally() || function.isBlank()) {
              if (options.splitIntoAtoms) {
                globalPredicates.addAll(
                    predicateAbstractionManager.getPredicatesForAtomsOf(atomFormula));
              } else {
                globalPredicates.add(abstractionManager.makePredicate(atomFormula));
              }
            } else {

              if (options.splitIntoAtoms) {
                functionPredicates.putAll(
                    function, predicateAbstractionManager.getPredicatesForAtomsOf(atomFormula));
              } else {
                functionPredicates.put(function, abstractionManager.makePredicate(atomFormula));
              }
            }
          }
        } else {
          if (ExpressionTrees.getFalse().equals(invariant.getFormula())) {
            containsFalsePredicate = true;
            if (options.applyGlobally() || function.isBlank()) {
              globalPredicates.add(abstractionManager.makeFalsePredicate());
            } else {
              functionPredicates.put(function, abstractionManager.makeFalsePredicate());
            }
          }
        }
      }
      result = result.addGlobalPredicates(globalPredicates);
      result = result.addFunctionPredicates(functionPredicates.entries());
      if (!containsFalsePredicate && !checkBlockFeasibility) {
        logger.log(
            Level.WARNING,
            "We recommend to enable option cpa.predicate.checkBlockFeasibility "
                + "when reusing precision from correctness witness.");
      }
    } catch (InvalidConfigurationException | CPATransferException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Predicate precision from correctness witness invariants could not be (fully) computed");
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
        if (options.splitIntoAtoms) {
          // get atom predicates from invariant
          predicates.addAll(splitToPredicates(invariant));
        } else {
          // get predicate from invariant
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
    } catch (InvalidWitnessException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Could not match witness to CFA. When reading invariants as predicates the semantics of"
              + " the witness need not be strictly followed. If necessary disable the options"
              + " responsible for strictly checking the witness.");
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
      Throwables.throwIfInstanceOf(e.getCause(), CPATransferException.class);
      Throwables.throwIfInstanceOf(e.getCause(), InterruptedException.class);
      Throwables.throwIfUnchecked(e.getCause());
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
}
