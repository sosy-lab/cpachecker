package cmdline.stubs;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import cmdline.stubs.StubPath;
import cmdline.stubs.StubProject;


public class StubFile implements IFile {
	
	private String filePath;
	
	public StubFile(String p) {
		filePath = p;
	}

	
	public void appendContents(InputStream arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	
	public void appendContents(InputStream arg0, boolean arg1, boolean arg2,
			IProgressMonitor arg3) throws CoreException {
		
		
	}

	
	public void create(InputStream arg0, boolean arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	
	public void create(InputStream arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	
	public void createLink(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	
	public void createLink(URI arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	
	public void delete(boolean arg0, boolean arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	
	public String getCharset() throws CoreException {
		
		return null;
	}

	
	public String getCharset(boolean arg0) throws CoreException {
		
		return null;
	}

	
	public String getCharsetFor(Reader arg0) throws CoreException {
		
		return null;
	}

	
	public IContentDescription getContentDescription() throws CoreException {
		
		return null;
	}

	
	public InputStream getContents() throws CoreException {
		
		return null;
	}

	
	public InputStream getContents(boolean arg0) throws CoreException {
		
		return null;
	}

	
	public int getEncoding() throws CoreException {
		
		return 0;
	}

	
	public IPath getFullPath() {
		
		return null;
	}

	
	public IFileState[] getHistory(IProgressMonitor arg0) throws CoreException {
		
		return null;
	}

	
	public String getName() {
		
		return null;
	}

	
	public boolean isReadOnly() {
		
		return false;
	}

	
	public void move(IPath arg0, boolean arg1, boolean arg2,
			IProgressMonitor arg3) throws CoreException {
		
		
	}

	
	public void setCharset(String arg0) throws CoreException {
		
		
	}

	
	public void setCharset(String arg0, IProgressMonitor arg1)
			throws CoreException {
		
		
	}

	
	public void setContents(InputStream arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	
	public void setContents(IFileState arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	
	public void setContents(InputStream arg0, boolean arg1, boolean arg2,
			IProgressMonitor arg3) throws CoreException {
		
		
	}

	
	public void setContents(IFileState arg0, boolean arg1, boolean arg2,
			IProgressMonitor arg3) throws CoreException {
		
		
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

	
	public long getLocalTimeStamp() {
		
		return 0;
	}

	
	public IPath getLocation() {
		// TODO Must implement!
		return new StubPath(filePath);
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

	
	public IContainer getParent() {
		
		return null;
	}

	
	@SuppressWarnings("unchecked")
  public Map getPersistentProperties() throws CoreException {
		
		return null;
	}

	
	public String getPersistentProperty(QualifiedName arg0)
			throws CoreException {
		
		return null;
	}

	
	public IProject getProject() {
		// TODO Must implement
		return new StubProject();
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

	
	@SuppressWarnings("unchecked")
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

	
	@SuppressWarnings("unchecked")
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
