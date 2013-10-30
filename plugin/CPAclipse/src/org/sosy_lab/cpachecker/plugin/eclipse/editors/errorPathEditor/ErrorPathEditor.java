package org.sosy_lab.cpachecker.plugin.eclipse.editors.errorPathEditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.TextEditor;

public class ErrorPathEditor extends TextEditor {
	public class LinePartitionScanner extends RuleBasedPartitionScanner {
		public LinePartitionScanner() {
				setPredicateRules(
						new IPredicateRule[] {
						new SingleLineRule("Line", ":", new Token(ErrorPathEditor.ERROR_PATH_LINE_PARTITION))
				});
			}
		}
	static final String ERROR_PATH_LINE_PARTITION = "__ERROR_PATH_LINE_PARTITION";
	final Color LINE_COLOR = new Color (Display.getCurrent(), 0, 0, 205);
	public ErrorPathEditor() {
		super();
	}
	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setSourceViewerConfiguration(new ErrorPathSourceViewerConfiguration(this, getPreferenceStore()));
		setDocumentProvider(new FileDocumentProvider() {
			@Override
			protected IDocument createDocument(Object element) throws CoreException {
				IDocument document = super.createDocument(element);
				if (document != null) {
					IDocumentPartitioner partitioner =
						new FastPartitioner(
								new LinePartitionScanner()
							, new String[] {ErrorPathEditor.ERROR_PATH_LINE_PARTITION});
					partitioner.connect(document);
					document.setDocumentPartitioner(partitioner);
				}
				return document;
			}
		});
	}
	@Override
	public void dispose() {
		this.LINE_COLOR.dispose();
	}
}
