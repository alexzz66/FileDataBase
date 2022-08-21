package alexnick.filedatabase;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import alexnick.CommonLib;

public class RenameTable extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final int defaultCmbLengthLimitIndex = 6;

	final private static String[] columns = { "Current ('old') name", "New name", "Rename info, old/new length ",
			"Path" };

	final private static String[] arRenameResults = { "", "<...stop on error>", "<ERROR>", "<OK>", "<NAME_NO_CHANGED>",
			"<REGISTER_CHANGED_ONLY>", "<FILE_CHECK_ERROR>" };

//!!!indexes defines position in 'arRenameResults'
	private static final int INDEX_RENAME_RESULT_NO_DEFINED = 0;
	private static final int INDEX_RENAME_RESULT_STOP_ON_ERROR = 1;
	private static final int INDEX_RENAME_RESULT_ERROR = 2;
	private static final int INDEX_RENAME_RESULT_OK = 3;
	private static final int INDEX_RENAME_RESULT_NAME_NO_CHANGED = 4;
	private static final int INDEX_RENAME_RESULT_REGISTER_CHANGED_ONLY = 5;
	private static final int INDEX_RENAME_RESULT_FILE_CHECK_ERROR = 6;

	private int isCheckResult = Const.MR_NO_CHOOSED;

	int getIsCheckResult() {
		return isCheckResult;
	}

	private int chosenToRename = 0; // init after result 'MR_RENAME'

	int getChosenToRename() {
		return chosenToRename;
	}

	private String standardTitle;
	private BeansFourTableDefault myTable;

//!!! if been removing elements from 'beans', need re init of 'namesNumbersNoSort', 'namesOldNoSort', 'namesList', namesNeed
	private List<MyBean> beans;

	List<MyBean> getBeans() {
		return beans;
	}

	// ===variables for 'names' rename===
	private List<String> namesNumbersNoSort = null; // init with 'beans', numbers only
	private List<String> namesOldNoSort = null; // init with 'beans', without numbers

	// if 'namesNeed' is 'true', defined 'namesList' with size equals 'beans'
	private boolean namesNeed = false;
	private List<String> namesList = null;

