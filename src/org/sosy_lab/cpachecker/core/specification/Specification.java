// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.CFAWithACSLAnnotations;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.PropertyFileParser.InvalidPropertyFileException;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonACSLParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.ltl.Ltl2BuechiConverter;
import org.sosy_lab.cpachecker.util.ltl.LtlParseException;
import org.sosy_lab.cpachecker.util.ltl.LtlParser;
import org.sosy_lab.cpachecker.util.ltl.formulas.LabelledFormula;

/**
 * Class that encapsulates the specification that should be used for an analysis. Most code of
 * CPAchecker should not need to access this file, because a separate CPA handles the specification,
 * though it can be necessary to pass around Specification objects for sub-analyses.
 */
public final class Specification {

  /**
   * Maps well-known properties to automaton from <code>config/specification/*.spc</code>. Use
   * {@link #getAutomatonForProperty(Property)} for lookups.
   */
  private static final ImmutableMap<Property, String> AUTOMATA_FOR_PROPERTIES =
      ImmutableMap.<Property, String>builder()
          .put(CommonVerificationProperty.REACHABILITY_LABEL, "sv-comp-errorlabel")
          .put(CommonVerificationProperty.REACHABILITY, "sv-comp-reachability")
          .put(CommonVerificationProperty.REACHABILITY_ERROR, "sv-comp-reachability")
          .put(CommonVerificationProperty.VALID_FREE, "sv-comp-memorysafety")
          .put(CommonVerificationProperty.VALID_DEREF, "sv-comp-memorysafety")
          .put(CommonVerificationProperty.VALID_MEMTRACK, "sv-comp-memorysafety")
          .put(CommonVerificationProperty.VALID_MEMCLEANUP, "sv-comp-memorycleanup")
          .put(CommonVerificationProperty.OVERFLOW, "sv-comp-overflow")
          .put(CommonVerificationProperty.DEADLOCK, "deadlock")
          .put(CommonVerificationProperty.ASSERT, "JavaAssertion")
          // .put(CommonPropertyType.TERMINATION, "none needed")
          .buildOrThrow();

  private static Path getAutomatonForProperty(Property property) {
    String name = AUTOMATA_FOR_PROPERTIES.get(property);
    if (name == null) {
      return null;
    }

    return Classes.getCodeLocation(Specification.class)
        .resolveSibling("config")
        .resolve("specification")
        .resolve(name + ".spc");
  }

  private final ImmutableSet<Path> specificationFiles;
  private final ImmutableSet<Property> properties;
  private final ImmutableListMultimap<Path, Automaton> pathToSpecificationAutomata;

  public static Specification alwaysSatisfied() {
    return new Specification(ImmutableSet.of(), ImmutableSet.of(), ImmutableListMultimap.of());
  }

  /** Create an instance that just encapsulates the given automata. */
  public static Specification fromAutomata(List<Automaton> automata) {
    ImmutableListMultimap<Path, Automaton> pathToSpecificationAutomata =
        ImmutableListMultimap.<Path, Automaton>builder().putAll(Path.of(""), automata).build();
    return new Specification(ImmutableSet.of(), ImmutableSet.of(), pathToSpecificationAutomata);
  }

