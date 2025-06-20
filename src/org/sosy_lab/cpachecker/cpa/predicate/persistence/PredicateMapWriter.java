// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.splitFormula;
import static org.sosy_lab.cpachecker.util.expressions.ExpressionTrees.FUNCTION_DELIMITER;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocalPrecisionScope;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.MetadataRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionDeclaration;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.PrecisionExchangeSetEntry;

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
    Set<String> definitions = new LinkedHashSet<>();

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
      ImmutableList.Builder<PrecisionDeclaration> pDeclarationBuilder) {
    return switch (pFormat) {
      case SMTLIB2 -> {
        Pair<String, List<String>> p = splitFormula(pFmgr, pPredicate.getSymbolicAtom());
        pDeclarationBuilder.addAll(
            FluentIterable.from(p.getSecond()).transform(value -> new PrecisionDeclaration(value)));
        yield Optional.of(Objects.requireNonNull(p.getFirst()));
      }
      case C -> {
        try {
          yield Optional.of(
              AbstractionFormula.asExpressionTree(
                      pPredicate.getSymbolicAtom(), pFmgr, pIncludeVariablesFilter, y -> y)
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
        && !pQualifiedVariableName.contains("__ADDRESS_OF_");
  }

  public static boolean variableNameInFunction(
      String pQualifiedVariableName, String pFunctionName) {
    return !pQualifiedVariableName.contains(FUNCTION_DELIMITER)
        || pQualifiedVariableName.startsWith(pFunctionName + FUNCTION_DELIMITER);
  }

  public static boolean variableInOriginalProgram(
      String pQualifiedVariableName, AstCfaRelation pAstCfaRelation, CFANode pLocation) {
    return pAstCfaRelation
        .getVariablesAndParametersInScope(pLocation)
        .orElseThrow()
        .anyMatch(
            var ->
                // For local variables
                (pLocation.getFunctionName()
                            + FUNCTION_DELIMITER
                            + Objects.requireNonNull(var).getName())
                        .equals(pQualifiedVariableName)
                    // For global variables
                    || var.getName().equals(pQualifiedVariableName));
  }

  public void writePredicateMapAsWitness(
      SetMultimap<CFANode, AbstractionPredicate> pLocation,
      SetMultimap<String, AbstractionPredicate> pFunction,
      Set<AbstractionPredicate> pGlobal,
      PathTemplate pPathTemplate,
      MetadataRecord pMetadataRecord) {

    for (PredicateDumpFormat witnessPredicateFormat : witnessPredicateFormats) {

      // Build the data structures that contain the predicates
      ImmutableList.Builder<PrecisionExchangeEntry> entriesBuilder = ImmutableList.builder();
      ImmutableList.Builder<PrecisionDeclaration> declarationBuilder = ImmutableList.builder();

      YAMLWitnessExpressionType witnessExpressionType =
          YAMLWitnessExpressionType.fromPredicateFormat(witnessPredicateFormat);

      // Add all global predicates
      entriesBuilder.add(
          new PrecisionExchangeEntry(
              witnessExpressionType,
              new GlobalPrecisionScope(),
              FluentIterable.from(pGlobal)
                  .transform(
                      pFormula ->
                          getPredicateString(
                              pFormula,
                              witnessPredicateFormat,
                              name ->
                                  // TODO: The ADDRESS_OF problem should not be solved here, but in
                                  //  the translation back from SMT to C
                                  notInternalVariable(name),
                              fmgr,
                              declarationBuilder))
                  .filter(Optional::isPresent)
                  .transform(Optional::orElseThrow)
                  .toSet()
                  .asList()));

      // Add all function predicates
      entriesBuilder.addAll(
          FluentIterable.from(pFunction.keys())
              .transform(
                  functionName ->
                      new PrecisionExchangeEntry(
                          witnessExpressionType,
                          new FunctionPrecisionScope(functionName),
                          FluentIterable.from(pFunction.get(functionName))
                              .transform(
                                  pFormula ->
                                      getPredicateString(
                                          pFormula,
                                          witnessPredicateFormat,
                                          name ->
                                              notInternalVariable(name)
                                                  && variableNameInFunction(name, functionName),
                                          fmgr,
                                          declarationBuilder))
                              .filter(Optional::isPresent)
                              .transform(Optional::orElseThrow)
                              .toSet()
                              .asList()))
              .toList());

      // Add all local predicates
      if (cfa.isPresent()) {
        AstCfaRelation astCfaRelation = cfa.orElseThrow().getAstCfaRelation();

        for (CFANode cfaNode : pLocation.keySet()) {
          String functionName = cfaNode.getFunctionName();

          Optional<FileLocation> fileLocation =
              astCfaRelation.getStatementFileLocationForNode(cfaNode);

          if (fileLocation.isEmpty()) {
            // TODO: This should never happen, it is a bug in the AST-CFA relation.

            // As a workaround, we export the predicates as function-scoped
            // predicates, but this is not quite correct.
            entriesBuilder.add(
                new PrecisionExchangeEntry(
                    witnessExpressionType,
                    new FunctionPrecisionScope(functionName),
                    FluentIterable.from(pLocation.get(cfaNode))
                        .transform(
                            pFormula ->
                                getPredicateString(
                                    pFormula,
                                    witnessPredicateFormat,
                                    name ->
                                        notInternalVariable(name)
                                            && variableNameInFunction(name, functionName),
                                    fmgr,
                                    declarationBuilder))
                        .filter(Optional::isPresent)
                        .transform(Optional::orElseThrow)
                        .toSet()
                        .asList()));

          } else {

            entriesBuilder.add(
                new PrecisionExchangeEntry(
                    witnessExpressionType,
                    new LocalPrecisionScope(
                        LocationRecord.createLocationRecordAtStart(
                            fileLocation.orElseThrow(), cfaNode.getFunctionName())),
                    FluentIterable.from(pLocation.get(cfaNode))
                        .transform(
                            pFormula ->
                                getPredicateString(
                                    pFormula,
                                    witnessPredicateFormat,
                                    variableName ->
                                        notInternalVariable(variableName)
                                            && variableNameInFunction(
                                                variableName, cfaNode.getFunctionName())
                                            && variableInOriginalProgram(
                                                variableName, astCfaRelation, cfaNode),
                                    fmgr,
                                    declarationBuilder))
                        .filter(Optional::isPresent)
                        .transform(Optional::orElseThrow)
                        .toSet()
                        .asList()));
          }
        }
      } else {
        // If no CFA is present, we cannot export local predicates
        logger.log(
            Level.INFO,
            "No CFA present, skipping export of local predicates in precision exchange set.");
      }

      PrecisionExchangeSetEntry precisionExchangeSetEntry =
          new PrecisionExchangeSetEntry(
              pMetadataRecord, declarationBuilder.build(), entriesBuilder.build());

      Path exportPath = pPathTemplate.getPath(witnessPredicateFormat.toString());
      AbstractYAMLWitnessExporter.exportEntries(
          ImmutableList.of(precisionExchangeSetEntry), exportPath, logger);
    }
  }
}
