// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapParser;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor.InvalidWitnessException;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.precisionConverter.Converter.PrecisionConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.Invariant;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.InvariantExchangeFormatTransformer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.GlobalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionDeclaration;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionScope;
import org.sosy_lab.java_smt.api.BooleanFormula;

@Options(prefix = "cpa.predicate")
public final class PredicatePrecisionBootstrapper {

  @Option(
      secure = true,
      name = "abstraction.initialPredicates",
      description =
          "get an initial map of predicates from a list of files (see source"
              + " doc/examples/predmap.txt for an example)")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
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
              case CORRECTNESS_WITNESS ->
                  result =
                      result.mergeWith(
                          parseInvariantsFromCorrectnessWitnessAsPredicates(predicatesFile));
              case VIOLATION_WITNESS ->
                  logger.log(Level.WARNING, "Invariants do not exist in a violaton witness");
              case PRECISION_WITNESS ->
                  logger.log(Level.WARNING, "Witnesses for precision are not valid v1 witnesses.");
            }
          } else if (AutomatonWitnessV2ParserUtils.isYAMLWitness(predicatesFile)) {
            WitnessType witnessType =
                AutomatonWitnessV2ParserUtils.getWitnessTypeIfYAML(predicatesFile).orElseThrow();
            if (witnessType.equals(WitnessType.CORRECTNESS_WITNESS)) {
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
            } else if (witnessType.equals(WitnessType.PRECISION_WITNESS)) {
              result = result.mergeWith(parsePredicateWintess(predicatesFile));
            } else {
              logger.log(
                  Level.WARNING, "Importing predicates from violation witness is not supported");
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

  private Optional<BooleanFormula> parseLocalPredicate(
      YAMLWitnessExpressionType pExpressionType,
      String predicateString,
      String commonDefinitions,
      LocationRecord locationRecord,
      Deque<String> callStack,
      Scope programScope,
      InvariantExchangeFormatTransformer transformer)
      throws InterruptedException {
    return switch (pExpressionType) {
      case C -> {
        try {
          yield Optional.of(
              toFormula(
                  transformer.createExpressionTreeFromString(
                      Optional.ofNullable(locationRecord.getFunction()),
                      predicateString,
                      locationRecord.getLine(),
                      callStack,
                      programScope)));
        } catch (CPATransferException e) {
          logger.logDebugException(e);
          yield Optional.empty();
        }
      }
      case ACSL -> {
        logger.log(Level.WARNING, "ACSL expressions are not supported to exchange precision");
        yield Optional.empty();
      }
      case SMTLIB ->
          Optional.of(
              formulaManagerView.parse(
                  Joiner.on(System.lineSeparator())
                      .join(ImmutableList.of(commonDefinitions, predicateString))));
    };
  }

  private Optional<BooleanFormula> parseFunctionPredicate(
      YAMLWitnessExpressionType pExpressionType,
      String predicateString,
      String commonDefinitions,
      String functionName,
      InvariantExchangeFormatTransformer transformer)
      throws InterruptedException {
    return switch (pExpressionType) {
      case C -> {
        try {
          yield Optional.of(
              toFormula(
                  transformer.createExpressionTreeFromStringInFunctionScope(
                      predicateString, functionName)));
        } catch (CPATransferException e) {
          logger.logDebugException(e);
          yield Optional.empty();
        }
      }
      case ACSL -> {
        logger.log(Level.WARNING, "ACSL expressions are not supported to exchange precision");
        yield Optional.empty();
      }
      case SMTLIB ->
          Optional.of(
              formulaManagerView.parse(
                  Joiner.on(System.lineSeparator())
                      .join(ImmutableList.of(commonDefinitions, predicateString))));
    };
  }

  private Optional<BooleanFormula> parseGlobalPredicate(
      YAMLWitnessExpressionType pExpressionType,
      String predicateString,
      String commonDefinitions,
      InvariantExchangeFormatTransformer transformer)
      throws InterruptedException {
    return switch (pExpressionType) {
      case C -> {
        try {
          yield Optional.of(
              toFormula(transformer.createExpressionTreeFromStringInGlobalScope(predicateString)));
        } catch (CPATransferException e) {
          logger.logDebugException(e);
          yield Optional.empty();
        }
      }
      case ACSL -> {
        logger.log(Level.WARNING, "ACSL expressions are not supported to exchange precision");
        yield Optional.empty();
      }
      case SMTLIB ->
          Optional.of(
              formulaManagerView.parse(
                  Joiner.on(System.lineSeparator())
                      .join(ImmutableList.of(commonDefinitions, predicateString))));
    };
  }

  private PredicatePrecision parsePredicateWintess(final Path pWitnessFile)
      throws InterruptedException {
    PredicatePrecision result = PredicatePrecision.empty();
    try (InputStream witness = MoreFiles.asByteSource(pWitnessFile).openStream()) {
      List<AbstractEntry> entries = AutomatonWitnessV2ParserUtils.parseYAML(witness);

      InvariantExchangeFormatTransformer transformer =
          new InvariantExchangeFormatTransformer(config, logger, shutdownNotifier, cfa);

      for (AbstractEntry entry : entries) {
        if (!(entry instanceof PrecisionExchangeSetEntry pExchangeSetEntry)) {
          logger.logf(
              Level.WARNING,
              "Witness file %s does not contain a precision exchange set entry, ignoring it.",
              pWitnessFile);
          return result;
        }

        Builder<AbstractionPredicate> globalPredicatesBuilder = ImmutableList.builder();
        ImmutableSetMultimap.Builder<String, AbstractionPredicate> functionPredicatesBuilder =
            ImmutableSetMultimap.builder();
        ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> localPredicatesBuilder =
            ImmutableSetMultimap.builder();

        AstCfaRelation astCfaRelation = cfa.getAstCfaRelation();
        String commonDefinitions =
            FluentIterable.from(pExchangeSetEntry.getDeclarations())
                .transform(PrecisionDeclaration::value)
                .join(Joiner.on(System.lineSeparator()));

        for (PrecisionExchangeEntry precisionExchangeEntry : pExchangeSetEntry.getContent()) {
          PrecisionScope scope = precisionExchangeEntry.scope();
          if (scope instanceof GlobalPrecisionScope) {

            for (String predicateString : precisionExchangeEntry.values()) {

              Optional<BooleanFormula> globalPredicate =
                  parseGlobalPredicate(
                      precisionExchangeEntry.format(),
                      predicateString,
                      commonDefinitions,
                      transformer);

              if (globalPredicate.isEmpty()) {
                logger.logf(
                    Level.WARNING,
                    "The global predicate '%s' in witness file %s is invalid, ignoring it.",
                    predicateString,
                    pWitnessFile);
                continue;
              }

              globalPredicatesBuilder.add(
                  abstractionManager.makePredicate(globalPredicate.orElseThrow()));
            }
          } else if (scope instanceof FunctionPrecisionScope pFunctionScope) {
            for (String predicateString : precisionExchangeEntry.values()) {

              Optional<BooleanFormula> functionPredicate =
                  parseFunctionPredicate(
                      precisionExchangeEntry.format(),
                      predicateString,
                      commonDefinitions,
                      pFunctionScope.getFunctionName(),
                      transformer);
              if (functionPredicate.isEmpty()) {
                logger.logf(
                    Level.WARNING,
                    "Witness file %s contains an invalid function predicate for function %s, "
                        + "ignoring it.",
                    pWitnessFile,
                    pFunctionScope.getFunctionName());
                continue;
              }

              functionPredicatesBuilder.put(
                  pFunctionScope.getFunctionName(),
                  abstractionManager.makePredicate(functionPredicate.orElseThrow()));
            }
          } else if (scope instanceof LocalPrecisionScope pLocalScope) {
            LocationRecord locationRecord = pLocalScope.getLocation();

            Set<CFANode> location =
                astCfaRelation.getNodeForStatementLocation(
                    locationRecord.getLine(), locationRecord.getColumn());

            if (location.isEmpty()) {
              logger.logf(
                  Level.FINE,
                  "Witness file %s contains a local precision scope for location %s, "
                      + "but the location is not present in the CFA, ignoring it.",
                  pWitnessFile,
                  locationRecord);
              // TODO: Whenever this happens this is a bug in the AstCfaRelation
              continue;
            }

            Deque<String> callStack = new ArrayDeque<>();
            callStack.push(locationRecord.getFunction());

            Scope programScope =
                switch (cfa.getLanguage()) {
                  case C -> new CProgramScope(cfa, logger);
                  default -> DummyScope.getInstance();
                };

            for (String predicateString : precisionExchangeEntry.values()) {

              Optional<BooleanFormula> precisionFormula =
                  parseLocalPredicate(
                      precisionExchangeEntry.format(),
                      predicateString,
                      commonDefinitions,
                      locationRecord,
                      callStack,
                      programScope,
                      transformer);

              for (CFANode loc : location) {
                if (precisionFormula.isEmpty()) {
                  logger.logf(
                      Level.WARNING,
                      "Witness file %s contains an invalid precision formula for location %s, "
                          + "ignoring it.",
                      pWitnessFile,
                      loc);
                  continue;
                }

                localPredicatesBuilder.put(
                    loc, abstractionManager.makePredicate(precisionFormula.orElseThrow()));
              }
            }
          } else {
            logger.logf(
                Level.WARNING,
                "Witness file %s contains an unknown precision scope %s, ignoring it.",
                pWitnessFile,
                scope);
          }
        }
        result = result.addGlobalPredicates(globalPredicatesBuilder.build());
        result = result.addFunctionPredicates(functionPredicatesBuilder.build().entries());
        result = result.addLocalPredicates(localPredicatesBuilder.build().entries());
      }
    } catch (InvalidConfigurationException | IOException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Predicate precision from predicate witness could not be (fully) computed");
    }
    return result;
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
