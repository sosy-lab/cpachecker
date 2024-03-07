// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.base.Verify;
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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.MetadataRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ProducerRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.TaskRecord;

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
    producerRecord = ProducerRecord.getProducerRecord(pConfig);
  }

  protected MetadataRecord getMetadata(YAMLWitnessVersion version) throws IOException {
    return MetadataRecord.createMetadataRecord(
        producerRecord, TaskRecord.getTaskDescription(cfa, specification), version);
  }

  protected AstCfaRelation getASTStructure() {
    AstCfaRelation astCFARelation = cfa.getASTStructure();
    return Verify.verifyNotNull(astCFARelation);
  }

  protected void exportEntries(AbstractEntry entry, Path outFile) {
    if (outFile == null) {
      logger.log(Level.FINE, "Output file is null, not exporting witness.");
      return;
    }

    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      String entryYaml = mapper.writeValueAsString(ImmutableList.of(entry));
      writer.write(entryYaml);
    } catch (JsonProcessingException e) {
      logger.log(
          Level.INFO,
          "witness export to "
              + outFile
              + " was not possible due to an internal error when transforming "
              + "the internal witness data structure into a YAML string.");
    } catch (IOException e) {
      logger.log(
          Level.INFO,
          "witness export to "
              + outFile
              + " failed due to not being able to write to the output file.");
    }
  }
}
