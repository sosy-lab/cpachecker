package cpaplugin.cmdline.stubs;

import java.io.File;

import org.eclipse.core.runtime.IPath;


public class StubPath implements IPath {
	
	private String pth;
	
	public StubPath(String p) {
		pth = p;
	}

	public Object clone() {
		return new StubPath(pth);
	}
	
	@Override
	public IPath addFileExtension(String arg0) {
		
		return null;
	}

	@Override
	public IPath addTrailingSeparator() {
		
		return null;
	}

	@Override
	public IPath append(String arg0) {
		
		return null;
	}

	@Override
	public IPath append(IPath arg0) {
		
		return null;
	}

	@Override
	public String getDevice() {
		
		return null;
	}

	@Override
	public String getFileExtension() {
		
		return null;
	}

	@Override
	public boolean hasTrailingSeparator() {
		
		return false;
	}

	@Override
	public boolean isAbsolute() {
		
		return false;
	}

	@Override
	public boolean isEmpty() {
		
		return false;
	}

	@Override
	public boolean isPrefixOf(IPath arg0) {
		
		return false;
	}

	@Override
	public boolean isRoot() {
		
		return false;
	}

	@Override
	public boolean isUNC() {
		
		return false;
	}

	@Override
	public boolean isValidPath(String arg0) {
		
		return false;
	}

	@Override
	public boolean isValidSegment(String arg0) {
		
		return false;
	}

	@Override
	public String lastSegment() {
		
		return null;
	}

	@Override
	public IPath makeAbsolute() {
		
		return null;
	}

	@Override
	public IPath makeRelative() {
		
		return null;
	}

	@Override
	public IPath makeUNC(boolean arg0) {
		
		return null;
	}

	@Override
	public int matchingFirstSegments(IPath arg0) {
		
		return 0;
	}

	@Override
	public IPath removeFileExtension() {
		
		return null;
	}

	@Override
	public IPath removeFirstSegments(int arg0) {
		
		return null;
	}

	@Override
	public IPath removeLastSegments(int arg0) {
		
		return null;
	}

	@Override
	public IPath removeTrailingSeparator() {
		
		return null;
	}

	@Override
	public String segment(int arg0) {
		
		return null;
	}

	@Override
	public int segmentCount() {
		
		return 0;
	}

	@Override
	public String[] segments() {
		
		return null;
	}

	@Override
	public IPath setDevice(String arg0) {
		
		return null;
	}

	@Override
	public File toFile() {
		
		return null;
	}

	@Override
	public String toOSString() {
		// TODO must implement
		return pth;
	}

	@Override
	public String toPortableString() {
		
		return null;
	}

	@Override
	public IPath uptoSegment(int arg0) {
		
		return null;
	}

}
