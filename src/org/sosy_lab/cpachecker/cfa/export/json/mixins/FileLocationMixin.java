// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.IntSequenceGenerator;
import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

/**
 * This class is a mixin for {@link FileLocation}.
 *
 * <p>Redundant file locations are serialized as references ("fileLocationNumber") to the original
 * serialized location.
 *
 * <p>It sets the order of the fields to ensure deterministic serialization.
 *
 * <p>It forces the serialization of the {@link Path} fileName field.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
@JsonIdentityInfo(
    generator = IntSequenceGenerator.class,
    scope = FileLocation.class,
    property = "fileLocationNumber")
@JsonPropertyOrder({
  "fileName",
  "niceFileName",
  "offset",
  "length",
  "startingLine",
  "endingLine",
  "startColumnInLine",
  "endColumnInLine",
  "startingLineInOrigin",
  "endingLineInOrigin",
  "offsetRelatedToOrigin"
})
public final class FileLocationMixin {

  @SuppressWarnings("unused")
  @JsonProperty
  private Path fileName;

  @SuppressWarnings("unused")
  @JsonCreator
  public FileLocationMixin(
      @JsonProperty("fileName") Path pFileName,
      @JsonProperty("niceFileName") String pNiceFileName,
      @JsonProperty("offset") int pOffset,
      @JsonProperty("length") int pLength,
      @JsonProperty("startingLine") int pStartingLine,
      @JsonProperty("endingLine") int pEndingLine,
      @JsonProperty("startColumnInLine") int pStartColumnInLine,
      @JsonProperty("endColumnInLine") int pEndColumnInLine,
      @JsonProperty("startingLineInOrigin") int pStartingLineInOrigin,
      @JsonProperty("endingLineInOrigin") int pEndingLineInOrigin,
      @JsonProperty("offsetRelatedToOrigin") boolean pOffsetRelatedToOrigin) {}
}