//components for renaming:		
	private JTextField tfPrefixStart;
	private JComboBox<String> cmbNumbers;
	private JTextField tfNumbersFrom;
	private JTextField tfNumbersSeparator;

	private JComboBox<String> cmbNameWork;
	private JTextField tfNameWorkOne;
	private JTextField tfNameWorkTwo;
	private JTextField tfNameWorkThree;

	private JComboBox<String> cmbRegister;

	private JTextField tfPostfix;
	private JComboBox<String> cmbPostFixListOrRandom;

	private JComboBox<String> cmbLengthLimit;
	private JCheckBox cbStopOnError;

	private int startNumbering = Integer.MAX_VALUE;
	private boolean replaceNoSubstringError;
	private boolean initFrameFinished;
	volatile private int lastSortType = SortBeans.sortNoDefined;

	ActionListener autoApply = (e -> renaiming(false));

	public RenameTable(JFrame frame, boolean replaceNoSubstringError, List<Path> paths) {
		super(frame, true);
		this.initFrameFinished = false; // to avoid call 'autoApply' while creating this frame
		this.replaceNoSubstringError = replaceNoSubstringError;

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (isCheckResult == Const.MR_NO_CHOOSED) {
					isCheckResult = Const.MR_CANCEL;
				}
			}
		});

		cbStopOnError = new JCheckBox("stopOnError");
		cbStopOnError.setToolTipText("stop filling column 'new name' on error");

		beans = new ArrayList<MyBean>();
		if (!initBeans(paths)) { // 'false' if beans is empty
			dispose();
			return;
		}

		standardTitle = "Files to rename: " + beans.size();
		setStandardTitle();

		Box contents = new Box(BoxLayout.Y_AXIS);
		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, false, false, false, columns[0],
				columns[1], columns[2], columns[3], beans);

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
					return;
				}
				if (e.getClickCount() == 2) {
					FileDataBase.openDirectory(1, false, myTable, beans);
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

		buttons.add(cbDrag);

//add renaming components:	
		initComponentsForRenaming();

		buttons.add(tfPrefixStart);
		buttons.add(cmbNumbers);
		buttons.add(tfNumbersFrom);
		buttons.add(tfNumbersSeparator);

		buttons.add(cmbNameWork);
		buttons.add(tfNameWorkOne);
		buttons.add(tfNameWorkTwo);
		buttons.add(tfNameWorkThree);
		buttons.add(cmbRegister);

		buttons.add(tfPostfix);
		buttons.add(cmbPostFixListOrRandom);
		buttons.add(cmbLengthLimit);

		JButton butSet = new JButton("set");
		butSet.setToolTipText("apply settings; fill column 'new name'");
		butSet.addActionListener(autoApply);

		buttons.add(cbStopOnError);
		buttons.add(butSet);

		JButton butRename = new JButton("Rename");
		butRename.setToolTipText("apply settings with a checking suitable for renaming files");
		butRename.addActionListener(e -> doRename());
		buttons.add(butRename);

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
		this.setBounds(0, 0, t.width - 100, t.height - 50);

		setLocationRelativeTo(null);
		initFrameFinished = true;
		renaiming(false);
		setVisible(true);
	}

	private void doRename() {
		var checkCount = renaiming(true);
		if (checkCount == 0) {
			JOptionPane.showMessageDialog(this, "No valid names found to rename");
			return;
		}
		var message = CommonLib
				.formatConfirmYesNoMessage(
						"Only CHECKED files (" + checkCount + ") in the table will be renamed."
								+ CommonLib.NEW_LINE_UNIX + "New name will be taken from the column 'New name'."
								+ CommonLib.NEW_LINE_UNIX + "Choose action:",
						"rename files", "save to list only", null);

		var confirm = JOptionPane.showConfirmDialog(this, message, "Rename", JOptionPane.YES_NO_CANCEL_OPTION);
		if (confirm == JOptionPane.NO_OPTION) {
			FileDataBase.beansToList(false, 0, null, beans);
			return;
		}
		if (confirm == JOptionPane.YES_OPTION) {
			chosenToRename = checkCount;
			isCheckResult = Const.MR_RENAME;
			dispose();
		}
	}

	/**
	 * Apply components settings
	 * 
	 * @param checkForRename if 'true', before 'doRename', be checked existing files
	 *                       and be set flags in table if can rename
	 * @return if 'checkForRename', filling b.serviceString and returns checked
	 *         count
	 */
	private int renaiming(boolean checkForRename) {
		if (!initFrameFinished || beans.isEmpty()) { // 'false' on frame init, no created components
			return 0;
		}

		int cmbIndex = cmbPostFixListOrRandom.getSelectedIndex();
		if (cmbIndex >= cmbPostFixListOrRandom.getItemCount() - 1 && !namesNeed) { // names
			var size = beans.size();
			var correctLists = namesNumbersNoSort.size() == size && namesOldNoSort.size() == size;
			if (correctLists) {

				List<String> columnNewNames = new ArrayList<String>();
				Map<Integer, String> map = new TreeMap<Integer, String>();
				for (var b : beans) {
					map.put(b.serviceIntOne, b.getTwo());
				}

				for (var e : map.entrySet()) {
					columnNewNames.add(e.getValue());
				}

				var nameFrame = new NamesFrame(this, "Names list, count: " + size, columnNewNames, namesNumbersNoSort,
						namesOldNoSort, namesList);
				correctLists = nameFrame.getIsCheckResult() == Const.MR_OK;
				if (correctLists) {
					namesList = nameFrame.getNamesList(); // will be correct list
					namesNeed = true;
				}
			}

			if (!correctLists) {
				cmbPostFixListOrRandom.setSelectedIndex(0);
				return 0;
			}
		}

		Set<String> newPathsInLowerCaseSet = checkForRename ? new HashSet<>() : null;
		int checkCount = 0;
		boolean stopError = false;

		try { // finally
			if (checkForRename) {
				for (var b : beans) {
					b.check = false;
					b.serviceStringOne = null;
				}
			}

			int indexResult = INDEX_RENAME_RESULT_NO_DEFINED;

			for (int i = 0; i < beans.size(); i++) { // 'b.one' -> 'b.two'; resultTo 'b.three'
				var b = beans.get(i);
				if (stopError) {
					b.setTwo("");
					indexResult = INDEX_RENAME_RESULT_STOP_ON_ERROR;
				} else {
					var newName = getNewNameOrEmpty(i == 0, b.serviceIntOne, b.getOne(), b.binPath);

					if (cbStopOnError.isSelected() && newName.isEmpty()) {
						stopError = true;
					}

					b.setTwo(newName);
					if (newName.isEmpty()) {
						indexResult = INDEX_RENAME_RESULT_ERROR;
					} else {
						indexResult = newName.equals(b.getOne()) ? INDEX_RENAME_RESULT_NAME_NO_CHANGED
								: newName.equalsIgnoreCase(b.getOne()) ? INDEX_RENAME_RESULT_REGISTER_CHANGED_ONLY
										: INDEX_RENAME_RESULT_OK;

						if (checkForRename && (indexResult == INDEX_RENAME_RESULT_REGISTER_CHANGED_ONLY
								|| indexResult == INDEX_RENAME_RESULT_OK)) {
							var path = checkForRenameFile(indexResult == INDEX_RENAME_RESULT_REGISTER_CHANGED_ONLY, i,
									newPathsInLowerCaseSet);
							if (path == null) {
								indexResult = INDEX_RENAME_RESULT_FILE_CHECK_ERROR;
								if (cbStopOnError.isSelected()) {
									stopError = true;
								}
							} else {
								b.check = true;
								b.serviceStringOne = path.toString();
								checkCount++;
							}
						}
					}
				}

				var three = b.getThree();
				var pos = three.indexOf(Const.BRACE_END_WITH_SPACE);
				if (pos > 0) {
					three = three.substring(0, pos + Const.BRACE_END_WITH_SPACE.length());
				}
				b.setThree(three + arRenameResults[indexResult]);
			}

		} finally {
			updating();
		}
		return checkCount;
	}

