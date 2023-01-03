// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling.test;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.sampling.Sample;
import org.sosy_lab.cpachecker.core.algorithm.sampling.Sample.SampleClass;
import org.sosy_lab.cpachecker.core.algorithm.sampling.SampleClassificationAlgorithm;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ClassificationTest {

  private static final String TEST_PROGRAM_DIR = "test/programs/induction/";
  private static final String TEST_CONFIG =
      "config/valueAnalysis-predicateAnalysis-Cegar-ABEl.properties";
  private static final String TEST_SPECIFICATION = "config/specification/default.spc";

  private Configuration testConfig;
  private LogManager testLogger;
  private CFACreator cfaCreator;

  @Parameter public String pathToProgram;

  @Before
  public void setup() throws InvalidConfigurationException, IOException {
    testConfig = Configuration.builder().loadFromFile(TEST_CONFIG).build();
    testLogger = LogManager.createTestLogManager();
    cfaCreator = new CFACreator(testConfig, testLogger, ShutdownNotifier.createDummy());
  }

  @Test
  public void induction1_positiveSample()
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException,
          CPAException {
    String fileName = "induction1.c";
    SampleClass expected = SampleClass.POSITIVE;

    CFA cfa = createCFAForProgram(fileName);
    assertThat(cfa.getAllLoopHeads().isPresent()).isTrue();

    for (CFANode loopHead : cfa.getAllLoopHeads().orElseThrow()) {

      MemoryLocation varX = MemoryLocation.forIdentifier("x");
      MemoryLocation varY = MemoryLocation.forIdentifier("y");

      Value value = new NumericValue(BigInteger.valueOf(1));
      Type type = CNumericTypes.INT;
      ValueAndType valueAndType = new ValueAndType(value, type);

      Sample sample =
          new Sample(
              ImmutableMap.of(varX, valueAndType, varY, valueAndType),
              loopHead,
              null,
              SampleClass.UNKNOWN);
      performTest(cfa, sample, expected);
    }
    // TODO: Make more tests, but probably better to automize first -> read in samples from file (or
    //  string for tests like these)
  }

  private CFA createCFAForProgram(String filename)
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException {
    String fullPath = Path.of(TEST_PROGRAM_DIR, filename).toString();
    return cfaCreator.parseFileAndCreateCFA(ImmutableList.of(fullPath));
  }

  private void performTest(CFA cfa, Sample sample, SampleClass expected)
      throws InterruptedException, InvalidConfigurationException, CPAException {
    Specification specification =
        Specification.fromFiles(
            ImmutableList.of(Path.of(TEST_SPECIFICATION)),
            cfa,
            testConfig,
            testLogger,
            ShutdownNotifier.createDummy());
    SampleClassificationAlgorithm classificationAlgorithm =
        new SampleClassificationAlgorithm(
            testConfig, testLogger, ShutdownManager.create(), cfa, specification);
    assertThat(classificationAlgorithm.run(sample, cfa.getMainFunction())).isEqualTo(expected);
  }
}
