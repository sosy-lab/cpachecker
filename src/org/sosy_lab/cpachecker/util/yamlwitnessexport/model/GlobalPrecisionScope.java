// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = JsonDeserializer.None.class)
public final class GlobalPrecisionScope extends PrecisionScope {

  public static final String GLOBAL_TYPE_IDENTIFIER = "global";

  public GlobalPrecisionScope() {

    super(GLOBAL_TYPE_IDENTIFIER);
  }
}
