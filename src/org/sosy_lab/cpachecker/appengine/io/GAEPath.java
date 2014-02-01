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
package org.sosy_lab.cpachecker.appengine.io;

import java.nio.charset.Charset;

import org.sosy_lab.common.io.FileSystemPath;
import org.sosy_lab.cpachecker.appengine.dao.JobFileDAO;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;

/**
 * This class extends a {@link FileSystemPath} and makes it work on Google App
 * Engine. GAE does not allow writes to the file system and therefore any method
 * that would do so is adapted to redirect writes to a fitting means.
 */
public class GAEPath extends FileSystemPath {

  private JobFile jobFile = null;

  /**
   * Constructs a new instance that depends on the given job.
   *
   * @see FileSystemPath#FileSystemPath(String, String...)
   * @param job The job this instance depends on
   */
  public GAEPath(String path, Job job, String... more) {
    super(path, more);

    if (isFile()) {
      try {
        if (!super.exists()) {
          jobFile = job.getFile(getPath());
        }
      } catch (Exception e) {
        // if super.exists() fails because it accesses the file system
        jobFile = job.getFile(getPath());
      }
    }

    if (jobFile == null) {
      jobFile = new JobFile(getPath(), job);
    }
  }

  @Override
  public ByteSink asByteSink(FileWriteMode... mode) {
    return new DataStoreByteSink(jobFile, mode);
  }

  /**
   * If the file represented by this instance is available on the file system
   * the returned ByteSource points to the file system.
   */
  @Override
  public ByteSource asByteSource() {
    if (super.exists()) {
      return super.asByteSource();
    }

    return new DataStoreByteSource(jobFile);
  }

  @Override
  public CharSink asCharSink(Charset charset, FileWriteMode... mode) {
    return new DataStoreCharSink(jobFile, charset, mode);
  }

  /**
   * If the file represented by this instance is available on the file system
   * the returned CharSource points to the file system.
   */
  @Override
  public CharSource asCharSource(Charset charset) {
    if (super.exists()) {
      return super.asCharSource(charset);
    }

    return new DataStoreCharSource(jobFile, charset);
  }

  /**
   * Will always return true ignoring the success/failure of the delete operation.
   */
  @Override
  public boolean delete() {
    JobFileDAO.delete(jobFile);
    return true;
  }

  /**
   * Currently does nothing since implementing this method on GAE seems not
   * feasible.
   */
  @Override
  public void deleteOnExit() {
    // TODO how??
  }

  @Override
  public boolean isDirectory() {
    return !isFile();
  }

  /**
   * Returns always true and does nothing.
   */
  @Override
  public boolean mkdirs() {
    return true;
  }

  /**
   * Returns always true and does nothing.
   */
  @Override
  public boolean exists() {
    return true;
  }

  /**
   * Returns always true and does nothing.
   */
  @Override
  public boolean canRead() {
    return true;
  }

  /**
   * Assumes a file always has an extension.
   * For example: foo.bar or foo.bar.baz
   */
  @Override
  public boolean isFile() {
    // assume a file always has an extension
    return getPath().matches(".*\\..*$");
  }
}
