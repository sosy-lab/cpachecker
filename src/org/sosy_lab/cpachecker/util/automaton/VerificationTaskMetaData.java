// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.automaton;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;

public class VerificationTaskMetaData {

  /**
   * This is a temporary hack to easily obtain specification and verification tasks. TODO: Move the
   * witness export out of the ARG CPA after the new error report has been integrated and obtain the
   * values without this hack.
   */
  @Options
  private static class HackyOptions {

    @Option(
        secure = true,
        name = "invariantGeneration.kInduction.invariantsAutomatonFile",
        description =
            "Provides additional candidate invariants to the k-induction invariant generator.")
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private Path invariantsAutomatonFile = null;
  }

  private final VerificationTaskMetaData.HackyOptions hackyOptions = new HackyOptions();

  private final Specification specification;

  private List<Path> nonWitnessAutomatonFiles = null;

  private List<Path> witnessAutomatonFiles = null;

  private final String producerString;

  public VerificationTaskMetaData(Configuration pConfig, Specification pSpecification)
      throws InvalidConfigurationException {
    pConfig.inject(hackyOptions);
    specification = checkNotNull(pSpecification);
    producerString = CPAchecker.getVersion(pConfig);
  }

  public List<Path> getInputWitnessFiles() throws IOException {
    classifyAutomataFiles();
    return witnessAutomatonFiles;
  }

  /**
   * Gets the specifications considered for this verification task that are not associated with
   * specification properties (see {@link VerificationTaskMetaData#getProperties}.
   *
   * @return the specifications considered for this verification task.
   * @throws IOException if the specification files cannot be accessed.
   */
  public List<Path> getNonPropertySpecificationFiles() throws IOException {
    classifyAutomataFiles();
    return nonWitnessAutomatonFiles;
  }

  /** Return the properties considered for this verification task. */
  public Set<Property> getProperties() {
    return specification.getProperties();
  }

  /**
   * Returns a string that describes the program that produced an output file. This is primarily
   * intended for use in the producer field of verification witnesses.
   */
  public String getProducerString() {
    return producerString;
  }

  private void classifyAutomataFiles() throws IOException {
    if (nonWitnessAutomatonFiles == null) {
      assert witnessAutomatonFiles == null;
      ImmutableList.Builder<Path> nonWitnessAutomatonFilesBuilder = ImmutableList.builder();
      ImmutableList.Builder<Path> witnessAutomatonFilesBuilder = ImmutableList.builder();
      Set<Path> specs = transformedImmutableSetCopy(specification.getFiles(), Path::normalize);
      for (Path path : specs) {
        if (!MoreFiles.getFileExtension(path).equals("prp")) {
          if (AutomatonGraphmlParser.isGraphmlAutomaton(path)) {
            witnessAutomatonFilesBuilder.add(path);
          } else {
            nonWitnessAutomatonFilesBuilder.add(path);
          }
        }
      }
      Optional<Path> correctnessWitness =
          Optional.ofNullable(hackyOptions.invariantsAutomatonFile).map(Path::normalize);
      if (correctnessWitness.isPresent() && !specs.contains(correctnessWitness.orElseThrow())) {
        witnessAutomatonFilesBuilder.add(correctnessWitness.orElseThrow());
      }
      witnessAutomatonFiles = witnessAutomatonFilesBuilder.build();
      nonWitnessAutomatonFiles = nonWitnessAutomatonFilesBuilder.build();
    } else {
      assert witnessAutomatonFiles != null;
    }
  }
}
