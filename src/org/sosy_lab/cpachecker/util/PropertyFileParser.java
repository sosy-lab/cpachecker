/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.cfa.CFACreator;

/**
 * A simple class that reads a property, i.e. basically an entry function and a proposition, from a given property,
 * and maps the proposition to a file from where to read the specification automaton.
 */
public class PropertyFileParser {

  private static final String REACHABILITY_LABEL_SPECIFICATION_FILE =
      "config/specification/sv-comp-errorlabel.spc";
  private static final String REACHABILITY_SPECIFICATION_FILE =
      "config/specification/sv-comp-reachability.spc";
  private static final String MEMORYSAFETY_SPECIFICATION_FILE_DEREF =
      "config/specification/memorysafety-deref.spc";
  private static final String MEMORYSAFETY_SPECIFICATION_FILE_FREE =
      "config/specification/memorysafety-free.spc";
  private static final String MEMORYSAFETY_SPECIFICATION_FILE_MEMTRACK =
      "config/specification/memorysafety-memtrack.spc";
  private static final String OVERFLOW_SPECIFICATION_FILE = "config/specification/overflow.spc";
  private static final String DEADLOCK_SPECIFICATION_FILE = "config/specification/deadlock.spc";

  public static class InvalidPropertyFileException extends Exception {

    private static final long serialVersionUID = -5880923544560903123L;

    public InvalidPropertyFileException(String msg) {
      super(msg);
    }

    public InvalidPropertyFileException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  private final Path propertyFile;

  private String entryFunction;
  private final Set<Property> properties = Sets.newHashSetWithExpectedSize(1);

  private static final Pattern PROPERTY_PATTERN =
      Pattern.compile("CHECK\\( init\\((" + CFACreator.VALID_C_FUNCTION_NAME_PATTERN + ")\\(\\)\\), LTL\\((.+)\\) \\)");

  public PropertyFileParser(final Path pPropertyFile) {
    propertyFile = pPropertyFile;
  }

  public void parse() throws InvalidPropertyFileException {
    String rawProperty = null;
    try (BufferedReader br = Files.newBufferedReader(propertyFile, Charset.defaultCharset())) {
      while ((rawProperty = br.readLine()) != null) {
        if (!rawProperty.isEmpty()) {
          properties.add(parsePropertyLine(rawProperty));
        }
      }
    } catch (IOException e) {
      throw new InvalidPropertyFileException("Could not read file: " + e.getMessage(), e);
    }

    if (properties.isEmpty()) {
      throw new InvalidPropertyFileException("No property in file.");
    }
  }

  private Property parsePropertyLine(String rawProperty) throws InvalidPropertyFileException {
    Matcher matcher = PROPERTY_PATTERN.matcher(rawProperty);

    if (rawProperty == null || !matcher.matches() || matcher.groupCount() != 2) {
      throw new InvalidPropertyFileException(String.format(
          "The given property '%s' is not well-formed!", rawProperty));
    }

    if (entryFunction == null) {
      entryFunction = matcher.group(1);
    } else if (!entryFunction.equals(matcher.group(1))) {
      throw new InvalidPropertyFileException(String.format(
          "Specifying two different entry functions %s and %s is not supported.", entryFunction, matcher.group(1)));
    }

    PropertyType propertyType = PropertyType.AVAILABLE_PROPERTIES.get(matcher.group(2));
    if (propertyType == null) {
      throw new InvalidPropertyFileException(String.format(
          "The property '%s' is not supported.", matcher.group(2)));
    }
    return propertyType.withFunctionEntry(entryFunction);
  }

  public String getEntryFunction() {
    return entryFunction;
  }

  public Set<Property> getProperties() {
    return Collections.unmodifiableSet(properties);
  }

  public interface Property {

    /**
     * Gets the function entry.
     *
     * @return the function entry.
     */
    String getInitialFunction();

    /**
     * Gets the options that are associated with this property by default.
     *
     * @return the options that are associated with this property by default.
     */
    Map<String, String> getAssociatedOptions();

