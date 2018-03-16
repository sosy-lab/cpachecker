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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

@SuppressWarnings("deprecation")
class DummyProject implements IProject {

  @Override
  public void build(
      final int pI,
      final String pS,
      final Map<String, String> pMap,
      final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void build(final int pI, final IProgressMonitor pIProgressMonitor) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void build(
      final IBuildConfiguration pIBuildConfiguration,
      final int pI,
      final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void clearCachedDynamicReferences() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void close(final IProgressMonitor pIProgressMonitor) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void create(
      final IProjectDescription pIProjectDescription, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void create(final IProgressMonitor pIProgressMonitor) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void create(
      final IProjectDescription pIProjectDescription,
      final int pI,
      final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void delete(final boolean pB, final boolean pB1, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IBuildConfiguration getActiveBuildConfig() throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IBuildConfiguration getBuildConfig(final String pS) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IBuildConfiguration[] getBuildConfigs() throws CoreException {
    return new IBuildConfiguration[0];
  }

  @Override
  public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IProjectDescription getDescription() throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IFile getFile(final String pS) {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IFolder getFolder(final String pS) {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IProjectNature getNature(final String pS) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IPath getPluginWorkingLocation(
      final org.eclipse.core.runtime.IPluginDescriptor pIPluginDescriptor) {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IPath getWorkingLocation(final String pS) {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IProject[] getReferencedProjects() throws CoreException {
    return new IProject[0];
  }

  @Override
  public IProject[] getReferencingProjects() {
    return new IProject[0];
  }

  @Override
  public IBuildConfiguration[] getReferencedBuildConfigs(final String pS, final boolean pB)
      throws CoreException {
    return new IBuildConfiguration[0];
  }

  @Override
  public boolean hasBuildConfig(final String pS) throws CoreException {
    return false;
  }

  @Override
  public boolean hasNature(final String pS) throws CoreException {
    return false;
  }

  @Override
  public boolean isNatureEnabled(final String pS) throws CoreException {
    return false;
  }

  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public void loadSnapshot(final int pI, final URI pURI, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void move(
      final IProjectDescription pIProjectDescription,
      final boolean pB,
      final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void open(final int pI, final IProgressMonitor pIProgressMonitor) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void open(final IProgressMonitor pIProgressMonitor) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void saveSnapshot(final int pI, final URI pURI, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setDescription(
      final IProjectDescription pIProjectDescription, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setDescription(
      final IProjectDescription pIProjectDescription,
      final int pI,
      final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public boolean exists(final IPath pIPath) {
    return false;
  }

  @Override
  public IResource findMember(final String pS) {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IResource findMember(final String pS, final boolean pB) {
    return null;
  }

  @Override
  public IResource findMember(final IPath pIPath) {
    return null;
  }

  @Override
  public IResource findMember(final IPath pIPath, final boolean pB) {
    return null;
  }

  @Override
  public String getDefaultCharset() throws CoreException {
    return null;
  }

  @Override
  public String getDefaultCharset(final boolean pB) throws CoreException {
    return null;
  }

  @Override
  public IFile getFile(final IPath pIPath) {
    return null;
  }

  @Override
  public IFolder getFolder(final IPath pIPath) {
    return null;
  }

  @Override
  public IResource[] members() throws CoreException {
    return new IResource[0];
  }

  @Override
  public IResource[] members(final boolean pB) throws CoreException {
    return new IResource[0];
  }

  @Override
  public IResource[] members(final int pI) throws CoreException {
    return new IResource[0];
  }

  @Override
  public IFile[] findDeletedMembersWithHistory(
      final int pI, final IProgressMonitor pIProgressMonitor) throws CoreException {
    return new IFile[0];
  }

  @Override
  public void setDefaultCharset(final String pS) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setDefaultCharset(final String pS, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IResourceFilterDescription createFilter(
      final int pI,
      final FileInfoMatcherDescription pFileInfoMatcherDescription,
      final int pI1,
      final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IResourceFilterDescription[] getFilters() throws CoreException {
    return new IResourceFilterDescription[0];
  }

  @Override
  public void accept(final IResourceProxyVisitor pIResourceProxyVisitor, final int pI)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void accept(
      final IResourceProxyVisitor pIResourceProxyVisitor, final int pI, final int pI1)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void accept(final IResourceVisitor pIResourceVisitor) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void accept(final IResourceVisitor pIResourceVisitor, final int pI, final boolean pB)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void accept(final IResourceVisitor pIResourceVisitor, final int pI, final int pI1)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void clearHistory(final IProgressMonitor pIProgressMonitor) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void copy(final IPath pIPath, final boolean pB, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void copy(final IPath pIPath, final int pI, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void copy(
      final IProjectDescription pIProjectDescription,
      final boolean pB,
      final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void copy(
      final IProjectDescription pIProjectDescription,
      final int pI,
      final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IMarker createMarker(final String pS) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IResourceProxy createProxy() {
    return null;
  }

  @Override
  public void delete(final boolean pB, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void delete(final int pI, final IProgressMonitor pIProgressMonitor) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void deleteMarkers(final String pS, final boolean pB, final int pI) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public IMarker findMarker(final long pL) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IMarker[] findMarkers(final String pS, final boolean pB, final int pI)
      throws CoreException {
    return new IMarker[0];
  }

  @Override
  public int findMaxProblemSeverity(final String pS, final boolean pB, final int pI)
      throws CoreException {
    return 0;
  }

  @Override
  public String getFileExtension() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IPath getFullPath() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public long getLocalTimeStamp() {
    return 0;
  }

  @Override
  public IPath getLocation() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public URI getLocationURI() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IMarker getMarker(final long pL) {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public long getModificationStamp() {
    return 0;
  }

  @Override
  public String getName() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IPathVariableManager getPathVariableManager() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IContainer getParent() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public String getPersistentProperty(final QualifiedName pQualifiedName) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IProject getProject() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IPath getProjectRelativePath() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public IPath getRawLocation() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public URI getRawLocationURI() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public ResourceAttributes getResourceAttributes() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public Object getSessionProperty(final QualifiedName pQualifiedName) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public int getType() {
    return 0;
  }

  @Override
  public IWorkspace getWorkspace() {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public boolean isAccessible() {
    return false;
  }

  @Override
  public boolean isDerived() {
    return false;
  }

  @Override
  public boolean isDerived(final int pI) {
    return false;
  }

  @Override
  public boolean isHidden() {
    return false;
  }

  @Override
  public boolean isHidden(final int pI) {
    return false;
  }

  @Override
  public boolean isLinked() {
    return false;
  }

  @Override
  public boolean isVirtual() {
    return false;
  }

  @Override
  public boolean isLinked(final int pI) {
    return false;
  }

  @Override
  public boolean isLocal(final int pI) {
    return false;
  }

  @Override
  public boolean isPhantom() {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public boolean isSynchronized(final int pI) {
    return false;
  }

  @Override
  public boolean isTeamPrivateMember() {
    return false;
  }

  @Override
  public boolean isTeamPrivateMember(final int pI) {
    return false;
  }

  @Override
  public void move(final IPath pIPath, final boolean pB, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void move(final IPath pIPath, final int pI, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void move(
      final IProjectDescription pIProjectDescription,
      final boolean pB,
      final boolean pB1,
      final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void move(
      final IProjectDescription pIProjectDescription,
      final int pI,
      final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void refreshLocal(final int pI, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void revertModificationStamp(final long pL) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setDerived(final boolean pB) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setDerived(final boolean pB, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setHidden(final boolean pB) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setLocal(final boolean pB, final int pI, final IProgressMonitor pIProgressMonitor)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public long setLocalTimeStamp(final long pL) throws CoreException {
    return 0;
  }

  @Override
  public void setPersistentProperty(final QualifiedName pQualifiedName, final String pS)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setReadOnly(final boolean pB) {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setResourceAttributes(final ResourceAttributes pResourceAttributes)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setSessionProperty(final QualifiedName pQualifiedName, final Object pO)
      throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void setTeamPrivateMember(final boolean pB) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public void touch(final IProgressMonitor pIProgressMonitor) throws CoreException {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public <T> T getAdapter(final Class<T> pClass) {
    throw new RuntimeException("Dummy not implemented");
  }

  @Override
  public boolean contains(final ISchedulingRule pISchedulingRule) {
    return false;
  }

  @Override
  public boolean isConflicting(final ISchedulingRule pISchedulingRule) {
    return false;
  }
}
