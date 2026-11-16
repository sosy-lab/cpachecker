// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.specification.Property.CommonCoverageProperty;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Property.CoverFunctionCallProperty;
import org.sosy_lab.cpachecker.core.specification.Property.OtherLtlProperty;

/**
 * A simple class that reads a property, i.e. basically an entry function and a proposition, from a
 * given property, and maps the proposition to a file from where to read the specification
 * automaton.
 */
public class PropertyFileParser {

  public static class InvalidPropertyFileException extends Exception {

    @Serial private static final long serialVersionUID = -5880923544560903123L;

    public InvalidPropertyFileException(String msg) {
      super(msg);
    }

    public InvalidPropertyFileException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  private final CharSource propertyFile;

  private @Nullable String entryFunction;
  private @Nullable ImmutableSet<Property> properties;

  private static final Pattern PROPERTY_PATTERN =
      Pattern.compile(
          "CHECK\\( init\\(("
              + CFACreator.VALID_C_FUNCTION_NAME_PATTERN
              + "|"
              + CFACreator.VALID_JAVA_FUNCTION_NAME_PATTERN
              + ")\\(\\)\\), LTL\\((.+)\\) \\)");

  private static final Pattern SV_LIB_PROPERTY_PATTERN =
      Pattern.compile("CHECK\\(annotations, (.+)\\)");

  private static final Pattern COVERAGE_PATTERN =
      Pattern.compile(
          "COVER\\( init\\(("
              + CFACreator.VALID_C_FUNCTION_NAME_PATTERN
              + "|"
              + CFACreator.VALID_JAVA_FUNCTION_NAME_PATTERN
              + ")\\(\\)\\), FQL\\((.+)\\) \\)");

  private static final ImmutableMap<String, ? extends Property> AVAILABLE_VERIFICATION_PROPERTIES =
      Maps.uniqueIndex(EnumSet.allOf(CommonVerificationProperty.class), Property::toString);

  private static final ImmutableMap<String, ? extends Property> AVAILABLE_COVERAGE_PROPERTIES =
      Maps.uniqueIndex(EnumSet.allOf(CommonCoverageProperty.class), Property::toString);

  public PropertyFileParser(final CharSource pPropertyFile) {
    propertyFile = checkNotNull(pPropertyFile);
  }

  public PropertyFileParser(final Path pPropertyFile) {
    propertyFile = MoreFiles.asCharSource(pPropertyFile, Charset.defaultCharset());
  }

  public void parse() throws InvalidPropertyFileException, IOException {
    checkState(properties == null, "single-use only");
    ImmutableSet.Builder<Property> propertiesBuilder = ImmutableSet.builder();
    String rawProperty = null;

    try (BufferedReader br = propertyFile.openBufferedStream()) {
      while ((rawProperty = br.readLine()) != null) {
        if (rawProperty.startsWith("#")) {
          continue;
        }
        if (!rawProperty.isEmpty()) {
          propertiesBuilder.add(parsePropertyLine(rawProperty));
        }
      }
    }
    properties = propertiesBuilder.build();
  }

  private Property parsePropertyLine(String rawProperty) throws InvalidPropertyFileException {
    Matcher propertyMatcher = PROPERTY_PATTERN.matcher(rawProperty);
    Matcher coverageMatcher = COVERAGE_PATTERN.matcher(rawProperty);
    Matcher svLibPropertyMatcher = SV_LIB_PROPERTY_PATTERN.matcher(rawProperty);

    if (rawProperty == null) {
      throw new InvalidPropertyFileException("The property is not well-formed!");
    }

    if (propertyMatcher.matches() && propertyMatcher.groupCount() == 2) {
      // handle the case that the property is a verification property
      setEntryFunction(propertyMatcher.group(1));

      String rawLtlProperty = propertyMatcher.group(2);
      return Objects.requireNonNullElseGet(
          AVAILABLE_VERIFICATION_PROPERTIES.get(rawLtlProperty),
          () -> new OtherLtlProperty(rawLtlProperty));
    } else if (coverageMatcher.matches() && coverageMatcher.groupCount() == 2) {
      // Handle the case that the property is a coverage pattern
      setEntryFunction(coverageMatcher.group(1));

      return Objects.requireNonNullElseGet(
          AVAILABLE_COVERAGE_PROPERTIES.get(coverageMatcher.group(2)),
          () -> CoverFunctionCallProperty.getProperty(rawProperty));
    } else if (svLibPropertyMatcher.matches() && svLibPropertyMatcher.groupCount() == 1) {
      // Handle the case that it is an SV-LIB property
      //
      // No setting of the entry function is needed, since SV-LIB follows
      // the `verify-call` commands as defined in the standard
      String tagsToCheck = svLibPropertyMatcher.group(1);
      if (tagsToCheck.equals("all")) {
        return CommonVerificationProperty.CORRECT_ANNOTATIONS;
      } else {
        throw new InvalidPropertyFileException(
            String.format(
                "Only checking all annotations is currently supported "
                    + "through the keyword 'all', but '%s' was specified.",
                tagsToCheck));
      }
    }

    throw new InvalidPropertyFileException(
        String.format("The property '%s' is not well-formed!", rawProperty));
  }

  private void setEntryFunction(String pEntryFunction) throws InvalidPropertyFileException {
    if (entryFunction == null) {
      entryFunction = pEntryFunction;
    } else if (!entryFunction.equals(pEntryFunction)) {
      throw new InvalidPropertyFileException(
          String.format(
              "Specifying two different entry functions %s and %s is not supported.",
              entryFunction, pEntryFunction));
    }
  }

  public Optional<String> getEntryFunction() {
    return Optional.ofNullable(entryFunction);
  }

  public ImmutableSet<Property> getProperties() {
    checkState(properties != null);
    return properties;
  }
}
