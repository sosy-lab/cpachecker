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
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;

/**
 * This class is a mixin for {@link CCompositeType}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CCompositeTypeMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CCompositeTypeMixin(
      @JsonProperty("isConst") boolean pConst,
      @JsonProperty("isVolatile") boolean pVolatile,
      @JsonProperty("kind") ComplexTypeKind pKind,
      @JsonProperty("members") List<CCompositeTypeMemberDeclaration> pMembers,
      @JsonProperty("name") String pName,
      @JsonProperty("origName") String pOrigName) {}
}
