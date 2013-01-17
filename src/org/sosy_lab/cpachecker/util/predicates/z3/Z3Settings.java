/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.z3;

import java.io.File;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import com.google.common.base.Splitter;
import com.google.common.base.Splitter.MapSplitter;
import com.google.common.collect.ImmutableMap;

@Options(prefix="cpa.predicate.z3")
public class Z3Settings {
  @Option(description = "Use UIFs (recommended because its more precise)")
  public boolean useUIFs = true;

  @Option(description = "List of further options which will be passed to Z3. "
      + "Format is 'key1=value1,key2=value2'")
  public String furtherOptions = "";

  @Option(description = "Export solver queries in Smtlib format into a file (for Z3).")
  public boolean logAllQueries = false;
  @Option(description = "Export solver queries in Smtlib format into a file (for Z3).")
  @FileOption(Type.OUTPUT_FILE)
  public File logfile = new File("mathsat5.%d.smt2");
  public ImmutableMap<String,String> furtherOptionsMap ;

  public Z3Settings(Configuration config) throws InvalidConfigurationException{
    config.inject(this);

    MapSplitter optionSplitter = Splitter.on(',').trimResults().omitEmptyStrings()
                    .withKeyValueSeparator(Splitter.on('=').limit(2).trimResults());

    try {
      furtherOptionsMap = ImmutableMap.copyOf(optionSplitter.split(furtherOptions));
    } catch (IllegalArgumentException e) {
      throw new InvalidConfigurationException("Invalid Z3 option in \"" + furtherOptions + "\": " + e.getMessage(), e);
    }
  }

}
