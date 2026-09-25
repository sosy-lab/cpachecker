// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.splitFormula;
import static org.sosy_lab.cpachecker.util.expressions.ExpressionTrees.FUNCTION_DELIMITER;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.TranslationToExpressionTreeFailedException;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateDumpFormat;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.AbstractYAMLWitnessExporter;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessExpressionType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.GlobalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.MetadataRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionDeclaration;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionType;

/**
 * This class writes a set of predicates to a file in the same format that is also used by {@link
 * PredicateMapParser}.
 */
@Options(prefix = "cpa.predicate")
public final class PredicateMapWriter {

  @Option(
      secure = true,
      name = "predmap.predicateFormat",
      description = "Format for exporting predicates from precisions.")
  private PredicateDumpFormat format = PredicateDumpFormat.SMTLIB2;

  @Option(
      secure = true,
      name = "predmap.witnessPredicateFormats",
      description = "List of formats for exporting predicates from precisions in witnesses.")
  private List<PredicateDumpFormat> witnessPredicateFormats =
      ImmutableList.of(PredicateDumpFormat.C, PredicateDumpFormat.SMTLIB2);

  private final FormulaManagerView fmgr;
  private final LogManager logger;
  private final Optional<CFA> cfa;

  public PredicateMapWriter(
      Configuration config, FormulaManagerView pFmgr, LogManager pLogManager, Optional<CFA> pCFA)
      throws InvalidConfigurationException {
    config.inject(this);
    fmgr = pFmgr;
    logger = pLogManager;
    cfa = pCFA;
  }

  public void writePredicateMap(
      SetMultimap<PredicatePrecision.LocationInstance, AbstractionPredicate>
          locationInstancePredicates,
      SetMultimap<CFANode, AbstractionPredicate> localPredicates,
      SetMultimap<String, AbstractionPredicate> functionPredicates,
      Set<AbstractionPredicate> globalPredicates,
      Collection<AbstractionPredicate> allPredicates,
      Appendable sb)
      throws IOException {

    // In this set, we collect the definitions and declarations necessary
    // for the predicates (e.g., for variables)
    // The order of the definitions is important!
    SequencedSet<String> definitions = new LinkedHashSet<>();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    Map<AbstractionPredicate, String> predToString = new HashMap<>();

    // fill the above set and map
    for (AbstractionPredicate pred : allPredicates) {
      String predString;

      if (format == PredicateDumpFormat.SMTLIB2) {
        Pair<String, List<String>> p = splitFormula(fmgr, pred.getSymbolicAtom());
        predString = p.getFirst();
        definitions.addAll(p.getSecond());
      } else {
        predString = pred.getSymbolicAtom().toString();
      }

      predToString.put(pred, predString);
    }

    Joiner.on('\n').appendTo(sb, definitions);
    sb.append("\n\n");

    writeSetOfPredicates(sb, "*", globalPredicates, predToString);

    for (Entry<String, Collection<AbstractionPredicate>> e :
        functionPredicates.asMap().entrySet()) {
      writeSetOfPredicates(sb, e.getKey(), e.getValue(), predToString);
    }

    for (Entry<CFANode, Collection<AbstractionPredicate>> e : localPredicates.asMap().entrySet()) {
      String key = e.getKey().getFunctionName() + " " + e.getKey();
      writeSetOfPredicates(sb, key, e.getValue(), predToString);
    }

    for (Entry<PredicatePrecision.LocationInstance, Collection<AbstractionPredicate>> e :
        locationInstancePredicates.asMap().entrySet()) {
      String key =
          String.format(
              "%s %s@%d",
              e.getKey().getFunctionName(), e.getKey().getLocation(), e.getKey().getInstance());
      writeSetOfPredicates(sb, key, e.getValue(), predToString);
    }
  }

  private void writeSetOfPredicates(
      Appendable sb,
      String key,
      Collection<AbstractionPredicate> predicates,
      Map<AbstractionPredicate, String> predToString)
      throws IOException {
    if (!predicates.isEmpty()) {
      sb.append(key);
      sb.append(":\n");
      for (AbstractionPredicate pred : predicates) {
        sb.append(checkNotNull(predToString.get(pred)));
        sb.append('\n');
      }
      sb.append('\n');
    }
  }

