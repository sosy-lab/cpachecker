/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.util.cwriter.tests;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator;
import org.sosy_lab.cpachecker.util.test.AbstractARGTranslationTest;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class ARGToCTranslatorTest {

  @RunWith(Parameterized.class)
  public static class TranslationTest extends AbstractARGTranslationTest {
    protected final Configuration reConfig;
    private final Path residualProgramPath;
    protected final boolean verdict;
    private final Path program;
    private final boolean hasGotoDecProblem;
    protected String generationPropfile;

    public TranslationTest(
        @SuppressWarnings("unused") String pTestLabel,
        String pProgram,
        boolean pVerdict,
        boolean pHasGotoDecProblem)
        throws InvalidConfigurationException, IOException {
      filePrefix = "residual";
      program = Paths.get(TEST_DIR_PATH, pProgram);
      verdict = pVerdict;
      hasGotoDecProblem = pHasGotoDecProblem;
      residualProgramPath =
          TempFile.builder().prefix("residual").suffix(".c").create().toAbsolutePath();
      generationPropfile = "inline-errorlabel.properties";
      reConfig =
          TestDataTools.configurationForTest()
              .loadFromResource(ARGToCTranslatorTest.class, "predicateAnalysis.properties")
              .build();
    }

    protected ConfigurationBuilder getGenerationConfig(String propfile)
        throws InvalidConfigurationException {
      return TestDataTools.configurationForTest()
          .loadFromResource(ARGToCTranslatorTest.class, propfile)
          .setOption("cpa.arg.export.code.handleTargetStates", "VERIFIERERROR")
          .setOption("cpa.arg.export.code.header", "false");
    }

    protected ARGToCTranslator getTranslator() throws InvalidConfigurationException {
      final Configuration generationConfig = getGenerationConfig(generationPropfile).build();
      return new ARGToCTranslator(logger, generationConfig, MachineModel.LINUX32);
    }

    @Test
    public void testVerdictsStaySame() throws Exception {
      createAndWriteARGProgram(program, residualProgramPath, hasGotoDecProblem);

      // test whether C program still gives correct verdict:
      check(reConfig, residualProgramPath, verdict);
    }

    @Test
    public void testProgramsParsable() throws Exception {
      createAndWriteARGProgram(program, residualProgramPath, hasGotoDecProblem);

      checkProgramValid(residualProgramPath);
    }

    @Test
    public void testProgramsCompilable() throws Exception {
      createAndWriteARGProgram(program, residualProgramPath, hasGotoDecProblem);

      checkProgramValid(residualProgramPath);
    }

    private void createAndWriteARGProgram(
        final Path pOriginalProgram, final Path pTargetPath, final boolean pHasGotoDecProblem)
        throws Exception {
      ARGToCTranslator translator = getTranslator();
      Configuration config = getGenerationConfig(generationPropfile).build();

      // generate ARG for C program
      ARGState root = run(config, pOriginalProgram);

      // translate write ARG to new C program
      String res = translator.translateARG(root, pHasGotoDecProblem);
      Files.write(pTargetPath, res.getBytes("utf-8"));
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
      ImmutableList.Builder<Object[]> b = ImmutableList.builder();

      // test whether writing essentially the same program will yield the same verdict
      b.add(simpleTask("main.c", true));
      b.add(simpleTask("main2.c", false));
      b.add(simpleTask("functionreturn.c", false));
      b.add(simpleTask("gotos.c", false));

      // test program generation with hasGotoDecProblem enabled.
      // I do not know good cases for when to use this flag => TODO: add better tests
      b.add(simpleTestWithGotoDecProblem("main.c", true));
      b.add(simpleTestWithGotoDecProblem("main2.c", false));
      b.add(simpleTestWithGotoDecProblem("functionreturn.c", false));

      return b.build();
    }

    private static Object[] simpleTask(String program, boolean verdict) {
      String label = String.format("SimpleTest(%s is %s)", program, Boolean.toString(verdict));
      return new Object[] {label, program, verdict, false};
    }

    private static Object[] simpleTestWithGotoDecProblem(String program, boolean verdict) {
      String label =
          String.format(
              "SimpleTestWithGotoDecProblem(%s is %s)", program, Boolean.toString(verdict));
      return new Object[] {label, program, verdict, true};
    }
  }

  @RunWith(Parameterized.class)
  public static class SpecificationCombinationTest extends TranslationTest {
    private final String spec;

    public SpecificationCombinationTest(
        @SuppressWarnings("unused") String pTestLabel,
        String pProgram,
        boolean pVerdict,
        boolean pHasGotoDecProblem,
        String pSpec,
        boolean useOverflows)
        throws InvalidConfigurationException, IOException {
      super(pTestLabel, pProgram, pVerdict, pHasGotoDecProblem);

      spec = pSpec;
      generationPropfile =
          useOverflows ? "inline-overflow.properties" : "inline-errorlabel.properties";
    }

    @Override
    protected ConfigurationBuilder getGenerationConfig(String propfile)
        throws InvalidConfigurationException {
      ConfigurationBuilder config = super.getGenerationConfig(propfile);
      if (spec != null) {
        String specPath = Paths.get(TEST_DIR_PATH, spec).toString();
        config.setOption("specification", specPath);
      }
      return config;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
      ImmutableList.Builder<Object[]> b = ImmutableList.builder();

      // Test whether two or more outgoing edges are handled properly, i.e., whether the verdicts of
      // the original program+spec is the same as the verdict for the generated program with default
      // reachability spec:
      b.add(specCausedMultiBranchingTest("main.c", false, "main_additional_spec.spc"));
      b.add(specCausedMultiBranchingTest("simple.c", false, "simple_additional_spec.spc"));
      b.add(specCausedMultiBranchingTest("simple.c", true, "simple_additional_spec2.spc"));
      b.add(specCausedMultiBranchingTest("simple2.c", false, "simple2_additional_spec.spc"));

      // Test whether we can encode overflows correctly:
      b.add(overflowToCTest("simple.c", false));

      // b.addAll(conditionAutomataTests());

      return b.build();
    }

    private static Object[] specCausedMultiBranchingTest(
        String program, boolean verdict, String spec) {
      String label =
          String.format("specCausedMultiBranchingTest(%s with %s is %s)", program, spec, verdict);
      return new Object[] {label, program, verdict, false, spec, false};
    }

    private static Object[] overflowToCTest(String program, boolean verdict) {
      String label = String.format("overflowToCTest(%s is %s)", program, verdict);
      return new Object[] {label, program, verdict, true, null, true};
    }
  }

}
