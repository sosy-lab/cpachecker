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
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

/**
 * This class is a mixin for {@link CEnumType}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CEnumTypeMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CEnumTypeMixin(
      @JsonProperty("isConst") boolean pConst,
      @JsonProperty("isVolatile") boolean pVolatile,
      @JsonProperty("compatibleType") CSimpleType pCompatibleType,
      @JsonProperty("enumerators") List<CEnumerator> pEnumerators,
      @JsonProperty("name") String pName,
      @JsonProperty("origName") String pOrigName) {}
}
