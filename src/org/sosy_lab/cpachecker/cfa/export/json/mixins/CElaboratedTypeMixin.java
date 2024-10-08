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
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;

/**
 * This class is a mixin for {@link CElaboratedType}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CElaboratedTypeMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CElaboratedTypeMixin(
      @JsonProperty("isConst") boolean pConst,
      @JsonProperty("isVolatile") boolean pVolatile,
      @JsonProperty("kind") ComplexTypeKind pKind,
      @JsonProperty("name") String pName,
      @JsonProperty("origName") String pOrigName,
      @JsonProperty("realType") CComplexType pRealType) {}
}
