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
package org.sosy_lab.cpachecker.core;

import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.util.PropertyFileParser.SpecificationProperty;

/**
 * Class that encapsulates the specification that should be used for an analysis.
 * Most code of CPAchecker should not need to access this file,
 * because a separate CPA handles the specification,
 * though it can be necessary to pass around Specification objects for sub-analyses.
 */
public final class Specification {

  private final Set<SpecificationProperty> properties;

  private final Set<Path> specFiles;

  private final ImmutableList<Automaton> specificationAutomata;

  public static Specification alwaysSatisfied() {
    return new Specification(ImmutableList.of());
  }

  public static Specification fromAutomata(Iterable<Automaton> automata) {
    return new Specification(automata);
  }

  public static Specification fromFiles(
      Set<SpecificationProperty> pProperties,
      Collection<Path> specFiles,
      CFA cfa,
      Configuration config,
      LogManager logger)
      throws InvalidConfigurationException {
    if (specFiles.isEmpty()) {
      return Specification.alwaysSatisfied();
    }

    Scope scope =
        cfa.getLanguage() == Language.C ? new CProgramScope(cfa, logger) : DummyScope.getInstance();
    List<Automaton> allAutomata = new ArrayList<>();

    for (Path specFile : specFiles) {
      List<Automaton> automata = ImmutableList.of();
      // Check that the automaton file exists and is not empty
      try {
        if (Files.size(specFile) == 0) {
          throw new InvalidConfigurationException("The specification file is empty: " + specFile);
        }
      } catch (IOException e) {
        throw new InvalidConfigurationException(
            "Could not load automaton from file " + e.getMessage(), e);
      }

      if (AutomatonGraphmlParser.isGraphmlAutomatonFromConfiguration(specFile)) {
        AutomatonGraphmlParser graphmlParser =
            new AutomatonGraphmlParser(config, logger, cfa.getMachineModel(), scope);
        automata = graphmlParser.parseAutomatonFile(specFile);

      } else {
        automata =
            AutomatonParser.parseAutomatonFile(
                specFile, config, logger, cfa.getMachineModel(), scope, cfa.getLanguage());
      }

      if (automata.isEmpty()) {
        throw new InvalidConfigurationException(
            "Specification file contains no automata: " + specFile);
      }

      for (Automaton automaton : automata) {
        logger.logf(
            Level.FINER,
            "Loaded Automaton %s with %d states.",
            automaton.getName(),
            automaton.getNumberOfStates());
      }
      allAutomata.addAll(automata);
    }
    return new Specification(pProperties, specFiles, allAutomata);
  }

  private Specification(Iterable<Automaton> pSpecificationAutomata) {
    this(ImmutableSet.of(), ImmutableSet.of(), pSpecificationAutomata);
  }

  private Specification(
      Set<SpecificationProperty> pProperties,
      Collection<Path> pSpecFiles,
      Iterable<Automaton> pSpecificationAutomata) {
    properties = ImmutableSet.copyOf(pProperties);
    specFiles = ImmutableSet.copyOf(pSpecFiles);
    specificationAutomata = ImmutableList.copyOf(pSpecificationAutomata);
  }

  /**
   * This is not public by intention! Only CPABuilder should need to access this method.
   */
  ImmutableList<Automaton> getSpecificationAutomata() {
    return specificationAutomata;
  }

  @Override
  public int hashCode() {
    return specificationAutomata.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Specification)) {
      return false;
    }
    Specification other = (Specification) obj;
    return specificationAutomata.equals(other.specificationAutomata);
  }

  @Override
  public String toString() {
    return "Specification"
        + specificationAutomata.stream().map(Automaton::getName).collect(joining(", ", "[", "]"));
  }

  /**
   * Gets the set of specification properties, which represents a subset of the specification
   * automata.
   *
   * @return the set of specification properties, which represents a subset of the specification
   *     automata.
   */
  public Set<SpecificationProperty> getProperties() {
    return properties;
  }

  /**
   * Gets the set of specification files, which represents a subset of the specification automata.
   *
   * @return the set of specification files, which represents a subset of the specification
   *     automata.
   */
  public Set<Path> getSpecFiles() {
    return specFiles;
  }
}
