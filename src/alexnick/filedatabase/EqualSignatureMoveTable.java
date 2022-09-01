package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import alexnick.CommonLib;

public class EqualSignatureMoveTable extends JDialog {
	private static final long serialVersionUID = 1L;
	final private static String[] columns = { "<id; signature> size", "Result", "Source full path (will be moved)",
			"Generated full path for moving" };
	private int isCheckResult = Const.MR_NO_CHOOSED;

	private List<String> moveLog = new ArrayList<>();

	public List<String> getMoveLog() {
		return moveLog;
	}

	public int getIsCheckResult() {
		int count = getRealMovedCount(true);
		isCheckResult = count > 0 ? Const.MR_NEED_UPDATE_BASE : Const.MR_CANCEL;
		return isCheckResult;
	}

	private List<MyBean> beans;
	final private String standardTitle;
	volatile private int lastSortType = SortBeans.sortNoDefined;
	private BeansFourTableDefault myTable;
	private JLabel checkInfo;
	private Set<Integer> lastMovedSet = new HashSet<>();
	private Set<Integer> lastMovedMinusOneSet = new HashSet<>();
	private Set<Integer> lastMovedMinusTwoSet = new HashSet<>();

//CONSTRUCTOR	
	public EqualSignatureMoveTable(JFrame frame, String standardTitle, List<MyBean> beans0) {
		super(frame, true);
		FileDataBase.isShiftDown = false;

		if (CommonLib.nullEmptyList(beans0)) {
			CommonLib.errorArgument("error of creating Equal Signature Table");
		}

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
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

		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, true, true, false, columns[0],
				columns[1], columns[2], columns[3], beans);
		initComponents();

		printCount();

		CommonLib.addLog(CommonLib.ADDLOG_SEP, false, moveLog);
		CommonLib.addLog("Was showed window Equal Signatures MOVE Table. Information about moving files", false,
				moveLog);

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds(0, 0, t.width - 8, t.height - 40);

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initComponents() { // on constructor
//MY_TABLE INIT			
		myTable.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1 && e.getButton() == 1) {
					sorting(myTable.convertColumnIndexToModel(myTable.columnAtPoint(e.getPoint())));
				}
			}
		});

		myTable.addMouseListener(new MouseAdapter() {
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
					FileDataBase.openDirectory(3, FileDataBase.isShiftDown, myTable, beans);
				}
			}
		});

		Box contents = new Box(BoxLayout.Y_AXIS);

//FILL JPANEL		
		JPanel buttons = new JPanel();
		List<String> list = new ArrayList<String>();
		list.add("no moved (empty result)"); // 0
		list.add("all check"); // 1
		list.add("no check"); // 2
		list.add("invert check"); // 3
		list.add("marked as " + Const.MOVE_PREFIX_FOR_COLUMN_RESULT); // 4
		list.add("marked as " + Const.MOVE_PREFIX_FOR_COLUMN_GENERATED_PATH); // 5
		list.add("last moved/undo set");// 6
		list.add("last moved/undo - 1");// 7
		list.add("last moved/undo - 2");// 8

		JComboBox<String> cmbCheckChoose = new JComboBox<>(CommonLib.getArrayFromListOrNull(list));
		cmbCheckChoose.addActionListener(e -> checking(cmbCheckChoose.getSelectedIndex()));

		checkInfo = new JLabel();
		JCheckBox cbAny = new JCheckBox("any");
		cbAny.setToolTipText("if checked, for moving be chosen also with no empty column 'Result'");

		// 'Move' must be first, because 'cbAny' is enabled on start this window
		JComboBox<String> cmbAction = new JComboBox<>(new String[] { "Move ", "Undo ", "toList " });
		cmbAction.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cbAny.setEnabled(cmbAction.getSelectedIndex() == 0);
			}
		});

		JButton butAction = new JButton(">>");
		butAction.addActionListener(e -> doAction(!cbAny.isSelected(), cmbAction.getSelectedIndex()));

//ADDING
		buttons.add(cmbCheckChoose);
		buttons.add(checkInfo);
		buttons.add(cmbAction);
		buttons.add(cbAny);
		buttons.add(butAction);

		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));

		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents, BorderLayout.CENTER);
		getContentPane().add(buttons, BorderLayout.SOUTH);

