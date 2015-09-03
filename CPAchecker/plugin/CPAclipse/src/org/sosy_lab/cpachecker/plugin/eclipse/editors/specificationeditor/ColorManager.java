package org.sosy_lab.cpachecker.plugin.eclipse.editors.specificationeditor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager {
	static RGB INCLUDE = new RGB(42, 0, 255);
	static RGB COMMENT = new RGB(63, 127, 95);
	static RGB KEYWORD = new RGB(127, 0, 85);
	static RGB STRING = new RGB(42, 0, 255);
	static RGB DEFAULT = new RGB(128, 128, 128);

	protected Map<RGB, Color> fColorTable = new HashMap<RGB, Color>(10);

	public void dispose() {
		Iterator<Color> e = fColorTable.values().iterator();
		while (e.hasNext())
			 e.next().dispose();
	}
	public Color getColor(RGB rgb) {
		Color color = fColorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			fColorTable.put(rgb, color);
		}
		return color;
	}
}
