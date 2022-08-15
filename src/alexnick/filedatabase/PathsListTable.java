package alexnick.filedatabase;

import static alexnick.CommonLib.dateModifiedToString;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import alexnick.CommonLib;
import alexnick.CopyMove;

public class PathsListTable extends JFrame implements Callable<Integer> {
	private static final long serialVersionUID = 1L;
	final private static String[] columns = { "Signature", "<number> <Rename result>", "Modified", "Path" };

	private BeansFourTableDefault myTable;
	private JTextField tfFindPath;
	private JLabel checkInfo;

	private int isCheckResult = Const.MR_NO_CHOOSED;
	private List<MyBean> beans;

	private int countFolders; // RECOUNT, if was a removing
	private int countFiles;

	private int checkNow = -1; // '-1' that is 'no defined' else
	private final int CHECK_NOW_MAX = 4; // minimum == 0

	// are dependent on each other
	private Set<Integer> setChecked = new HashSet<>();
	private Set<Path> setCheckedPaths = null;

	private TreeMap<Path, Path> resultMapOldNew = null;

	TreeMap<Path, Path> getResultMapOldNew() {
		return resultMapOldNew;
	}

	private String standardTitle = "";
	volatile private int lastSortType = SortBeans.sortNoDefined;
	private boolean needCalculateCrc;

	private String lastFind = "";
	private boolean replaceNoSubstringError;
	private int renameNumber = 0; // increases before rename/undo

	private List<String> renameLog = new ArrayList<String>();
	private final Path pathSaveRenameLog;
	private JCheckBox cbShowRenameLog;

	/**
	 * @param options          Program options
	 * @param needCalculateCrc need calculated crc in column 'Signature'
	 * @param listFullPaths    not must be null/empty, must contains exists files
	 */
	public PathsListTable(String options, boolean needCalculateCrc, List<File> listFullPaths) {
		FileDataBase.isShiftDown = false;
		pathSaveRenameLog = FileDataBase.getTempPath("renameLog.txt");

		this.replaceNoSubstringError = options.contains(Const.OPTIONS_RENAME_REPLACE_NO_SUBSTRING_ERROR);
		this.needCalculateCrc = needCalculateCrc;

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (isCheckResult == Const.MR_NO_CHOOSED) {
					isCheckResult = Const.MR_CANCEL;
				}
			}
		});

//INIT BEANS		
		var countFilesFolders = initBeans(listFullPaths);
		countFiles = countFilesFolders[0];
		countFolders = countFilesFolders[1];
		setNewStandardTitle();

		Box contents = new Box(BoxLayout.Y_AXIS);
//by default, set SINGLE_SELECTION, and there are other options
		var singleOnly = options.contains(Const.OPTIONS_PATHSLIST_SINGLE_ONLY);// no 'multiSelect'
		var startSetMulti = options.contains(Const.OPTIONS_PATHSLIST_SET_MULTI);// by default,'multiSelect'
		if (singleOnly && startSetMulti) {
			singleOnly = false;
			startSetMulti = false;
		}
		myTable = new BeansFourTableDefault(
				startSetMulti ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION,
				false, false, true, columns[0], columns[1], columns[2], columns[3], beans);

		myTable.setDropTarget(new DropTarget() {
			private static final long serialVersionUID = 1L;

			@Override
			public synchronized void drop(DropTargetDropEvent dtde) {
				Point point = dtde.getLocation();
				dragging(myTable.rowAtPoint(point));
			}
		});

		myTable.addKeyListener(FileDataBase.keyListenerShiftDown);
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
						printCount(true, 0, null);
					}
					return;
				}
				if (e.getClickCount() == 2) {
					FileDataBase.openDirectory(1, FileDataBase.isShiftDown, myTable, beans);
				}
			}
		});

