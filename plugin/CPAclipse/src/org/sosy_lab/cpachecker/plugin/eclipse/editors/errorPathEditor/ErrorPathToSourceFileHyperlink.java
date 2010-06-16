package org.sosy_lab.cpachecker.plugin.eclipse.editors.errorPathEditor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.ui.CDTUITools;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
import org.sosy_lab.cpachecker.plugin.eclipse.Task;

public class ErrorPathToSourceFileHyperlink implements IHyperlink {
	
	private final IRegion fRegion;
	//private final SelectionDispatchAction fOpenAction;
	//private final IJavaElement fElement;
	//private final boolean fQualify = false;
	private int sourceLineNumber;
	private IFile inputFile;

	public ErrorPathToSourceFileHyperlink(Region hyperlinkRegion,
			int sourceLineNo, IFile inputFile) {
		this.inputFile = inputFile;
		this.fRegion = hyperlinkRegion;
		this.sourceLineNumber = sourceLineNo;
	}

	/**
	 * Creates a new Java element implementation hyperlink for methods.
	 * 
	 * @param region the region of the link
	 * @param openAction the action to use to open the java elements
	 * @param element the java element to open
	 * @param qualify <code>true</code> if the hyperlink text should show a qualified name for
	 *            element.
	 * @param editor the active java editor
	 */
	/*public ErrorPathToSourceFileHyperlink(IRegion region, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, ITextEditor editor) {
		Assert.isNotNull(openAction);
		Assert.isNotNull(region);
		Assert.isNotNull(element);

		fRegion= region;
		fOpenAction= openAction;
		fElement= element;
		fQualify= qualify;
		fEditor= editor;
	}*/

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
		return "goto Source File";
		//return null;
		/*if (fQualify) {
			String elementLabel= JavaElementLabels.getElementLabel(fElement, JavaElementLabels.ALL_FULLY_QUALIFIED);
			return Messages.format(JavaEditorMessages.JavaElementImplementationHyperlink_hyperlinkText_qualified, new Object[] { elementLabel });
		} else {
			return JavaEditorMessages.JavaElementImplementationHyperlink_hyperlinkText;
		}*/
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getTypeLabel()
	 */
	public String getTypeLabel() {
		return null;
	}

	/**
	 * Opens the given implementation hyperlink for methods.
	 * <p>
	 * If there's only one implementor that hyperlink is opened in the editor, otherwise the
	 * Quick Hierarchy is opened.
	 * </p>
	 */
	public void open() {
		System.out.println("Open Line " + sourceLineNumber + " in Source of " + inputFile.getFullPath());
		String folderName = inputFile.getParent().getName();
		List<Task> tasks = CPAclipse.getPlugin().getTasks();
		Task myTask = null;
		for (Task t : tasks) {
			if (t.getName().equalsIgnoreCase(folderName)) {
				myTask = t;
				break;
			}
		}
		if (myTask == null) {
			CPAclipse.logInfo("could not find task for Hyperlink (search by foldername:\"" + folderName + "\")");
		} else {
			try {
				
				ICElement element = myTask.getTranslationUnit().getElementAtLine(sourceLineNumber);
								
				System.out.println("Hyperlink to a " + element.getElementName() + "(" + element.toString() + ")");
				
				/*IEditorPart edPart = */CDTUITools.openInEditor(element,true, true);
				//revealInEditor(edPart, fRegion.getOffset(), fRegion.getLength());
		
			} catch (CModelException e) {
				CPAclipse.logError(e);
			} catch (PartInitException e) {
				CPAclipse.logError(e);
			}
		}
		
	}
	/**
	 * I dont have a way to determine the offset of the line that is to be selected
	 * 
	 * Selects and reveals the given offset and length in the given editor part.
	 * @param editor the editor part
	 * @param offset the offset
	 * @param length the length
	 */
	public static void revealInEditor(IEditorPart editor, final int offset, final int length) {
		// copied from org.eclipse.jdt.internal.ui.javaeditor.EditorUtility.revealInEditor(IEditorPart, int, int)
		if (editor instanceof ITextEditor) {
			((ITextEditor)editor).selectAndReveal(offset, length);
			return;
		}

		// Support for non-text editor - try IGotoMarker interface
		 if (editor instanceof IGotoMarker) {
			final IEditorInput input= editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				final IGotoMarker gotoMarkerTarget= (IGotoMarker)editor;
				WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
					@Override
					protected void execute(IProgressMonitor monitor) throws CoreException {
						IMarker marker= null;
						try {
							marker= ((IFileEditorInput)input).getFile().createMarker(IMarker.TEXT);
							marker.setAttribute(IMarker.CHAR_START, offset);
							marker.setAttribute(IMarker.CHAR_END, offset + length);

							gotoMarkerTarget.gotoMarker(marker);

						} finally {
							if (marker != null)
								marker.delete();
						}
					}
				};

				try {
					op.run(null);
				} catch (InvocationTargetException ex) {
					// reveal failed
				} catch (InterruptedException e) {
					Assert.isTrue(false, "this operation can not be canceled"); //$NON-NLS-1$
				}
			}
			return;
		}

		/*
		 * Workaround: send out a text selection
		 * XXX: Needs to be improved, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=32214
		 */
		if (editor != null && editor.getEditorSite().getSelectionProvider() != null) {
			IEditorSite site= editor.getEditorSite();
			if (site == null)
				return;

			ISelectionProvider provider= editor.getEditorSite().getSelectionProvider();
			if (provider == null)
				return;

			provider.setSelection(new TextSelection(offset, length));
		}
	}
}
