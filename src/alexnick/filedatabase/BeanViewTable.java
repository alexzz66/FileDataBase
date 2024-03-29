package alexnick.filedatabase;

import javax.swing.*;

import alexnick.CommonLib;
import alexnick.CopyMove;

import java.awt.*;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BeanViewTable extends JDialog {
	private static final long serialVersionUID = 1L;
	final private String[] columns;

	private int isCheckResult = Const.MR_NO_CHOOSED;

	int getIsCheckResult() {
		return isCheckResult;
	}

	private List<MyBean> beans;
	private List<MyBean> beansTotal;
	private BeansFourTableDefault myTable;

	private JTextField tfFindSubstrings;
	private JLabel checkInfo;
	private JButton butMark;
	private JCheckBox cbFilter;
	private JComboBox<String> cmbFindPosition;
	private JComboBox<String> cmbFindFullPathOrName;

	private JTextField tfFindPath;
	private JLabel choosed;

	private JTextField tfInfo;

	private int[] lastIndex = { 0, 0 }; // first as cmbCheckItems index;second as cmbCheckItemsApp

	private String[] cmbCheckItems = new String[] { "all", "no", "invert", "exist", "no exist", "by BinFolder",
			"by Signature", "by Modified", "by Path", "textSearch", "remove from table" };// !!! 'remove' must be LAST
																							// ITEM
	// append const indices from 'cmbCheckItems'
	private final int cmbAppEnabStartIndex = 3;
	private final int cmbAppEnabEndIndex = 9;
	private final int cmbAppEnabStartFindColumnIndex = 5;
	private final int textSearchIndex = 9; // cmbApp enabled
	private String[] cmbCheckItemsApp = new String[] { "only", "add", "sub", "onlyCase", "addCase", "subCase", "TEST" };

	private final String caption;
	volatile private int lastSortType = SortBeans.sortNoDefined;

	// 'beans0' not null
	public BeanViewTable(JFrame frame, boolean viewNoId3, boolean viewNoMark, List<MyBean> beans0) {
		super(frame, true);
		FileDataBase.isShiftDown = false;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (isCheckResult == Const.MR_NO_CHOOSED) {
					isCheckResult = Const.MR_CANCEL;
				}
			}
		});

		var sb = new StringBuilder();
		sb.append("Total items: ").append(beans0.size());
		if (viewNoId3 || viewNoMark) {
			sb.append(" [");
			if (viewNoId3) {
				sb.append("viewNoId3");
			}
			if (viewNoMark) {
				if (viewNoId3) {
					sb.append("; ");
				}
				sb.append("viewNoMark");
			}
			sb.append("]");
		}

		this.caption = sb.toString();
		setStandardTitle();

		sb.setLength(0);
		sb.append("BinFolder");
		if (!viewNoId3) {
			sb.append(" <ID3 tag>");
		}
		if (!viewNoMark) {
			sb.append(" ").append(Const.BRACE_MARK).append("mark");
		}
		this.columns = new String[] { sb.toString(), "Signature; size", "Modified", "Path" };

		beans = new ArrayList<MyBean>();
		beansTotal = beans0;

		// !!! path must be last string argument (fourCapt)

		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, false, true, true, columns[0],
				columns[1], columns[2], columns[3], beans);

		initComponents(viewNoMark, beans0);

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(0, 0, t.width - 100, t.height - 100);

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initComponents(boolean viewNoMark, List<MyBean> beans0) {
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
						printCount(null, false, null);
					}
					return;
				}
				if (e.getClickCount() == 2) {
					FileDataBase.openDirectory(1, FileDataBase.isShiftDown, myTable, beans);
				}
			}
		});

