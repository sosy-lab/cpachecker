/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.automaton;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
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
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.util.SpecificationProperty;

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
          "Provides additional candidate invariants to the k-induction invariant generator."
    )
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private Path invariantsAutomatonFile = null;
  }

  private final VerificationTaskMetaData.HackyOptions hackyOptions = new HackyOptions();

  private final Specification specification;

  private List<Path> nonWitnessAutomatonFiles = null;

  private List<Path> witnessAutomatonFiles = null;

  public VerificationTaskMetaData(Configuration pConfig, Specification pSpecification)
      throws InvalidConfigurationException {
    pConfig.inject(hackyOptions);
    specification = checkNotNull(pSpecification);
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
    Set<String> pathsAssociatedWithPropertyFiles =
        FluentIterable.from(getProperties())
            .transform(SpecificationProperty::getInternalSpecificationPath)
            .filter(Optional::isPresent)
            .transform(Optional::get)
            .toSet();
    return FluentIterable.from(nonWitnessAutomatonFiles)
        .filter(p -> !pathsAssociatedWithPropertyFiles.contains(p.toString()))
        .toList();
  }

  /**
   * The specification properties considered for this verification task.
   *
   * @return the specification properties considered for this verification task.
   */
  public Set<SpecificationProperty> getProperties() {
    return specification.getProperties();
  }

  private final void classifyAutomataFiles() throws IOException {
    if (nonWitnessAutomatonFiles == null) {
      assert witnessAutomatonFiles == null;
      ImmutableList.Builder<Path> nonWitnessAutomatonFilesBuilder = ImmutableList.builder();
      ImmutableList.Builder<Path> witnessAutomatonFilesBuilder = ImmutableList.builder();
      Set<Path> specs = from(specification.getSpecFiles()).transform(Path::normalize).toSet();
      for (Path path : specs) {
        if (AutomatonGraphmlParser.isGraphmlAutomaton(path)) {
          witnessAutomatonFilesBuilder.add(path);
        } else {
          nonWitnessAutomatonFilesBuilder.add(path);
        }
      }
      Optional<Path> correctnessWitness =
          Optional.ofNullable(hackyOptions.invariantsAutomatonFile).map(Path::normalize);
      if (correctnessWitness.isPresent()
          && !specs.contains(correctnessWitness.get())) {
        witnessAutomatonFilesBuilder.add(correctnessWitness.get());
      }
      witnessAutomatonFiles = witnessAutomatonFilesBuilder.build();
      nonWitnessAutomatonFiles = nonWitnessAutomatonFilesBuilder.build();
    } else {
      assert witnessAutomatonFiles != null;
    }
  }
}
