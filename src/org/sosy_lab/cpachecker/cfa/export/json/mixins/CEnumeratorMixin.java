// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;

/**
 * This class is a mixin for {@link CEnumerator}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CEnumeratorMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CEnumeratorMixin(
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("name") @JsonAlias("origName") String pName,
      @JsonProperty("qualifiedName") String pQualifiedName,
      @JsonProperty("value") BigInteger pValue) {}
}