//FILL JPANEL (first)
		var cmbChecking = new JComboBox<String>(cmbCheckItems);
		var cmbCheckingApp = new JComboBox<String>(cmbCheckItemsApp);
		cmbCheckingApp.setEnabled(false);
		cmbCheckingApp.setToolTipText(Const.cmbOnlyAddSubToolTip);

		ActionListener butCheckActionListener = e -> checking(cmbChecking.getSelectedIndex(),
				cmbCheckingApp.getSelectedIndex());

		cmbChecking.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				var index = cmbChecking.getSelectedIndex();
				var bEnabTfFindSubstrings = index >= cmbAppEnabStartFindColumnIndex && index <= cmbAppEnabEndIndex;
				var bNeedFilterApp = bEnabTfFindSubstrings
						|| (index >= cmbAppEnabStartIndex && index <= cmbAppEnabEndIndex);

				tfFindSubstrings.setEnabled(bEnabTfFindSubstrings);
				cmbCheckingApp.setEnabled(bNeedFilterApp);
			}
		});

		tfFindSubstrings = new JTextField(FileDataBase.sizeTextField);
		tfFindSubstrings.addActionListener(butCheckActionListener);
		tfFindSubstrings.setToolTipText(Const.textFieldBinFolderToolTip);
		tfFindSubstrings.setEnabled(false);

		var butCheck = new JButton("set");
		butCheck.addActionListener(butCheckActionListener);

		checkInfo = new JLabel();
		printCount(null, false, null); // check on show window

		if (!viewNoMark) {
			butMark = new JButton("mark");
			butMark.setToolTipText("set 'mark' for checked");
			butMark.addActionListener(e -> marking(viewNoMark));
		}

//FILTERS				
		cbFilter = new JCheckBox("filter");

		cmbFindPosition = new JComboBox<>(new String[] { "any", "starts", "ends" });
		cmbFindPosition.setEnabled(false);

		cmbFindFullPathOrName = new JComboBox<>(new String[] { "fullPath", "name.ext", "name", "parent" });
		cmbFindFullPathOrName.setEnabled(false);

		tfFindPath = new JTextField(FileDataBase.sizeTextField);
		tfFindPath.setEnabled(false);

//CBFILTER-ACTION
		cbFilter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				var select = cbFilter.isSelected();
				cmbFindPosition.setEnabled(select);
				cmbFindFullPathOrName.setEnabled(select);
				tfFindPath.setEnabled(select);
			}
		});

		JButton butFind = new JButton(">>");
		// below both equals Listeners
		butFind.addActionListener(e -> showChoosedItems());
		tfFindPath.addActionListener(e -> showChoosedItems());
		tfFindPath.setToolTipText(Const.textFieldPathToolTip);

		choosed = new JLabel();

		tfInfo = new JTextField(14);
		tfInfo.setEditable(false);

		setLabelChoosed(beans0);

		JComboBox<String> cmbAction = new JComboBox<>(new String[] { "copy/move", "toList all/paths",
				"toList pathsNoRoot", "generate *.bin", "openWithFiles" });
		JButton butAction = new JButton("do");
		butAction.addActionListener(e -> doAction(cmbAction.getSelectedIndex()));

//FILL JPANEL (second)
		JPanel buttons = new JPanel();
		buttons.add(cmbChecking);
		buttons.add(cmbCheckingApp);

		buttons.add(tfFindSubstrings);
		buttons.add(butCheck);
		buttons.add(checkInfo);

		if (!viewNoMark && butMark != null) {
			buttons.add(butMark);
			butMark.addKeyListener(FileDataBase.keyListenerShiftDown);
		}

		buttons.add(cbFilter);
		buttons.add(cmbFindPosition);
		buttons.add(cmbFindFullPathOrName);
		buttons.add(tfFindPath);

		buttons.add(butFind);
		buttons.add(choosed);
		buttons.add(tfInfo);
		buttons.add(cmbAction);
		buttons.add(butAction);

//end constructor:		
		JTextArea area = new JTextArea(3, 0); // add 'buttons' height
		area.setBackground(buttons.getBackground());
		area.setEditable(false);
		buttons.add(area);
		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));

		getContentPane().add(new JScrollPane(myTable), BorderLayout.CENTER);

		var scrollPan = new JScrollPane(buttons, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPan, BorderLayout.SOUTH);