//FILLING JPANEL
		JPanel buttons = new JPanel();
		JCheckBox cbDrag = new JCheckBox("drag");
		cbDrag.setToolTipText("allow row dragging");
		cbDrag.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				myTable.setDragEnabled(cbDrag.isSelected());
			}
		});

		if (options.contains(Const.OPTIONS_PATHSLIST_SET_DRAG)) {
			cbDrag.setSelected(true);
			myTable.setDragEnabled(true);
		}

		buttons.add(cbDrag);
		cbDrag.addKeyListener(FileDataBase.keyListenerShiftDown);

		if (!singleOnly) {
			JComboBox<String> cmbSingleMulti = new JComboBox<>(new String[] { "single", "multi" });
			cmbSingleMulti.setToolTipText("set 'single' or 'multi' selection");
			cmbSingleMulti.setSelectedIndex(startSetMulti ? 1 : 0);

			cmbSingleMulti.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					var listSelectionModel = cmbSingleMulti.getSelectedIndex() == 1
							? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
							: ListSelectionModel.SINGLE_SELECTION;
					if (listSelectionModel == myTable.getSelectionModel().getSelectionMode()) {
						return;
					}
					myTable.setSelectionMode(listSelectionModel);
				}
			});

			JCheckBox select = new JCheckBox("select");
			select.setToolTipText("check/uncheck selected items");
			select.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					var rows = myTable.getSelectedRows();
					if (rows.length == 0) {
						return;
					}
					var needSelect = select.isSelected();
					var needUpdate = false;
					for (var row : rows) {
						if (beans.get(row).check == needSelect) {
							continue;
						}
						needUpdate = true;
						beans.get(row).check = needSelect;
					}
					if (needUpdate) {
						updating(true, 0);
					}
				}
			});
			buttons.add(cmbSingleMulti);
			buttons.add(select);
			cmbSingleMulti.addKeyListener(FileDataBase.keyListenerShiftDown);
			select.addKeyListener(FileDataBase.keyListenerShiftDown);
		}

		JComboBox<String> cmbFindPosition = new JComboBox<>(new String[] { "any", "starts", "ends" });
		JComboBox<String> cmbFindFullPathOrName = new JComboBox<>(new String[] { "fullPath", "name" });
		var butCheckActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (beans.isEmpty()) {
					return;
				}
				var clickOnTfFind = e.getActionCommand().equals(Const.textFieldFindPathClick);
				var findLowerCase = tfFindPath.getText().toLowerCase();

				if (!findLowerCase.equals(lastFind) || clickOnTfFind) {
					checkNow = findLowerCase.isEmpty() ? 0 : 1;
					lastFind = findLowerCase;
				} else {
					nextCheckNow(); // 0:no;1:filter(optional);2:files(if > 0);3:folders(if > 0); 4:all
					if (checkNow == 1 && findLowerCase.isEmpty()) {// filter, no need if empty
						nextCheckNow();
					}
				}

				int findPosition = 0;
				boolean findByName = false;
				String find[] = null;

				if (checkNow == 1) {
					findPosition = cmbFindPosition.getSelectedIndex();
					findByName = cmbFindFullPathOrName.getSelectedIndex() == 1;
					find = FileDataBase.getCorrectFindOrNull(findLowerCase);
					if (find == null) {
						nextCheckNow();
					}
				}

				for (var b : beans) {
					b.check = (checkNow == 1)
							? findFilter(findPosition,
									findByName ? b.getNameLowerCaseFromFour() : b.getFourLowerCase(true, true), find, b)
							: (checkNow >= CHECK_NOW_MAX) ? true
									: (checkNow == 2) ? b.serviceIntOne == CommonLib.SIGN_FILE
											: (checkNow == 3) ? b.serviceIntOne == CommonLib.SIGN_FOLDER : false;// <= 0
				}
				updating(false, 0);
			}

			private boolean findFilter(int findPosition, final String stringInLowerCase, String[] find, MyBean b) {
				if (!find[1].isEmpty() && !b.findInLowerCase(findPosition, stringInLowerCase, find[1],
						Const.textFieldFindORSeparator)) {
					return false;
				}

				return b.findInLowerCase(findPosition, stringInLowerCase, find[0], Const.textFieldFindORSeparator);
			}

			private void nextCheckNow() {
				if (checkNow < 0 || checkNow > CHECK_NOW_MAX) {
					checkNow = 0;
				}
				checkNow++;
				if (checkNow == 2 && countFiles == 0) {
					checkNow++;
				}
				if (checkNow == 3 && countFolders == 0) {
					checkNow++;
				}
				if (checkNow > CHECK_NOW_MAX) {
					checkNow = 0;
				}
			}
		};

		tfFindPath = new JTextField(FileDataBase.sizeTextField);
		tfFindPath.setActionCommand(Const.textFieldFindPathClick);
		tfFindPath.addActionListener(butCheckActionListener);
		tfFindPath.setToolTipText(Const.textFieldPathToolTip);

		var butCheck = new JButton("check");
		butCheck.addActionListener(butCheckActionListener);
		butCheck.setToolTipText(Const.butCheckToolTipFilesFolders);

		var butInvert = new JButton("invert");
		butInvert.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (var b : beans) {
					b.check = !b.check;
				}
				updating(true, 0);
			}
		});

		checkInfo = new JLabel();

		cbShowRenameLog = new JCheckBox("log");
		cbShowRenameLog.setEnabled(false);
		cbShowRenameLog.setToolTipText("show rename log after rename/undo");

		JComboBox<String> cmbActions = new JComboBox<>(new String[] { "export to list", "remove from table",
				"copy/move files to", "delete files", "rename files", "undo rename files", "show rename log" });
		cmbActions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				var index = cmbActions.getSelectedIndex(); // rename and undo >> enabled
				cbShowRenameLog.setEnabled(index == 4 || index == 5);
			}
		});

		JButton butDoAction = new JButton(">>");
		butDoAction.setToolTipText("assign the chosen action to checked/selected items");
		butDoAction.addActionListener(e -> doAction(cmbActions));

		buttons.add(cmbFindPosition);
		buttons.add(cmbFindFullPathOrName);

		buttons.add(tfFindPath);
		buttons.add(butCheck);
		buttons.add(butInvert);
		buttons.add(checkInfo);

		buttons.add(cmbActions);
		buttons.add(butDoAction);
		buttons.add(cbShowRenameLog);

		cmbFindPosition.addKeyListener(FileDataBase.keyListenerShiftDown);
		cmbFindFullPathOrName.addKeyListener(FileDataBase.keyListenerShiftDown);

		tfFindPath.addKeyListener(FileDataBase.keyListenerShiftDown);
		butCheck.addKeyListener(FileDataBase.keyListenerShiftDown);
		butInvert.addKeyListener(FileDataBase.keyListenerShiftDown);

		cmbActions.addKeyListener(FileDataBase.keyListenerShiftDown);
		butDoAction.addKeyListener(FileDataBase.keyListenerShiftDown);
		cbShowRenameLog.addKeyListener(FileDataBase.keyListenerShiftDown);

