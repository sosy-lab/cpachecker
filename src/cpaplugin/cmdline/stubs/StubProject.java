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

	
	public void build(int arg0, IProgressMonitor arg1) throws CoreException {
		

	}

	
	public void build(int arg0, String arg1, Map arg2, IProgressMonitor arg3)
			throws CoreException {
		

	}

	
	public void close(IProgressMonitor arg0) throws CoreException {
		

	}

	
	public void create(IProgressMonitor arg0) throws CoreException {
		

	}

	
	public void create(IProjectDescription arg0, IProgressMonitor arg1)
			throws CoreException {
		

	}

	
	public void create(IProjectDescription arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	
	public void delete(boolean arg0, boolean arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	
	public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
	    // TODO must implement
	    throw new CoreException(null);
	}

	
	public IProjectDescription getDescription() throws CoreException {
		
		return null;
	}

	
	public IFile getFile(String arg0) {
		
		return null;
	}

	
	public IFolder getFolder(String arg0) {
		
		return null;
	}

	
	public IProjectNature getNature(String arg0) throws CoreException {
		
		return null;
	}

	
	public IPath getPluginWorkingLocation(IPluginDescriptor arg0) {
		
		return null;
	}

	
	public IProject[] getReferencedProjects() throws CoreException {
		
		return null;
	}

	
	public IProject[] getReferencingProjects() {
		
		return null;
	}

	
	public IPath getWorkingLocation(String arg0) {
		
		return null;
	}

	
	public boolean hasNature(String arg0) throws CoreException {
		
		return false;
	}

	
	public boolean isNatureEnabled(String arg0) throws CoreException {
		
		return false;
	}

	
	public boolean isOpen() {
		
		return false;
	}

	
	public void move(IProjectDescription arg0, boolean arg1,
			IProgressMonitor arg2) throws CoreException {
		

	}

	
	public void open(IProgressMonitor arg0) throws CoreException {
		

	}

	
	public void open(int arg0, IProgressMonitor arg1) throws CoreException {
		

	}

	
	public void setDescription(IProjectDescription arg0, IProgressMonitor arg1)
			throws CoreException {
		

	}

	
	public void setDescription(IProjectDescription arg0, int arg1,
			IProgressMonitor arg2) throws CoreException {
		

	}

	
	public boolean exists(IPath arg0) {
		
		return false;
	}

	
	public IFile[] findDeletedMembersWithHistory(int arg0, IProgressMonitor arg1)
			throws CoreException {
		
		return null;
	}

	
	public IResource findMember(String arg0) {
		
		return null;
	}

	
	public IResource findMember(IPath arg0) {
		
		return null;
	}

	
	public IResource findMember(String arg0, boolean arg1) {
		
		return null;
	}

	
	public IResource findMember(IPath arg0, boolean arg1) {
		
		return null;
	}

	
	public String getDefaultCharset() throws CoreException {
		
		return null;
	}

	
	public String getDefaultCharset(boolean arg0) throws CoreException {
		
		return null;
	}

	
	public IFile getFile(IPath arg0) {
		
		return null;
	}

	
	public IFolder getFolder(IPath arg0) {
		
		return null;
	}

	
	public IResource[] members() throws CoreException {
		
		return null;
	}

	
	public IResource[] members(boolean arg0) throws CoreException {
		
		return null;
	}

	
	public IResource[] members(int arg0) throws CoreException {
		
		return null;
	}

	
	public void setDefaultCharset(String arg0) throws CoreException {
		

	}

	
	public void setDefaultCharset(String arg0, IProgressMonitor arg1)
			throws CoreException {
		

	}

	
	public void accept(IResourceVisitor arg0) throws CoreException {
		

	}

	
	public void accept(IResourceProxyVisitor arg0, int arg1)
			throws CoreException {
		

	}

	
	public void accept(IResourceVisitor arg0, int arg1, boolean arg2)
			throws CoreException {
		

	}

	
	public void accept(IResourceVisitor arg0, int arg1, int arg2)
			throws CoreException {
		

	}

	
	public void clearHistory(IProgressMonitor arg0) throws CoreException {
		

	}

	
	public void copy(IPath arg0, boolean arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	
	public void copy(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	
	public void copy(IProjectDescription arg0, boolean arg1,
			IProgressMonitor arg2) throws CoreException {
		

	}

	
	public void copy(IProjectDescription arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	
	public IMarker createMarker(String arg0) throws CoreException {
		
		return null;
	}

	
	public IResourceProxy createProxy() {
		
		return null;
	}

	
	public void delete(boolean arg0, IProgressMonitor arg1)
			throws CoreException {
		

	}

	
	public void delete(int arg0, IProgressMonitor arg1) throws CoreException {
		

	}

	
	public void deleteMarkers(String arg0, boolean arg1, int arg2)
			throws CoreException {
		

	}

	
	public boolean exists() {
		
		return false;
	}

	
	public IMarker findMarker(long arg0) throws CoreException {
		
		return null;
	}

	
	public IMarker[] findMarkers(String arg0, boolean arg1, int arg2)
			throws CoreException {
		
		return null;
	}

	
	public int findMaxProblemSeverity(String arg0, boolean arg1, int arg2)
			throws CoreException {
		
		return 0;
	}

	
	public String getFileExtension() {
		
		return null;
	}

	
	public IPath getFullPath() {
		
		return null;
	}

	
	public long getLocalTimeStamp() {
		
		return 0;
	}

	
	public IPath getLocation() {
		
		return null;
	}

	
	public URI getLocationURI() {
		
		return null;
	}

	
	public IMarker getMarker(long arg0) {
		
		return null;
	}

	
	public long getModificationStamp() {
		
		return 0;
	}

	
	public String getName() {
		
		return null;
	}

	
	public IContainer getParent() {
		
		return null;
	}

	
	public Map getPersistentProperties() throws CoreException {
		
		return null;
	}

	
	public String getPersistentProperty(QualifiedName arg0)
			throws CoreException {
		
		return null;
	}

	
	public IProject getProject() {
		
		return null;
	}

	
	public IPath getProjectRelativePath() {
		
		return null;
	}

	
	public IPath getRawLocation() {
		
		return null;
	}

	
	public URI getRawLocationURI() {
		
		return null;
	}

	
	public ResourceAttributes getResourceAttributes() {
		
		return null;
	}

	
	public Map getSessionProperties() throws CoreException {
		
		return null;
	}

	
	public Object getSessionProperty(QualifiedName arg0) throws CoreException {
		
		return null;
	}

	
	public int getType() {
		
		return 0;
	}

	
	public IWorkspace getWorkspace() {
		
		return null;
	}

	
	public boolean isAccessible() {
		
		return false;
	}

	
	public boolean isDerived() {
		
		return false;
	}

	
	public boolean isDerived(int arg0) {
		
		return false;
	}

	
	public boolean isHidden() {
		
		return false;
	}

	
	public boolean isLinked() {
		
		return false;
	}

	
	public boolean isLinked(int arg0) {
		
		return false;
	}

	
	public boolean isLocal(int arg0) {
		
		return false;
	}

	
	public boolean isPhantom() {
		
		return false;
	}

	
	public boolean isReadOnly() {
		
		return false;
	}

	
	public boolean isSynchronized(int arg0) {
		
		return false;
	}

	
	public boolean isTeamPrivateMember() {
		
		return false;
	}

	
	public void move(IPath arg0, boolean arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	
	public void move(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	
	public void move(IProjectDescription arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	
	public void move(IProjectDescription arg0, boolean arg1, boolean arg2,
			IProgressMonitor arg3) throws CoreException {
		

	}

	
	public void refreshLocal(int arg0, IProgressMonitor arg1)
			throws CoreException {
		

	}

	
	public void revertModificationStamp(long arg0) throws CoreException {
		

	}

	
	public void setDerived(boolean arg0) throws CoreException {
		

	}

	
	public void setHidden(boolean arg0) throws CoreException {
		

	}

	
	public void setLocal(boolean arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		

	}

	
	public long setLocalTimeStamp(long arg0) throws CoreException {
		
		return 0;
	}

	
	public void setPersistentProperty(QualifiedName arg0, String arg1)
			throws CoreException {
		

	}

	
	public void setReadOnly(boolean arg0) {
		

	}

	
	public void setResourceAttributes(ResourceAttributes arg0)
			throws CoreException {
		

	}

	
	public void setSessionProperty(QualifiedName arg0, Object arg1)
			throws CoreException {
		

	}

	
	public void setTeamPrivateMember(boolean arg0) throws CoreException {
		

	}

	
	public void touch(IProgressMonitor arg0) throws CoreException {
		

	}

	
	public Object getAdapter(Class arg0) {
		
		return null;
	}

	
	public boolean contains(ISchedulingRule arg0) {
		
		return false;
	}

	
	public boolean isConflicting(ISchedulingRule arg0) {
		
		return false;
	}

}