//SHIFT DOWN
		myTable.addKeyListener(FileDataBase.keyListenerShiftDown);
		cmbCheckChoose.addKeyListener(FileDataBase.keyListenerShiftDown);
		cmbAction.addKeyListener(FileDataBase.keyListenerShiftDown);
		cbAny.addKeyListener(FileDataBase.keyListenerShiftDown);
		butAction.addKeyListener(FileDataBase.keyListenerShiftDown);
	}

	private void setStandardTitle() {
		setNewTitle(standardTitle);

	}

	private void setNewTitle(String s) {
		if (!getTitle().equals(s)) {
			setTitle(s);
		}
	}

	private void sorting(int columnIndex) {
		if (columnIndex < 0 || beans.size() < 2) {
			return;
		}

		int sortType = SortBeans.sortNoDefined;
		String sortCaption = "";
		boolean noDubleSort = false;// columns 0,4 and if's shift: always sort

//columns = { "<id; signature> size", "Result", "Source full path (will be moved)","Generated full path for moving" };

		final String column = (columnIndex >= 1 && columnIndex <= 3) ? columns[columnIndex - 1] : columns[3];

		if (columnIndex == 0) {
			sortType = SortBeans.sortCheck_ThenFour;
		} else if (columnIndex == 1) {
			sortType = SortBeans.sortOne;
			sortCaption = column;
			noDubleSort = true;
		} else if (columnIndex == 2) {
			sortType = SortBeans.sortTwo;
			sortCaption = column;
			noDubleSort = true;
		} else if (columnIndex == 3) {
			sortType = SortBeans.sortThreeLowerCase;
			sortCaption = column;
			noDubleSort = true;
		} else {
			sortType = SortBeans.sortFourLowerCase;
			sortCaption = column;
			noDubleSort = true;
		}

		if (sortType == lastSortType && noDubleSort) {
			return;
		}

		lastSortType = sortType;
		setStandardTitle();
		var sortBeans = new SortBeans(sortType, sortCaption, beans);
		setNewTitle(standardTitle.concat(sortBeans.getAppendCaption()));
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

//MARK
	/**
	 * @param markType false: column 'Result' starts with '$MOVED '<br>
	 *                 true: column 'Generated path' starts with 'MOVED_TO '
	 * @param b
	 * @return true, if found renamed mark according 'markType'
	 */
	private boolean markedMoved(boolean markType, MyBean b) {
		return markType ? b.getFour(false, false).startsWith(Const.MOVE_PREFIX_FOR_COLUMN_GENERATED_PATH)
				: b.getTwo().startsWith(Const.MOVE_PREFIX_FOR_COLUMN_RESULT);
	}

	private String getGeneranedPathString(MyBean b) {
		String result = b.getFour(false, false);
		return result.startsWith(Const.MOVE_PREFIX_FOR_COLUMN_GENERATED_PATH)
				? result.substring(Const.MOVE_PREFIX_FOR_COLUMN_GENERATED_PATH.length())
				: result;
	}

	private int getRealMovedCount(final boolean oneIsEnough) {
		int result = 0;
		for (var b : beans) {
			if (markedMoved(true, b)) {
				result++;
				if (oneIsEnough) {
					break;
				}
			}
		}
		return result;
	}

// 0:rename; 1: undo; 2: toList; 'checkColumnResult' for 'moving', if false - no check empty column 'Result'
	private void doAction(boolean checkColumnResult, int index) {
		int checkCount = printCount();
		if (index < 0 || index > 2 || checkCount <= 0) {
			return;
		}
		if (index == 2) { // toList
			FileDataBase.beansToList(true, 0, null, beans);
			return;
		}

		if (index == 1) {
			doUndo();
			return;
		}

		if (index != 0) {
			return;
		}

//MOVING

// for moving use indexes, no 'id'; 
		Set<Integer> checkSet = new TreeSet<>();

		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);
			if (!b.check) {
				continue;
			}

			if (checkColumnResult && !b.getTwo().isEmpty()) {
				continue;
			}

			checkSet.add(i);
		}
		moving("", "moving", checkSet);
	}