//theSameFile set as true for 'register change only', must be exist
	private Path checkForRenameFile(boolean theSameFileMustExists, int index, Set<String> newPathsInLowerCaseSet) {
		try {
			var b = beans.get(index);
			var path = b.binPath;
			var file = path.toFile();
			if (!file.exists() || file.isDirectory()) { // source file
				return null;
			}
			var newName = b.getTwo();
			if (CommonLib.nullEmptyString(newName)) {
				return null;
			}

			newName += b.getFourApp(false, false);
			if (newName.isEmpty()) {
				return null;
			}

			Path resultPath = path.resolveSibling(newName);

			if (resultPath.toFile().exists() != theSameFileMustExists) { // dest file
				return null;
			}

			if (newPathsInLowerCaseSet.add(resultPath.toString().toLowerCase())) {
				return resultPath;
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Get new name from 'name' as defined components settings
	 * 
	 * @param bFirstItem
	 * @param noSortIndex
	 * @param name        source string, if null/empty -> no result (returns empty)
	 * @param path        full path to 'name'
	 * @return empty, if error; else new name
	 */
	private String getNewNameOrEmpty(final boolean bFirstItem, final int noSortIndex, final String name, Path path) {
		if (CommonLib.nullEmptyString(name)) {
			return "";
		}
		var sb = new StringBuilder();
//PREFIX >>
		CommonLib.appendNotNullEmpty(tfPrefixStart.getText(), sb);
		String result = "";
		try {

//NUMBERING >>			
//0:"noNumbering/parent", 1: "++numbering'0',from", 2:"++numbering'00',from", 3:"++numbering'000',from",
//4:"--numbering'0',from", 5:"--numbering'00',from", 6:"--numbering'000',from", 7: "extractParent"				
			var index = cmbNumbers.getSelectedIndex();
			if (index >= 1 && index <= 6) {
				startNumbering = bFirstItem ? Integer.valueOf(tfNumbersFrom.getText())
						: (index <= 3) ? startNumbering + 1 : startNumbering - 1;
				sb.append(CommonLib.formatInt(startNumbering,
						(index == 2 || index == 5) ? 2 : (index == 3 || index == 6) ? 3 : 1, null, null));
			} else if (index == 7) {
				var p = path.getParent();
				if (p == null) {
					return "";
				}
				p = p.getFileName();
				if (p == null) {
					return "";
				}
				sb.append(p);
			}

			if (index >= 1) {
				CommonLib.appendNotNullEmpty(tfNumbersSeparator.getText(), sb);
			}

//CHANGE NAME >>
			index = cmbNameWork.getSelectedIndex();
			if (index == 9) { // change to date modification
				sb.append(CommonLib.getFormatDateForFileName(path.toFile().lastModified()));
			} else if (index > 0) {
				if (!setChangedNameOrFalseOrThrow(index, name, tfNameWorkOne.getText(), tfNameWorkTwo.getText(),
						tfNameWorkThree.getText(), sb)) {
					return "";
				}
			} else {
				sb.append(name);
			}

			result = sb.toString();

//REGISTER >>
//0:"noChange", 1:"toLowerCase", 2:"toUpperCase", 3:"FirstUpper" 			
			index = result.isEmpty() ? 0 : cmbRegister.getSelectedIndex();
			if (index == 2) {
				result = result.toUpperCase();
			} else if (index == 1 || index == 3) {
				result = result.toLowerCase();
				if (index == 3) {
					String first = result.substring(0, 1).toUpperCase();
					result = first.concat(result.substring(1));
				}
			}
//POSTFIX >>
			result += tfPostfix.getText();

//POSTFIX RANDOM >>			
//"noAction", "random3", "random4", "random5", "random6","random7", "random8", "random9", "random10",
//"random11", "random12", "random13", "random14", "random15","random16", "random17", "random18", "random19", "random20"	, "names'		
			index = cmbPostFixListOrRandom.getSelectedIndex();
			if (index >= cmbPostFixListOrRandom.getItemCount() - 1) {
				if (!namesNeed || noSortIndex < 0 || noSortIndex >= namesList.size()) {
					return "";
				}
				result += namesList.get(noSortIndex);
			} else if (index >= 1) {
				result += CommonLib.getRandomDigitString(index + 2);
			}

//CLEAN UP >> remove restricted Windows file name symbols	

//LIMIT >>			
//0:"20", 1:"30", 2:"40", 3:"50", 4:"60", 5:"80", 6:"100", 7:"120", 8:"130", 9:"150", 10:"180", 11:"200"			

			index = cmbLengthLimit.getSelectedIndex();
			if (index < 0 || index > 11) {
				index = defaultCmbLengthLimitIndex;
			}
			int limit = Integer.valueOf(cmbLengthLimit.getItemAt(index));
			result = CommonLib.getCorrectFileNameToRename(limit, result);
		} catch (Exception e) {
			return "";
		}
		return result;
	}

	// index must not be 0, 9; 'name' not null/empty
	private boolean setChangedNameOrFalseOrThrow(final int index, final String name, final String one, final String two,
			final String three, StringBuilder sb) throws InterruptedException {
//0:"noChangeName", 1: "substringFromLength", 2:"endsubstringFromLength",3:"replaceFromLengthDest", 4:"endreplaceFromLengthDest",
// 5:"replaceSourceDest(first)", 6:"replaceSourceDest(last)", 7:"replaceSourceDest(all)",8:"changeToNowDate", 9:"changeToDateModificaton",
//10:"changeToRandom5",11: "changeToRandom6", 12:"changeToRandom7",13: "changeToRandom8", 14: "changeNameTo"		

//enabled, if index >> one: 14;  one,two:1,2,5,6,7;  one,two,three:3,4; no:0,8,9,10,11,12,13;
		if (index == 14) {
			CommonLib.appendNotNullEmpty(one, sb);
			return true;
		}

		if (index == 8) {
			sb.append(CommonLib.getFormatDateNowForFileName());
			Thread.sleep(2); // for different dates
			return true;
		}

		if (index >= 10) {// means random : 10,11,12,13
			int length = index - 5; // means 5..8
			sb.append(CommonLib.getRandomDigitString((length < 5 || length > 8) ? 8 : length));
			return true;
		}

		if (index < 1 || index > 7) {
			return false;
		}
		int nameLength = name.length();

//DIGIT IN 'one','two' NEED FOR indexes: 1,2,3,4 >> 'from','length'
		if (index <= 4) { // means 1,2,3,4
			int length = Integer.valueOf(two);

			int from = Integer.valueOf(one);

			if (from < 0 || length <= 0 || (from + length) > nameLength) {
				return false;
			}

			if (index == 2 || index == 4) {
				from = (nameLength - from - length);
			}
			if (index == 1 || index == 2) {
				CommonLib.appendNotNullEmpty(name.substring(from, from + length), sb);
				return true;
			}
			if (index == 3 || index == 4) { // remove substring, change on 'three'
				CommonLib.appendNotNullEmpty(name.substring(0, from), sb);
				CommonLib.appendNotNullEmpty(three, sb);
				CommonLib.appendNotNullEmpty(name.substring(from + length), sb);
				return true;
			}
			return false;// not must be so...
		}

// remains 5,6,7 >> replace one -> two
		if (one.isEmpty()) {
			return false; // no defined, think as error
		}

		var pos = index == 6 ? name.lastIndexOf(one) : name.indexOf(one);
		if (pos < 0) { // no found substring, by default 'no error', else set in 'options'
			sb.append(name);
			return !replaceNoSubstringError;
		}

		if (index == 7) {
			CommonLib.appendNotNullEmpty(name.replace(one, two), sb);
		} else {
			CommonLib.appendNotNullEmpty(name.substring(0, pos), sb);
			CommonLib.appendNotNullEmpty(two, sb);
			CommonLib.appendNotNullEmpty(name.substring(pos + one.length()), sb);
		}
		return true;
	}

	private void initComponentsForRenaming() {// on constructor
		tfPrefixStart = new JTextField(6);
		tfPrefixStart.addActionListener(autoApply);
		tfPrefixStart.setToolTipText("prefix at new name start");

		cmbNumbers = new JComboBox<>(new String[] { "noNumbering/parent", "++numbering'0',from", "++numbering'00',from",
				"++numbering'000',from", "--numbering'0',from", "--numbering'00',from", "--numbering'000',from",
				"extractParent" });
		cmbNumbers.setToolTipText("numbering/parent variants");

		tfNumbersFrom = new JTextField("1", 3);
		tfNumbersFrom.addActionListener(autoApply);
		tfNumbersFrom.setToolTipText("start number");

		tfNumbersSeparator = new JTextField("-", 2);
		tfNumbersSeparator.addActionListener(autoApply);
		tfNumbersSeparator.setToolTipText("text after number");

		cmbNumbers.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				var index = cmbNumbers.getSelectedIndex();
				tfNumbersFrom.setEnabled(index > 0 && index < cmbNumbers.getItemCount() - 1);
				tfNumbersSeparator.setEnabled(index > 0);
				renaiming(false);
			}
		});

		tfNumbersFrom.setEnabled(false);
		tfNumbersSeparator.setEnabled(false);

		cmbNameWork = new JComboBox<>(new String[] { "noChangeName", "substringFromLength", "endsubstringFromLength",
				"replaceFromLengthDest", "endreplaceFromLengthDest", "replaceSourceDest(first)",
				"replaceSourceDest(last)", "replaceSourceDest(all)", "changeToNowDate", "changeToDateModificaton",
				"changeToRandom5", "changeToRandom6", "changeToRandom7", "changeToRandom8", "changeNameTo" });
		cmbNameWork.setToolTipText(
				"name change variants; for 'from' indexes from 0; for 'end...' starts at the end of name");

		tfNameWorkOne = new JTextField(4);
		tfNameWorkTwo = new JTextField(4);
		tfNameWorkThree = new JTextField(4);

		tfNameWorkOne.addActionListener(autoApply);
		tfNameWorkTwo.addActionListener(autoApply);
		tfNameWorkThree.addActionListener(autoApply);

		cmbNameWork.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				var index = cmbNameWork.getSelectedIndex();
//one: 14;  one,two:1,2,5,6,7;  one,two,three:3,4; no:0,8,9,10,11,12,13;
				var bOne = index == 14;
				var bTwo = index == 1 || index == 2 || index == 5 || index == 6 || index == 7;
				var bThree = index == 3 || index == 4;

				tfNameWorkOne.setEnabled(bOne || bTwo || bThree);
				tfNameWorkTwo.setEnabled(bTwo || bThree);
				tfNameWorkThree.setEnabled(bThree);
				renaiming(false);
			}
		});

		tfNameWorkOne.setEnabled(false);
		tfNameWorkTwo.setEnabled(false);
		tfNameWorkThree.setEnabled(false);

		cmbRegister = new JComboBox<>(new String[] { "noChange", "toLowerCase", "toUpperCase", "FirstUpper" });
		cmbRegister.addActionListener(autoApply);
		cmbRegister.setToolTipText("set result character case; example: 'name','NAME','Name'");

		tfPostfix = new JTextField(6);
		tfPostfix.addActionListener(autoApply);
		tfPostfix.setToolTipText("postfix at new name end");

		cmbPostFixListOrRandom = new JComboBox<>(new String[] { "noAction", "random3", "random4", "random5", "random6",
				"random7", "random8", "random9", "random10", "random11", "random12", "random13", "random14", "random15",
				"random16", "random17", "random18", "random19", "random20", "names" });
		cmbPostFixListOrRandom.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				namesNeed = false;
				renaiming(false);
			}
		});
		cmbPostFixListOrRandom.setToolTipText("generate random digit string specific length");

		cmbLengthLimit = new JComboBox<>(
				new String[] { "20", "30", "40", "50", "60", "80", "100", "120", "130", "150", "180" });
		cmbLengthLimit.addActionListener(autoApply);
		cmbLengthLimit.setToolTipText(
				"finally removing spaces on start/end name, restricted symbols and trim to specified length");
		cmbLengthLimit.setSelectedIndex(defaultCmbLengthLimitIndex);
	}

	synchronized private void dragging(int rowDest) {
		if (FileDataBase.dragging(rowDest, myTable, beans)) {
			updating();
		}
	}

	private void updating() {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
	}

	private void setNewTitle(String s) {
		if (!getTitle().equals(s)) {
			setTitle(s);
		}
	}

	private void setStandardTitle() {
		setNewTitle(standardTitle);
	}

	private void sorting(int columnIndex) {
		if (columnIndex < 0 || beans.size() < 2) {
			return;
		}

		int sortType = SortBeans.sortNoDefined;
		String sortCaption = "";
		boolean noDubleSort = false;// columns 0,4 and if's shift: always sort

//columns = { "Current ('old') name", "New name", "Rename info, old/new length ","Path" };
		final String column = (columnIndex >= 1 && columnIndex <= 3) ? columns[columnIndex - 1] : columns[3];

		if (columnIndex == 0) {
			sortType = SortBeans.sortCheck_ThenFour;
			sortCaption = "Checked -> " + column;
		} else if (columnIndex == 1) {
			sortType = SortBeans.sortOneLowerCase;
			sortCaption = column;
			noDubleSort = true;
		} else if (columnIndex == 2) {
			sortType = SortBeans.sortTwoLowerCase;
			sortCaption = column;
			noDubleSort = true;
		} else if (columnIndex == 3) {
			sortType = SortBeans.sortThree;
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

	private boolean initBeans(List<Path> paths) {
		namesNumbersNoSort = new ArrayList<String>();
		namesOldNoSort = new ArrayList<String>();

		int countError = 0;
		int count = 0;
		for (var path : paths) {
			try {
				String pathString = path.toString();
				int lastSeparator = pathString.lastIndexOf(File.separator);
				if (!path.toFile().exists() || path.toFile().isDirectory() || lastSeparator <= 0) {
					throw new IllegalArgumentException("file no exists or directory, or no found file separator");
				}
				var ar = ConverterBinFunc.dividePathToAll_Ext(lastSeparator + 1, pathString);
				if (ar[0] == null) {
					throw new IllegalArgumentException("error extract extension");
				}
//columns = { "Current ('old') name", "New name", "Rename info, old/new length ", "Path" };
				String fourApp = ar[2].isEmpty() ? "" : "." + ar[2];
				String four = fourApp.isEmpty() ? pathString
						: pathString.substring(0, pathString.length() - fourApp.length());
				var sb = new StringBuilder();
				count++;
				sb.append(CommonLib.formatInt(count, 3, Const.BRACE_START, Const.BRACE_END_WITH_SPACE));
				namesNumbersNoSort.add(sb.toString());
				namesOldNoSort.add(ar[1]);

//to 'bean.serviceString' be set result after closing this window; if null/empty - not result; else - correct path to string
				var bean = new MyBean(ar[1], "", sb.toString(), four, fourApp);
				bean.serviceIntOne = count - 1;
				bean.binPath = path;
				beans.add(bean);

			} catch (Exception e) {
				System.out.println("Error: " + e.getMessage() + " -> " + path);
				countError++;
			}
		}
		if (countError > 0 || beans.isEmpty()) {
			String empty = beans.isEmpty() ? "." + CommonLib.NEW_LINE_UNIX + "No files found, window will close" : "";
			JOptionPane.showMessageDialog(null, "Excluded " + countError + " items" + empty);

		}
		return !beans.isEmpty();
	}

}