//end constructor:		
		JTextArea area = new JTextArea(3, 0); // add 'buttons' height
		area.setBackground(buttons.getBackground());
		area.setEditable(false);
		buttons.add(area);
		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));

		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents, BorderLayout.CENTER);

		var scrollPan = new JScrollPane(buttons, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPan, BorderLayout.SOUTH);

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(0, 0, t.width - 200, t.height - 200);
		setLocationRelativeTo(null);
		sorting(4);
		printCount(true, 0, null);// set count on start window
		setVisible(true);
	}

//0:"export to list", 1:"remove from table", 2:"copy/move files to", 3:"delete files", 4:"rename files", 5:"undo rename files", 6:"show rename log"	
	private void doAction(JComboBox<String> cmbActions) {
		var index = cmbActions.getSelectedIndex();

		if (index == 6) {
			saveShowRenameLog(3, true);
			return;
		}

		if (index < 0 || beans.isEmpty()) {
			return;
		}

		final int FILES_COPY_MOVE = 2;
		final int FILES_DELETE = 3;
		final int FILES_RENAME = 4;
		final int FILES_RENAME_UNDO = 5;

		boolean bFiles = index >= 2; // 2,3,4,5

		String stringFiles = bFiles ? " (files only)" : "";

		Set<Integer> setSelected = new HashSet<Integer>();
		printCount(false, bFiles ? 1 : 2, setSelected);

		if (setChecked.isEmpty() && setSelected.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No checked/selected rows found " + stringFiles);
			return;
		}

		String actionCaption = cmbActions.getItemAt(index);
		stringFiles += ": ";

		var bOneRename = ((index == FILES_RENAME || index == FILES_RENAME_UNDO) && setChecked.size() == 1
				&& setChecked.equals(setSelected));
		if (bOneRename) {
			if (oneRename(index == FILES_RENAME_UNDO)) {// for 'undo' return always true
				return;
			}
		}

		int confirm = bOneRename ? JOptionPane.YES_OPTION : JOptionPane.CANCEL_OPTION;
		if (!bOneRename) {
			var message = CommonLib.formatConfirmYesNoMessage("Action '" + actionCaption + "'.",
					"for CHECKED items" + stringFiles + setChecked.size(),
					"for SELECTED items" + stringFiles + setSelected.size(),
					(index == FILES_COPY_MOVE || index == FILES_DELETE) ? "This window be closed" : null);

			confirm = JOptionPane.showConfirmDialog(this, message, actionCaption, JOptionPane.YES_NO_CANCEL_OPTION);
		}
//!!! remains 'setChecked' only		
		if (confirm == JOptionPane.YES_OPTION) {
			if (setChecked.isEmpty()) {
				return;
			}
		} else if (confirm == JOptionPane.NO_OPTION) {
			setChecked.clear();
			if (setSelected.isEmpty()) {
				return;
			}
			setChecked.addAll(setSelected);
		} else {
			setChecked.clear();
			return;
		}

		if (index == 0) { // to list
			FileDataBase.beansToList(false, 2, setChecked, beans); // sort needed, because in 'set' no ordered as in
																	// table
			setChecked.clear();
			return;
		}

		if (index == 1) { // remove from table
			removeFromTable(setChecked);
			setChecked.clear();
			return;
		}

		List<Path> renameList = new ArrayList<>();// no sorted (as is in table) list of Paths; content the same
													// as in 'setChecked'
		int countError = setResultPaths(setChecked, renameList); // get files paths
		if (countError > 0) {
			JOptionPane.showMessageDialog(this, "Errors found (equals or null paths), excluded from result: "
					+ countError + CommonLib.NEW_LINE_UNIX + "Current result size: " + setChecked.size());
		}

		final String errorInfo = "Error define paths";
		String info = "";
		if (setChecked.isEmpty() || CommonLib.nullEmptySet(setCheckedPaths)
				|| setChecked.size() != setCheckedPaths.size() || renameList.size() != setCheckedPaths.size()) {
			info = errorInfo;
		}

		if (info.isEmpty() && index == FILES_RENAME) {
			info = doRename(errorInfo, renameList);
		}

		if (info.isEmpty() && index == FILES_RENAME_UNDO) {
			info = doRenameUndo(errorInfo);
		}

		setChecked.clear();

		if (!info.isEmpty()) {
			JOptionPane.showMessageDialog(this, info);
			return;
		}

		if (index == FILES_DELETE) {
			isCheckResult = Const.MR_DELETE;
			dispose();
			return;
		}

		if (index == FILES_COPY_MOVE) { // copy/move
			var tempPath = FileDataBase.getTempPathForCopyMove();
			if (!CommonLib.saveToFile(true, 1, CopyMove.DeleteIfExists_OLD_RENAME_TO_BAK, tempPath, null,
					getResultStringPaths())) {
				JOptionPane.showMessageDialog(this, "error of saving path list to " + tempPath);
				return;
			}
			isCheckResult = Const.MR_COPY_MOVE;
			dispose();
			return;
		}
	}

	/**
	 * Takes first integer from 'setChecked'; and renamed bean.get() to user typed
	 * name
	 * 
	 * @param undo if 'true' need undo; if 'false' renaming
	 * 
	 * @return 'true' if been pressed 'OK'; for 'undo' ALWAYS TRUE
	 */
	private boolean oneRename(boolean undo) {
		if (CommonLib.nullEmptySet(setChecked)) {
			return undo ? true : false;
		}
		int foundNumber = -1;
		for (var i : setChecked) { // need first integer
			if (i < 0 || i >= beans.size()) {
				return undo ? true : false;
			}
			foundNumber = i;
			break;
		}

		if (undo) {
			increaseRenameNumber(true);
			if (renameUndoItem(foundNumber, false, true)) {
				updating(true, getNeedSaveType());
			}
			return true;
		}

		String errorMessage = ""; // empty if not errors
		var b = beans.get(foundNumber);
		Path oldPath = b.binPath;
		var ar = ConverterBinFunc.dividePathToAll_Ext(0, oldPath.toString());
		String oldName = null;

		try { // try-finally
			oldName = oldPath.toFile().getName();
			if (CommonLib.nullEmptyString(oldName) || ar[0] == null) {
				errorMessage = "Error extract extension from " + oldPath;
				return false;
			}

			String ext = ar[2].isEmpty() ? "" : "." + ar[2];

			if (!ext.isEmpty()) {
				oldName = oldName.substring(0, oldName.length() - ext.length());
			}

			var inf = new InputTextGUI(this, true, "You can enter a new file name", oldName);
			// if user press 'OK', try renaming; else open show rename form without answer
			if (inf.result == null) {
				return false;
			}

			boolean result = true; // user press 'OK'

			var newName = CommonLib.getCorrectFileNameToRename(100, inf.result);

			if (newName.isEmpty() || newName.equals(oldName)) {
				errorMessage = "Result name is empty or no changed";
				return result;
			}

			Path newPath = oldPath.resolveSibling(newName.concat(ext));
			System.out.println("newPath: " + newPath);
			increaseRenameNumber(false);

			if (renameItem(foundNumber, oldName, oldPath, newPath)) {
				updating(true, getNeedSaveType());
			} else {
				errorMessage = "Error rename, sourse file:" + CommonLib.NEW_LINE_UNIX + oldPath
						+ CommonLib.NEW_LINE_UNIX + "to new file name:" + CommonLib.NEW_LINE_UNIX + newPath;
			}

			return result;
		} finally {
			if (!errorMessage.isEmpty()) {
				JOptionPane.showMessageDialog(this, errorMessage);
			}
		}
	}

	private int getNeedSaveType() {
		return cbShowRenameLog.isSelected() ? 2 : 1;
	}

