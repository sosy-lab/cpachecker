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
import org.sosy_lab.cpachecker.appengine.dao.JobDAO;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;


public class GAEPath extends FileSystemPath {

  private JobFile jobFile = null;

  public GAEPath(String path, Job job, String... more) {
    super(path, more);

    jobFile = new JobFile(getPath());
    jobFile.setJob(job);
    jobFile.setContent("");

    // TODO load jobfile from DS
    // TODO if jobFile == null it must be created prior of writing/reading
  }

  @Override
  public ByteSink asByteSink(FileWriteMode... mode) {
    return new DataStoreByteSink(jobFile, mode);
  }

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

  @Override
  public CharSource asCharSource(Charset charset) {
    if (super.exists()) {
      return super.asCharSource(charset);
    }

    // TODO charset
    return new DataStoreCharSource(jobFile);
  }

  /**
   * Will always return true ignoring the success/failure of the delete operation.
   */
  @Override
  public boolean delete() {
    JobDAO.delete(jobFile);
    return true;
  }

  @Override
  public void deleteOnExit() {
    // TODO how??
  }

  @Override
  public boolean mkdirs() {
    return true;
  }
}
