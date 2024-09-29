// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

/**
 * This class is a mixin for {@link CFunctionDeclaration}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CFunctionDeclarationMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CFunctionDeclarationMixin(
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("type") CFunctionType pType,
      @JsonProperty("name") String pName,
      @JsonProperty("origName") String pOrigName,
      @JsonProperty("parameters") List<CParameterDeclaration> parameters,
      @JsonProperty("attributes") ImmutableSet<FunctionAttribute> pAttributes) {}
}
