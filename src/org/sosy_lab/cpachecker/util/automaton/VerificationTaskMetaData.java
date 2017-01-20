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

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
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
      name = "analysis.programNames",
      description = "A String, denoting the programs to be analyzed"
    )
    private String programs;

    @Option(
      secure = true,
      name = "specification",
      description =
          "comma-separated list of files with specifications that should be checked"
              + "\n(see config/specification/ for examples)"
    )
    @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
    private List<Path> specificationFiles = ImmutableList.of();

    @Option(
      secure = true,
      name = "witness.validation.file",
      description = "The witness to validate."
    )
    @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
    private Path inputWitness;

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

  private final Optional<Iterable<String>> programNames;

  private final Optional<Specification> specification;

  private List<String> programHash;

  private List<String> inputWitnessHashes = null;

  private List<Path> nonWitnessAutomatonFiles = null;

  private List<Path> witnessAutomatonFiles = null;

  public VerificationTaskMetaData(Configuration pConfig, Optional<Specification> pSpecification)
      throws InvalidConfigurationException {
    pConfig.inject(hackyOptions);
    specification = pSpecification;
    if (hackyOptions.programs == null) {
      programNames = Optional.empty();
    } else {
      Splitter commaSplitter = Splitter.on(',').omitEmptyStrings().trimResults();
      programNames = Optional.of(commaSplitter.split(hackyOptions.programs));
    }
  }

  private static String computeHash(Path pPath) throws IOException {
    HashCode hash = MoreFiles.asByteSource(pPath).hash(Hashing.sha1());
    return BaseEncoding.base16().lowerCase().encode(hash.asBytes());
  }

  /**
   * Gets the names of the source-code files of the verification task if this information is
   * available.
   *
   * @return the names of the source-code files of the verification task.
   */
  public Optional<Iterable<String>> getProgramNames() {
    return programNames;
  }

  /**
   * Gets the SHA-1 hash values of the source-code files of the verification task if they are
   * available.
   *
   * @return the SHA-1 hash values of the source-code files of the verification task.
   * @throws IOException if an {@code IOException} occurs while trying to read the source-code
   *     files.
   */
  public Optional<List<String>> getProgramHashes() throws IOException {
    if (!programNames.isPresent()) {
      return Optional.empty();
    }
    if (programHash == null) {
      ImmutableList.Builder<String> programHashesBuilder = ImmutableList.builder();
      for (String programDenotation : programNames.get()) {
        programHashesBuilder.add(computeHash(Paths.get(programDenotation)));
      }
      programHash = programHashesBuilder.build();
    }
    return Optional.of(programHash);
  }

  public List<String> getInputWitnessHashes() throws IOException {
    if (inputWitnessHashes == null) {
      classifyAutomataFiles();
      ImmutableList.Builder<String> inputWitnessHashesBuilder = ImmutableList.builder();
      for (Path witnessAutomatonFile : witnessAutomatonFiles) {
        inputWitnessHashesBuilder.add(computeHash(witnessAutomatonFile));
      }
      inputWitnessHashes = inputWitnessHashesBuilder.build();
    }
    return inputWitnessHashes;
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
    if (specification.isPresent()) {
      return specification.get().getProperties();
    }
    return Collections.emptySet();
  }

  private final void classifyAutomataFiles() throws IOException {
    if (nonWitnessAutomatonFiles == null) {
      assert witnessAutomatonFiles == null;
      ImmutableList.Builder<Path> nonWitnessAutomatonFilesBuilder = ImmutableList.builder();
      ImmutableList.Builder<Path> witnessAutomatonFilesBuilder = ImmutableList.builder();
      Iterable<Path> specs =
          specification.isPresent()
              ? specification.get().getSpecFiles()
              : hackyOptions.specificationFiles;
      specs = FluentIterable.from(specs).transform(Path::normalize);
      for (Path path : specs) {
        if (AutomatonGraphmlParser.isGraphmlAutomaton(path)) {
          witnessAutomatonFilesBuilder.add(path);
        } else {
          nonWitnessAutomatonFilesBuilder.add(path);
        }
      }
      Optional<Path> inputWitness =
          Optional.ofNullable(hackyOptions.inputWitness).map(Path::normalize);
      if (inputWitness.isPresent() && !Iterables.contains(specs, inputWitness.get())) {
        witnessAutomatonFilesBuilder.add(inputWitness.get());
      }
      Optional<Path> correctnessWitness =
          Optional.ofNullable(hackyOptions.invariantsAutomatonFile).map(Path::normalize);
      if (correctnessWitness.isPresent()
          && !correctnessWitness.equals(inputWitness)
          && !Iterables.contains(specs, correctnessWitness.get())) {
        witnessAutomatonFilesBuilder.add(correctnessWitness.get());
      }
      witnessAutomatonFiles = witnessAutomatonFilesBuilder.build();
      nonWitnessAutomatonFiles = nonWitnessAutomatonFilesBuilder.build();
    } else {
      assert witnessAutomatonFiles != null;
    }
  }
}