//'setChecked' and 'setCheckedPaths' is filled and with equal sizes
//method return any string if error; or empty string if no need final message to user
	private String doRenameUndo(String errorInfo) { // 'errorInfo': Error define paths
		int totalSize = setChecked.size();
		if (totalSize == 0) {
			return errorInfo;
		}
		Set<Integer> tmp = new HashSet<Integer>(setChecked);
		for (int i : tmp) {
			if (!renameUndoItem(i, true, false)) {
				setChecked.remove(i);
			}
		}

		if (setChecked.isEmpty()) {
			return errorInfo;
		}

		var confirm = JOptionPane.showConfirmDialog(this, "Found files for UNDO renaming: " + setChecked.size() + " / "
				+ totalSize + CommonLib.NEW_LINE_UNIX + "Start UNDO renaming?");
		if (confirm != JOptionPane.YES_OPTION) {
			return "";
		}

		increaseRenameNumber(true);
		int count = 0;
		int sizeToUndo = setChecked.size();
		for (int i : setChecked) {
			if (renameUndoItem(i, false, false)) {
				count++;
			}
		}
		if (count > 0) {
			updating(true, getNeedSaveType());
		}
		return "Renamed: " + count + " from " + sizeToUndo + "; total been chosen: " + totalSize;
	}

	private boolean renameUndoItem(int i, boolean checkedOnly, boolean confirmBeforeUndo) {
		var b = beans.get(i);
		String getTwoName = b.getTwo();
		if (!getTwoName.contains(Const.OLD_NAME_START) || !getTwoName.endsWith(Const.OLD_NAME_END)) {
			return false;
		}

		if (checkedOnly) {
			return true;
		}

		getTwoName = getTwoName.substring(0, getTwoName.length() - Const.OLD_NAME_END.length());
		var pos = getTwoName.lastIndexOf(Const.OLD_NAME_START);
		if (pos < 0) {
			return false;
		}
		getTwoName = getTwoName.substring(pos + Const.OLD_NAME_START.length());

		var oldPath = b.binPath;
		if (oldPath == null) { // not must be so...
			return false;
		}

		var ar = CommonLib.extractFileNameExtensionOrNull(oldPath.toFile());
		if (ar == null) {
			return false;
		}
		Path newPath = oldPath.resolveSibling(getTwoName + ar[1]);

		if (confirmBeforeUndo && JOptionPane.showConfirmDialog(this,
				"UNDO rename. File:" + CommonLib.NEW_LINE_UNIX + oldPath + CommonLib.NEW_LINE_UNIX + "be renamed to:"
						+ CommonLib.NEW_LINE_UNIX + newPath + CommonLib.NEW_LINE_UNIX + "Continue?",
				"UNDO -> one file rename", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
			return false;
		}

		return renameItem(i, ar[0], oldPath, newPath);
	}

	private String doRename(final String errorInfo, List<Path> renameList) {
		var renameTable = new RenameTable(this, replaceNoSubstringError, renameList);
		if (renameTable.getIsCheckResult() == Const.MR_RENAME) {
			int chosenToRename = renameTable.getChosenToRename();
			var beansRename = renameTable.getBeans();

			if (chosenToRename <= 0 || CommonLib.nullEmptyList(beansRename)) {
				return errorInfo + " to rename";
			}

			increaseRenameNumber(false);

			int countRenameError = 0;
			int countRenamed = 0;

			for (var beanRen : beansRename) {
				if (setChecked.isEmpty()) {
					break;
				}

				if (!beanRen.check || CommonLib.nullEmptyString(beanRen.serviceStringOne)) {
					continue;
				}

				Path newPath = Path.of(beanRen.serviceStringOne);
				Path oldPath = beanRen.binPath;

				if (newPath != null && setCheckedPaths.contains(oldPath)) {
					int foundNumber = -1;

					for (var i : setChecked) { // find in table by 'binPath'
						if (beans.get(i).binPath.equals(oldPath)) {
							foundNumber = i;
							break;
						}
					}

					if (foundNumber >= 0) {
						if (renameItem(foundNumber, beanRen.getOne(), oldPath, newPath)) {
							countRenamed++;
						} else {
							countRenameError++;
						}

						setChecked.remove(foundNumber);
						setCheckedPaths.remove(oldPath);
					}

				}
			}

			if ((countRenamed + countRenameError) > 0) {
				updating(true, getNeedSaveType());
			}

			var sb = new StringBuilder();
			sb.append("Rename operation number: ").append(renameNumber).append(". Chosen to rename: ")
					.append(chosenToRename).append(CommonLib.NEW_LINE_UNIX + "Renamed count: ").append(countRenamed)
					.append(CommonLib.NEW_LINE_UNIX + "Error rename count: ").append(countRenameError);
			return sb.toString();
		}
		return "";
	}

	private void increaseRenameNumber(boolean undo) {
		renameNumber++;
		CommonLib.addLog(CommonLib.ADDLOG_SEP, false, renameLog);
		var s = undo ? " UNDO" : "";
		CommonLib.addLog(CommonLib.formatInt(renameNumber, 3, "<rename" + s + " operation number: ", ">"), false,
				renameLog);
	}

	/**
	 * @param foundNumber number items in 'beans'
	 * @param oldName     if null/empty, will be rename error
	 * @param oldPath     file to rename
	 * @param newPath     new name of file
	 * @return 'true' if file be renamed. Note: if old name equals new name,
	 *         'renameTo' may return 'true'
	 */
	private boolean renameItem(int foundNumber, String oldName, Path oldPath, Path newPath) {
		if (CommonLib.nullEmptyString(oldName)) {
			return false;
		}

		var res = oldPath.toFile().renameTo(newPath.toFile());
		addRenameLog(res, oldPath, newPath);

		var b = beans.get(foundNumber);
		var sb = new StringBuilder();
		sb.append(Const.BRACE_START).append(CommonLib.formatInt(b.serviceIntTwo, 3, null, null))
				.append(Const.BRACE_END_WITH_SPACE).append(Const.BRACE_START).append("REN-")
				.append(CommonLib.formatInt(renameNumber, 3, null, null)).append(res ? ":OK" : ":Error")
				.append(Const.BRACE_END_WITH_SPACE);

		if (res) {
//for 'undo' -> this string MUST ENDS on 'OLD_NAME_END'; and contain Const.OLD_NAME_START
			sb.append(Const.OLD_NAME_START).append(oldName).append(Const.OLD_NAME_END);
			b.binPath = newPath;
			b.setFour(newPath.toString(), "");
		}

		b.setTwo(sb.toString());
		return res;
	}

	private void addRenameLog(boolean res, Path oldPath, Path newPath) {
		renameLog.add(oldPath + " >> " + newPath + " <" + res + ">");
	}

	private void removeFromTable(Set<Integer> set) {
		if (CommonLib.nullEmptySet(set)) {
			return;
		}
		if (set.size() >= beans.size()) {
			if (JOptionPane.showConfirmDialog(this, "Do you want close this window?", "Remove all",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				dispose();
			}
			return;
		}

		final String errorMessage = "Removal error";
		try {
			myTable.clearSelection();
			for (int i : set) {
				var b = beans.get(i);
				if (b.serviceIntOne == CommonLib.SIGN_FILE) {
					countFiles--;
				} else if (b.serviceIntOne == CommonLib.SIGN_FOLDER) {
					countFolders--;
				} else { // not must be so
					throw new Exception(errorMessage);
				}
				beans.set(i, null);
			}

			beans.removeIf(Objects::isNull);
			if (countFiles < 0 || countFolders < 0 || (countFiles + countFolders) != beans.size()) {
				throw new Exception(errorMessage);
			}
			setNewStandardTitle();
			updating(true, 0);
			return;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage() + ", window be closed");
			dispose();
		}
	}

	synchronized private void dragging(int rowDest) {
		if (FileDataBase.dragging(rowDest, myTable, beans)) {
			updating(true, 0);
		}
	}

	/**
	 * Set standard window caption and sort type, updates table, writes checkCount
	 * 
	 * @param resetCheckNow     if 'true', in result label be written 'count'; else
	 *                          label depends on 'checkNow'
	 * @param needSaveRenameLog 0 (by default): no action; 1: save renameLog; 2:
	 *                          save and start saved file
	 */
	private void updating(boolean resetCheckNow, int needSaveRenameLog) {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount(resetCheckNow, 0, null);
		if (needSaveRenameLog >= 1 && needSaveRenameLog <= 2) {
			saveShowRenameLog(needSaveRenameLog, needSaveRenameLog == 2);
		}
	}

	/**
	 * @param needSaveRenameLog 0 (by default): no action; 1: save renameLog; 2:
	 *                          save and start saved file; 3: start file only
	 * @param messageOnEmptyLog if 'true' and renameLog is empty, will be showed
	 *                          message
	 */
	private void saveShowRenameLog(int needSaveRenameLog, boolean messageOnEmptyLog) {
		if (renameLog.isEmpty()) {
			if (messageOnEmptyLog) {
				JOptionPane.showMessageDialog(this, "Rename log not created yet");
			}
			return;
		}
		if (needSaveRenameLog == 1 || needSaveRenameLog == 2) {
			CommonLib.saveToFile(false, 0, CopyMove.DeleteIfExists_OLD_DELETE, pathSaveRenameLog, null, renameLog);
		}
		if (needSaveRenameLog == 2 || needSaveRenameLog == 3) {
			CommonLib.startProcess(false, pathSaveRenameLog);
		}
	}

	private void sorting(int columnIndex) {
		if (columnIndex < 0 || beans.size() < 2) {
			return;
		}

		int sortType = SortBeans.sortNoDefined;
		String sortCaption = "";
		boolean noDubleSort = false;// columns 0,4 and if's shift: always sort

//columns = { "Signature", "<number> <Rename result>", "Modified", "Path" };
		final String column = (columnIndex >= 1 && columnIndex <= 3) ? columns[columnIndex - 1] : columns[3];
		final String sortFileSize = " [file size]";
		final String sortNames = " [names]";

		if (FileDataBase.isShiftDown) {
			if (columnIndex == 0) {
				sortType = SortBeans.sortCheck_Shift_ThenFourName;
				sortCaption = "Checked (Shift) -> " + column + sortNames;
			} else if (columnIndex == 1) { // signature / file length
				if (lastSortType != SortBeans.sortOne_Shift_CheckOnly) {
					sortType = SortBeans.sortOne_Shift_CheckOnly;
					sortCaption = "Checked only (Shift) -> " + column;
				} else {
					sortType = SortBeans.sortServiceLong_Shift_CheckOnly;
					sortCaption = "Checked only (Shift) -> " + column + sortFileSize;
				}

			} else if (columnIndex == 2) {
				sortType = SortBeans.sortTwo_Shift_CheckOnly;
				sortCaption = "Checked only (Shift) -> " + column;
			} else if (columnIndex == 3) {
				sortType = SortBeans.sortThree_Shift_CheckOnly;
				sortCaption = "Checked only (Shift) -> " + column;
			} else {
				if (lastSortType != SortBeans.sortFourLowerCase_Shift_CheckOnly) {
					sortType = SortBeans.sortFourLowerCase_Shift_CheckOnly;
					sortCaption = "Checked only (Shift) -> " + column;
				} else {
					sortType = SortBeans.sortFourNameLowerCase_Shift_CheckOnly;
					sortCaption = "Checked only (Shift) -> " + column + sortNames;
				}
			}
		} else {
			if (columnIndex == 0) {
				sortType = SortBeans.sortCheck_ThenFour;
				sortCaption = "Checked -> " + column;
			} else if (columnIndex == 1) { // signature/size; noDubleSort no sense ('sortType' always different)
				if (lastSortType != SortBeans.sortOne) {
					sortType = SortBeans.sortOne;
					sortCaption = column;
				} else {
					sortType = SortBeans.sortServiceLong;
					sortCaption = column + sortFileSize;
				}
			} else if (columnIndex == 2) {
				sortType = SortBeans.sortTwo;
				sortCaption = column;
				noDubleSort = true;
			} else if (columnIndex == 3) {
				sortType = SortBeans.sortThree;
				sortCaption = column;
				noDubleSort = true;
			} else { // four; noDubleSort no sense ('sortType' always different)
				if (lastSortType != SortBeans.sortFourLowerCase) {
					sortType = SortBeans.sortFourLowerCase;
					sortCaption = column;
				} else {
					sortType = SortBeans.sortFourNameLowerCase;
					sortCaption = column + sortNames;
				}
			}
		}

		if (sortType == lastSortType && noDubleSort) {
			return;
		}
		lastSortType = sortType;
		setStandardTitle();
		var sortBeans = new SortBeans(sortType, sortCaption, beans);
		setNewTitle(standardTitle.concat(sortBeans.getAppendCaption()));
	}

	private void setNewTitle(String s) {
		if (!getTitle().equals(s)) {
			setTitle(s);
		}
	}

	private void setStandardTitle() {
		setNewTitle(standardTitle);
	}

	private void setNewStandardTitle() { // call after changing 'countFolders' or 'countFiles'
		standardTitle = "Paths list, size: " + beans.size() + " (folders: " + countFolders + ", files: " + countFiles
				+ ")";
		setStandardTitle();
	}

	/**
	 * @param resetCheckNow if 'true', in result label be written 'count'; else
	 *                      label depends on 'checkNow'
	 * @param needCount     0 (by default): no recount global 'Set' counters
	 *                      checked/selected; 1:count files only; 2:count all
	 * @param setSelected   if null, 'needCount' will be set to '0'; not will be
	 *                      filling 'setChecked' and 'setSelected'; else (if
	 *                      'needCount' != 0) BOTH be filled
	 * @return count of checked
	 */
	private int printCount(boolean resetCheckNow, int needCount, Set<Integer> setSelected) {
		int checkCount = 0;

		if (needCount <= 0 || needCount > 2 || setSelected == null) {
			needCount = 0;
		} else { // needCount == 1,2 and 'setSelected' not null
			setChecked.clear();
			setSelected.clear();
		}

		if (resetCheckNow) {
			checkNow = -1;
		}

		var arSelectedRows = myTable.getSelectedRows();
		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);

			if (needCount != 0) {
				for (int j = 0; j < arSelectedRows.length; j++) {
					if (arSelectedRows[j] == i) {
						if (needCount == 1 && b.serviceIntOne != CommonLib.SIGN_FILE) {
						} else {
							setSelected.add(i);
						}
						break;
					}
				}
			}

			if (!b.check) {
				continue;
			}

			checkCount++;
			if (needCount != 0) {
				if (needCount == 1 && b.serviceIntOne != CommonLib.SIGN_FILE) {
				} else {
					setChecked.add(i);
				}
			}
		}
