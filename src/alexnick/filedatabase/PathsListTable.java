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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
	private JLabel checkInfo;

	private int isCheckResult = Const.MR_NO_CHOOSED;
	private List<MyBean> beans;

	private int countFolders; // RECOUNT, if was a removing
	private int countFiles;

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

	private boolean replaceNoSubstringError;
	private int renameNumber = 0; // increases before rename/undo

	private List<String> renameLog = new ArrayList<String>();
	private final Path pathSaveRenameLog;
	private JCheckBox cbShowRenameLog;

	private JTextField tfFindSubstrings;
	private String[] cmbCheckItemsApp = new String[] { "only", "add", "sub", "onlyCase", "addCase", "subCase", "TEST" };// TEST_is_last
	private String[] cmbCheckItemsAppPosition = new String[] { "any", "starts", "ends" };

	private int[] lastIndex = { 0, 0, 0 }; // cmbCheckItems, cmbCheckItemsApp, cmbFindPosition indexes

	private String[] cmbCheckItems = new String[] { "all", "no", "invert", "by Signature", "by Number,result",
			"by Modified", "by Path", "by Name", "textSearch" }; // textSearch LAST index

// append const indexes from 'cmbCheckItems'; enabled together cmbCheckItemsApp, cmbFindPosition
	private final int cmbAppEnabStartIndex = 3;
	private final int cmbAppEnabEndIndex = 7;
	private final int textSearchIndex = 8; // cmbApp enabled, but cmbAppPos not enabled

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

//by default, set SINGLE_SELECTION, and there are other options
		var singleOnly = options.contains(Const.OPTIONS_PATHSLIST_SINGLE_ONLY);// no 'multiSelect'
		var startSetMulti = options.contains(Const.OPTIONS_PATHSLIST_SET_MULTI);// by default,'multiSelect'
		if (singleOnly && startSetMulti) {
			singleOnly = false;
			startSetMulti = false;
		}
		myTable = new BeansFourTableDefault(
				startSetMulti ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION,
				true, false, true, columns[0], columns[1], columns[2], columns[3], beans);

		initComponents(singleOnly, startSetMulti, options);

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(0, 0, t.width - 200, t.height - 200);
		setLocationRelativeTo(null);
		sorting(4);
		printCount(null, null, 0, null);// set count on start window
		setVisible(true);
	}

//INIT COMPONENTS	
	private void initComponents(boolean singleOnly, boolean startSetMulti, String options) {
		myTable.setDropTarget(new DropTarget() {
			private static final long serialVersionUID = 1L;

			@Override
			public synchronized void drop(DropTargetDropEvent dtde) {
				Point point = dtde.getLocation();
				dragging(myTable.rowAtPoint(point));
			}
		});

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
						printCount(null, null, 0, null);
					}
					return;
				}
				if (e.getClickCount() == 2) {
					FileDataBase.openDirectory(1, FileDataBase.isShiftDown, myTable, beans);
				}
			}
		});

// FILLING JPANEL	
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
			select.addActionListener(e -> checkSelect(select.isSelected()));

			buttons.add(cmbSingleMulti);
			buttons.add(select);

			cmbSingleMulti.addKeyListener(FileDataBase.keyListenerShiftDown);
			select.addKeyListener(FileDataBase.keyListenerShiftDown);
		}

		var cmbChecking = new JComboBox<String>(cmbCheckItems);
		var cmbCheckingApp = new JComboBox<String>(cmbCheckItemsApp);
		var cmbCheckingAppPosition = new JComboBox<>(cmbCheckItemsAppPosition);

		cmbCheckingApp.setEnabled(false);
		cmbCheckingApp.setToolTipText(Const.cmbOnlyAddSubToolTip);
		cmbCheckingAppPosition.setEnabled(false);

		ActionListener butCheckActionListener = e -> checking(cmbChecking.getSelectedIndex(),
				cmbCheckingApp.getSelectedIndex(), cmbCheckingAppPosition.getSelectedIndex());

		cmbChecking.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				var index = cmbChecking.getSelectedIndex();
				var enabApp = index >= cmbAppEnabStartIndex && index <= cmbAppEnabEndIndex;
				cmbCheckingAppPosition.setEnabled(enabApp); // 'position' no enabled for textSearch

				enabApp = enabApp || index == textSearchIndex;
				cmbCheckingApp.setEnabled(enabApp);
				tfFindSubstrings.setEnabled(enabApp);
			}
		});

		tfFindSubstrings = new JTextField(FileDataBase.sizeTextField);
		tfFindSubstrings.addActionListener(butCheckActionListener);
		tfFindSubstrings.setToolTipText(Const.textFieldBinFolderToolTip);
		tfFindSubstrings.setEnabled(false);

		var butCheck = new JButton("set");
		butCheck.addActionListener(butCheckActionListener);

		checkInfo = new JLabel();
		printCount(null, null, 0, null); // check on show window

		cbShowRenameLog = new JCheckBox("log");
		cbShowRenameLog.setEnabled(false);
		cbShowRenameLog.setToolTipText("show rename log after rename/undo");

		JComboBox<String> cmbActions = new JComboBox<>(
				new String[] { "export to list", "remove from table", "copy/move files to", "delete files",
						"rename files", "undo rename files", "show rename log", "openWithFiles", "openWithFolders" });
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

