package simulator.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

class ViewUtils {

	/*
	 * return the frame to which 'c' belongs
	 */
	static Frame getWindow(Component c) {
		Frame w = null;
		if (c != null) {
			if (c instanceof Frame)
				w = (Frame) c;
			else
				w = (Frame) SwingUtilities.getWindowAncestor(c);
		}
		return w;
	}

	/*
	 * opens a dialog box to show an error
	 */
	static void showErrorMsg(String msg) {
		showErrorMsg(null, msg);
	}

	/*
	 * opens a dialog box to show an error, it will be places relative to the window
	 * to which 'c' belongs
	 */
	static void showErrorMsg(Component c, String msg) {
		JOptionPane.showMessageDialog(getWindow(c), msg, "ERROR", JOptionPane.ERROR_MESSAGE);
	}

	/*
	 * asks the user to confirm a quit action, and quits using System.exit. The
	 * dialog is places relative to the window to which 'c' belongs
	 */
	static void quit(Component c) {

		int n = JOptionPane.showOptionDialog(getWindow(c), "Are sure you want to quit?", "Quit",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

		if (n == 0) {
			System.exit(0);
		}
	}

	/*
	 * convert the hash code of 's' to a Color.
	 */
	static Color getColor(Object s) {
		return new Color(s.hashCode());
	}

}
