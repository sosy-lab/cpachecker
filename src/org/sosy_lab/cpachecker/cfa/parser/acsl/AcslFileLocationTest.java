// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class AcslFileLocationTest {

  @Test
  public void termsHaveTheirLocationInTheProgram() throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromResource(AcslFileLocationTest.class, "withAcslMetadata.properties")
            .build();
    CFACreator cfaCreator =
        new CFACreator(config, LogManager.createTestLogManager(), ShutdownNotifier.createDummy());
    // Line 24 of the program is "    //@ assert y == i * x;"
    CFA cfa =
        cfaCreator.parseFileAndCreateCFA(
            ImmutableList.of("test/programs/acsl/shadowed_variable.c"));

    AcslAssertion assertion = Iterables.getOnlyElement(cfa.getAcslMetadata().assertions().values());
    ImmutableMap<String, FileLocation> locations =
        CFAUtils.traverseRecursively(assertion.getPredicate())
            .filter(AcslIdTerm.class)
            .uniqueIndex(term -> term.getDeclaration().getOrigName())
            .entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(e -> e.getKey(), e -> e.getValue().getFileLocation()));

    FileLocation y = locations.get("y");
    assertThat(y.getStartingLineNumber()).isEqualTo(24);
    assertThat(y.getEndingLineNumber()).isEqualTo(24);
    assertThat(y.getStartColumnInLine()).isEqualTo(16);
    assertThat(y.getEndColumnInLine()).isEqualTo(17);
    assertThat(y.getNodeLength()).isEqualTo(1);
    assertThat(locations.get("x").getStartColumnInLine()).isEqualTo(25);
    assertThat(assertion.getPredicate().getFileLocation().getStartColumnInLine()).isEqualTo(16);
    assertThat(assertion.getPredicate().getFileLocation().getEndColumnInLine()).isEqualTo(26);
  }
}