  /**
   * Create an instance from a set of files. All kinds of files are supported: property files,
   * regular specification files with automata, and witnesses.
   */
  public static Specification fromFiles(
      Iterable<Path> pSpecFiles,
      CFA cfa,
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, InterruptedException {
    checkNotNull(cfa);
    checkNotNull(config);
    checkNotNull(logger);
    checkNotNull(pShutdownNotifier);

    ImmutableSet<Path> specFiles = ImmutableSet.copyOf(pSpecFiles);
    if (specFiles.isEmpty()) {
      return alwaysSatisfied();
    }

    ImmutableSet.Builder<Property> properties = ImmutableSet.builder();
    ImmutableListMultimap.Builder<Path, Automaton> specificationAutomata =
        ImmutableListMultimap.builder();

    Scope scope;
    switch (cfa.getLanguage()) {
      case C:
        scope = new CProgramScope(cfa, logger);
        break;
      default:
        scope = DummyScope.getInstance();
        break;
    }

    // for deduplicating values returned by getAutomatonForProperty()
    Set<Path> handledAutomataForProperties = new HashSet<>();

    for (Path specFile : specFiles) {
      if (MoreFiles.getFileExtension(specFile).equals("prp")) {
        PropertyFileParser parser = new PropertyFileParser(specFile);
        try {
          parser.parse();
        } catch (InvalidPropertyFileException | IOException e) {
          throw new InvalidConfigurationException(
              String.format("Cannot parse property file %s: %s", specFile, e.getMessage()), e);
        }
        @SuppressWarnings("deprecation") // just a sanity check, not real option usage
        String configuredEntryFunction = config.getProperty("analysis.entryFunction");
        if (!parser.getEntryFunction().equals(configuredEntryFunction)) {
          // Will happen only if "specification=foo.prp" is used in config file or if CPAchecker
          // is used as library instead of from CPAMain.
          throw new InvalidConfigurationException(
              String.format(
                  "Entry function %s specified in %s is not consistent with configured entry"
                      + " function %s. Please set 'analysis.entryFunction=%s' or pass property file"
                      + " on command line with '-spec %s'.",
                  parser.getEntryFunction(),
                  specFile,
                  configuredEntryFunction,
                  parser.getEntryFunction(),
                  specFile));
        }

        for (Property prop : parser.getProperties()) {
          properties.add(prop);

          if (prop instanceof Property.OtherLtlProperty) {
            Automaton automaton =
                parseLtlFormula(prop.toString(), cfa, config, logger, pShutdownNotifier, scope);
            specificationAutomata.put(specFile, automaton);

          } else {
            Path automatonFile = getAutomatonForProperty(prop);

            if (automatonFile != null && handledAutomataForProperties.add(automatonFile)) {
              List<Automaton> automata =
                  parseSpecificationFile(
                      automatonFile, cfa, config, logger, pShutdownNotifier, scope);
              specificationAutomata.putAll(specFile, automata);
            }
          }
        }
      } else {
        List<Automaton> automata =
            parseSpecificationFile(specFile, cfa, config, logger, pShutdownNotifier, scope);
        specificationAutomata.putAll(specFile, automata);
      }
    }

    return new Specification(specFiles, properties.build(), specificationAutomata.build());
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

    } else if (AutomatonACSLParser.isACSLAnnotatedFile(specFile)) {
      logger.logf(Level.INFO, "Parsing CFA with ACSL annotations from file \"%s\"", specFile);
      CFACreator cfaCreator = new CFACreator(config, logger, pShutdownNotifier);
      CFAWithACSLAnnotations annotatedCFA;
      try {
        annotatedCFA =
            (CFAWithACSLAnnotations)
                cfaCreator.parseFileAndCreateCFA(ImmutableList.of(specFile.toString()));
      } catch (ParserException | IOException e) {
        throw new InvalidConfigurationException(
            "Could not load automaton from file: " + e.getMessage(), e);
      }
      AutomatonACSLParser acslParser = new AutomatonACSLParser(annotatedCFA, logger);
      assert acslParser.areIsomorphicCFAs(cfa)
          : "CFAs of task program and annotated program differ, "
              + "annotated program is probably unrelated to this task";
      automata = ImmutableList.of(acslParser.parseAsAutomaton());
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

  private static Automaton parseLtlFormula(
      String ltl,
      CFA cfa,
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      Scope scope)
      throws InvalidConfigurationException, InterruptedException {
    LabelledFormula formula;
    try {
      formula = LtlParser.parseProperty(ltl);
      return Ltl2BuechiConverter.convertFormula(
          formula.not(),
          cfa.getMainFunction().getFunctionName(),
          config,
          logger,
          cfa.getMachineModel(),
          scope,
          pShutdownNotifier);
    } catch (LtlParseException e) {
      throw new InvalidConfigurationException(
          String.format("Could not parse property '%s' (%s)", ltl, e.getMessage()), e);
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          String.format(
              "An exception occured during the execution of the external ltl converter tool:%n%s",
              e.getMessage()),
          e);
    }
  }

  /**
   * Return a new specification instance that has everything that the current instance has, and
   * additionally some new specification files as interpreted by {@link #fromFiles(Iterable, CFA,
   * Configuration, LogManager, ShutdownNotifier)}.
   */
  public Specification withAdditionalSpecificationFile(
      Set<Path> pSpecificationFiles,
      CFA cfa,
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, InterruptedException {
    checkNotNull(cfa);
    checkNotNull(config);
    checkNotNull(logger);
    checkNotNull(pShutdownNotifier);

    Set<Path> newSpecFiles =
        Sets.difference(pSpecificationFiles, pathToSpecificationAutomata.keySet()).immutableCopy();
    if (newSpecFiles.isEmpty()) {
      return this;
    }

    Specification newSpec =
        Specification.fromFiles(newSpecFiles, cfa, config, logger, pShutdownNotifier);

    return new Specification(
        Sets.union(specificationFiles, newSpecFiles),
        Sets.union(properties, newSpec.properties),
        ImmutableListMultimap.<Path, Automaton>builder()
            .putAll(pathToSpecificationAutomata)
            .putAll(newSpec.pathToSpecificationAutomata)
            .build());
  }

  /**
   * Return a new specification instance that has everything that the current instance has, and
   * additionally some new properties. Note that this only affects {@link #getProperties()} and not
   * the actual list of automata.
   */
  public Specification withAdditionalProperties(Set<Property> pProperties) {
    Set<Property> newProperties = Sets.union(properties, pProperties).immutableCopy();
    if (newProperties.size() == properties.size()) {
      return this;
    }

    return new Specification(specificationFiles, newProperties, pathToSpecificationAutomata);
  }

  @VisibleForTesting
  Specification(
      Set<Path> pSpecificationFiles,
      Set<Property> pProperties,
      ImmutableListMultimap<Path, Automaton> pSpecification) {
    specificationFiles = ImmutableSet.copyOf(pSpecificationFiles);
    properties = ImmutableSet.copyOf(pProperties);
    pathToSpecificationAutomata = checkNotNull(pSpecification);
  }

  /** This method should only be used by {@link CPABuilder} when creating the set of CPAs. */
  public ImmutableList<Automaton> getSpecificationAutomata() {
    return pathToSpecificationAutomata.values().asList();
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
        + pathToSpecificationAutomata.values().stream()
            .map(Automaton::getName)
            .collect(joining(", ", "[", "]"));
  }

  /**
   * Gets the set of properties that were read from the property files that were used to create this
   * instance. Note that there can be additional automata read from specification files and
   * witnesses.
   */
  public Set<Property> getProperties() {
    return properties;
  }

  /**
   * Get the set of files from which this instance was constructed. This can be property files,
   * regular specification files, or witnesses.
   */
  public ImmutableSet<Path> getFiles() {
    return specificationFiles;
  }

  public ImmutableListMultimap<Path, Automaton> getPathToSpecificationAutomata() {
    return pathToSpecificationAutomata;
  }
}
