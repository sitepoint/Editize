package com.editize;

import java.awt.*;
import java.awt.event.*;
/**
 * This type was created in VisualAge.
 */
public class BasicWindowMonitor extends WindowAdapter {
	/**
	 * This method was created in VisualAge.
	 * @param e java.awt.event.WindowEvent
	 */
	public void windowClosing(WindowEvent e) {
		Window w = e.getWindow();
		w.setVisible(false);
		w.dispose();
		System.exit(0);
	}
}
