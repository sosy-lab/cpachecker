package org.sosy_lab.cpachecker.plugin.eclipse.editors.specificationeditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;

public class IncludeHyperlink implements IHyperlink {
	
	private final IRegion fRegion;
	private final String fileName;
	private final IProject parentProject;
	private final IWorkbenchPage page;
	

	public IncludeHyperlink(Region linkRegion, String fileName, IProject iProject, IWorkbenchPage iWorkbenchPage) {
		this.fileName = fileName;
		this.fRegion = linkRegion;
		this.parentProject = iProject;
		this.page = iWorkbenchPage;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getHyperlinkRegion()
	 */
	public IRegion getHyperlinkRegion() {
		return fRegion;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getHyperlinkText()
	 */
	public String getHyperlinkText() {
		return "goto included Specification File";
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getTypeLabel()
	 */
	public String getTypeLabel() {
		return null;
	}
	
	public void open() {
		IFile file = parentProject.getFile(fileName);
		if (file != null && file.exists()) {
			IEditorInput input = new FileEditorInput(file);
			try {
				page.openEditor(input, "org.sosy_lab.cpachecker.plugin.eclipse.editors.specificationeditor.SpecificationEditor", true);
			} catch (PartInitException e) {
				CPAclipse.logError(e);
			}
		} else {
			MessageDialog.openInformation(null, "Could not find File", "Could not find the file " + fileName);
			return;
		}
	}
}
