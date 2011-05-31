/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.common.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to mark fields or methods which should get configuration values
 * injected. Such a field or method must be contained in a class annotated with
 * {@link Options}.
 *
 * While it is possible to mark final fields with this annotation, the java
 * documentation says that the behavior of setting final fields will be
 * undetermined. It might happen that some parts of the code do not see the
 * new value.
 *
 * It is possible to mark private fields with this annotation, all access
 * modifiers will be ignored when setting the value.
 *
 * If a method is marked with this annotation, it has to have exactly one
 * parameter. An result value would be ignored, so the method should by of type
 * void. If the method throws an {@link IllegalArgumentException}, the
 * configuration will be rejected as invalid. If the method throws any other
 * exception, the behavior is undefined (so it should not throw any other
 * exception!).
 *
 * If an option is not present in the configuration file, the corresponding field
 * is not touched. Similarly, a corresponding method is not called.
 *
 * It is possible to specify one option more than once, both for fields and
 * methods. All of the fields will be set and all of the methods will be called.
 *
 * @see Configuration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Option {

  /**
   * An optional name for the option as it appears in the configuration file.
   * If not specified, the name of the field will be used.
   * In both cases it will be prefixed with the prefix specified in the {@link Options}
   * annotation (if given).
   */
  public String name() default "";

  /**
   * An optional flag if this option needs to be specified in the configuration
   * file. The default is false. If set to true, an exception will be thrown if
   * the option is not in the file.
   */
  public boolean required() default false;

  /**
   * An optional flag that specifies if a configuration value should be converted
   * to upper case after it was read. For options with enum types, this flag is
   * always assumed to be true.
   */
  public boolean toUppercase() default false;

  /**
   * If regexp is specified, the value of this option (prior to conversion to
   * the correct type) will be checked against this regular expression. If it
   * does not match, an exception will be thrown.
   */
  public String regexp() default "";

  /**
   * If values is non-empty, the value of this option (prior to conversion to
   * the correct type) will be checked if it is listed in this array. If it
   * is not contained, an exception will be thrown.
   *
   */
  public String[] values() default {};
  
  /**
   * An optional minimum value for this option (only works with types int and long).
   */
  public long min() default Long.MIN_VALUE;
  
  /**
   * An optional maximum value for this option (only works with types int and long).
   */
  public long max() default Long.MAX_VALUE;
  
  /**
   * A text that describes the current option (this will be part of the user documentation).
   */
  public String description();
  
  /**
   * If the type of the option needs further specification, this field provides
   * it. See {@link Type} for possible values. The type of the option and the
   * value of this field have to match.
   */
  public Type type() default Type.NOT_APPLICABLE;
  
  public static enum Type { 
    NOT_APPLICABLE,
    REQUIRED_INPUT_FILE,
    OPTIONAL_INPUT_FILE,
    OUTPUT_FILE,
  }
}
