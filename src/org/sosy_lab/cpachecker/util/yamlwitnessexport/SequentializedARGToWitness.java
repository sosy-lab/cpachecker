// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

/**
 * Exports a correctness witness for a proof that was found by analyzing a sequentialization of a
 * concurrent program. The witness holds no invariant, because every invariant of the
 * sequentialization is expressed over locations and variables that the concurrent input program
 * does not have. Only the metadata, which refers to the input program, is exported.
 */
public class SequentializedARGToWitness extends AbstractYAMLWitnessExporter {

  /**
   * Creates an exporter for the input program of a sequentialization.
   *
   * @param pOriginalCfa the CFA of the concurrent input program, i.e. {@code
   *     MporSequentialization#originalCfa()}, which the metadata refers to
   */
  public SequentializedARGToWitness(
      Configuration pConfig, CFA pOriginalCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pOriginalCfa, pSpecification, pLogger);
  }

  /** Exports an empty invariant set for every requested witness version. */
  public void export(PathTemplate pOutputFileTemplate) throws IOException {
    for (YAMLWitnessVersion witnessVersion : ImmutableSet.copyOf(witnessVersions)) {
      // as in ARGToYAMLWitnessExport, correctness witnesses have no version 2.2
      YAMLWitnessVersion version =
          witnessVersion == YAMLWitnessVersion.V2d2 ? YAMLWitnessVersion.V2d1 : witnessVersion;
      exportEntries(
          new InvariantSetEntry(getMetadata(version), ImmutableList.of()),
          pOutputFileTemplate.getPath(witnessVersion.toString()));
    }
  }
}
