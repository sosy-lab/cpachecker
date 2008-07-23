package cpaplugin.cmdline.stubs;

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


public class StubFile implements IFile {
	
	private String filePath;
	
	public StubFile(String p) {
		filePath = p;
	}

	@Override
	public void appendContents(InputStream arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	@Override
	public void appendContents(InputStream arg0, boolean arg1, boolean arg2,
			IProgressMonitor arg3) throws CoreException {
		
		
	}

	@Override
	public void create(InputStream arg0, boolean arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	@Override
	public void create(InputStream arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	@Override
	public void createLink(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	@Override
	public void createLink(URI arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	@Override
	public void delete(boolean arg0, boolean arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	@Override
	public String getCharset() throws CoreException {
		
		return null;
	}

	@Override
	public String getCharset(boolean arg0) throws CoreException {
		
		return null;
	}

	@Override
	public String getCharsetFor(Reader arg0) throws CoreException {
		
		return null;
	}

	@Override
	public IContentDescription getContentDescription() throws CoreException {
		
		return null;
	}

	@Override
	public InputStream getContents() throws CoreException {
		
		return null;
	}

	@Override
	public InputStream getContents(boolean arg0) throws CoreException {
		
		return null;
	}

	@Override
	public int getEncoding() throws CoreException {
		
		return 0;
	}

	@Override
	public IPath getFullPath() {
		
		return null;
	}

	@Override
	public IFileState[] getHistory(IProgressMonitor arg0) throws CoreException {
		
		return null;
	}

	@Override
	public String getName() {
		
		return null;
	}

	@Override
	public boolean isReadOnly() {
		
		return false;
	}

	@Override
	public void move(IPath arg0, boolean arg1, boolean arg2,
			IProgressMonitor arg3) throws CoreException {
		
		
	}

	@Override
	public void setCharset(String arg0) throws CoreException {
		
		
	}

	@Override
	public void setCharset(String arg0, IProgressMonitor arg1)
			throws CoreException {
		
		
	}

	@Override
	public void setContents(InputStream arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	@Override
	public void setContents(IFileState arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		
		
	}

	@Override
	public void setContents(InputStream arg0, boolean arg1, boolean arg2,
			IProgressMonitor arg3) throws CoreException {
		
		
	}

	@Override
	public void setContents(IFileState arg0, boolean arg1, boolean arg2,
			IProgressMonitor arg3) throws CoreException {
		
		
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
	public long getLocalTimeStamp() {
		
		return 0;
	}

	@Override
	public IPath getLocation() {
		// TODO Must implement!
		return new StubPath(filePath);
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
		// TODO Must implement
		return new StubProject();
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
