// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.directexport;

import static java.util.logging.Level.WARNING;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.invariantwitness.directexport.DataTypes.WitnessVersion;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantStoreUtil;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter.YamlWitnessExportException;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.ProducerRecord;

@Options(prefix = "witness.directexport")
public abstract class DirectWitnessExporter {

  @Option(secure = true, description = "The version for which to export the witness.")
  protected List<WitnessVersion> witnessVersions = ImmutableList.of(WitnessVersion.V2);

  @Option(
      secure = true,
      name = "outputFileTemplate",
      description =
          "The template from which the different export"
              + "versions of the witnesses will be exported. "
              + "Each version replaces the string '<<VERSION>>' "
              + "with its version number.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate outputFileTemplate = PathTemplate.ofFormatString("witness-%s.yml");

  protected CFA cfa;
  protected LogManager logger;
  protected Specification specification;
  protected final ObjectMapper mapper;

  // Should only be accessed through the getter method, since having computations be done in the
  // constructor does not seem ideal. In particular creating these methods may throw IOExceptions
  // which is not ideal in a constructor
  private ListMultimap<String, Integer> privateLineOffsetsByFile;
  private final ProducerRecord producerRecord;
  private MetadataRecord metadata = null;

  protected DirectWitnessExporter(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this, DirectWitnessExporter.class);
    specification = pSpecification;
    logger = pLogger;
    cfa = pCfa;

    mapper =
        new ObjectMapper(
            YAMLFactory.builder()
                .disable(Feature.WRITE_DOC_START_MARKER, Feature.SPLIT_LINES)
                .build());
    mapper.setSerializationInclusion(Include.NON_NULL);
    producerRecord = WitnessV2ExportUtils.getProducerRecord(pConfig);
  }

  protected ListMultimap<String, Integer> getlineOffsetsByFile() throws IOException {
    if (privateLineOffsetsByFile == null) {
      privateLineOffsetsByFile = InvariantStoreUtil.getLineOffsetsByFile(cfa.getFileNames());
    }
    return privateLineOffsetsByFile;
  }

  protected MetadataRecord getMetadata(WitnessVersion version) throws IOException {
    if (metadata == null) {
      metadata =
          WitnessV2ExportUtils.createMetadataRecord(
              producerRecord, WitnessV2ExportUtils.getTaskDescription(cfa, specification), version);
    }
    return metadata;
  }

  protected Path getOutputFile(WitnessVersion version) {
    if (outputFileTemplate == null) {
      return null;
    }

    return outputFileTemplate.getPath(version.toString());
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