//SHIFT DOWN AND KEYADAPTER
		var keyAdapterEnter = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					checking(cmbChecking.getSelectedIndex(), cmbCheckingApp.getSelectedIndex());
				}
			}
		};

		myTable.addKeyListener(FileDataBase.keyListenerShiftDown);

		cmbChecking.addKeyListener(keyAdapterEnter);
		cmbChecking.addKeyListener(FileDataBase.keyListenerShiftDown);

		cmbCheckingApp.addKeyListener(keyAdapterEnter);
		cmbCheckingApp.addKeyListener(FileDataBase.keyListenerShiftDown);

		tfFindSubstrings.addKeyListener(FileDataBase.keyListenerShiftDown);
		butCheck.addKeyListener(FileDataBase.keyListenerShiftDown);

		cbFilter.addKeyListener(FileDataBase.keyListenerShiftDown);
		cmbFindPosition.addKeyListener(FileDataBase.keyListenerShiftDown);
		cmbFindFullPathOrName.addKeyListener(FileDataBase.keyListenerShiftDown);
		tfFindPath.addKeyListener(FileDataBase.keyListenerShiftDown);

		butFind.addKeyListener(FileDataBase.keyListenerShiftDown);
		tfInfo.addKeyListener(FileDataBase.keyListenerShiftDown);
		cmbAction.addKeyListener(FileDataBase.keyListenerShiftDown);
		butAction.addKeyListener(FileDataBase.keyListenerShiftDown);
	}

