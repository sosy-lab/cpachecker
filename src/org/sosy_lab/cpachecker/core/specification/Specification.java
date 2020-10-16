// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
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
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
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
      return new Specification(pProperties, ImmutableListMultimap.of());
    }

    ImmutableListMultimap<Path, Automaton> specificationAutomata =
        parseSpecificationFiles(specFiles, cfa, config, logger, pShutdownNotifier);

    return new Specification(pProperties, specificationAutomata);
  }

  private static ImmutableListMultimap<Path, Automaton> parseSpecificationFiles(
      Iterable<Path> specFiles,
      CFA cfa,
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, InterruptedException {
    if (Iterables.isEmpty(specFiles)) {
      return ImmutableListMultimap.of();
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

    ImmutableListMultimap.Builder<Path, Automaton> multiplePropertiesBuilder =
        ImmutableListMultimap.builder();

    for (Path specFile : specFiles) {
      List<Automaton> automata =
          parseSpecificationFile(specFile, cfa, config, logger, pShutdownNotifier, scope);
      multiplePropertiesBuilder.putAll(specFile, automata);
    }
    return multiplePropertiesBuilder.build();
  }

  private static List<Automaton> parseSpecificationFile(
      Path specFile,
      CFA cfa,
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      Scope scope)
      throws InvalidConfigurationException, InterruptedException {
    List<Automaton> automata;
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
      automata = ImmutableList.of(graphmlParser.parseAutomatonFile(specFile));

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

      if (automata.isEmpty()) {
        throw new InvalidConfigurationException(
            "Specification file contains no automata: " + specFile);
      }
    }

    for (Automaton automaton : automata) {
      logger.logf(
          Level.FINER,
          "Loaded Automaton %s with %d states from %s.",
          automaton.getName(),
          automaton.getNumberOfStates(),
          specFile);
    }
    return automata;
  }

  /**
   * Return a new specification instance that has everything that the current instance has, and
   * additionally some new specification files.
   */
  public Specification withAdditionalSpecificationFile(
      Set<Path> pSpecificationFiles,
      CFA cfa,
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, InterruptedException {
    Set<Path> newSpecFiles =
        Sets.difference(pSpecificationFiles, pathToSpecificationAutomata.keySet()).immutableCopy();
    if (newSpecFiles.isEmpty()) {
      return this;
    }

    ImmutableListMultimap<Path, Automaton> newSpecificationAutomata =
        parseSpecificationFiles(newSpecFiles, cfa, config, logger, pShutdownNotifier);

    return new Specification(
        properties,
        ImmutableListMultimap.<Path, Automaton>builder()
            .putAll(pathToSpecificationAutomata)
            .putAll(newSpecificationAutomata)
            .build());
  }

  /**
   * Return a new specification instance that has everything that the current instance has, and
   * additionally some new properties.
   */
  public Specification withAdditionalProperties(Set<SpecificationProperty> pProperties) {
    Set<SpecificationProperty> newProperties = Sets.union(properties, pProperties).immutableCopy();
    if (newProperties.size() == properties.size()) {
      return this;
    }

    return new Specification(newProperties, pathToSpecificationAutomata);
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

  /** This method should only be used by {@link CPABuilder} when creating the set of CPAs. */
  public ImmutableList<Automaton> getSpecificationAutomata() {
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
