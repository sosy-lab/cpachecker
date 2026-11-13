// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import java.io.IOException;
import java.nio.file.Path;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

/**
 * Exports correctness witnesses in version 2.4, which adds memory-safety correctness witnesses to
 * the format. Compared to version 2.1, function contracts must use the format {@code
 * ext_c_expression}.
 *
 * @see <a
 *     href="https://gitlab.com/sosy-lab/benchmarking/sv-witnesses/-/blob/main/user-guide/Witness-Format.md">Witness
 *     format</a>
 */
class ARGToWitnessV2d4 extends ARGToWitnessV2d1 {

  ARGToWitnessV2d4(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  // TODO: export predicate definitions (entry type 'predicate_definition_set') once the
  //  invariants of the SMG2 states contain memory predicates.
  @Override
  WitnessExportResult exportWitness(ARGState pRootState, Path pOutputFile)
      throws InterruptedException, IOException, ReportingMethodNotImplementedException {
    InvariantSetContentResult content =
        createInvariantSetContent(pRootState, YAMLWitnessExpressionType.EXT_C);

    exportEntries(
        new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2d4), content.entries()),
        pOutputFile);

    return new WitnessExportResult(content.translationAlwaysSuccessful());
  }
}