//0:"all", 1:"no", 2:"invert", 3:"exists" (3 and more: with indexTwo), 4: "no exists"
//5:"by BinFolder", 6:"by Start path only", 7:"by Modified ", 8:"by Result"
//9: "textSearch",  10:remove from table (last index)
//indexTwo, app: 0:"only", 1:"add", 2:"sub" , 3:"onlyCase", 4:"addCase", 5:"subCase", 6: "TEST" 	
	private void checking(final int indexOne, int indexTwo) {
		if (beans.isEmpty() || indexOne < 0 || indexOne >= cmbCheckItems.length) {
			return;
		}

		if (indexOne >= cmbCheckItems.length - 1) { // last index -> remove
			removeFromTable();
			return;
		}

//defined: cmbAppEnabStartIndex = 3; cmbAppEnabEndIndex = 9; cmbAppEnabStartFindColumnIndex = 5;	
		var bEnabTfFindSubstrings = indexOne >= cmbAppEnabStartFindColumnIndex && indexOne <= cmbAppEnabEndIndex;
		var bNeedFilterApp = bEnabTfFindSubstrings
				|| (indexOne >= cmbAppEnabStartIndex && indexOne <= cmbAppEnabEndIndex);
		int indexTwoResult = indexTwo;

		if (indexTwo < 0 || indexTwo >= cmbCheckItemsApp.length) {
			if (bNeedFilterApp) {
				return;
			}

			indexTwo = 0; // for 'all','no','invert' no matter, but in array write correct number
		}

		boolean test = bNeedFilterApp && indexTwo == 6;// TEST

		if (test && (indexOne == textSearchIndex)) { // TEST text search
			int[] addedInfo = FileDataBase.testTextSearchOrNull(beans);
			if (addedInfo == null) {
				return;
			}

			int[] index = { indexOne, indexTwo };
			updating(index, addedInfo);
			return;
		}

		boolean toLowerCase = indexTwo <= 2;
		if (!toLowerCase) {
			indexTwoResult -= 3; // 3..5 -> 0..2
		}

		// remains all but the last
		int[] addedInfo = new int[2]; // plus and minus info
		addedInfo[0] = 0;
		addedInfo[1] = 0;

		boolean bAdd = bNeedFilterApp && indexTwoResult == 1;
		boolean bSub = !bAdd && bNeedFilterApp && indexTwoResult == 2;

		List<String> substringsAND = null;
		List<String> substringsOr = null;
		int[] arCodePoints = null;

		if (bEnabTfFindSubstrings) {
			substringsOr = new ArrayList<String>();
			arCodePoints = new int[2];

			substringsAND = FileDataBase.getSubstringsAND_DivideByOR_NullIfError(test, true, toLowerCase,
					tfFindSubstrings.getText(), substringsOr, arCodePoints);

			if (arCodePoints[1] <= 0) {
				arCodePoints = null;
			}

			if (substringsOr.isEmpty()) {
				updating(lastIndex, null);
				if (!test) { // for 'test' return below
					return;
				}
			}

			if (test) {
				FileDataBase.testInfo(this, substringsAND, substringsOr, arCodePoints);
				return;
			}
		} else if (test && bNeedFilterApp) {
			return; // for exists, no exists
		}

		for (var b : beans) {
			var res = false; // false by default

			if (bNeedFilterApp) { // by exists, column
				if ((b.check && bAdd) || (!b.check && bSub)) {
					continue;
				}

				if (indexOne == 3 || indexOne == 4) { // exists, no exists; 'toLowerCase' no matters
					res = b.isFourPrefixNoExists(); // no exists
					if (indexOne == 3) { // exists
						res = !res;
					}

				} else if (indexOne == textSearchIndex) {

					var one = b.getOne();
					if (one.startsWith(Const.BRACE_TEST_ERROR_FULL)) {
						// res == false
					} else {
						var count = FileDataBase.getTextSearchResult(false, toLowerCase ? 1 : 0, b.getFour(false, true),
								substringsAND, substringsOr, arCodePoints);

						if (count > 0) {
							res = true;
						} else if (count == -1) {
							b.setOne(Const.BRACE_TEST_ERROR_FULL + one);
						}

					}

				} else { // by column 5..8->1..4; find' not null here
					res = true;

					if (CommonLib.notNullEmptyList(substringsAND)) { // first finding by AND, if defined
						res = b.findSubstringsInColumn(indexOne - 4, toLowerCase ? 1 : 0, substringsAND);
					}

					if (res) { // substringsOr not null/empty
						res = b.findSubstringsInColumn(indexOne - 4, toLowerCase ? 1 : 0, substringsOr);
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

		int[] index = { indexOne, indexTwo };
		updating(index, addedInfo);
	}

	private void removeFromTable() {
		int checkCount = printCount(lastIndex, true, null);
		if (checkCount <= 0) {
			return;
		}
		var sb = new StringBuilder();
		sb.append("Remove checked items from table: ").append(checkCount).append(" / ").append(beans.size());
		sb.append(CommonLib.NEW_LINE_UNIX).append("[ items can be restored by pressing '>>' ]");
		sb.append(CommonLib.NEW_LINE_UNIX).append(CommonLib.NEW_LINE_UNIX).append("Continue?");

		var confirm = JOptionPane.showConfirmDialog(this, sb.toString(), "Remove items", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		List<MyBean> tmp = new ArrayList<MyBean>();
		tmp.addAll(beans);
		beans.clear();
		for (var b : tmp) {
			if (!b.check) {
				beans.add(b);
			}
		}

		tfInfo.setText("");
		updating(null, null);
	}

//0:copy/move, 1:toList all/paths, 2:toList pathsNoRoot, 3:generate .*bin, 4: "openWithFiles"
	private void doAction(int selectedIndex) {
		if (selectedIndex == 0) {
			doCopyMove();
			return;
		}

		if (selectedIndex == 3) {
			generateBin();
			return;
		}

		if (selectedIndex == 4) {
			toCommandLine();
			return;
		}

		FileDataBase.beansToList(false, selectedIndex == 2 ? 3 : 2, null, beans);
	}

	private void toCommandLine() {
		int checkCount = printCount(lastIndex, true, null);
		if (checkCount <= 0) {
			return;
		}

		Set<Integer> numbers = new HashSet<>();

		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);
			if (!b.check) {
				continue;
			}

			numbers.add(i);
		}

		FileDataBase.toCommandLine(this, 1, 1, 0, numbers, beans);
	}

	private void generateBin() {
		int checkCount = printCount(lastIndex, true, null);
		if (checkCount <= 0) {
			return;
		}

		Set<String> beansSet = new HashSet<String>();
		Set<Integer> errorNumbersSet = new HashSet<>();

		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);
			if (!b.check) {
				continue;
			}

			if (CommonLib.nullEmptyString(b.serviceStringOne)) {
				errorNumbersSet.add(i);
				continue;
			}

			if (!beansSet.add(b.serviceStringOne)) {
				errorNumbersSet.add(i);
				continue;
			}
		}

		var sb = new StringBuilder();
		sb.append("Checked: ").append(checkCount).append(" / ").append(beans.size()).append(CommonLib.NEW_LINE_UNIX)
				.append("Found items to generate '*.bin': ").append(beansSet.size());

		int confirm = JOptionPane.CANCEL_OPTION;
		String yes = "Generate file '" + Const.GENERATED_BIN_NAME + "', show in Explorer";

		if (errorNumbersSet.isEmpty()) {
			sb.append(CommonLib.NEW_LINE_UNIX).append(yes).append(CommonLib.NEW_LINE_UNIX).append("Continue?");

			confirm = JOptionPane.showConfirmDialog(null, sb.toString(), "Generate .*bin", JOptionPane.YES_NO_OPTION);
		} else {
			String message = CommonLib.formatConfirmYesNoMessage(sb.toString(), "uncheck erroneous items. " + yes,
					"uncheck erroneous items (" + errorNumbersSet.size() + ")", null);

			confirm = JOptionPane.showConfirmDialog(null, message, "Generate .*bin", JOptionPane.YES_NO_CANCEL_OPTION);

			if (confirm == JOptionPane.YES_OPTION || confirm == JOptionPane.NO_OPTION) {
				for (var i : errorNumbersSet) {
					beans.get(i).check = false;
				}

				updating(null, null);
			}
		}

		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		var resultList = CommonLib.getListFromSet(2, beansSet);
		Path path = FileDataBase.getTempPath(Const.GENERATED_BIN_NAME);

		if (CommonLib.saveToFile(false, false, 1, CopyMove.DeleteIfExists_OLD_DELETE, path, null, resultList)) {
			CommonLib.startProcess(false, path.getParent());
		}
	}

	private void doCopyMove() {
		int checkCount = printCount(lastIndex, true, null);
		if (checkCount <= 0) {
			return;
		}

		List<String> copyFilesEqualSignatureList = new ArrayList<>();
		Set<Integer> copyFilesEqualSignatureNumbers = new HashSet<>();
		List<String> copyFilesList = new ArrayList<>();
		Set<Integer> nonExistingNumbers = new HashSet<>();
		int equalsPathCountForCopying = fillExistFilesLists(nonExistingNumbers, copyFilesList,
				copyFilesEqualSignatureNumbers, copyFilesEqualSignatureList);

		String equalsPathInfo = "";

		if (equalsPathCountForCopying > 0) {
			updating(null, null);
			equalsPathInfo = "NB: was been excluded and unchecked equal paths: " + equalsPathCountForCopying
					+ CommonLib.NEW_LINE_UNIX;
		}

		if (!copyFilesEqualSignatureList.isEmpty()) {
			CommonLib.saveAndShowList(false, false, 1, FileDataBase.getTempPath("equalSignaturesList.txt"),
					copyFilesEqualSignatureList);

			String message = CommonLib.formatConfirmYesNoMessage(
					equalsPathInfo + "Will be excluded for copy/move files with equal column 'Signature, size', count: "
							+ copyFilesEqualSignatureList.size(),
					"uncheck equals and continue", "uncheck equals only", null);

			var confirm = JOptionPane.showConfirmDialog(null, message, "Equals signatures",
					JOptionPane.YES_NO_CANCEL_OPTION);

			if (confirm == JOptionPane.YES_OPTION || confirm == JOptionPane.NO_OPTION) {
				for (var i : copyFilesEqualSignatureNumbers) {
					beans.get(i).check = false;
				}

				updating(null, null);
			}

			if (confirm != JOptionPane.YES_OPTION) {
				return;
			}
			equalsPathInfo = ""; // there's been info, no need
		}

		if (copyFilesList.isEmpty()) {
			JOptionPane.showMessageDialog(null, equalsPathInfo + "No found files for copy/move");
			return;
		}

		checkCount = printCount(lastIndex, true, null);
		if (checkCount <= 0) {
			return;
		}

		var sb = new StringBuilder();
		sb.append(equalsPathInfo).append("Checked: ").append(checkCount).append(" / ").append(beans.size())
				.append(CommonLib.NEW_LINE_UNIX).append("Existing files for copy/move: ").append(copyFilesList.size());

		int confirm = JOptionPane.CANCEL_OPTION;

		if (nonExistingNumbers.isEmpty()) {
			sb.append(CommonLib.NEW_LINE_UNIX).append("Continue?");
			confirm = JOptionPane.showConfirmDialog(null, sb.toString(), "Copy/move", JOptionPane.YES_NO_OPTION);
		} else {
			String message = CommonLib.formatConfirmYesNoMessage(sb.toString(), "uncheck non-existing and continue",
					"uncheck non-existing", null);

			confirm = JOptionPane.showConfirmDialog(null, message, "Copy/move", JOptionPane.YES_NO_CANCEL_OPTION);

			if (confirm == JOptionPane.YES_OPTION || confirm == JOptionPane.NO_OPTION) {
				for (var i : nonExistingNumbers) {
					beans.get(i).check = false;
				}
				updating(null, null);
			}

		}

		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		var tempPath = FileDataBase.getTempPathForCopyMove();
		if (!CommonLib.saveToFile(false, true, 1, CopyMove.DeleteIfExists_OLD_RENAME_TO_BAK, tempPath, null,
				copyFilesList)) {
			JOptionPane.showMessageDialog(null, "error of saving path list to " + tempPath);
			return;
		}
		isCheckResult = Const.MR_COPY_MOVE;
		dispose();
	}

	private void marking(boolean viewNoMark) {
		if (viewNoMark) {
			return;
		}
		int checkCount = printCount(lastIndex, true, null); // recount, because click of mouse on table may be no
															// corrected
		if (checkCount <= 0) {
			return;
		}

		if (!FileDataBase.initMarkIsProperty()) { // init 'markPropertySet'
			return;
		}

		String allEquals = ""; // no defined

		Set<String> signatureSet = new HashSet<String>();
		for (var b : beans) {
			if (!b.check) {
				continue;
			}

			String signature = getSignatureFromColumnOrEmpty(b.getTwo());
			if (!signature.isEmpty()) {
				signatureSet.add(signature);
			}

			if (allEquals == null) {
				continue; // no need define equal string
			}

			var pos = b.getOne().indexOf(Const.BRACE_MARK);

			var tmp = (pos <= 0) ? Const.REMOVE_MARK : b.getOne().substring(pos);
			if (allEquals.isEmpty()) {
				allEquals = tmp;
			} else if (!allEquals.equals(tmp)) {
				allEquals = null;
			}
		}

		allEquals = CommonLib.nullEmptyString(allEquals) ? ""
				: allEquals != Const.REMOVE_MARK ? FileDataBase.formatMark(allEquals, false) : Const.REMOVE_MARK;

		var markSetFrame = new MarkSetFrame(this, "Set 'mark' for checked: " + checkCount + "; type * to delete",
				allEquals, FileDataBase.markPropertySet);

		if (markSetFrame.getIsCheckResult() == Const.MR_OK) {
			String mark = markSetFrame.getResultMark();// 'getResultMark()' formatted and not empty
			String markWithBrace = mark.equals(Const.REMOVE_MARK) ? "" : FileDataBase.formatMark(mark, true);

			if (mark.isEmpty()) {
				return;
			}

			for (var b : beans) {
				String signature = getSignatureFromColumnOrEmpty(b.getTwo());
				if (signature.isEmpty() || !signatureSet.contains(signature)) {
					continue;
				}

//				if (!b.check) { UPDATE >> checking by signatures
//					continue;}				

				var one = b.getOne();
				var pos = one.indexOf(Const.BRACE_MARK);
				if (pos <= 0) {
					if (!markWithBrace.isEmpty()) {
						b.setOne(one + " " + markWithBrace);
					}
				} else {
					b.setOne(one.substring(0, pos) + markWithBrace);
				}
				FileDataBase.addMarkToProperties(false, signature, mark);
			}

			updating(null, null);
		}
		return;
	}

	private String getSignatureFromColumnOrEmpty(String two) {
		var pos = two.lastIndexOf(Const.columnSignatureSeparator);
		if (pos <= 0) {
			return "";
		}
		String signature = two.substring(0, pos);
		if (!signature.endsWith(")")) {
			return "";
		}
		return signature;
	}

//'addedInfo' if not null and length == 2, be added info to label	
//'index' must be null OR as indices in 'cmbCheckItems', 'cmbCheckItemsApp'
	private void updating(int index[], int[] addedInfo) {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount(index, false, addedInfo);
	}

	private void sorting(int columnIndex) {
		if (columnIndex < 0 || beans.size() < 2) {
			return;
		}

		int sortType = SortBeans.sortNoDefined;
		String sortCaption = "";
		boolean noDubleSort = false;// columns 0,4 and if's shift: always sort

//columns = { "BinFolder <ID3 tag>", "Size; signature", "Modified", "Path" }; 
		final String column = (columnIndex >= 1 && columnIndex <= 3) ? columns[columnIndex - 1] : columns[3];
		if (FileDataBase.isShiftDown) {
			if (columnIndex == 0) {
				sortType = SortBeans.sortCheck_Shift_ThenFourName;
				sortCaption = "Checked (Shift) -> " + column + " [names]";
			} else if (columnIndex == 1) {
				sortType = SortBeans.sortOneLowerCase_Shift_CheckOnly;
				sortCaption = "Checked only (Shift) -> " + column;
			} else if (columnIndex == 2) {
				sortType = SortBeans.sortTwo_Shift_CheckOnly;
				sortCaption = "Checked only (Shift) -> " + column;
			} else if (columnIndex == 3) {
				sortType = SortBeans.sortThree_Shift_CheckOnly;
				sortCaption = "Checked only (Shift) -> " + column;
			} else {
				if (lastSortType != SortBeans.sortFourLowerCase_Shift_CheckOnly) {
					sortType = SortBeans.sortFourLowerCase_Shift_CheckOnly;
					sortCaption = "Checked only (Shift) -> column";
				} else {
					sortType = SortBeans.sortFourNameLowerCase_Shift_CheckOnly;
					sortCaption = "Checked only (Shift) -> " + column + " [names]";
				}
			}
		} else {
			if (columnIndex == 0) {
				sortType = SortBeans.sortCheck;
				sortCaption = "Checked -> " + column;
			} else if (columnIndex == 1) {
				sortType = SortBeans.sortOneLowerCase;
				sortCaption = column;
				noDubleSort = true;
			} else if (columnIndex == 2) {
				sortType = SortBeans.sortTwo;
				sortCaption = column;
				noDubleSort = true;
			} else if (columnIndex == 3) {
				sortType = SortBeans.sortThree;
				sortCaption = column;
				noDubleSort = true;
			} else {
				if (lastSortType != SortBeans.sortFourLowerCase) {
					sortType = SortBeans.sortFourLowerCase;
					sortCaption = column;
				} else {
					sortType = SortBeans.sortFourNameLowerCase;
					sortCaption = column + " [names]";
				}
			}
		}

		if (sortType == lastSortType && noDubleSort) {
			return;
		}
		lastSortType = sortType;
		setStandardTitle();

		var sortBeans = new SortBeans(sortType, sortCaption, beans, myTable);
		if (!sortBeans.isBeansWasSorted()) {
			return;
		}

		setNewTitle(caption.concat(sortBeans.getAppendCaption()));
	}

	private void setNewTitle(String s) {
		if (!getTitle().equals(s)) {
			setTitle(s);
		}
	}

	private void setStandardTitle() {
		setNewTitle(caption);
	}

	// all parameters is created and empty, and will be filled
	private int fillExistFilesLists(Set<Integer> nonExistingNumbers, List<String> copyFilesList,
			Set<Integer> copyFilesEqualSignatureNumbers, List<String> copyFilesEqualSignatureList) {
		int equalsPathCountForCopying = 0;
		Set<String> signaturesSet = new HashSet<String>();
		Set<String> pathsToLowerCaseSet = new HashSet<String>();

		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);
			if (!b.check) {
				continue;
			}

			var pathString = b.isFourPrefixNoExists() ? "" : b.getFour(false, true);
			Path path = pathString.isEmpty() ? null : Path.of(pathString);

			if (path == null || !path.toFile().exists()) {
				nonExistingNumbers.add(i);
				continue;
			}

			if (pathsToLowerCaseSet.contains(pathString.toLowerCase())) {
				equalsPathCountForCopying++;
				b.check = false;
				continue;
			} else {
				pathsToLowerCaseSet.add(pathString.toLowerCase());
			}

			var sign = b.getTwo();
			if (signaturesSet.contains(sign)) {
				copyFilesEqualSignatureList.add(path.toString());
				copyFilesEqualSignatureNumbers.add(i);
			} else {
				signaturesSet.add(sign);
				copyFilesList.add(path.toString());
			}
		}
		return equalsPathCountForCopying;
	}