    /**
     * Gets the path to the specification automaton used to represent the property, if it exists.
     *
     * @return the path to the specification automaton used to represent the property, if it exists.
     */
    Optional<String> getInternalSpecificationPath();

  }

  private enum PropertyType {
    REACHABILITY_LABEL {

      @Override
      public Optional<String> getInternalSpecificationPath() {
        return Optional.of(REACHABILITY_LABEL_SPECIFICATION_FILE);
      }

      @Override
      public String toString() {
        return "G ! label(ERROR)";
      }
    },

    REACHABILITY {

      @Override
      public Optional<String> getInternalSpecificationPath() {
        return Optional.of(REACHABILITY_SPECIFICATION_FILE);
      }

      @Override
      public String toString() {
        return "G ! call(__VERIFIER_error())";
      }
    },

    VALID_FREE {

      @Override
      public Map<String, String> getAssociatedOptions() {
        return ImmutableMap.of("memorysafety.check", "true");
      }

      @Override
      public Optional<String> getInternalSpecificationPath() {
        return Optional.of(MEMORYSAFETY_SPECIFICATION_FILE_FREE);
      }

      @Override
      public String toString() {
        return "G valid-free";
      }
    },

    VALID_DEREF {

      @Override
      public Map<String, String> getAssociatedOptions() {
        return ImmutableMap.of("memorysafety.check", "true");
      }

      @Override
      public Optional<String> getInternalSpecificationPath() {
        return Optional.of(MEMORYSAFETY_SPECIFICATION_FILE_DEREF);
      }

      @Override
      public String toString() {
        return "G valid-deref";
      }
    },

    VALID_MEMTRACK {

      @Override
      public Map<String, String> getAssociatedOptions() {
        return ImmutableMap.of("memorysafety.check", "true");
      }

      @Override
      public Optional<String> getInternalSpecificationPath() {
        return Optional.of(MEMORYSAFETY_SPECIFICATION_FILE_MEMTRACK);
      }

      @Override
      public String toString() {
        return "G valid-memtrack";
      }
    },

    OVERFLOW {

      @Override
      public Map<String, String> getAssociatedOptions() {
        return ImmutableMap.of("overflow.check", "true");
      }

      @Override
      public Optional<String> getInternalSpecificationPath() {
        return Optional.of(OVERFLOW_SPECIFICATION_FILE);
      }

      @Override
      public String toString() {
        return "G ! overflow";
      }
    },

    DEADLOCK {

      @Override
      public Optional<String> getInternalSpecificationPath() {
        return Optional.of(DEADLOCK_SPECIFICATION_FILE);
      }

      @Override
      public String toString() {
        return "G ! deadlock";
      }
    },

    TERMINATION {

      @Override
      public Map<String, String> getAssociatedOptions() {
        return ImmutableMap.of("termination.check", "true");
      }

      @Override
      public String toString() {
        return "F end";
      }
    },
    ;

    public Map<String, String> getAssociatedOptions() {
      return Collections.emptyMap();
    }

    public Optional<String> getInternalSpecificationPath() {
      return Optional.empty();
    }

    public Property withFunctionEntry(String pFunctionEntry) {
      return new Property() {

        @Override
        public String getInitialFunction() {
          return pFunctionEntry;
        }

        @Override
        public Map<String, String> getAssociatedOptions() {
          return PropertyType.this.getAssociatedOptions();
        }

        @Override
        public Optional<String> getInternalSpecificationPath() {
          return PropertyType.this.getInternalSpecificationPath();
        }

        @Override
        public String toString() {
          return String.format(
              "CHECK( init(%s()), LTL(%s) )", getInitialFunction(), PropertyType.this.toString());
        }
      };
    }

    private static Map<String, PropertyType> AVAILABLE_PROPERTIES =
        Maps.<String, PropertyType>uniqueIndex(
            EnumSet.allOf(PropertyType.class), PropertyType::toString);
  }
}