//ADDING

		buttons.add(cmbCheckingAppPosition);
		buttons.add(cmbChecking);
		buttons.add(cmbCheckingApp);

		buttons.add(tfFindSubstrings);
		buttons.add(butCheck);

		buttons.add(checkInfo);

		buttons.add(cmbActions);
		buttons.add(butDoAction);
		buttons.add(cbShowRenameLog);

		JTextArea area = new JTextArea(3, 0); // add 'buttons' height
		area.setBackground(buttons.getBackground());
		area.setEditable(false);
		buttons.add(area);
		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));

// end constructor:
		Box contents = new Box(BoxLayout.Y_AXIS);
		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents, BorderLayout.CENTER);

		var scrollPan = new JScrollPane(buttons, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPan, BorderLayout.SOUTH);

//SHIFT DOWN AND KEYADAPTER	
		var keyAdapterEnter = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					checking(cmbChecking.getSelectedIndex(), cmbCheckingApp.getSelectedIndex(),
							cmbCheckingAppPosition.getSelectedIndex());
				}
			}
		};

		myTable.addKeyListener(FileDataBase.keyListenerShiftDown);
		cbDrag.addKeyListener(FileDataBase.keyListenerShiftDown);

		cmbCheckingAppPosition.addKeyListener(keyAdapterEnter);
		cmbCheckingAppPosition.addKeyListener(FileDataBase.keyListenerShiftDown);

		cmbChecking.addKeyListener(keyAdapterEnter);
		cmbChecking.addKeyListener(FileDataBase.keyListenerShiftDown);

		cmbCheckingApp.addKeyListener(keyAdapterEnter);
		cmbCheckingApp.addKeyListener(FileDataBase.keyListenerShiftDown);

		tfFindSubstrings.addKeyListener(FileDataBase.keyListenerShiftDown);
		butCheck.addKeyListener(FileDataBase.keyListenerShiftDown);

		cmbActions.addKeyListener(FileDataBase.keyListenerShiftDown);
		butDoAction.addKeyListener(FileDataBase.keyListenerShiftDown);
		cbShowRenameLog.addKeyListener(FileDataBase.keyListenerShiftDown);
	}