  private static Optional<String> getPredicateString(
      AbstractionPredicate pPredicate,
      PredicateDumpFormat pFormat,
      Function<String, Boolean> pIncludeVariablesFilter,
      FormulaManagerView pFmgr,
      MachineModel pMachineModel,
      Set<PrecisionDeclaration> pDeclarations) {
    if (!FluentIterable.from(pFmgr.extractVariableNames(pPredicate.getSymbolicAtom()))
        .allMatch(pIncludeVariablesFilter::apply)) {
      return Optional.empty();
    }
    return switch (pFormat) {
      case SMTLIB2 -> {
        Pair<String, List<String>> p = splitFormula(pFmgr, pPredicate.getSymbolicAtom());
        FluentIterable.from(p.getSecond())
            .transform(PrecisionDeclaration::new)
            .copyInto(pDeclarations);
        yield Optional.of(Objects.requireNonNull(p.getFirst()));
      }
      case C -> {
        try {
          yield Optional.of(
              AbstractionFormula.asExpressionTree(
                      pPredicate.getSymbolicAtom(),
                      pFmgr,
                      pIncludeVariablesFilter,
                      y -> y,
                      pMachineModel)
                  .toString());
        } catch (TranslationToExpressionTreeFailedException | InterruptedException e) {
          yield Optional.empty();
        }
      }
      default -> Optional.empty();
    };
  }

  public static boolean notInternalVariable(String pQualifiedVariableName) {
    return !pQualifiedVariableName.contains("__CPAchecker_")
        && !pQualifiedVariableName.contains("__ADDRESS_OF_")
        // Renaming of same variables in scope is done by appending `__i` for some `i` to the
        // variable name
        && !pQualifiedVariableName.matches(".*__[1-9][0-9]*");
  }

  public static boolean variableNameInFunction(
      String pQualifiedVariableName, String pFunctionName) {
    return !pQualifiedVariableName.contains(FUNCTION_DELIMITER)
        || pQualifiedVariableName.startsWith(pFunctionName + FUNCTION_DELIMITER);
  }

  public static boolean variableInOriginalProgram(
      String pQualifiedVariableName, AstCfaRelation pAstCfaRelation, CFANode pLocation) {
    Optional<FluentIterable<AbstractSimpleDeclaration>> variablesInScope =
        pAstCfaRelation.getVariablesAndParametersInScope(pLocation);
    if (variablesInScope.isEmpty()) {
      // Without scope information we cannot tell, so we do not export the variable
      return false;
    }
    return variablesInScope
        .orElseThrow()
        .anyMatch(
            var ->
                // For local variables
                (pLocation.getFunctionName()
                            + FUNCTION_DELIMITER
                            + Objects.requireNonNull(var).getOrigName())
                        .equals(pQualifiedVariableName)
                    // For global variables
                    || var.getOrigName().equals(pQualifiedVariableName));
  }

