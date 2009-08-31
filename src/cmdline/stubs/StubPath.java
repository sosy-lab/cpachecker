/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cmdline.stubs;

import java.io.File;

import org.eclipse.core.runtime.IPath;

import cmdline.stubs.StubPath;


public class StubPath implements IPath {

	private String pth;

	public StubPath(String p) {
		pth = p;
	}

	@Override
	public Object clone() {
		return new StubPath(pth);
	}


	public IPath addFileExtension(String arg0) {

		return null;
	}


	public IPath addTrailingSeparator() {

		return null;
	}


	public IPath append(String arg0) {

		return null;
	}


	public IPath append(IPath arg0) {

		return null;
	}


	public String getDevice() {

		return null;
	}


	public String getFileExtension() {

		return null;
	}


	public boolean hasTrailingSeparator() {

		return false;
	}


	public boolean isAbsolute() {

		return false;
	}


	public boolean isEmpty() {

		return false;
	}


	public boolean isPrefixOf(IPath arg0) {

		return false;
	}


	public boolean isRoot() {

		return false;
	}


	public boolean isUNC() {

		return false;
	}


	public boolean isValidPath(String arg0) {

		return false;
	}


	public boolean isValidSegment(String arg0) {

		return false;
	}


	public String lastSegment() {

		return null;
	}


	public IPath makeAbsolute() {

		return null;
	}


	public IPath makeRelative() {

		return null;
	}


	public IPath makeUNC(boolean arg0) {

		return null;
	}


	public int matchingFirstSegments(IPath arg0) {

		return 0;
	}


	public IPath removeFileExtension() {

		return null;
	}


	public IPath removeFirstSegments(int arg0) {

		return null;
	}


	public IPath removeLastSegments(int arg0) {

		return null;
	}


	public IPath removeTrailingSeparator() {

		return null;
	}


	public String segment(int arg0) {

		return null;
	}


	public int segmentCount() {

		return 0;
	}


	public String[] segments() {

		return null;
	}


	public IPath setDevice(String arg0) {

		return null;
	}


	public File toFile() {

		return null;
	}


	public String toOSString() {
		// TODO must implement
		return pth;
	}


	public String toPortableString() {

		return null;
	}


	public IPath uptoSegment(int arg0) {

		return null;
	}

  public IPath makeRelativeTo(IPath pArg0) {
    // added for CDT 6.0 compatibility
    return null;
  }

}
