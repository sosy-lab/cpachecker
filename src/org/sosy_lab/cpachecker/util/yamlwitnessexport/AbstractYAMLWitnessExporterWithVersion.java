// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import java.io.IOException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.MetadataRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ProducerRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.TaskRecord;

abstract class AbstractYAMLWitnessExporterWithVersion extends AbstractYAMLWitnessExporter {

  final YAMLWitnessVersion witnessVersion;

  private final ProducerRecord producerRecord;

  protected AbstractYAMLWitnessExporterWithVersion(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      YAMLWitnessVersion pWitnessVersion)
      throws InvalidConfigurationException {

    super(pConfig, pCfa, pSpecification, pLogger);
    witnessVersion = pWitnessVersion;
    producerRecord = ProducerRecord.getProducerRecord(pConfig);
  }

  protected MetadataRecord getMetadata() throws IOException {
    return MetadataRecord.createMetadataRecord(
        producerRecord, TaskRecord.getTaskDescription(cfa, specification), witnessVersion);
  }
}
