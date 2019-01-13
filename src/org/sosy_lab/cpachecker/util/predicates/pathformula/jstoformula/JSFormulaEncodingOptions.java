/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

/**
 * This class collects some configurations options that are specific for the JS-to-formula encoding
 * process.
 */
@Options(prefix = "cpa.predicate.js")
public class JSFormulaEncodingOptions {
  // TODO this option should be removed as soon as NaN and float interpolation can be used together
  @Option(secure = true, description = "Do not check for NaN in operations")
  boolean useNaN = true;

  /**
   * The maximum length of an object prototype chain that is assumed. If a field is not set on an
   * object, the field is looked up in the prototype of the object. If the field is not set on that
   * prototype, the field is looked up in the prototype of the prototype and so on till a prototype
   * is null. This prototype chains in JavaScript programs might be arbitrary long but it is always
   * finite. The analysis only supports a maximum length of prototype chain. It is assumed that no
   * prototype chain is longer as this maximum.
   */
  @Option(
      secure = true,
      description = "The maximum length of an object prototype chain that is assumed")
  int maxPrototypeChainLength = 5;

  /**
   * Count of string constants ist restricted to a limit to avoid quantifier in object encoding.
   * Note that some string constants (like the empty sting or field names) are implicitly present in
   * all programs and are counted, too. Each string constant is mapped to an integer (field-ID).
   * Object fields are encoded as an array that maps field-ID to field variable. A special field
   * variable marks a field as unset. Since the Object fields array maps all fields that might exist
   * in the program, all field-IDs have to be known to avoid a "for all" quantifier.
   */
  @Option(
      secure = true,
      description = "Maximum count of different constants used as string or field name")
  int maxFieldNameCount = 3;

  public JSFormulaEncodingOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this, JSFormulaEncodingOptions.class);
  }
}
