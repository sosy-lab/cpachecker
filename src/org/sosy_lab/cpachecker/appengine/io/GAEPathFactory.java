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
package org.sosy_lab.cpachecker.appengine.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import javax.annotation.Nullable;

import org.sosy_lab.common.io.AbstractPathFactory;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.appengine.entity.Job;


public class GAEPathFactory implements AbstractPathFactory {

  private Job job;

  public GAEPathFactory(Job job) {
    this.job = job;
  }

  @Override
  public Path getPath(@Nullable String path, @Nullable String... more) {
    return new GAEPath(path, job, more);
  }

  @Override
  public Path getTempPath(String prefix, @Nullable String suffix) throws IOException {
    checkNotNull(prefix);
    if (prefix.length() < 3) {
      throw new IllegalArgumentException("The prefix must at least be three characters long.");
    }

    if (suffix == null) {
      suffix = ".tmp";
    }

    String filename = prefix + suffix;

    return getPath(filename);
  }

}
