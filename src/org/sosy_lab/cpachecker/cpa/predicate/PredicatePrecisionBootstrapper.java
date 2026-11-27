// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
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
import org.sosy_lab.cpachecker.util.yamlwitnessexport.PrecisionScopeVariables;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.Invariant;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.InvariantExchangeFormatTransformer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.GlobalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocalLoopPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionDeclaration;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionType;
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

    logger.log(
        Level.INFO,
        "Amount of initial location predicates loaded: ",
        result.getLocalPredicates().size());
    logger.log(
        Level.INFO,
        "Amount of initial function predicates loaded: ",
        result.getFunctionPredicates().size());
    logger.log(
        Level.INFO,
        "Amount of initial global predicates loaded: ",
        result.getGlobalPredicates().size());

    return result;
  }

  /**
   * Parses a predicate of a precision witness. A name of a predicate in C denotes the variable of
   * the given scope in the given function.
   */
  private Optional<BooleanFormula> parsePredicate(
      PrecisionExchangeEntry pEntry,
      String pPredicate,
      String pCommonDefinitions,
      Optional<String> pFunction,
      Supplier<Scope> pScope,
      InvariantExchangeFormatTransformer pTransformer)
      throws InterruptedException {
    return switch (pEntry.format()) {
      case C -> {
        try {
          yield Optional.of(
              toFormula(
                  pTransformer.createExpressionTreeFromStringInScope(
                      pFunction, pPredicate, pScope.get())));
        } catch (CPATransferException e) {
          logger.logDebugException(e);
          yield Optional.empty();
        }
      }
      case ACSL, EXT_C -> {
        logger.log(
            Level.WARNING,
            "ACSL expressions, and ext_c_expressions are not supported to exchange precision");
        yield Optional.empty();
      }
      case SMTLIB ->
          Optional.of(
              formulaManagerView.parse(
                  Joiner.on(System.lineSeparator())
                      .join(ImmutableList.of(pCommonDefinitions, pPredicate))));
    };
  }

  /**
   * Returns the scope of the program in which a name denotes the variable of the input program with
   * this name among the given ones.
   */
  private static Scope scopeWith(
      CProgramScope pProgramScope,
      Map<String, AbstractSimpleDeclaration> pVariables,
      Optional<String> pFunction) {
    CProgramScope scope =
        pProgramScope.withVariablesInScope(
            FluentIterable.from(pVariables.values()).filter(CSimpleDeclaration.class).toList());
    return pFunction.map(scope::withFunctionScope).orElse(scope);
  }

  private PredicatePrecision parsePredicateWintess(final Path pWitnessFile)
      throws InterruptedException {
    PredicatePrecision result = PredicatePrecision.empty();
    try (InputStream witness = MoreFiles.asByteSource(pWitnessFile).openStream()) {
      List<AbstractEntry> entries = AutomatonWitnessV2ParserUtils.parseYAML(witness);

      InvariantExchangeFormatTransformer transformer =
          new InvariantExchangeFormatTransformer(config, logger, shutdownNotifier, cfa);
      AstCfaRelation astCfaRelation = cfa.getAstCfaRelation();
      // The names of predicates in C are those of the input program, which needs the relation to
      // the AST. Building the scope walks the whole CFA, so it is only built for such predicates.
      Optional<PrecisionScopeVariables> programVariables =
          cfa.getLanguage() == Language.C && astCfaRelation != null
              ? Optional.of(new PrecisionScopeVariables(cfa))
              : Optional.empty();
      Supplier<CProgramScope> programScope =
          Suppliers.memoize(() -> new CProgramScope(cfa, logger));

      for (AbstractEntry entry : entries) {
        if (!(entry instanceof PrecisionExchangeSetEntry pExchangeSetEntry)) {
          logger.logf(
              Level.WARNING,
              "Witness file %s does not contain a precision exchange set entry, ignoring it.",
              pWitnessFile);
          continue;
        }

        ImmutableList.Builder<AbstractionPredicate> globalPredicatesBuilder =
            ImmutableList.builder();
        ImmutableSetMultimap.Builder<String, AbstractionPredicate> functionPredicatesBuilder =
            ImmutableSetMultimap.builder();
        ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> localPredicatesBuilder =
            ImmutableSetMultimap.builder();

        String commonDefinitions =
            FluentIterable.from(pExchangeSetEntry.getDeclarations())
                .transform(PrecisionDeclaration::value)
                .join(Joiner.on(System.lineSeparator()));

        for (PrecisionExchangeEntry precisionExchangeEntry : pExchangeSetEntry.getContent()) {
          PrecisionScope scope = precisionExchangeEntry.scope();
          if (precisionExchangeEntry.type() != PrecisionType.PREDICATES) {
            logger.logf(
                Level.WARNING,
                "Witness file %s contains a precision exchange entry with type %s, "
                    + "but only predicate precision is supported, ignoring it.",
                pWitnessFile,
                precisionExchangeEntry.type());
            continue;
          }
          if (precisionExchangeEntry.format() == YAMLWitnessExpressionType.C
              && programVariables.isEmpty()) {
            logger.logf(
                Level.WARNING,
                "Witness file %s contains predicates in C, which can only be read for a program in"
                    + " C, ignoring them.",
                pWitnessFile);
            continue;
          }

          switch (scope) {
            case GlobalPrecisionScope _ -> {
              Supplier<Scope> globalScope =
                  Suppliers.memoize(() ->
                      scopeWith(
                          programScope.get(),
                          programVariables.orElseThrow().globalVariables(),
                          Optional.empty()));
              for (String predicateString : precisionExchangeEntry.values()) {
                Optional<BooleanFormula> globalPredicate =
                    parsePredicate(
                        precisionExchangeEntry,
                        predicateString,
                        commonDefinitions,
                        Optional.empty(),
                        globalScope,
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
            }
            case FunctionPrecisionScope pFunctionScope -> {
              String functionName = pFunctionScope.getFunctionName();
              Supplier<Scope> functionScope =
                  Suppliers.memoize(() ->
                      scopeWith(
                          programScope.get(),
                          programVariables.orElseThrow().variablesIn(functionName),
                          Optional.of(functionName)));
              for (String predicateString : precisionExchangeEntry.values()) {
                Optional<BooleanFormula> functionPredicate =
                    parsePredicate(
                        precisionExchangeEntry,
                        predicateString,
                        commonDefinitions,
                        Optional.of(functionName),
                        functionScope,
                        transformer);
                if (functionPredicate.isEmpty()) {
                  logger.logf(
                      Level.WARNING,
                      "Witness file %s contains an invalid function predicate for function %s, "
                          + "ignoring it.",
                      pWitnessFile,
                      functionName);
                  continue;
                }

                functionPredicatesBuilder.put(
                    functionName,
                    abstractionManager.makePredicate(functionPredicate.orElseThrow()));
              }
            }
            case LocalPrecisionScope pLocalScope -> {
              if (astCfaRelation == null) {
                logger.logf(
                    Level.WARNING,
                    "Witness file %s contains a precision at a location, which can only be matched"
                        + " for a program in C, ignoring it.",
                    pWitnessFile);
                continue;
              }
              LocationRecord locationRecord = pLocalScope.getLocation();

              if (locationRecord.getColumn().isEmpty()) {
                logger.logf(
                    Level.WARNING,
                    "Witness file %s contains a local precision scope without a column for %s, "
                        + "which we cannot match to the CFA, ignoring it.",
                    pWitnessFile,
                    locationRecord);
                continue;
              }

              Set<CFANode> location =
                  astCfaRelation.getNodesForStatementLocation(
                      locationRecord.getLine(), locationRecord.getColumn().orElseThrow());

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

              addLocalPredicates(
                  location,
                  pWitnessFile,
                  transformer,
                  precisionExchangeEntry,
                  commonDefinitions,
                  node ->
                      scopeWith(
                          programScope.get(),
                          programVariables
                              .orElseThrow()
                              .variablesAt(node)
                              .orElse(ImmutableMap.of()),
                          Optional.of(node.getFunctionName())),
                  localPredicatesBuilder);
            }
            case LocalLoopPrecisionScope pLocalLoopPrecisionScope -> {
              if (astCfaRelation == null) {
                logger.logf(
                    Level.WARNING,
                    "Witness file %s contains a precision at a location, which can only be matched"
                        + " for a program in C, ignoring it.",
                    pWitnessFile);
                continue;
              }
              LocationRecord locationRecord = pLocalLoopPrecisionScope.getLocation();

              if (locationRecord.getColumn().isEmpty()) {
                logger.logf(
                    Level.WARNING,
                    "Witness file %s contains a local loop precision scope without a column for %s,"
                        + " which we cannot match to the CFA, ignoring it.",
                    pWitnessFile,
                    locationRecord);
                continue;
              }

              Optional<CFANode> location =
                  cfa.getLoopStructure()
                      .flatMap(loopStructure ->
                          astCfaRelation.getNodeForIterationStatementLocation(
                              locationRecord.getLine(),
                              locationRecord.getColumn().orElseThrow(),
                              loopStructure));

              if (location.isEmpty()) {
                logger.logf(
                    Level.FINE,
                    "Witness file %s contains a local loop precision scope for location %s, "
                        + "but the location is not present in the CFA, ignoring it.",
                    pWitnessFile,
                    locationRecord);
                continue;
              }

              addLocalPredicates(
                  ImmutableSet.of(location.orElseThrow()),
                  pWitnessFile,
                  transformer,
                  precisionExchangeEntry,
                  commonDefinitions,
                  node ->
                      scopeWith(
                          programScope.get(),
                          programVariables
                              .orElseThrow()
                              .variablesAt(node)
                              .orElse(ImmutableMap.of()),
                          Optional.of(node.getFunctionName())),
                  localPredicatesBuilder);
            }
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

  /** Adds the predicates of the entry at the given nodes, with the names of the given scopes. */
  private void addLocalPredicates(
      Set<CFANode> pNodes,
      Path pWitnessFile,
      InvariantExchangeFormatTransformer pTransformer,
      PrecisionExchangeEntry pPrecisionExchangeEntry,
      String pCommonDefinitions,
      Function<CFANode, Scope> pScopeAt,
      ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pLocalPredicatesBuilder)
      throws InterruptedException {
    for (CFANode node : pNodes) {
      Supplier<Scope> scope = Suppliers.memoize(() -> pScopeAt.apply(node));
      for (String predicateString : pPrecisionExchangeEntry.values()) {
        Optional<BooleanFormula> precisionFormula =
            parsePredicate(
                pPrecisionExchangeEntry,
                predicateString,
                pCommonDefinitions,
                Optional.of(node.getFunctionName()),
                scope,
                pTransformer);
        if (precisionFormula.isEmpty()) {
          logger.logf(
              Level.WARNING,
              "Witness file %s contains an invalid precision formula for location %s, "
                  + "ignoring it.",
              pWitnessFile,
              node);
          continue;
        }

        pLocalPredicatesBuilder.put(
            node, abstractionManager.makePredicate(precisionFormula.orElseThrow()));
      }
    }
  }

  private PredicatePrecision parseInvariantFromYMLCorrectnessWitnessNonLocally(
      final Path pWitnessFile) throws IOException, InterruptedException {
    PredicatePrecision result = PredicatePrecision.empty();
    try {
      List<AbstractEntry> entries = AutomatonWitnessV2ParserUtils.parseYAML(pWitnessFile);

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
