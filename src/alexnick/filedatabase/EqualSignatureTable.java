package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import alexnick.CommonLib;

//!!! no sort table; no remove items from table
public class EqualSignatureTable extends JDialog {
	private static final long serialVersionUID = 1L;
	final private static String[] columns = { "<id; signature> size", "Source name", "Dest (new) name",
			"Full path info (on show table)" };

	private List<MyBean> beans;
	final private String standardTitle;
	private int isCheckResult = Const.MR_NO_CHOOSED;
	private BeansFourTableDefault myTable;
	private JLabel checkInfo;
	private Set<Integer> lastRenamedSet = new HashSet<>();
	private Set<Integer> previousLastRenamedSet = new HashSet<>();
	private Set<Integer> lastUNDOSet = new HashSet<>();
	boolean wasRenamedGlobal = false;

//CONSTRUCTOR	
	public EqualSignatureTable(JFrame frame, int equalSignId, String standardTitle, List<MyBean> beans0) {
		super(frame, true);
		FileDataBase.isShiftDown = false;

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
		list.add("marked as renamed"); // 4
		list.add("last renamed set");// 5
		list.add("previous last renamed");// 6
		list.add("last UNDO set");// 7
		// optionally, end on list
		if (beans.size() - equalSignId > 0) { // is a several options
			list.add("a few variants, each first"); // 8
			list.add("a few variants, each second");// 9
		}

		JComboBox<String> cmbCheckChoose = new JComboBox<>(CommonLib.getArrayFromListOrNull(list));
		cmbCheckChoose.addActionListener(e -> checking(cmbCheckChoose.getSelectedIndex()));

		checkInfo = new JLabel();

		JComboBox<String> cmbAction = new JComboBox<>(new String[] { "Rename", "Undo", "toList" });

		JButton butAction = new JButton(">>");
		butAction.addActionListener(e -> doAction(cmbAction.getSelectedIndex()));

//ADDING
		buttons.add(cmbCheckChoose);
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

	private boolean isRenamed() {
		for (var b : beans) {
			if (markedRenamed(b)) {
				return true;
			}
		}
		return false;
	}

//RENAMED MARK
	private boolean markedRenamed(MyBean b) {
		return b.getOne().startsWith("$");
	}

	String getRenamedMarkPrefix(boolean mainChecked) {
		return mainChecked ? "$REN " : "$R-- ";
	}

	// 0:rename; 1: undo; 2: toList
	private void doAction(int index) { // TODO
		int checkCount = printCount();
		if (index < 0 || index > 2 || checkCount <= 0) {
			return;
		}
		if (index == 2) { // toList
			FileDataBase.beansToList(true, 0, null, beans);
		}

		if (index == 1) {
			doUndo();
			return;
		}

//RENAMING
		Set<Integer> errorIdSet = new TreeSet<>();

// for renaming use indexes, no 'id'
		Set<Integer> setIndexForRename = new TreeSet<>();
		Set<Integer> checkSet = new TreeSet<>(); // there's be result also

		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);
			if (!b.check || markedRenamed(b) || b.serviceIntOne < 1) {
				continue; // 'b.serviceIntOne' must not be less than 1, but let be this check
			}

			int id = b.serviceIntThree;

			if (checkSet.add(id)) { // for check add for renaming; 'id' here
				setIndexForRename.add(i);// for renaming
			} else {
				errorIdSet.add(id);
			}
		}