//'startPrefix' and 'typeMoving' may be empty, but must not be null
	private void moving(final String startErrorPrefix, final String typeMoving, Set<Integer> checkSet) {
		if (CommonLib.nullEmptySet(checkSet)) {
			JOptionPane.showMessageDialog(this, "No found items for " + typeMoving);
			return;
		}

		var sb = new StringBuilder();
		sb.append("Found items for " + typeMoving + ": ").append(checkSet.size());

		sb.append(CommonLib.NEW_LINE_UNIX).append(CommonLib.NEW_LINE_UNIX).append("Start ").append(typeMoving)
				.append(" files?");

		var confirm = JOptionPane.showConfirmDialog(this, sb.toString(), typeMoving, JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		if (!lastMovedMinusOneSet.isEmpty()) {
			lastMovedMinusTwoSet.clear();
			lastMovedMinusTwoSet.addAll(lastMovedMinusOneSet);
			lastMovedMinusOneSet.clear();
		}

		if (!lastMovedSet.isEmpty()) {
			lastMovedMinusOneSet.clear();
			lastMovedMinusOneSet.addAll(lastMovedSet);
		}

		lastMovedSet.clear();
		lastMovedSet.addAll(checkSet);

		int countMoved = 0;
		CommonLib.addLog(CommonLib.ADDLOG_SEP, false, moveLog);
		CommonLib.addLog(CommonLib.ADDLOG_DATE, false, moveLog);
		CommonLib.addLog("---", false, moveLog);
		String errorMove = "";

		for (var i : checkSet) {
			var b = beans.get(i);
//can be 'move' or 'undo'; for 'move', b.four must be equal b.binPath

			final String oldPathString = b.getThree();
			final String newPathString = getGeneranedPathString(b);

			try {
				final Path oldPath = Path.of(oldPathString);
				if (!oldPath.toFile().exists()) {
					errorMove = "source file no found";
					CommonLib.errorArgument("");
				}

				final Path newPath = Path.of(newPathString);
				if (newPath.toFile().exists()) {
					errorMove = "Const.ERROR_FILE_EXISTS_NO_BRACES";
					CommonLib.errorArgument("");
				}

				CommonLib.CreateDirectoriesIfNeed(newPath);
				Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);

				if (!newPath.toFile().exists() || oldPath.toFile().exists()) {
					CommonLib.errorArgument("");
				}

				b.setTwo(Const.MOVE_PREFIX_FOR_COLUMN_RESULT);
				b.setThree(newPathString);

				// if b.binPath equals result path (located in b.three) => file no moved
				b.setFour(markedMoved(true, b) || b.binPath.equals(newPath) ? oldPathString
						: Const.MOVE_PREFIX_FOR_COLUMN_GENERATED_PATH + oldPathString, null);
				countMoved++;

			} catch (Exception e) {
				b.setTwo(errorMove.isEmpty() ? Const.ERROR_FILE_NOT_MOVED
						: Const.BRACE_START + "error:" + errorMove + Const.BRACE_END_WITH_SPACE);
			}

			CommonLib.addLog(b.getTwo().concat(oldPathString.concat(" >> ").concat(newPathString)), false, moveLog);
		}

		myTable.updateUI();
		var sb2 = new StringBuilder();
		sb2.append("Moved files: ").append(countMoved).append(" from ").append(checkSet.size())
				.append(CommonLib.NEW_LINE_UNIX)
				.append("[ if any files has been moved, source base will be updated after closing this window ]")
				.append(CommonLib.NEW_LINE_UNIX).append("[ total moved: ").append(getRealMovedCount(false))
				.append(" / ").append(beans.size()).append(" ]");
		JOptionPane.showMessageDialog(this, sb2.toString());
	}

	private void doUndo() { // for marked '$MOVED'
		Set<Integer> undoIndexesSet = new TreeSet<>();

		for (var i = 0; i < beans.size(); i++) {
			var b = beans.get(i);
			if (!b.check || !markedMoved(false, b)) {
				continue;
			}
			undoIndexesSet.add(i);
		}

		final String startRen = Const.MOVE_PREFIX_FOR_COLUMN_RESULT;
		moving(startRen, "undo moving, marked " + startRen, undoIndexesSet);
	}

//0: empty result; 1: all; 2: no; 3: invert;4:marked as "$MOVED "
//5:marked as "<MOVED_FROM> ";
//6:last renamed set; 7:last renamed - 1; 8:last renamed - 2
	private void checking(int index) {
		if (index < 0 || index > 8) {
			return;
		}
		var needUpdate = index == 3; // for 'invert' anyway
		if ((index == 6 && lastMovedSet.isEmpty()) || (index == 7 && lastMovedMinusOneSet.isEmpty())
				|| (index == 8 && lastMovedMinusTwoSet.isEmpty())) {
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
			} else if (index == 6) {
				ch = checkIndexInSet(i, lastMovedSet);
			} else if (index == 7) {
				ch = checkIndexInSet(i, lastMovedMinusOneSet);
			} else if (index == 8) {
				ch = checkIndexInSet(i, lastMovedMinusTwoSet);
			} else { // // 0:empty result; 4,5: marked
				if (index == 4 || index == 5) {
					ch = markedMoved(index == 5, b);
				} else if (index == 0) { // empty result
					ch = b.getTwo().isEmpty();
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

}
