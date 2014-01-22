package org.sosy_lab.cpachecker.plugin.eclipse.editors.errorPathEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;

public class ErrorPathHyperlinkDetector extends AbstractHyperlinkDetector {

	/*
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		ITextEditor textEditor= (ITextEditor)getAdapter(ITextEditor.class);
		if (region == null || textEditor== null ) {
			//System.out.println("exit");
			return null;
		}
		//System.out.println("Region.toString() : " + region.toString());
		
		/*IAction openAction= textEditor.getAction("OpenEditor"); //$NON-NLS-1$
		if (!(openAction instanceof SelectionDispatchAction))
			return null;
		*/
		int offset= region.getOffset();
		try {
			IDocument doc = textViewer.getDocument();
			IRegion lineRegion = doc.getLineInformation(doc.getLineOfOffset(offset));
			String line = doc.get(lineRegion.getOffset(), lineRegion.getLength());
			int endOfNumber = line.indexOf(":");
			if (endOfNumber < 0) {
				return null; //line without hyperlink interest
			}
			if (offset > lineRegion.getOffset() + endOfNumber) {
				return null; // 
			}
			int sourceLineNo = Integer.parseInt(line.substring(5, endOfNumber)); // 5 chars for "L","i","n","e"," "
			Region hyperlinkRegion = new Region(lineRegion.getOffset(), endOfNumber);
			
			IFile inputFile = (IFile) textEditor.getEditorInput().getAdapter(IFile.class);
			if (inputFile == null) {
				return null;
			}
			
			return new IHyperlink[] {new ErrorPathToSourceFileHyperlink(hyperlinkRegion, sourceLineNo, inputFile)};
			
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		/*
		IJavaElement input= EditorUtility.getEditorInputJavaElement(textEditor, false);
		if (input == null)
			return null;
		*/
		/*
		try {
			IDocument document= textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
			IRegion wordRegion= JavaWordFinder.findWord(document, offset);
			if (wordRegion == null || wordRegion.getLength() == 0)
				return null;

			IJavaElement[] elements= null;
			elements= ((ICodeAssist) input).codeSelect(wordRegion.getOffset(), wordRegion.getLength());
			elements= selectOpenableElements(elements);
			if (elements.length == 0)
				return null;
			
			IHyperlink[] links= new IHyperlink[elements.length];
			int j= 0;
			for (int i= 0; i < elements.length; i++) {
				IHyperlink link= createHyperlink(wordRegion, (SelectionDispatchAction)openAction, elements[i], elements.length > 1, textEditor);
				if (link != null) {
					links[j++]= link;
				}
			}
			if (j == 0) {
				return null;
			} else if (j < elements.length) {
				IHyperlink[] result= new IHyperlink[j];
				System.arraycopy(links, 0, result, 0, j);
				return result;
			}
			return links;

		} catch (JavaModelException e) {
			return null;
		}*/
		return null;
	}

	/**
	 * Creates a java element hyperlink.
	 * 
	 * @param wordRegion the region of the link
	 * @param openAction the action to use to open the java elements
	 * @param element the java element to open
	 * @param qualify <code>true</code> if the hyperlink text should show a qualified name for
	 *            element
	 * @param editor the active java editor
	 * @return a Java element hyperlink or <code>null</code> if no hyperlink can be created for the
	 *         given arguments
	 * @since 3.5
	 */
	/*protected IHyperlink createHyperlink(IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, ITextEditor editor) {
		return new JavaElementHyperlink(wordRegion, openAction, element, qualify);
	}*/
	/*protected IHyperlink createHyperlink(IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, ITextEditor editor) {
		return new JavaElementHyperlink(wordRegion, openAction, element, qualify);
	}*/


	/**
	 * Selects the openable elements out of the given ones.
	 *
	 * @param elements the elements to filter
	 * @return the openable elements
	 * @since 3.4
	 */
	/*private IJavaElement[] selectOpenableElements(IJavaElement[] elements) {
		List result= new ArrayList(elements.length);
		for (int i= 0; i < elements.length; i++) {
			IJavaElement element= elements[i];
			switch (element.getElementType()) {
				case IJavaElement.PACKAGE_DECLARATION:
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.JAVA_MODEL:
					break;
				default:
					result.add(element);
					break;
			}
		}
		return (IJavaElement[]) result.toArray(new IJavaElement[result.size()]);
	}*/
}
