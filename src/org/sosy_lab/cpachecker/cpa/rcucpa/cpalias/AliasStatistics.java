/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.rcucpa.cpalias;

import com.google.common.collect.FluentIterable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import static com.google.common.collect.FluentIterable.from;

@Options(prefix = "cpa.rcucpa")
public class AliasStatistics implements Statistics {

  @Option(name = "precisionFile", secure = true, description = "name of a file containing "
      + "information on which pointers are RCU pointers")
  @FileOption(Type.OUTPUT_FILE)
  private Path path = Paths.get("rcuPointers");

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    List<AliasPrecision> precisions = from(reached.getPrecisions()).filter(AliasPrecision.class)
        .toList();
    Set<AbstractIdentifier> rcuPointers = new HashSet<>();

    for (AliasPrecision p : precisions) {
      rcuPointers.addAll(p.getRcuPtrs());
    }

    exportAsPrecision(rcuPointers);
    out.append("RCU Pointers found: " + rcuPointers.size());
    out.append("RCU pointers saved to: " + path.toString());
  }

  private void exportAsPrecision(Set<AbstractIdentifier> rcuPointers) {
    try {
      Writer w = MoreFiles.openOutputFile(path, Charset.defaultCharset());

    } catch (IOException e) {

    }
  }

  @Nullable
  @Override
  public String getName() {
    return "CPAlias statistics";
  }
}