  public void writePredicateMapAsWitness(
      SetMultimap<CFANode, AbstractionPredicate> pLocation,
      SetMultimap<String, AbstractionPredicate> pFunction,
      Set<AbstractionPredicate> pGlobal,
      PathTemplate pPathTemplate,
      MetadataRecord pMetadataRecord) {

    checkState(
        cfa.isPresent(), "Exporting a precision as a witness requires the CFA of the program.");
    MachineModel machineModel = cfa.orElseThrow().getMachineModel();
    AstCfaRelation astCfaRelation = cfa.orElseThrow().getAstCfaRelation();
    if (astCfaRelation == null) {
      Verify.verify(
          !cfa.orElseThrow().getLanguage().equals(Language.C),
          "We expect an AST-CFA relation for C programs, but it is not present.");
      logger.log(
          Level.INFO, "Currently we cannot export local predicates for programs other than C");
    }

    for (PredicateDumpFormat witnessPredicateFormat : witnessPredicateFormats) {
      if (witnessPredicateFormat == PredicateDumpFormat.PLAIN) {
        logger.log(
            Level.WARNING,
            "Predicates have no representation as PLAIN in a witness, skipping this format.");
        continue;
      }

      // The same variable is declared by every predicate using it, but redeclaring a symbol is an
      // error in SMT-LIB, so every declaration is exported only once.
      SequencedSet<PrecisionDeclaration> declarations = new LinkedHashSet<>();

      YAMLWitnessExpressionType witnessExpressionType =
          YAMLWitnessExpressionType.fromPredicateFormat(witnessPredicateFormat);

      // Several CFA nodes may describe the same scope, for example all nodes of one statement, so
      // collect the predicates per scope to export every scope exactly once. A scope without any
      // predicate carries no information, so it is left out.
      SequencedMap<PrecisionScope, Set<String>> predicatesPerScope = new LinkedHashMap<>();

      addPredicates(
          predicatesPerScope,
          new GlobalPrecisionScope(),
          pGlobal,
          witnessPredicateFormat,
          // TODO: The ADDRESS_OF problem should not be solved here, but in the translation back
          //  from SMT to C
          PredicateMapWriter::notInternalVariable,
          machineModel,
          declarations);

      for (String functionName : pFunction.keySet()) {
        addPredicates(
            predicatesPerScope,
            new FunctionPrecisionScope(functionName),
            pFunction.get(functionName),
            witnessPredicateFormat,
            name -> notInternalVariable(name) && variableNameInFunction(name, functionName),
            machineModel,
            declarations);
      }

      if (astCfaRelation != null) {
        for (CFANode cfaNode : pLocation.keySet()) {
          String functionName = cfaNode.getFunctionName();
          Optional<PrecisionScope> precisionScope =
              PrecisionScope.localPrecisionScopeFor(cfaNode, astCfaRelation);

          // Nodes without a scope of their own, for example the entry node of a function, are
          // exported as function-scoped predicates. This is sound, but less precise.
          addPredicates(
              predicatesPerScope,
              precisionScope.orElseGet(() -> new FunctionPrecisionScope(functionName)),
              pLocation.get(cfaNode),
              witnessPredicateFormat,
              variableName ->
                  notInternalVariable(variableName)
                      && variableNameInFunction(variableName, functionName)
                      && (precisionScope.isEmpty()
                          || variableInOriginalProgram(variableName, astCfaRelation, cfaNode)),
              machineModel,
              declarations);
        }
      }

      PrecisionExchangeSetEntry precisionExchangeSetEntry =
          new PrecisionExchangeSetEntry(
              pMetadataRecord,
              ImmutableList.copyOf(declarations),
              transformedImmutableListCopy(
                  predicatesPerScope.entrySet(),
                  scope ->
                      new PrecisionExchangeEntry(
                          witnessExpressionType,
                          scope.getKey(),
                          PrecisionType.PREDICATES,
                          ImmutableList.copyOf(scope.getValue()))));

      Path exportPath = pPathTemplate.getPath(witnessPredicateFormat.toString());
      AbstractYAMLWitnessExporter.exportEntries(
          ImmutableList.of(precisionExchangeSetEntry), exportPath, logger);
    }
  }

  private void addPredicates(
      Map<PrecisionScope, Set<String>> pPredicatesPerScope,
      PrecisionScope pScope,
      Collection<AbstractionPredicate> pPredicates,
      PredicateDumpFormat pFormat,
      Function<String, Boolean> pIncludeVariablesFilter,
      MachineModel pMachineModel,
      Set<PrecisionDeclaration> pDeclarations) {
    Set<String> predicateStrings = new LinkedHashSet<>();
    for (AbstractionPredicate predicate : pPredicates) {
      getPredicateString(
              predicate, pFormat, pIncludeVariablesFilter, fmgr, pMachineModel, pDeclarations)
          .ifPresent(predicateStrings::add);
    }
    if (!predicateStrings.isEmpty()) {
      pPredicatesPerScope
          .computeIfAbsent(pScope, scope -> new LinkedHashSet<>())
          .addAll(predicateStrings);
    }
  }
}
