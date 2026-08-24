// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cmdline;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import org.junit.Test;

public class CmdLineArgumentsTest {

  @Test
  public void testResolveSpecificationFileOrExit_ValidSpecificationIsResolved() {
    // A valid specification name is resolved to its .spc file without ever consulting
    // getSpecificationSuggestion(), so no "Did you mean" logic is triggered.
    Path specFile = CmdLineArguments.resolveSpecificationFileOrExit("memorysafety");

    assertThat(specFile.toString()).endsWith("memorysafety.spc");
  }

  @Test
  public void testGetSpecificationSuggestion_TypoSuggestsClosestKnownSpecification() {
    // "memsafety" is not a valid specification name, but "memorysafety" (config/specification
    // /memorysafety.spc) is close enough to be suggested, cf. ISSUE.md.
    assertThat(CmdLineArguments.getSpecificationSuggestion("memsafety"))
        .isEqualTo(" Did you mean 'memorysafety'?");
  }

  @Test
  public void testGetSpecificationSuggestion_UnrelatedNameHasNoSuggestion() {
    assertThat(CmdLineArguments.getSpecificationSuggestion("totallybogusxyz")).isEmpty();
  }
}
