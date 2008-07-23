package cpaplugin.cmdline.stubs;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.ISchedulingRule;


public class StubProject implements IProject {

	@Override
	public void build(int arg0, IProgressMonitor arg1) throws CoreException {
		

	}

	@Override
	public void build(int arg0, String arg1, Map arg2, IProgressMonitor arg3)
			throws CoreException {
		

	}

	@Override
	public void close(IProgressMonitor arg0) throws CoreException {
		

	}

	@Override
	public void create(IProgressMonitor arg0) throws CoreException {
		

	}

	@Override
	public void create(IProjectDescription arg0, IProgressMonitor arg1)
			throws CoreException {
		

	}

	@Override
	public void create(IProjectDescription arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	@Override
	public void delete(boolean arg0, boolean arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	@Override
	public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
	    // TODO must implement
	    throw new CoreException(null);
	}

	@Override
	public IProjectDescription getDescription() throws CoreException {
		
		return null;
	}

	@Override
	public IFile getFile(String arg0) {
		
		return null;
	}

	@Override
	public IFolder getFolder(String arg0) {
		
		return null;
	}

	@Override
	public IProjectNature getNature(String arg0) throws CoreException {
		
		return null;
	}

	@Override
	public IPath getPluginWorkingLocation(IPluginDescriptor arg0) {
		
		return null;
	}

	@Override
	public IProject[] getReferencedProjects() throws CoreException {
		
		return null;
	}

	@Override
	public IProject[] getReferencingProjects() {
		
		return null;
	}

	@Override
	public IPath getWorkingLocation(String arg0) {
		
		return null;
	}

	@Override
	public boolean hasNature(String arg0) throws CoreException {
		
		return false;
	}

	@Override
	public boolean isNatureEnabled(String arg0) throws CoreException {
		
		return false;
	}

	@Override
	public boolean isOpen() {
		
		return false;
	}

	@Override
	public void move(IProjectDescription arg0, boolean arg1,
			IProgressMonitor arg2) throws CoreException {
		

	}

	@Override
	public void open(IProgressMonitor arg0) throws CoreException {
		

	}

	@Override
	public void open(int arg0, IProgressMonitor arg1) throws CoreException {
		

	}

	@Override
	public void setDescription(IProjectDescription arg0, IProgressMonitor arg1)
			throws CoreException {
		

	}

	@Override
	public void setDescription(IProjectDescription arg0, int arg1,
			IProgressMonitor arg2) throws CoreException {
		

	}

	@Override
	public boolean exists(IPath arg0) {
		
		return false;
	}

	@Override
	public IFile[] findDeletedMembersWithHistory(int arg0, IProgressMonitor arg1)
			throws CoreException {
		
		return null;
	}

	@Override
	public IResource findMember(String arg0) {
		
		return null;
	}

	@Override
	public IResource findMember(IPath arg0) {
		
		return null;
	}

	@Override
	public IResource findMember(String arg0, boolean arg1) {
		
		return null;
	}

	@Override
	public IResource findMember(IPath arg0, boolean arg1) {
		
		return null;
	}

	@Override
	public String getDefaultCharset() throws CoreException {
		
		return null;
	}

	@Override
	public String getDefaultCharset(boolean arg0) throws CoreException {
		
		return null;
	}

	@Override
	public IFile getFile(IPath arg0) {
		
		return null;
	}

	@Override
	public IFolder getFolder(IPath arg0) {
		
		return null;
	}

	@Override
	public IResource[] members() throws CoreException {
		
		return null;
	}

	@Override
	public IResource[] members(boolean arg0) throws CoreException {
		
		return null;
	}

	@Override
	public IResource[] members(int arg0) throws CoreException {
		
		return null;
	}

	@Override
	public void setDefaultCharset(String arg0) throws CoreException {
		

	}

	@Override
	public void setDefaultCharset(String arg0, IProgressMonitor arg1)
			throws CoreException {
		

	}

	@Override
	public void accept(IResourceVisitor arg0) throws CoreException {
		

	}

	@Override
	public void accept(IResourceProxyVisitor arg0, int arg1)
			throws CoreException {
		

	}

	@Override
	public void accept(IResourceVisitor arg0, int arg1, boolean arg2)
			throws CoreException {
		

	}

	@Override
	public void accept(IResourceVisitor arg0, int arg1, int arg2)
			throws CoreException {
		

	}

	@Override
	public void clearHistory(IProgressMonitor arg0) throws CoreException {
		

	}

	@Override
	public void copy(IPath arg0, boolean arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	@Override
	public void copy(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	@Override
	public void copy(IProjectDescription arg0, boolean arg1,
			IProgressMonitor arg2) throws CoreException {
		

	}

	@Override
	public void copy(IProjectDescription arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	@Override
	public IMarker createMarker(String arg0) throws CoreException {
		
		return null;
	}

	@Override
	public IResourceProxy createProxy() {
		
		return null;
	}

	@Override
	public void delete(boolean arg0, IProgressMonitor arg1)
			throws CoreException {
		

	}

	@Override
	public void delete(int arg0, IProgressMonitor arg1) throws CoreException {
		

	}

	@Override
	public void deleteMarkers(String arg0, boolean arg1, int arg2)
			throws CoreException {
		

	}

	@Override
	public boolean exists() {
		
		return false;
	}

	@Override
	public IMarker findMarker(long arg0) throws CoreException {
		
		return null;
	}

	@Override
	public IMarker[] findMarkers(String arg0, boolean arg1, int arg2)
			throws CoreException {
		
		return null;
	}

	@Override
	public int findMaxProblemSeverity(String arg0, boolean arg1, int arg2)
			throws CoreException {
		
		return 0;
	}

	@Override
	public String getFileExtension() {
		
		return null;
	}

	@Override
	public IPath getFullPath() {
		
		return null;
	}

	@Override
	public long getLocalTimeStamp() {
		
		return 0;
	}

	@Override
	public IPath getLocation() {
		
		return null;
	}

	@Override
	public URI getLocationURI() {
		
		return null;
	}

	@Override
	public IMarker getMarker(long arg0) {
		
		return null;
	}

	@Override
	public long getModificationStamp() {
		
		return 0;
	}

	@Override
	public String getName() {
		
		return null;
	}

	@Override
	public IContainer getParent() {
		
		return null;
	}

	@Override
	public Map getPersistentProperties() throws CoreException {
		
		return null;
	}

	@Override
	public String getPersistentProperty(QualifiedName arg0)
			throws CoreException {
		
		return null;
	}

	@Override
	public IProject getProject() {
		
		return null;
	}

	@Override
	public IPath getProjectRelativePath() {
		
		return null;
	}

	@Override
	public IPath getRawLocation() {
		
		return null;
	}

	@Override
	public URI getRawLocationURI() {
		
		return null;
	}

	@Override
	public ResourceAttributes getResourceAttributes() {
		
		return null;
	}

	@Override
	public Map getSessionProperties() throws CoreException {
		
		return null;
	}

	@Override
	public Object getSessionProperty(QualifiedName arg0) throws CoreException {
		
		return null;
	}

	@Override
	public int getType() {
		
		return 0;
	}

	@Override
	public IWorkspace getWorkspace() {
		
		return null;
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
	public boolean isDerived(int arg0) {
		
		return false;
	}

	@Override
	public boolean isHidden() {
		
		return false;
	}

	@Override
	public boolean isLinked() {
		
		return false;
	}

	@Override
	public boolean isLinked(int arg0) {
		
		return false;
	}

	@Override
	public boolean isLocal(int arg0) {
		
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
	public boolean isSynchronized(int arg0) {
		
		return false;
	}

	@Override
	public boolean isTeamPrivateMember() {
		
		return false;
	}

	@Override
	public void move(IPath arg0, boolean arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	@Override
	public void move(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	@Override
	public void move(IProjectDescription arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	@Override
	public void move(IProjectDescription arg0, boolean arg1, boolean arg2,
			IProgressMonitor arg3) throws CoreException {
		

	}

	@Override
	public void refreshLocal(int arg0, IProgressMonitor arg1)
			throws CoreException {
		

	}

	@Override
	public void revertModificationStamp(long arg0) throws CoreException {
		

	}

	@Override
	public void setDerived(boolean arg0) throws CoreException {
		

	}

	@Override
	public void setHidden(boolean arg0) throws CoreException {
		

	}

	@Override
	public void setLocal(boolean arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	@Override
	public long setLocalTimeStamp(long arg0) throws CoreException {
		
		return 0;
	}

	@Override
	public void setPersistentProperty(QualifiedName arg0, String arg1)
			throws CoreException {
		

	}

	@Override
	public void setReadOnly(boolean arg0) {
		

	}

	@Override
	public void setResourceAttributes(ResourceAttributes arg0)
			throws CoreException {
		

	}

	@Override
	public void setSessionProperty(QualifiedName arg0, Object arg1)
			throws CoreException {
		

	}

	@Override
	public void setTeamPrivateMember(boolean arg0) throws CoreException {
		

	}

	@Override
	public void touch(IProgressMonitor arg0) throws CoreException {
		

	}

	@Override
	public Object getAdapter(Class arg0) {
		
		return null;
	}

	@Override
	public boolean contains(ISchedulingRule arg0) {
		
		return false;
	}

	@Override
	public boolean isConflicting(ISchedulingRule arg0) {
		
		return false;
	}

}