		checkSet.clear(); // result will be here; indexes, no 'id'

//remove 'indexes', if contains in 'errorIdSet		
		if (!setIndexForRename.isEmpty()) {
			for (int i : setIndexForRename) {
				// check 'id'
				if (!errorIdSet.contains(beans.get(i).serviceIntThree)) {
					checkSet.add(i);
				}
			}
		}
		renaming(checkSet, errorIdSet);
	}

	private void renaming(Set<Integer> checkSet, Set<Integer> errorIdSet) {
		if (CommonLib.nullEmptySet(checkSet)) {
			JOptionPane.showMessageDialog(this, "No found items for renaming");
			return;
		}

		var sb = new StringBuilder();
		sb.append("Found items for renaming: ").append(checkSet.size());
		if (CommonLib.notNullEmptySet(errorIdSet)) {
			sb.append(CommonLib.NEW_LINE_UNIX).append("error id (").append(errorIdSet.size()).append(") [");
			for (var i : errorIdSet) {
				sb.append(CommonLib.formatInt(i, 3, null, ";"));
			}
			sb.append("]");
		}

		sb.append(CommonLib.NEW_LINE_UNIX).append(CommonLib.NEW_LINE_UNIX).append("Rename files?");

		var confirm = JOptionPane.showConfirmDialog(this, sb.toString(), "Rename files", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		if (!lastRenamedSet.isEmpty()) {
			previousLastRenamedSet.clear();
			previousLastRenamedSet.addAll(lastRenamedSet);
		}

		lastRenamedSet.clear();
		lastRenamedSet.addAll(checkSet);

		boolean wasRenamed = false;
		
		for (var i : checkSet) {
			var b = beans.get(i);
			Path oldPath = b.binPath;
			String newName = b.getThree();
			Path newPath = oldPath.resolveSibling(newName);
			var res = oldPath.toFile().renameTo(newPath.toFile());
			if (res && !wasRenamed) {
				wasRenamed = true; // need for update base *.bin
			}

			String prefix = null;

			if (res) {
				b.binPath = newPath;
				b.setThree(b.getTwo());
				b.setTwo(newName); // was in 'three'

				prefix = getRenamedMarkPrefix(true);

				// there's no sorted table, can do like below:
				if (b.serviceIntOne > 1) { // destSetSize; other names need correct
					int id = b.serviceIntThree;

					for (int y = i - 1; y >= 0; y--) {
						if (!formatOtherInDestSet(id, y, newName, newPath)) {
							break;
						}
					}

					for (int y = i + 1; y < beans.size(); y++) {
						if (!formatOtherInDestSet(id, y, newName, newPath)) {
							break;
						}
					}
				}

			} else {// not renamed
				prefix = "<error rename> ";
			}
			FileDataBase.formatBeanOneForEqualTable(prefix, b);
		}
		
		if (wasRenamed) {
			wasRenamedGlobal = true;
			myTable.updateUI();
		}

	}

	// return true, if need continue cycle
	private boolean formatOtherInDestSet(int id, int y, String newName, Path newPath) {
		var b = beans.get(y);
		if (b.serviceIntThree != id) {
			return false;
		}

		b.binPath = newPath;
		b.setTwo(newName);
		FileDataBase.formatBeanOneForEqualTable(getRenamedMarkPrefix(false), b);

		return true;
	}

	private void doUndo() {
		if (!isRenamed()) {
			return; // TODO
		}

	}

//0: one; 1: all; 2: no; 3: invert;4:marked as renamed
//5:last renamed set; 6:previous last renamed; 7:last UNDO set
//optional: 8:a few, each first; 9:a few, each second 
	private void checking(int index) { // TODO
		if (index < 0 || index > 9) {
			return;
		}
		var needUpdate = index == 3; // for 'invert' anyway
		if ((index == 5 && lastRenamedSet.isEmpty()) || (index == 6 && previousLastRenamedSet.isEmpty())
				|| (index == 7 && lastUNDOSet.isEmpty())) {
			index = 2; // no
		}

		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);

			if (index == 3) { // invert
				b.check = !b.check;
				continue;
			}

			var ch = false;

			if (index == 2) { // no
			} else if (index == 1) {
				ch = true; // all
			} else if (index == 5) {
				ch = checkIndexInSet(i, lastRenamedSet);
			} else if (index == 6) {
				ch = checkIndexInSet(i, previousLastRenamedSet);
			} else if (index == 7) {
				ch = checkIndexInSet(i, lastUNDOSet);
			} else { // renamed; one; each first; each second
				var renamed = markedRenamed(b);
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
							if (index == 8) {
								ch = b.serviceIntTwo == 0;
							} else if (index == 9) {
								ch = b.serviceIntTwo == 1;
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

	private boolean checkIndexInSet(int i, Set<Integer> set) {
		return set.contains(i);
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