// 0:no;1:filter(optional);2:files(if > 0);3:folders(if > 0); 4:all		
		var s = (checkNow < 0 || checkNow > CHECK_NOW_MAX) ? "count"
				: checkNow == 2 ? "files"
						: checkNow == 3 ? "folders"
								: checkNow == CHECK_NOW_MAX ? "all" : checkNow == 1 ? "filter" : "no";

		checkInfo.setText(s.concat(": ") + checkCount);
		return checkCount;
	}

	private int[] initBeans(List<File> listFullPaths) {
		int[] countFilesFolders = new int[2];
		Arrays.fill(countFilesFolders, 0);
		beans = new ArrayList<MyBean>();

		if (CommonLib.nullEmptyList(listFullPaths)) {
			return countFilesFolders;
		}

		Set<String> equalsPath = new HashSet<String>();
		int count = 0;
		int countForInfCount = 0;
		if (needCalculateCrc) {
			System.out.println("");
			System.out.println("start calculate of crc, list size: " + listFullPaths.size());
			System.out.println("");
		}

		for (var file : listFullPaths) {
			try {
				if (file == null || !file.exists()) {
					throw new IllegalArgumentException("file don't exist");
				}

				var bIsDirectory = file.isDirectory();
				String signature = bIsDirectory ? Const.DIRECTORY_ALIAS : "";

				long fileLength = 0;

				if (!bIsDirectory) {
					if (needCalculateCrc) {
						var crc = new CalcCrc(1, "", file.toPath());
						signature = crc.getCrcResult() <= 0 ? Const.CRC_ERROR_ALIAS : crc.getBinItemSignature();
						crc = null;
						countForInfCount++;
						if ((countForInfCount & 1023) == 0) {
							System.out.println("...processed files:" + countForInfCount);
						}
					} else {
						signature = Const.CRC_NO_CALCULATED_ALIAS;
					}

					fileLength = file.length();
					signature += " " + CommonLib.bytesToKBMB(false, 0, fileLength);
				}

				if (!equalsPath.add(file.toString().toLowerCase())) {
					throw new IllegalArgumentException("duplicate path");
				}

				if (bIsDirectory) {
					countFilesFolders[1]++;
				} else {
					countFilesFolders[0]++;
				}
//!!! bean.fourApp is empty, means that where will be 'filter' as 'ends of path/name', will be found 'with extension'
//!!! after renaming, bean.fourApp will be set as "", and bean.four will be set as bean.binPath.toString
				var sb = new StringBuilder();
				count++;
				sb.append(CommonLib.formatInt(count, 3, Const.BRACE_START, Const.BRACE_END_WITH_SPACE));

				var bean = new MyBean(signature, sb.toString(), dateModifiedToString(file.lastModified()),
						file.toString(), "");
				bean.serviceIntOne = bIsDirectory ? CommonLib.SIGN_FOLDER : CommonLib.SIGN_FILE;
				bean.serviceIntTwo = count;
				bean.serviceLong = fileLength; // for sort by file length
				bean.binPath = file.toPath();
				beans.add(bean);

			} catch (Exception e) {
				System.out.println("error, file: " + file + " >> '" + e.getMessage() + "'");
			}
		}
		return countFilesFolders;
	}

	@Override
	public Integer call() throws Exception {// 'while' need for 'JFrame', not for 'JDialog'
		while (isCheckResult == Const.MR_NO_CHOOSED) {
			Thread.sleep(1024);
		}
		return isCheckResult;
	}

	List<String> getResultStringPaths() {
		return CommonLib.getListFromPathsSet(2, setCheckedPaths);
	}

	/**
	 * Initialization global 'setCheckedPaths' from 'set'
	 * 
	 * @param set        not must be null/empty; result 'setCheckedPaths' will be
	 *                   the same size as 'set'; if 'error' -> from 'set' be removed
	 *                   that items
	 * @param renameList no sorted list, need for remaining
	 * @return count REMOVED items
	 */
	private int setResultPaths(Set<Integer> set, List<Path> renameList) {
		setCheckedPaths = new HashSet<Path>();
		Set<Integer> tempSet = new HashSet<Integer>();
		tempSet.addAll(set);
		set.clear();
		renameList.clear();
		for (int i : tempSet) {
			if (i >= 0 && i < beans.size()) {
				var path = beans.get(i).binPath;
				if (path != null) {
					if (setCheckedPaths.add(path)) {
						set.add(i);
						renameList.add(path);
						continue;
					}
				}
			}
		}
		return tempSet.size() - set.size();
	}
}
