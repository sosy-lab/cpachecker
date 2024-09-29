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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class is a mixin for {@link CVariableDeclaration}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CVariableDeclarationMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CVariableDeclarationMixin(
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("isGlobal") boolean pIsGlobal,
      @JsonProperty("cStorageClass") CStorageClass pCStorageClass,
      @JsonProperty("type") CType pType,
      @JsonProperty("name") String pName,
      @JsonProperty("origName") String pOrigName,
      @JsonProperty("qualifiedName") String pQualifiedName,
      @JsonProperty("initializer") CInitializer pInitializer) {}
}
