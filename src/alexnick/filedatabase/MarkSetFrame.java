package alexnick.filedatabase;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import alexnick.CommonLib;

public class MarkSetFrame extends JDialog {
	private static final long serialVersionUID = 1L;
	private int isCheckResult = Const.MR_NO_CHOOSED;
	private String resultMark = "";

	/**
	 * @param allEquals     if not empty, defined equals string for all checked,
	 *                      that is select this string no changes mark property
	 * @param markValuesSet defined in mark properties strings, without '*'
	 */
	public MarkSetFrame(JDialog frame, String caption, String allEquals, Set<String> markValuesSet) {
		super(frame, caption, true);
		List<String> list = new ArrayList<String>(markValuesSet);
		list.sort(null);

		String[] arMarks = new String[list.size()];
		for (int i = 0; i < arMarks.length; i++) {
			arMarks[i] = list.get(i);
		}
		Box contents = new Box(BoxLayout.Y_AXIS);
		JList<String> listMark = new JList<String>(arMarks);
		listMark.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel buttons = new JPanel();
		JTextField tfMark = new JTextField(Const.textFieldBigSize);
		ActionListener actionSet = e -> setMark(allEquals, tfMark.getText());
		tfMark.addActionListener(actionSet);
		buttons.add(tfMark);

		listMark.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				tfMark.setText(listMark.getSelectedValue());
			}
		});

		JButton butSet = new JButton("Set");
		butSet.setToolTipText("set 'mark' for checked table items; to delete, type " + Const.REMOVE_MARK);
		buttons.add(butSet);
		butSet.addActionListener(actionSet);

		contents.add(new JScrollPane(listMark));
		getContentPane().add(contents);
		getContentPane().add(buttons, "South");
		pack();
		setPreferredSize(new Dimension(getWidth(), 400));
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void setMark(String allEquals, String mark) {
		if (!mark.equals(Const.REMOVE_MARK)) {
			mark = FileDataBase.formatMark(mark, false);
		}

		if (mark.isEmpty()) {
			return;
		}

		if (!allEquals.isEmpty() && mark.equals(allEquals)) {
			JOptionPane.showMessageDialog(this, "This mark is already set for all checked");
			return;
		}

		var s = mark.equals(Const.REMOVE_MARK) ? "REMOVE marks for checked?" : "Set for checked this mark?";

		if (JOptionPane.showConfirmDialog(this,
				s + CommonLib.NEW_LINE_UNIX + mark + CommonLib.NEW_LINE_UNIX + CommonLib.NEW_LINE_UNIX
						+ "All marks be saved to properties on close table window",
				"Set mark", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			resultMark = mark;
			isCheckResult = Const.MR_OK;
			dispose();
		}
	}

	/**
	 * @return result mark in 'formatMark' without BRACE_MARK; if empty, not defined
	 */
	String getResultMark() {
		return resultMark;
	}

	int getIsCheckResult() {
		return isCheckResult;
	}
}
