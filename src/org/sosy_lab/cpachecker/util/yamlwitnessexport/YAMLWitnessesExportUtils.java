// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.MetadataRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ProducerRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.TaskRecord;

class YAMLWitnessesExportUtils {

  static MetadataRecord createMetadataRecord(
      ProducerRecord producerDescription, TaskRecord taskDescription, YAMLWitnessVersion pVersion) {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    String creationTime = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    final MetadataRecord metadata =
        new MetadataRecord(
            pVersion.toString(),
            // To generate a unique UUID which is also deterministic and reproducible, we use the
            // input files and the Version to generate a UUID.
            getUUID(taskDescription.getInputFiles().toString() + pVersion).toString(),
            creationTime,
            producerDescription,
            taskDescription);
    return metadata;
  }

  private static UUID getUUID(String pSeed) {
    return UUID.nameUUIDFromBytes(pSeed.getBytes(StandardCharsets.UTF_8));
  }

  static String getArchitecture(MachineModel pMachineModel) {
    final String architecture =
        switch (pMachineModel) {
          case LINUX32 -> "ILP32";
          case LINUX64 -> "LP64";
          default -> null;
        };
    if (architecture == null) {
      throw new AssertionError("Unknown architecture: " + pMachineModel);
    }

    return architecture;
  }

  static ProducerRecord getProducerRecord(Configuration pConfig) {
    return new ProducerRecord(
        "CPAchecker",
        CPAchecker.getPlainVersion(),
        CPAchecker.getApproachName(pConfig),
        null,
        null);
  }

  static TaskRecord getTaskDescription(CFA pCFA, Specification pSpecification) throws IOException {
    List<Path> inputFiles = pCFA.getFileNames();
    ImmutableMap.Builder<String, String> inputFileHashes = ImmutableMap.builder();
    for (Path inputFile : inputFiles) {
      inputFileHashes.put(inputFile.toString(), AutomatonGraphmlCommon.computeHash(inputFile));
    }

    String specification =
        pSpecification.getProperties().stream()
            .map(Property::toString)
            .collect(Collectors.joining(" && "));

    return new TaskRecord(
        Collections3.transformedImmutableListCopy(inputFiles, Path::toString),
        inputFileHashes.buildOrThrow(),
        specification,
        getArchitecture(pCFA.getMachineModel()),
        pCFA.getLanguage().toString());
  }

  static LocationRecord createLocationRecordAtStart(FileLocation location, String functionName) {
    return createLocationRecordAtStart(location, location.getFileName().toString(), functionName);
  }

  static LocationRecord createLocationRecordAtStart(
      FileLocation location, String fileName, String functionName) {
    final int lineNumber = location.getStartingLineInOrigin();

    return new LocationRecord(
        fileName, "file_hash", lineNumber, location.getStartColumnInLine(), functionName);
  }

  static LocationRecord createLocationRecordAfterLocation(
      FileLocation fLoc, String functionName, ASTStructure astStructure) {
    final String fileName = fLoc.getFileName().toString();
    FileLocation nextStatementFileLocation =
        astStructure.nextStartStatementLocation(fLoc.getNodeOffset() + fLoc.getNodeLength());

    return YAMLWitnessesExportUtils.createLocationRecordAtStart(
        nextStatementFileLocation, fileName, functionName);
  }
}