//one, cmpCheckItems:
//0:"all", 1:"no", 2:"invert", 3:"by Signature", 4:"by Number,result",5:"by Modified", 6:"by Path", 7:"by Name", 8:"textSearch"
//two, cmpCheckItemsApp:0:"only", 1:"add", 2:"sub" , 3:"onlyCase", 4:"addCase", 5:"subCase" , 6: "TEST"
//three:appPos:0:"any", 1:"starts", 2:"ends"	
	private void checking(int indexOne, int indexTwo, int indexThree) {
		if (beans.isEmpty() || indexOne < 0 || indexOne >= cmbCheckItems.length) {
			return;
		}

//defined: cmbAppEnabStartIndex = 3; cmbAppEnabEndIndex = 7; textSearchIndex = 8;
		var bNeedFilterAppPos = (indexOne >= cmbAppEnabStartIndex && indexOne <= cmbAppEnabEndIndex);// by columns only
		var bNeedFilterApp = bNeedFilterAppPos || indexOne == textSearchIndex; // TfFindSubstrings enabled too
		int indexTwoResult = indexTwo;

		if (indexTwo < 0 || indexTwo >= cmbCheckItemsApp.length) {
			if (bNeedFilterApp) {
				return;
			}

			indexTwo = 0; // for 'all','no','invert' no matter, but in array write correct number
		}

		if (indexThree < 0 || indexThree >= cmbCheckItemsAppPosition.length) {
			if (bNeedFilterAppPos) {
				return;
			}

			indexThree = 0; // for 'all','no','invert' no matters, but in array writes correct number
		}

		boolean test = bNeedFilterApp && indexTwo == 6;// TEST

		if (test && (indexOne == textSearchIndex)) { // TEST text search
			int[] addedInfo = FileDataBase.testTextSearchOrNull(beans);
			if (addedInfo == null) {
				return;
			}

			int[] index = { indexOne, indexTwo, indexThree };
			updating(index, addedInfo, 0);
			return;
		}

		boolean toLowerCase = indexTwo <= 2;
		if (!toLowerCase) {
			indexTwoResult -= 3; // 3..5 -> 0..2
		}

		int[] addedInfo = new int[2]; // plus and minus info
		addedInfo[0] = 0;
		addedInfo[1] = 0;

		boolean bAdd = bNeedFilterApp && indexTwoResult == 1;
		boolean bSub = !bAdd && bNeedFilterApp && indexTwoResult == 2;

		List<String> substringsAND = null;
		List<String> substringsOr = null;

		if (bNeedFilterApp) {
			substringsOr = new ArrayList<String>();
			substringsAND = FileDataBase.getSubstringsAND_DivideByOR_NullIfError(true, toLowerCase,
					tfFindSubstrings.getText(), substringsOr);

			if (substringsOr.isEmpty()) {
				updating(lastIndex, null, 0);
				return;
			}

			if (test) {
				FileDataBase.testInfo(this, substringsAND, substringsOr);
				return;
			}
		}

		for (var b : beans) {
			var res = false; // false by default

			if (bNeedFilterApp) { // by column, textSearch
				if ((b.check && bAdd) || (!b.check && bSub)) {
					continue;
				}

				if (indexOne == textSearchIndex) { // text search

					var one = b.getOne();
					if (one.startsWith(Const.BRACE_TEST_ERROR_FULL)) {
						// res == false
					} else {
						var count = FileDataBase.getTextSearchResult(false, toLowerCase ? 1 : 0, b.getFour(false, true),
								substringsAND, substringsOr);
						if (count > 0) {
							res = true;
						} else if (count == -1) {
							b.setOne(Const.BRACE_TEST_ERROR_FULL + one);
						}
					}

				} else { // by name (indexOne == 7) or by column 3..6->1..4; find' not null here
					res = true;

					String string = indexOne == 7 ? b.getNameFromFour(false)
							: b.getStringByColumnNumberOrEmpty(indexOne - 2);

					if (CommonLib.notNullEmptyList(substringsAND)) { // first finding by AND, if defined
						res = FileDataBase.findSubStringsInString(indexThree, toLowerCase ? 1 : 0, string,
								substringsAND);
					}

					if (res) { // substringsOr not null/empty
						res = FileDataBase.findSubStringsInString(indexThree, toLowerCase ? 1 : 0, string,
								substringsOr);
					}
				}

				if (bSub) {
					res = !res;
				}

			} else if (indexOne == 0) { // all
				res = true;
			} else if (indexOne == 1) { // no
				res = false;
			} else if (indexOne == 2) { // invert
				res = !b.check;
			} else {
				continue; // must not be so...
			}

			if (b.check != res) {
				if (res) {
					addedInfo[0]++;
				} else {
					addedInfo[1]++;
				}
				b.check = res;
			}
		}

		int[] index = { indexOne, indexTwo, indexThree };
		updating(index, addedInfo, 0);

	}

	private void checkSelect(boolean selected) {
		var rows = myTable.getSelectedRows();
		if (rows.length == 0) {
			return;
		}
		var needUpdate = false;
		for (var row : rows) {
			if (beans.get(row).check == selected) {
				continue;
			}
			needUpdate = true;
			beans.get(row).check = selected;
		}
		if (needUpdate) {
			updating(null, null, 0);
		}
	}

