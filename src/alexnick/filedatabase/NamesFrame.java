package alexnick.filedatabase;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import alexnick.CommonLib;

public class NamesFrame extends JDialog {
	private static final long serialVersionUID = 1L;

	private int isCheckResult = Const.MR_NO_CHOOSED;

	int getIsCheckResult() {
		return isCheckResult;
	}

	private JTextArea txtNamesNewRight;
	private JTextArea txtNamesOldLeft;
	private JCheckBox cbNumbersNewRight;

	private List<String> namesList;
	private List<String> namesNumbersNoSort;
	private List<String> namesOldNoSort; // without numbers

	public NamesFrame(JDialog dialog, String caption, List<String> columnNewNames, List<String> namesNumbersNoSort0,
			List<String> namesOldNoSort0, List<String> namesList0) {
		super(dialog, caption, true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (isCheckResult == Const.MR_NO_CHOOSED) {
					isCheckResult = Const.MR_CANCEL;
				}
			}
		});

		namesNumbersNoSort = new ArrayList<String>();
		namesNumbersNoSort.addAll(namesNumbersNoSort0);

		namesOldNoSort = new ArrayList<String>();
		namesOldNoSort.addAll(namesOldNoSort0);

		namesList = new ArrayList<String>();
		if (CommonLib.notNullEmptyList(namesList0)) {
			namesList.addAll(namesList0);
		}

		Box boxContents = new Box(BoxLayout.Y_AXIS);
		JPanel panGridNameLists = new JPanel();
		panGridNameLists.setLayout(new GridLayout(1, 2));

		txtNamesOldLeft = new JTextArea(CommonLib.listToStringOrEmpty(namesOldNoSort));
		txtNamesOldLeft.setEditable(false);
		panGridNameLists.add(new JScrollPane(txtNamesOldLeft));

		txtNamesNewRight = new JTextArea(CommonLib.listToStringOrEmpty(namesList));
		panGridNameLists.add(new JScrollPane(txtNamesNewRight));

		JPanel panGridButtons = new JPanel();
		panGridButtons.setLayout(new GridLayout(1, 2));

		JPanel panButtonsLeft = new JPanel();
		panButtonsLeft.setLayout(new FlowLayout(FlowLayout.LEADING));

		JPanel panButtonsRight = new JPanel();
		panButtonsRight.setLayout(new FlowLayout(FlowLayout.LEADING));

		JCheckBox cbNumbersOldLeft = new JCheckBox("numbers left");
		cbNumbersOldLeft.addActionListener(e -> addOldLeftNumbers(cbNumbersOldLeft.isSelected()));

		cbNumbersNewRight = new JCheckBox("numbers right");
		cbNumbersNewRight.addActionListener(e -> addNewRigthNumbers(cbNumbersNewRight.isSelected()));

//LEFT BUTTONS
		panButtonsLeft.add(cbNumbersOldLeft);

		JButton butReset = new JButton("Reset");
		butReset.addActionListener(e -> reset(columnNewNames));

		panButtonsLeft.add(butReset);

//RIGHT BUTTONS		
		panButtonsRight.add(cbNumbersNewRight);

		JButton butOk = new JButton("OK");
		butOk.addActionListener(e -> closing(Const.MR_OK));

		JButton butCancel = new JButton("Cancel");
		butCancel.addActionListener(e -> closing(Const.MR_CANCEL));

		panButtonsRight.add(butOk);
		panButtonsRight.add(butCancel);

//END ADD
		panGridButtons.add(panButtonsLeft);
		panGridButtons.add(panButtonsRight);

		boxContents.add(panGridNameLists);
		getContentPane().add(boxContents);
		getContentPane().add(panGridButtons, "South");

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(0, 0, t.width - 20, t.height - 100);
		setLocationRelativeTo(null);
		setVisible(true);

	}

	private void reset(List<String> columnNewNames) {
		var message = CommonLib.formatConfirmYesNoMessage("Choose action:", "restore new names",
				"set from column 'new names'", null);
		var confirm = JOptionPane.showConfirmDialog(this, message, "Reset", JOptionPane.YES_NO_CANCEL_OPTION);

		if (confirm != JOptionPane.YES_OPTION && confirm != JOptionPane.NO_OPTION) {
			return;
		}
		if (cbNumbersNewRight.isSelected()) {
			cbNumbersNewRight.setSelected(false);
		}

		if (confirm == JOptionPane.NO_OPTION) {
			txtNamesNewRight.setText(CommonLib.listToStringOrEmpty(columnNewNames));
		} else {
			txtNamesNewRight.setText(CommonLib.listToStringOrEmpty(namesList));
		}
	}

	private void closing(int result) {
		isCheckResult = result;
		dispose();
	}

	private void addOldLeftNumbers(boolean needNumbers) {
		List<String> list = new ArrayList<String>();
		if (!needNumbers) {
			list.addAll(namesOldNoSort);
		} else if (namesNumbersNoSort.size() != namesOldNoSort.size()) {
			CommonLib.errorArgument(getName());
			return;
		} else {
			for (var i = 0; i < namesNumbersNoSort.size(); i++) {
				list.add(namesNumbersNoSort.get(i).concat(namesOldNoSort.get(i)));
			}
		}

		txtNamesOldLeft.setText(CommonLib.listToStringOrEmpty(list));
	}

	private List<String> addNewRigthNumbers(boolean needNumbers) {
		List<String> list = CommonLib.stringToList(txtNamesNewRight.getText());

		for (int i = 0; i < list.size(); i++) {
			String s = list.get(i);

			var pos = s.indexOf(Const.BRACE_END_WITH_SPACE);
			if (pos >= 0) {
				s = s.substring(pos + Const.BRACE_END_WITH_SPACE.length());
			}

			if (needNumbers && i < namesNumbersNoSort.size()) {
				s = namesNumbersNoSort.get(i).concat(s);
			}

			list.set(i, s);
		}

		txtNamesNewRight.setText(CommonLib.listToStringOrEmpty(list));
		return list;
	}

	// returns list with size as 'beans' size
	List<String> getNamesList() {
		List<String> list = addNewRigthNumbers(false);
		List<String> result = new ArrayList<String>();

		int lastIndex = list.size() - 1;

		for (var i = 0; i < namesNumbersNoSort.size(); i++) {
			if (i > lastIndex) {
				result.add("");
			} else {
				result.add(list.get(i));
			}
		}
		return result;
	}
}
