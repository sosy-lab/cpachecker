// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.directexport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.invariantwitness.directexport.DataTypes.WitnessVersion;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.LocationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.ProducerRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.TaskRecord;

public class Utils {

  public static MetadataRecord createMetadataRecord(
      ProducerRecord producerDescription, TaskRecord taskDescription, WitnessVersion pVersion) {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    String creationTime = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    final MetadataRecord metadata =
        new MetadataRecord(
            pVersion.toString(),
            UUID.randomUUID().toString(),
            creationTime,
            producerDescription,
            taskDescription);
    return metadata;
  }

  public static String getArchitecture(MachineModel pMachineModel) {
    final String architecture =
        switch (pMachineModel) {
          case LINUX32 -> "ILP32";
          case LINUX64 -> "LP64";
          default -> pMachineModel.toString();
        };
    return architecture;
  }

  public static ProducerRecord getProducerRecord(Configuration pConfig) {
    return new ProducerRecord(
        "CPAchecker",
        CPAchecker.getPlainVersion(),
        CPAchecker.getApproachName(pConfig),
        null,
        null);
  }

  public static TaskRecord getTaskDescription(CFA pCFA, Specification pSpecification)
      throws IOException {
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
        inputFiles.stream().map(Path::toString).collect(ImmutableList.toImmutableList()),
        inputFileHashes.buildOrThrow(),
        specification,
        getArchitecture(pCFA.getMachineModel()),
        pCFA.getLanguage().toString());
  }

  public static LocationRecord createLocationRecordAtStart(
      FileLocation location, ListMultimap<String, Integer> lineOffsetsByFile, String functionName) {
    return createLocationRecordAtStart(
        location, lineOffsetsByFile, location.getFileName().toString(), functionName);
  }

  public static LocationRecord createLocationRecordAtStart(
      FileLocation location,
      ListMultimap<String, Integer> lineOffsetsByFile,
      String fileName,
      String functionName) {
    final int lineNumber = location.getStartingLineInOrigin();
    final int lineOffset = lineOffsetsByFile.get(fileName).get(lineNumber - 1);
    final int offsetInLine = location.getNodeOffset() - lineOffset;

    return new LocationRecord(fileName, "file_hash", lineNumber, offsetInLine, functionName);
  }

  public static LocationRecord createLocationRecordAfterLocation(
      FileLocation fLoc,
      ListMultimap<String, Integer> lineOffsetsByFile,
      String functionName,
      ASTStructure astStructure) {
    final String fileName = fLoc.getFileName().toString();
    FileLocation nextStatementFileLocation =
        astStructure.nextStartStatementLocation(fLoc.getNodeOffset() + fLoc.getNodeLength());

    return Utils.createLocationRecordAtStart(
        nextStatementFileLocation, lineOffsetsByFile, fileName, functionName);
  }
}
