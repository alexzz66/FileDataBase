package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import alexnick.CommonLib;

public class EqualSignatureTable extends JDialog {
	private static final long serialVersionUID = 1L;
	final private static String[] columns = { "<id; signature>", "Source name", "Dest (new) name",
			"Full path info (on show table)" };

	private List<MyBean> beans;
	final private String standardTitle;
	private int isCheckResult = Const.MR_NO_CHOOSED;
	private BeansFourTableDefault myTable;
	private JLabel checkInfo;

//CONSTRUCTOR	
	public EqualSignatureTable(JFrame frame, int equalSignId, String standardTitle, List<MyBean> beans0) {
		super(frame, true);
		if (CommonLib.nullEmptyList(beans0) || (beans0.size() - equalSignId < 0)) {
			CommonLib.errorArgument("error of creating Equal Signature Table");
		}

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (isCheckResult == Const.MR_NO_CHOOSED) {
					isCheckResult = Const.MR_CANCEL;
				}
			}
		});

		beans = new ArrayList<MyBean>();
		beans.addAll(beans0);

		this.standardTitle = standardTitle;
		setStandardTitle();

		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, false, false, false, columns[0],
				columns[1], columns[2], columns[3], beans);
		initComponents(equalSignId);

		printCount();

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds(0, 0, t.width - 8, t.height - 40);

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initComponents(int equalSignId) { // on constructor
		myTable.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() != 1) {
					return;
				}
				if (myTable.getSelectedColumn() == 0) {
					if (e.getClickCount() == 1) {
						printCount();
					}
					return;
				}
				if (e.getClickCount() == 2) {
					FileDataBase.openDirectory(true, FileDataBase.isShiftDown, myTable, beans);
				}
			}
		});

		Box contents = new Box(BoxLayout.Y_AXIS);

//FILL JPANEL		
		JPanel buttons = new JPanel();
		List<String> list = new ArrayList<String>();
		list.add("one variant check"); // 0
		list.add("all check"); // 1
		list.add("no check"); // 2
		list.add("invert check"); // 3
		list.add("renamed, for UNDO"); // 4

		// optionally, end on list
		if (beans.size() - equalSignId > 0) { // is a several options
			list.add("a few variants, each first"); // 5
			list.add("a few variants, each second"); // 6
		}

		JComboBox<String> cmbCheckChoose = new JComboBox<>(CommonLib.getArrayFromListOrNull(list));

		JButton butCheck = new JButton(">>");
		butCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				checking(cmbCheckChoose.getSelectedIndex());
			}
		});

		checkInfo = new JLabel();

		JComboBox<String> cmbAction = new JComboBox<>(new String[] { "Rename", "Undo", "toList" });

		JButton butAction = new JButton(">>");
		butCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				renaming(cmbAction.getSelectedIndex());
			}
		});

//ADDING
		buttons.add(cmbCheckChoose);
		buttons.add(butCheck);
		buttons.add(checkInfo);
		buttons.add(cmbAction);
		buttons.add(butAction);

		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));

		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents, BorderLayout.CENTER);
		getContentPane().add(buttons, BorderLayout.SOUTH);

//SHIFT DOWN
		myTable.addKeyListener(FileDataBase.keyListenerShiftDown);
		cmbCheckChoose.addKeyListener(FileDataBase.keyListenerShiftDown);
		butCheck.addKeyListener(FileDataBase.keyListenerShiftDown);
		cmbAction.addKeyListener(FileDataBase.keyListenerShiftDown);
		butAction.addKeyListener(FileDataBase.keyListenerShiftDown);
	}

	private int printCount() {
		int checkCount = 0;
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			checkCount++;
		}

		checkInfo.setText("count: " + checkCount);
		return checkCount;
	}

	private int getRenamedCount() {
		int result = 0;
		for (var b : beans) {
			if (b.getOne().startsWith("$")) {
				result++;
			}
		}
		return result;
	}

	// 0:rename; 1: undo; 2: toList
	private void renaming(int index) { // TODO
		int count = printCount();
		if (index < 0 || index > 2 || count <= 0) {
			return;
		}
		if (index == 2) { // toList
			FileDataBase.beansToList(0, null, beans);
		}

		if (index == 1) {
			doUndo();
			return;
		}

//RENAMING
		for (int i = 0; i < beans.size(); i++) {

		}
	}

	private void doUndo() {
		int renamedCount = getRenamedCount();
		if (renamedCount <= 0) {
			return; // TODO
		}

	}

//0: one; 1: all; 2: no; 3: invert; 4: renamed; optional: 5: a few,each first; 6:a few, each second	
	private void checking(int index) {
		if (index < 0 || index > 6) {
			return;
		}
		var needUpdate = index == 3; // for 'invert' anyway

		for (var b : beans) {
			if (index == 3) { // invert
				b.check = !b.check;
				continue;
			}

			var ch = false;

			if (index == 2) { // no
			} else if (index == 1) {
				ch = true; // all
			} else { // renamed; one; each first; each second
				var renamed = b.getOne().startsWith("$");
				if (index == 4) {
					ch = renamed;
				} else { // one; each first; each second
					if (!renamed) {
						if (index == 0) { // one
							ch = b.serviceIntOne == 1;// count of destSet
						} else { // each first; each second
							if (b.serviceIntOne < 2) {
								continue;
							}
							if (index == 5) {
								ch = b.serviceIntTwo == 1;
							} else if (index == 6) {
								ch = b.serviceIntTwo == 2;
							}
						}
					}
				}
			}

			if (ch == b.check) {
				continue;
			}
			b.check = ch;
			needUpdate = true;
		}

		if (needUpdate) {
			myTable.updateUI();
			printCount();
		}
	}

	private void setStandardTitle() {
		setNewTitle(standardTitle);

	}

	private void setNewTitle(String s) {
		if (!getTitle().equals(s)) {
			setTitle(s);
		}
	}

}
