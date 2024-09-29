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
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

/**
 * This class is a mixin for {@link CSimpleType}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CSimpleTypeMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CSimpleTypeMixin(
      @JsonProperty("isConst") boolean pConst,
      @JsonProperty("isVolatile") boolean pVolatile,
      @JsonProperty("type") CBasicType pType,
      @JsonProperty("isLong") boolean pIsLong,
      @JsonProperty("isShort") boolean pIsShort,
      @JsonProperty("isSigned") boolean pIsSigned,
      @JsonProperty("isUnsigned") boolean pIsUnsigned,
      @JsonProperty("isComplex") boolean pIsComplex,
      @JsonProperty("isImaginary") boolean pIsImaginary,
      @JsonProperty("isLongLong") boolean pIsLongLong) {}
}
