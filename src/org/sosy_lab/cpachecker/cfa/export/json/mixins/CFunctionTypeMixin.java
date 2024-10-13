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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class is a mixin for {@link CFunctionType}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
@JsonSubTypes({@Type(value = CFunctionTypeWithNames.class, name = "CFunctionWithNames")})
public final class CFunctionTypeMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CFunctionTypeMixin(
      @JsonProperty("returnType") CType pReturnType,
      @JsonProperty("parameters") List<CType> pParameters,
      @JsonProperty("takesVarArgs") boolean pTakesVarArgs) {}
}
