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
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.util.Property;
import org.sosy_lab.cpachecker.util.Property.CommonCoverageType;
import org.sosy_lab.cpachecker.util.SpecificationProperty;
import org.sosy_lab.cpachecker.util.ltl.Ltl2BuechiConverter;
import org.sosy_lab.cpachecker.util.ltl.formulas.LabelledFormula;

/**
 * Class that encapsulates the specification that should be used for an analysis.
 * Most code of CPAchecker should not need to access this file,
 * because a separate CPA handles the specification,
 * though it can be necessary to pass around Specification objects for sub-analyses.
 */
public final class Specification {

  private final Set<SpecificationProperty> properties;
  private final ImmutableListMultimap<Path, Automaton> pathToSpecificationAutomata;

  public static Specification alwaysSatisfied() {
    return new Specification(ImmutableList.of());
  }

  public static Specification fromAutomata(Iterable<Automaton> automata) {
    return new Specification(automata);
  }

  public static Specification fromFiles(
      Set<SpecificationProperty> pProperties,
      Iterable<Path> specFiles,
      CFA cfa,
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, InterruptedException {
    if (Iterables.isEmpty(specFiles)) {
      if (pProperties.stream().anyMatch(p -> p.getProperty() instanceof CommonCoverageType)) {
        return new Specification(pProperties, ImmutableListMultimap.of());
      }
      if (pProperties.size() == 1) {
        SpecificationProperty specProp = Iterables.getOnlyElement(pProperties);
        if (specProp.getProperty() instanceof LabelledFormula) {
          try {
            LabelledFormula formula = ((LabelledFormula) specProp.getProperty()).not();
            Automaton automaton =
                Ltl2BuechiConverter.convertFormula(
                    formula,
                    specProp.getEntryFunction(),
                    config,
                    logger,
                    cfa.getMachineModel(),
                    new CProgramScope(cfa, logger),
                    pShutdownNotifier);
            return new Specification(
                pProperties,
                ImmutableListMultimap.of(Paths.get(""), automaton));
          } catch (InterruptedException e) {
            throw new InvalidConfigurationException(
                String.format(
                    "Error when executing the external tool '%s': %s",
                    Ltl2BuechiConverter.getNameOfExecutable(),
                    e.getMessage()),
                e);
          }
        }
      }
      return Specification.alwaysSatisfied();
    }

    Scope scope;
    switch (cfa.getLanguage()) {
      case C:
        scope = new CProgramScope(cfa, logger);
        break;
      default:
        scope = DummyScope.getInstance();
        break;
    }

    Set<Property> properties =
        transformedImmutableSetCopy(pProperties, SpecificationProperty::getProperty);

    ImmutableListMultimap.Builder<Path, Automaton> multiplePropertiesBuilder =
        ImmutableListMultimap.builder();

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
            new AutomatonGraphmlParser(config, logger, pShutdownNotifier, cfa, scope);
        automata = graphmlParser.parseAutomatonFile(specFile, properties);

      } else {
        automata =
            AutomatonParser.parseAutomatonFile(
                specFile,
                config,
                logger,
                cfa.getMachineModel(),
                scope,
                cfa.getLanguage(),
                pShutdownNotifier);
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
      multiplePropertiesBuilder.putAll(specFile, automata);
    }
    return new Specification(pProperties, multiplePropertiesBuilder.build());
  }

  public static Specification combine(final Specification pSpec1, final Specification pSpec2) {
    return new Specification(
        ImmutableSet.<SpecificationProperty>builder()
            .addAll(pSpec1.properties)
            .addAll(pSpec2.properties)
            .build(),
        ImmutableListMultimap.<Path, Automaton>builder()
            .putAll(pSpec1.pathToSpecificationAutomata)
            .putAll(pSpec2.pathToSpecificationAutomata)
            .build());
  }

  private Specification(Iterable<Automaton> pSpecificationAutomata) {
    properties = ImmutableSet.of();
    ImmutableListMultimap.Builder<Path, Automaton> multiplePropertiesBuilder =
        ImmutableListMultimap.builder();
    multiplePropertiesBuilder.putAll(Paths.get(""), ImmutableList.copyOf(pSpecificationAutomata));
    pathToSpecificationAutomata = multiplePropertiesBuilder.build();
  }

  private Specification(
      Set<SpecificationProperty> pProperties,
      ImmutableListMultimap<Path, Automaton> pSpecification) {
    properties = ImmutableSet.copyOf(pProperties);
    pathToSpecificationAutomata = pSpecification;
  }

  /**
   * This is not public by intention! Only CPABuilder should need to access this method.
   */
  ImmutableList<Automaton> getSpecificationAutomata() {
    return ImmutableList.copyOf(pathToSpecificationAutomata.values());
  }

  @Override
  public int hashCode() {
    return pathToSpecificationAutomata.hashCode();
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
    return pathToSpecificationAutomata.equals(other.pathToSpecificationAutomata);
  }

  @Override
  public String toString() {
    return "Specification"
        + pathToSpecificationAutomata
            .values()
            .stream()
            .map(Automaton::getName)
            .collect(joining(", ", "[", "]"));
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
    return pathToSpecificationAutomata.keySet();
  }

  public ImmutableListMultimap<Path, Automaton> getPathToSpecificationAutomata() {
    return pathToSpecificationAutomata;
  }
}
