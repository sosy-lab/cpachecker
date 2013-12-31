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
package org.sosy_lab.cpachecker.appengine.common;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.sosy_lab.common.io.Path;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;


public class GAEPath implements Path {


  public GAEPath(String path, String... more) {
    // TODO Auto-generated constructor stub
  }

  @Override
  public ByteSink asByteSink(FileWriteMode... pArg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ByteSource asByteSource() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CharSink asCharSink(Charset pArg0, FileWriteMode... pArg1) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CharSource asCharSource(Charset pArg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean canRead() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean delete() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void deleteOnExit() {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean exists() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getAbsolutePath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getCanonicalPath() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getOriginalPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Path getParent() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isAbsolute() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isDirectory() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isEmpty() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isFile() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String[] list() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean mkdirs() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Path resolve(String pArg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Path resolve(Path pArg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Path toAbsolutePath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public File toFile() {
    // TODO Auto-generated method stub
    return null;
  }

}
