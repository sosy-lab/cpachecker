// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static java.util.logging.Level.WARNING;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.ProducerRecord;

@Options(prefix = "witness.yamlexporter")
abstract class AbstractYAMLWitnessExporter {

  @Option(secure = true, description = "The version for which to export the witness.")
  protected List<YAMLWitnessVersion> witnessVersions = ImmutableList.of(YAMLWitnessVersion.V2);

  protected final CFA cfa;
  protected final LogManager logger;
  private final Specification specification;
  protected final ObjectMapper mapper;
  private final ProducerRecord producerRecord;

  protected AbstractYAMLWitnessExporter(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this, AbstractYAMLWitnessExporter.class);
    specification = pSpecification;
    logger = pLogger;
    cfa = pCfa;

    mapper =
        new ObjectMapper(
            YAMLFactory.builder()
                .disable(Feature.WRITE_DOC_START_MARKER, Feature.SPLIT_LINES)
                .build());
    mapper.setSerializationInclusion(Include.NON_NULL);
    producerRecord = YAMLWitnessesExportUtils.getProducerRecord(pConfig);
  }

  protected MetadataRecord getMetadata(YAMLWitnessVersion version) throws IOException {
    return YAMLWitnessesExportUtils.createMetadataRecord(
        producerRecord, YAMLWitnessesExportUtils.getTaskDescription(cfa, specification), version);
  }

  protected static Path getOutputFile(YAMLWitnessVersion version, PathTemplate pPathTemplate) {
    if (pPathTemplate == null) {
      return null;
    }

    return pPathTemplate.getPath(version.toString());
  }

  protected ASTStructure getASTStructure() throws YamlWitnessExportException {
    if (cfa.getASTStructure().isEmpty()) {
      throw new YamlWitnessExportException(
          "Could not get ASTStructure which is required to export the witnesses!");
    }
    return cfa.getASTStructure().orElseThrow();
  }

  protected void exportEntries(AbstractEntry entry, Path outFile) {
    if (outFile == null) {
      logger.log(Level.INFO, "Output file is null, not exporting witness.");
      return;
    }

    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      String entryYaml = mapper.writeValueAsString(ImmutableList.of(entry));
      writer.write(entryYaml);
    } catch (IOException e) {
      logger.logfException(WARNING, e, "Invariant witness export to %s failed.", outFile);
    }
  }
}
