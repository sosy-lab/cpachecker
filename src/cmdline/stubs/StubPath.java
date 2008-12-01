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

}