// 0:"export to list", 1:"remove from table", 2:"copy/move files to", 3:"delete files", 4:"rename files", 5:"undo rename files", 
// 6:"show rename log", 7:"openWithFiles", 8:"openWithFolders"
	private void doAction(JComboBox<String> cmbActions) {
		var index = cmbActions.getSelectedIndex();

		if (index == 6) {
			saveShowRenameLog(3, true);
			return;
		}

		if (index < 0 || index > 8 || beans.isEmpty()) {
			return;
		}

		final int FILES_COPY_MOVE = 2;
		final int FILES_DELETE = 3;
		final int FILES_RENAME = 4;
		final int FILES_RENAME_UNDO = 5;

//needCount 1:count files only; 2: count folders 3:count all
		final int needCount = index == 8 ? 2 : index <= 1 ? 3 : 1;// for index 2,3,4,5,7

		String stringFiles = needCount == 1 ? " (files only)" : needCount == 2 ? " (folders only)" : "";

		Set<Integer> setSelected = new HashSet<Integer>();
		printCount(lastIndex, null, needCount, setSelected);

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
					(index == FILES_COPY_MOVE || index == FILES_DELETE) ? "This window will be closed" : null);

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

		if (index == 7 || index == 8) { // 7:"openWithFiles", 8:"openWithFolders"
			// !!!'needCount' equals 'typeInfo' in 'FileDataBase.toCommandLine'
			// first parameter is null, because need 'JDialog'
			FileDataBase.toCommandLine(null, true, needCount, 1, setChecked, beans);
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
			if (!CommonLib.saveToFile(false, true, 1, CopyMove.DeleteIfExists_OLD_RENAME_TO_BAK, tempPath, null,
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
				updating(null, null, getNeedSaveType());
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
				updating(null, null, getNeedSaveType());
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
			updating(null, null, getNeedSaveType());
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
				updating(null, null, getNeedSaveType());
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
			updating(null, null, 0);
			return;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage() + ", window be closed");
			dispose();
		}
	}

	synchronized private void dragging(int rowDest) {
		if (FileDataBase.dragging(rowDest, myTable, beans)) {
			updating(null, null, 0);
		}
	}

	/**
	 * Set standard window caption and sort type, updates table, writes checkCount
	 * 
	 * @param index
	 * @param needSaveRenameLog 0 (by default): no action; 1: save renameLog; 2:
	 *                          save and start saved file
	 */
	private void updating(int[] index, int[] addedInfo, int needSaveRenameLog) {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount(index, addedInfo, 0, null);

		if (needSaveRenameLog >= 1 && needSaveRenameLog <= 2) {
			saveShowRenameLog(needSaveRenameLog, needSaveRenameLog == 2);
		}
	}

	/**
	 * @param index       must be as indexes in 'cmbCheck's
	 * @param addedInfo   if not null and length == 2, be added info to label
	 * @param needCount   0 (by default): no recount global 'Set' counters
	 *                    checked/selected;<br>
	 *                    1:count files only<br>
	 *                    2: count folders only<br>
	 *                    3:count all
	 * @param setSelected if null, 'needCount' will be set to '0'; not will be
	 *                    filling 'setChecked' and 'setSelected'; else (if
	 *                    'needCount' != 0) BOTH be filled
	 * @return count of checked
	 */
	private int printCount(int[] index, int[] addedInfo, int needCount, Set<Integer> setSelected) {
		lastIndex = index;

		int checkCount = 0;

		if (needCount <= 0 || needCount > 3 || setSelected == null) {
			needCount = 0;
		} else { // needCount == 1,2,3 and 'setSelected' not null
			setChecked.clear();
			setSelected.clear();
		}

		int[] arSelectedRows = myTable.getSelectedRows();

		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);

			if (needCount != 0) {
				for (int j = 0; j < arSelectedRows.length; j++) {
					if (arSelectedRows[j] == i) {

						if (needCount == 1 && b.serviceIntOne != CommonLib.SIGN_FILE) { // need files
						} else if (needCount == 2 && b.serviceIntOne != CommonLib.SIGN_FOLDER) { // need folders
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

				if (needCount == 1 && b.serviceIntOne != CommonLib.SIGN_FILE) { // need files
				} else if (needCount == 2 && b.serviceIntOne != CommonLib.SIGN_FOLDER) { // need folders
				} else {
					setChecked.add(i);
				}
			}
		}

		var sb = new StringBuilder();
		sb.append((index == null) ? "count" : cmbCheckItems[index[0]]);

		if (index != null && ((index[0] == textSearchIndex)
				|| (index[0] >= cmbAppEnabStartIndex && index[0] <= cmbAppEnabEndIndex))) {
			sb.append(" ").append(cmbCheckItemsApp[index[1]]);
		}

		sb.append(": ").append(checkCount);

		if (addedInfo != null && addedInfo.length == 2) {
			sb.append(" (+").append(addedInfo[0]).append("/-").append(addedInfo[1]).append(")");
		}

		checkInfo.setText(sb.toString());

		return checkCount;
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
			CommonLib.saveToFile(false, false, 0, CopyMove.DeleteIfExists_OLD_DELETE, pathSaveRenameLog, null,
					renameLog);
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
