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
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.common.io.AbstractPathFactory;
import org.sosy_lab.common.io.FileSystemPath;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.appengine.entity.Task;

/**
 * This class is {@link AbstractPathFactory} that is specifically tailored to
 * work on Google App Engine. The {@link Path} instances it creates depend on a
 * {@link Task} instance.
 */
public class GAEPathFactory implements AbstractPathFactory {

  private Task task;
  private Map<Thread, Task> threadTaskMap;

  /**
   * Creates an instance that depends on the given {@link Task} when it creates
   * {@link Path} instances.
   *
   * @param task The {@link Task} to use when creating {@link Path} instances
   */
  public GAEPathFactory(Task task) {
    this.task = checkNotNull(task);
  }

  /**
   * Creates an instance that uses the given {@link Map} to retrieve a {@link Task}
   * instance when a {@link Path} instance is created.
   *
   * @param map A {@link Map} to retrieve the dependent {@link Task} from.
   */
  public GAEPathFactory(Map<Thread, Task> map) {
    threadTaskMap = checkNotNull(map);
  }

  /**
   * Usually returns a {@link GAEPath} that depends on a {@link Task} set via one of
   * the constructors. If no {@link Task} can be resolved a
   * {@link FileSystemPath} instance will be returned instead.
   */
  @Override
  public Path getPath(@Nullable String path, @Nullable String... more) {
    Task taskForPath = task;
    if (taskForPath == null) {
      taskForPath = threadTaskMap.get(Thread.currentThread());
    }

    if (taskForPath == null) {
      return new FileSystemPath(path, more);
    } else {
      return new GAEPath(path, taskForPath, more);
    }
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
