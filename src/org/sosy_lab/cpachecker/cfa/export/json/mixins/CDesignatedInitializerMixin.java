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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;

/**
 * This class is a mixin for {@link CDesignatedInitializer}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CDesignatedInitializerMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CDesignatedInitializerMixin(
      @JsonProperty("fileLocation") FileLocation pFileLocation,
      @JsonProperty("designators") List<CDesignator> pLeft,
      @JsonProperty("right") CInitializer pRight) {}
}
