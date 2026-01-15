// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.test.ACSLParserTest;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class AcslMetadataParsingTest {
  private static final String TEST_DIR = "test/programs/acsl/";
  private final String programName;
  private final String annotation;
  private final CodeLoctation loctations;
  private final CFACreator cfaCreator;
  private final LogManager logManager;

  public AcslMetadataParsingTest(
      String pProgramName, String pAnnotations, CodeLoctation pLoctations)
      throws InvalidConfigurationException {
    programName = pProgramName;
    annotation = pAnnotations;
    loctations = pLoctations;
    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromResource(ACSLParserTest.class, "acslToWitness.properties")
            .build();
    logManager = LogManager.createTestLogManager();
    cfaCreator = new CFACreator(config, logManager, ShutdownNotifier.createDummy());
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> b = ImmutableList.builder();
    b.add(task("minimal_example.c", "ensures x == 10;", new CodeLoctation(12, 5)));
    return b.build();
  }

  private static Object[] task(String program, String annotations, CodeLoctation locations) {
    return new Object[] {program, annotations, locations};
  }

  @Test
  public void parseMetadataTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());
    CFA cfa = cfaCreator.parseFileAndCreateCFA(files);

    CProgramScope programScope = new CProgramScope(cfa, logManager);
    AAcslAnnotation expectedAnnotation =
        AcslParser.parseAcslAnnotation(
            annotation, FileLocation.DUMMY, programScope, AcslScope.empty());
    //String expectedAstString = expectedAnnotation.toAstString();

    AcslMetadata acslMetadata = cfa.getMetadata().getAcslMetadata();
    if (acslMetadata != null) {
      ImmutableListMultimap<CFANode, AAcslAnnotation> annotations = acslMetadata.genericAnnotations();
    }
  }

  public record CodeLoctation(int line, int column) {}
}
