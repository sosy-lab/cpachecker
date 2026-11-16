// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import java.io.IOException;
import java.util.Map.Entry;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslStatementContract;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.acsl.AcslMetadata;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.acsl.AcslInvariants;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.FunctionContractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

/**
 * Exports the ACSL annotations of a program as correctness witness, whose invariants and contracts
 * are in ACSL.
 */
public class AcslToCorrectnessWitness extends AbstractYAMLWitnessExporter {

  private final CorrectnessWitnessExportOptions options;

  public AcslToCorrectnessWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
    options = new CorrectnessWitnessExportOptions(pConfig);
  }

  public void export() throws IOException {
    if (!options.hasYamlWitnessExport()) {
      return;
    }
    ImmutableListMultimap<WitnessInvariantType, AbstractInvariantEntry> entries = createEntries();
    for (YAMLWitnessVersion version : witnessVersions) {
      exportEntries(
          new InvariantSetEntry(
              getMetadata(version),
              FluentIterable.from(version.supportedInvariantTypes())
                  .transformAndConcat(entries::get)
                  .toList()),
          options.getYamlWitnessPathTemplate().getPath(version.toString()));
    }
  }

  private ImmutableListMultimap<WitnessInvariantType, AbstractInvariantEntry> createEntries() {
    ImmutableListMultimap.Builder<WitnessInvariantType, AbstractInvariantEntry> entries =
        ImmutableListMultimap.builder();
    AcslMetadata metadata = cfa.getAcslMetadata();
    if (metadata == null) {
      return entries.build();
    }

    metadata
        .loopAnnotations()
        .forEach(
            (node, annotation) -> {
              for (AcslLoopInvariant invariant : annotation.getLoopInvariants()) {
                entries.put(
                    WitnessInvariantType.LOOP_INVARIANT,
                    invariant(
                        invariant.getPredicate(),
                        InvariantRecordType.LOOP_INVARIANT,
                        annotation,
                        node));
              }
            });
    metadata
        .assertions()
        .forEach(
            (node, assertion) ->
                entries.put(
                    WitnessInvariantType.LOCATION_INVARIANT,
                    invariant(
                        assertion.getPredicate(),
                        InvariantRecordType.LOCATION_INVARIANT,
                        assertion,
                        node)));
    // The post-state of a statement contract cannot be expressed as location invariant
    for (Entry<CFANode, AcslStatementContract> entry : metadata.statementContracts().entries()) {
      AcslStatementContract contract = entry.getValue();
      if (contract.getEnclosingBehaviors().isEmpty()) {
        for (AcslPredicate predicate : AcslInvariants.preState(contract.getContract())) {
          entries.put(
              WitnessInvariantType.LOCATION_INVARIANT,
              invariant(
                  predicate, InvariantRecordType.LOCATION_INVARIANT, contract, entry.getKey()));
        }
      }
    }
    metadata
        .functionContracts()
        .forEach(
            (node, contract) ->
                entries.put(
                    WitnessInvariantType.FUNCTION_CONTRACT,
                    functionContract(contract, (FunctionEntryNode) node)));
    return entries.build();
  }

  /** Returns the invariant, which holds before the statement that follows the annotation. */
  private InvariantEntry invariant(
      AcslPredicate pPredicate,
      InvariantRecordType pType,
      AAcslAnnotation pAnnotation,
      CFANode pNode) {
    return new InvariantEntry(
        pPredicate.toASTString(),
        pType.getKeyword(),
        YAMLWitnessExpressionType.ACSL,
        LocationRecord.createLocationRecordAfterLocation(
            pAnnotation.getFileLocation(), pNode.getFunction().getOrigName(), getASTStructure()));
  }

  private FunctionContractEntry functionContract(
      AcslFunctionContract pContract, FunctionEntryNode pFunctionEntry) {
    return new FunctionContractEntry(
        AcslInvariants.conjunction(AcslInvariants.postState(pContract)).toASTString(),
        AcslInvariants.conjunction(AcslInvariants.preState(pContract)).toASTString(),
        YAMLWitnessExpressionType.ACSL,
        LocationRecord.createLocationRecordAtStart(
            pFunctionEntry.getFileLocation(), pFunctionEntry.getFunction().getOrigName()));
  }
}