//'addedInfo' if not null and length == 2, be added info to label	
//'index' (or 'lastIndex') must be -1 OR as index in 'cmbCheckItems'	
	private int printCount(int[] index, boolean messageIfNoChecked, int[] addedInfo) {
		lastIndex = index;

		int checkCount = 0;
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			checkCount++;
		}

		var sb = new StringBuilder();
		sb.append((index == null) ? "count" : cmbCheckItems[index[0]]);

		if (index != null && index[0] >= cmbAppEnabStartIndex && index[0] <= cmbAppEnabEndIndex) {
			sb.append(" ").append(cmbCheckItemsApp[index[1]]);
		}

		sb.append(": ").append(checkCount);

		if (addedInfo != null && addedInfo.length == 2) {
			sb.append(" (+").append(addedInfo[0]).append("/-").append(addedInfo[1]).append(")");
		}

		checkInfo.setText(sb.toString());
		if (checkCount == 0 && messageIfNoChecked) {
			JOptionPane.showMessageDialog(this, "No checked items");
		}
		return checkCount;
	}

	private void showChoosedItems() { // press '>>'
		boolean isFilter = cbFilter.isSelected();

		// findPosition:any,starts,ends
		int findPosition = isFilter ? cmbFindPosition.getSelectedIndex() : 0;
		if (findPosition < 0) {
			findPosition = 0;
		}

		// findArea:fullPath,name.ext,name,parent
		int findFullPathOrName = isFilter ? cmbFindFullPathOrName.getSelectedIndex() : 0;
		if (findFullPathOrName < 0) {
			findFullPathOrName = 0;
		}

		boolean withExt = (isFilter && findFullPathOrName <= 1);
		String tmpLastSearchHash = "";
		List<String> substringsAND = null;
		List<String> substringsOr = null;

		if (isFilter) {
			String substrings = tfFindPath.getText();
			if (substrings.isEmpty()) {
				return;
			}

			substringsOr = new ArrayList<String>();
			substringsAND = FileDataBase.getSubstringsAND_DivideByOR_NullIfError(true, true, true, substrings,
					substringsOr, null);

			if (substringsOr.isEmpty()) {
				return;
			}

			tmpLastSearchHash = "\"" + substrings.toLowerCase() + "\"" + Const.BRACE_START_FIRST_SPACE
					+ cmbFindPosition.getItemAt(findPosition) + ","
					+ cmbFindFullPathOrName.getItemAt(findFullPathOrName);
			tmpLastSearchHash += Const.BRACE_END;

		} else { // no filter
			tmpLastSearchHash = "<no filter>";
		}

		String lastSearchHash = tfInfo.getText();
		if (lastSearchHash.equals(tmpLastSearchHash)) {
			return;
		}
		lastSearchHash = tmpLastSearchHash;
		tfInfo.setText(tmpLastSearchHash);

		if (!isFilter && beans.size() == beansTotal.size()) {
			return;
		}

		beans.clear();
		if (!isFilter) {
			beans.addAll(beansTotal);
		} else {
			for (var b : beansTotal) {
				try {
					var rowString = b.getFour(true, withExt);
					if (rowString.isEmpty()) {
						continue;
					}
					// findArea:fullPath,name,parent
					if (findFullPathOrName > 0) {
						var sep = rowString.lastIndexOf(File.separator);
						if (sep < 0) {
							continue;
						}
						rowString = findFullPathOrName == 1 ? rowString.substring(sep + 1)
								: rowString.substring(0, sep);
					}

					if (CommonLib.notNullEmptyList(substringsAND)) { // first finding by AND, if defined
						if (!FileDataBase.findSubStringsInString(findPosition, 1, rowString, substringsAND)) {
							continue;
						}
					}
					// substringsOr not null/empty
					if (!FileDataBase.findSubStringsInString(findPosition, 1, rowString, substringsOr)) {
						continue;
					}
					beans.add(b);

				} catch (Exception e) {
				}
			}
		}
		sorting(4);
		setLabelChoosed(beans);
		updating(null, null);
	}

	private void setLabelChoosed(List<MyBean> beans0) {
		final String ch = "chosen: ";
		choosed.setText(ch + beans0.size());
	}

}
