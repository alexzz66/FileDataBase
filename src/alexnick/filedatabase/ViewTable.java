package alexnick.filedatabase;

import javax.swing.*;

import alexnick.CommonLib;

import static alexnick.CommonLib.startProcess;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ViewTable extends JFrame implements Callable<Integer> {
	private static final long serialVersionUID = 1L;

	final private static String[] columns = { "BinFolder", "Start path", "Modified", "Result *.bin" };

	private final boolean viewNoId3;
	private final boolean viewNoMark;
	private List<Map<String, MarkInfo>> arMarkInfoList = null;
	private int isCheckResult = Const.MR_NO_CHOOSED;
	Program program;

	int getIsCheckResult() {
		return isCheckResult;
	}

	private List<MyBean> beans;
	private BeansFourTableDefault myTable;

	private int[] lastIndex = { 0, 0 }; // first as cmbCheckItems index;second as cmbCheckItemsApp

	private String[] cmbCheckItems = new String[] { "all", "no", "invert", "exist", "no exist", "by BinFolder",
			"by Start path", "by Modified", "by Result" };
	// append const indexes from 'cmbCheckItems'
	private final int cmbAppEnabStartIndex = 3;
	private final int cmbAppEnabEndIndex = 8;
	private final int cmbAppEnabStartFindColumnIndex = 5; // endFindColumn == cmbAppEnabEndIndex (find column's last in
															// 'appEnap' indexes) == cmbAppEnabStartFindColumnIndex + 3
	private String[] cmbCheckItemsApp = new String[] { "only", "add", "sub" };

	private JTextField tfFindColumn;
	private JLabel checkInfo;
	private JButton butMark = null;
	volatile private int lastSortType = SortBeans.sortNoDefined;
	private final String captionPrefix;
	private String standardTitle = "";
	private int sizeInCaption = 0;
	private Path pathForUpdate = null;

	public Path getPathForUpdate() {
		return pathForUpdate;
	}

	public ViewTable(Program program, String captionPrefix, List<MyBean> beans0) {
		this.captionPrefix = captionPrefix;
		this.program = program;

		viewNoId3 = program.getOptions().contains(Const.OPTIONS_VIEW_NO_ID3);
		viewNoMark = program.getOptions().contains(Const.OPTIONS_VIEW_NO_MARK);

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
		setStandardTitle();

		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, false, false, true, columns[0],
				columns[1], columns[2], columns[3], beans);

		initComponents();

		setPreferredSize(new Dimension(1024, 600));
		pack();

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initComponents() {
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

				if (e.getClickCount() == 1 && myTable.getSelectedColumn() == 0) {
					printCount(null, null, null);
					return;
				}

				if (e.getClickCount() != 2 || myTable.getSelectedRowCount() != 1) {
					return;
				}
				showExplorerTableOrDeleteBin();
			}
		});

//FILL JPANEL
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
				tfFindColumn.setEnabled(index >= cmbAppEnabStartFindColumnIndex && index <= cmbAppEnabEndIndex);
				cmbCheckingApp.setEnabled(index >= cmbAppEnabStartIndex && index <= cmbAppEnabEndIndex);
			}
		});

		var keyAdapterEnter = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					checking(cmbChecking.getSelectedIndex(), cmbCheckingApp.getSelectedIndex());
				}
			}
		};

		cmbChecking.addKeyListener(keyAdapterEnter);
		cmbCheckingApp.addKeyListener(keyAdapterEnter);

		tfFindColumn = new JTextField(FileDataBase.sizeTextField);
		tfFindColumn.addActionListener(butCheckActionListener);
		tfFindColumn.setToolTipText(Const.textFieldBinFolderToolTip);
		tfFindColumn.setEnabled(false);

		var butCheck = new JButton("Set");
		butCheck.addActionListener(butCheckActionListener);

		checkInfo = new JLabel();
		printCount(null, null, null); // set count on start window

//ADDING		
		JPanel buttons = new JPanel();

		buttons.add(cmbChecking);
		buttons.add(cmbCheckingApp);
		buttons.add(tfFindColumn);
		buttons.add(butCheck);
		buttons.add(checkInfo);

		if (!viewNoMark && FileDataBase.initMarkIsProperty()) {
			butMark = new JButton("Mark");
			butMark.setToolTipText("show mark info for checked bases");
			butMark.addActionListener(e -> markInfo());
			fillMarkMaps(); // set enabled 'butMark'
			buttons.add(butMark);
		}

		JButton butViewExts = new JButton("View exts");
		butViewExts.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<String> extNew = new ArrayList<String>();
				if (printCount(lastIndex, extNew, null) == null || extNew.isEmpty()) {
					return;
				}
				var sc = new ExtensionFrame(ViewTable.this, true, extNew, null, null);
				if (sc.getIsCheckResult() == Const.MR_OK) {
					showBeanViewTable(getFillingBeanListOrNull(sc.getExtsResult()));
				}
			}
		});

		buttons.add(butViewExts);

		JButton butCompareTwoBin = new JButton("CompareTwoBin");
		butCompareTwoBin.addActionListener(e -> compareTwoBin());
		buttons.add(butCompareTwoBin);

		Box contents = new Box(BoxLayout.Y_AXIS);
		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents);
		getContentPane().add(buttons, "South");

	}

//0:"all", 1:"no", 2:"invert", 3:"exists" (3 and more: with indexTwo)
//4:"by BinFolder", 5:"by Start path only", 6:"by Modified ", 7:"by Result"
//app: 0:"only", 1:"add", 2:"sub" 
	private void checking(final int indexOne, int indexTwo) {
		if (beans.isEmpty() || indexOne < 0 || indexOne >= cmbCheckItems.length) {
			return;
		}

		var bNeedFilterApp = indexOne >= cmbAppEnabStartIndex && indexOne <= cmbAppEnabEndIndex;

		if (indexTwo < 0 || indexTwo >= cmbCheckItemsApp.length) {
			if (bNeedFilterApp) {
				return;
			}

			indexTwo = 0; // for 'all','no','invert' no matter, but in array write correct number
		}

		int[] addedInfo = new int[2]; // plus and minus info
		addedInfo[0] = 0;
		addedInfo[1] = 0;

		boolean bAdd = bNeedFilterApp && indexTwo == 1;
		boolean bSub = !bAdd && bNeedFilterApp && indexTwo == 2;

		List<String> substringsAND = null;
		List<String> substringsOr = null;

		if (bNeedFilterApp && indexOne >= cmbAppEnabStartFindColumnIndex) { // by column
			substringsOr = new ArrayList<String>();
			substringsAND = FileDataBase.getSubstringsAND_DivideByOR_NullIfError(true, true, true,
					tfFindColumn.getText(), substringsOr, null);

			if (substringsOr.isEmpty()) {
				updating(lastIndex, null);
				return;
			}
		}

		for (var b : beans) {
			var res = false;

			if (bNeedFilterApp) {
				if ((b.check && bAdd) || (!b.check && bSub)) {
					continue;
				}

				if (indexOne == 3 || indexOne == 4) { // exists, no exists
					res = getStartPathExists(b, null); // exists
					if (indexOne == 4) { // no exists
						res = !res;
					}
				} else { // by column 5..8->1..4; 'find' not null here
					res = true;
					if (CommonLib.notNullEmptyList(substringsAND)) { // first finding by AND, if defined
						res = b.findSubstringsInColumn(indexOne - 4, 1, substringsAND);
					}

					if (res) {
						res = b.findSubstringsInColumn(indexOne - 4, 1, substringsOr);
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

//'addedInfo' if not null and length == 2, be added info to label	
//'index' must be null OR as indexes in 'cmbCheckItems', 'cmbCheckItemsApp'
	private void updating(int index[], int[] addedInfo) {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount(index, null, addedInfo);
	}

	/**
	 * @param index     'index' (or 'lastIndex') must be null OR as index in
	 *                  {'cmbCheckItems','cmbCheckItemsApp'}
	 * @param extNew    set list for get result
	 * @param addedInfo if not null and length == 2, be added info to label
	 * @return null if was errors while counting 'arCheckCount' or countItems
	 *         (total, arCheckCount[1]) == 0
	 */
	private int[] printCount(int[] index, List<String> extNew, int[] addedInfo) {
		lastIndex = index;
		var arCheckCount = getBeanCountOrNull(extNew);

		if (arCheckCount == null || arCheckCount.length != 2) {
			checkInfo.setText("not chosen");
			return null;
		}

		var sb = new StringBuilder();
		sb.append((index == null) ? "count" : cmbCheckItems[index[0]]);

		if (index != null && index[0] >= cmbAppEnabStartIndex && index[0] <= cmbAppEnabEndIndex) {
			sb.append(" ").append(cmbCheckItemsApp[index[1]]);
		}

		sb.append(": ").append(arCheckCount[0]).append(" / ").append(arCheckCount[1]);

		if (addedInfo != null && addedInfo.length == 2) {
			sb.append(" (+").append(addedInfo[0]).append("/-").append(addedInfo[1]).append(")");
		}

		checkInfo.setText(sb.toString());
		return arCheckCount[1] == 0 ? null : arCheckCount;
	}

	private void fillMarkMaps() {
		if (viewNoMark || butMark == null) {
			return;
		}
		butMark.setEnabled(false);

		if (!FileDataBase.initMarkIsProperty() || FileDataBase.markPropertySet.isEmpty() || beans.isEmpty()) {
			return;
		}

		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);
			b.serviceIntTwo = i;
		}

		arMarkInfoList = new ArrayList<Map<String, MarkInfo>>();
		var ft = new FutureTask<>(new MarkFillThread(butMark, beans, arMarkInfoList));
		new Thread(ft).start();
	}

	private void markInfo() {
		if (printCount(lastIndex, null, null) == null) { // null, if checked items no contains 'countBinItems'
			// (serviceIntOne)
			return;
		}

		if (viewNoMark || CommonLib.nullEmptyList(arMarkInfoList) || arMarkInfoList.size() != beans.size()) {
			return;
		}

		Set<Integer> beansSet = new HashSet<Integer>();
		for (var b : beans) {
			if (!b.check || b.getFourApp(false, false).isEmpty()) {
				continue;
			}
			beansSet.add(b.serviceIntTwo);
		}

		if (beansSet.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No found mark for checked bases");
			return;
		}

		var markInfoTable = new MarkInfoTable(this, beansSet, arMarkInfoList);
		if (markInfoTable.getIsCheckResult() != Const.MR_OK) {
			return;
		}

		List<String> extList = markInfoTable.getExtListOrNull();
		if (CommonLib.nullEmptyList(extList)) {
			return;
		}
		var sc = new ExtensionFrame(this, true, extList, null, null);
		if (sc.getIsCheckResult() == Const.MR_OK) {
			var extSet = sc.getExtsResult();
			Set<String> signaturesSet = markInfoTable.getSignaturesSetOrNull(extSet);
			if (CommonLib.nullEmptySet(signaturesSet)) {
				return;
			}
			showBeanViewTable(getFillingBeanListMarkOrNull(extSet, signaturesSet));
		}
	}

	private void showBeanViewTable(List<MyBean> beans0) {
		if (beans0 == null) {
			return;
		}

		var ct = new BeanViewTable(ViewTable.this, viewNoId3, viewNoMark, beans0);
		if (!viewNoMark) {
			if (FileDataBase.saveMarkProperty()) {
				if (ct.getIsCheckResult() != Const.MR_COPY_MOVE) {
					fillMarkMaps();
					myTable.updateUI();
				}
			}
		}

		if (ct.getIsCheckResult() == Const.MR_COPY_MOVE) {
			isCheckResult = Const.MR_COPY_MOVE;
			dispose();
		}
	}

	private void sorting(int columnIndex) {
		if (columnIndex < 0 || beans.size() < 2) {
			return;
		}

		int sortType = SortBeans.sortNoDefined;
		String sortCaption = "";
		boolean noDubleSort = false;

//columns = { "BinFolder", "Start path", "Modified", "Result *.bin" };	
		final String column = (columnIndex >= 1 && columnIndex <= 3) ? columns[columnIndex - 1] : columns[3];

		if (columnIndex == 0) {
			sortType = SortBeans.sortCheck_ThenFourStartNumber;
			sortCaption = "Check -> " + column;
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
			sortType = SortBeans.sortFourStartNumber;
			sortCaption = column;
			noDubleSort = true;
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

		setNewTitle(standardTitle.concat(sortBeans.getAppendCaption()));
	}

	private void setNewTitle(String s) {
		if (!getTitle().equals(s)) {
			setTitle(s);
		}
	}

	private void setStandardTitle() {
		if (beans.size() != sizeInCaption) {
			sizeInCaption = beans.size();
			standardTitle = captionPrefix + " (" + beans.size() + ")";
		}
		setNewTitle(standardTitle);
	}

	private void showExplorerTableOrDeleteBin() {
		final int row = myTable.getSelectedRow();
		if (row < 0) {
			return;
		}
		final MyBean b = beans.get(row);

		if (b.binPath == null || !b.binPath.toFile().exists()) {
			return;
		}

		var message = CommonLib.formatConfirmYesNoMessage(
				"Select action for bin file:" + CommonLib.NEW_LINE_UNIX + b.binPath,
				"show in Explorer Table (" + (viewNoMark ? "without" : "with") + " mark)",
				((b.getTwo().startsWith(Const.NO_FOUND_PLUS)) ? "delete bin file or " : "")
						.concat("show in Windows Explorer"),
				"<Cancel> no action");

		var confirm = JOptionPane.showConfirmDialog(this, message, "Explorer table", JOptionPane.YES_NO_CANCEL_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			String startPath = getStartPathFromBTwoOrEmpty(false, b.getTwo());
			try {
				new ExplorerTable(this, viewNoMark, getStartPathExists(b, startPath), startPath, b.binPath, null);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Error show Explorer table: " + e);
			}
			return;
		}

		if (confirm == JOptionPane.NO_OPTION) {
			deleteOrShowExplorerBinFile(b.getTwo().startsWith(Const.NO_FOUND_PLUS), row, b);
		}

	}

	// b.binPath exists, checked before call this method
	private void deleteOrShowExplorerBinFile(boolean tryDelete, int row, MyBean b) {

		if (tryDelete) {
			if (!tryDeleting(row, b)) { // if returns 'true' - need open Windows Explorer
				return;
			}
		}
		// Show Windows Explorer
		startProcess(false, b.binPath.getParent());
	}

// return 'true' to continue (not been deleted and will be open in Windows Explorer)
	private boolean tryDeleting(int row, MyBean b) { // b.binPath exists, checked before call this method
		var sDat = CommonLib.changeEndOfStringOrEmpty(b.binPath.toString(), Const.extensionBinList,
				Const.extensionBinData);
		if (sDat.isEmpty()) {
			return true;
		}

		var fDat = Path.of(sDat);
		if (!fDat.toFile().exists()) {
			return true;
		}

		var message = CommonLib.formatConfirmYesNoMessage(
				"Delete selected item (number: " + row + ")?" + CommonLib.NEW_LINE_UNIX + "Will be DELETED files:"
						+ CommonLib.NEW_LINE_UNIX + b.binPath + CommonLib.NEW_LINE_UNIX + sDat,
				"delete this files", "show in Windows Explorer", "<Cancel> no action");

		var confirm = JOptionPane.showConfirmDialog(this, message, "Delete files", JOptionPane.YES_NO_CANCEL_OPTION);

		if (confirm != JOptionPane.YES_OPTION) {
			return confirm == JOptionPane.NO_OPTION; // show in Windows Explorer
		}

		try {
			Files.delete(b.binPath);
			Files.delete(fDat);
		} catch (Exception e) {
			System.out.println("error deleting file: " + e);
		}
		beans.remove(row);
		fillMarkMaps();
		updating(null, null);
		return false;
	}

	private void compareTwoBin() {
		String[] binFoldersForConfirmAndEqualTable = new String[2];
		String[] startPathsForConfirm = new String[2];
		boolean[] existsStartPaths = new boolean[2];
		Path[] binPaths = getBinPathForCompareOrNull(existsStartPaths, startPathsForConfirm,
				binFoldersForConfirmAndEqualTable);

		if (binPaths == null) {
			JOptionPane.showMessageDialog(this,
					"For compare *.bin, check 2 items in table, they must be with correct 'StartPath' and 'BinPath'");
			return;
		}

		var sb = new StringBuilder();
		sb.append("Will be compared two *.bin from start paths:").append(CommonLib.NEW_LINE_UNIX).append("SOURCE: ")
				.append(startPathsForConfirm[0]).append(binFoldersForConfirmAndEqualTable[0])
				.append(CommonLib.NEW_LINE_UNIX).append("DEST: ").append(startPathsForConfirm[1])
				.append(binFoldersForConfirmAndEqualTable[1]);

		sb.append(CommonLib.NEW_LINE_UNIX).append(CommonLib.NEW_LINE_UNIX).append("[No]: swap 'SOURCE' and 'DEST'")
				.append(CommonLib.NEW_LINE_UNIX).append(CommonLib.NEW_LINE_UNIX)
				.append("[Cancel]: compare of equals / cancel").append(Const.UPDATE_BIN_COMPARING_REMINDER);

		var confirm = JOptionPane.showConfirmDialog(this, sb.toString(), "Compare two *.bin",
				JOptionPane.YES_NO_CANCEL_OPTION);

		boolean equalComparing = false;
		if (confirm == JOptionPane.NO_OPTION) { // SWAP
			var path = binPaths[0];
			binPaths[0] = binPaths[2];
			binPaths[2] = path;

			path = binPaths[1];
			binPaths[1] = binPaths[3];
			binPaths[3] = path;

			var tmp = existsStartPaths[0];
			existsStartPaths[0] = existsStartPaths[1];
			existsStartPaths[1] = tmp;
		} else if (confirm != JOptionPane.YES_OPTION) { // equal comparing
			if (JOptionPane.showConfirmDialog(this,
					"Will be found duplicates by size and crc; then shown in table. Continue?", "Compare of equals",
					JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
				return;
			}
			equalComparing = true;
		}

//!!!'compareLogType' = 2 recommended for full log format, because need information about equals paths/signatures
		int compareLogType = 2;
		try {

//!!! copyMode MUST BE '0', because comparing only, without checking start path exists	
// binPaths: 0, 1: source: startPath,binPath; 2, 3: dest: startPath, binPath
			var cf = new CompareFolders(false, equalComparing, program, compareLogType, 0, binPaths[0].toString(),
					binPaths[1], binPaths[2].toString(), binPaths[3], existsStartPaths, binFoldersForConfirmAndEqualTable);
			if (cf.getIsCheckResult() == Const.MR_NEED_UPDATE_BASE) {
				isCheckResult = Const.MR_NEED_UPDATE_BASE;
				pathForUpdate = binPaths[0];
				dispose();
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error: " + e);
		}

	}

// 0, 1: source: startPath,binPath; 2, 3: dest: startPath, binPath;
//'existsStartPaths' will be filled of start paths exist information, created and size == 2; FOR RETURN
//'startPathsForConfirm','binFoldersForConfirmAndEqualTable' created and size == 2; FOR RETURN	
	private Path[] getBinPathForCompareOrNull(boolean[] existsStartPaths, String[] startPathsForConfirm,
			String[] binFoldersForConfirmAndEqualTable) {
		if (printCount(lastIndex, null, null) == null) { // null, if checked items no contains 'countBinItems'
															// (serviceIntOne)
			return null;
		}
		Path[] bp1 = null; // 2 items: startPath, binPath
		Path[] bp2 = null; // 2 items too

		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			if (bp1 == null) {
				bp1 = checkBinPathOrNull(b, 0, existsStartPaths);
				if (bp1 == null) {
					return null;
				}
				startPathsForConfirm[0] = formatStartPathsForConfirm(b, existsStartPaths[0]);
				binFoldersForConfirmAndEqualTable[0] = b.getOne();
			} else if (bp2 == null) {
				bp2 = checkBinPathOrNull(b, 1, existsStartPaths);
				if (bp2 == null) {
					return null;
				}
				startPathsForConfirm[1] = formatStartPathsForConfirm(b, existsStartPaths[1]);
				binFoldersForConfirmAndEqualTable[1] = b.getOne();
			} else { // more 2 items checked
				return null;
			}
		}
		if (bp1 == null || bp2 == null) {
			return null;
		}
		Path[] res = new Path[4];
		res[0] = bp1[0];
		res[1] = bp1[1];
		res[2] = bp2[0];
		res[3] = bp2[1];
		return res;
	}

	private String formatStartPathsForConfirm(MyBean b, boolean exists) {
		return (exists ? "(exists) " : "") + "\"" + b.getTwo() + "\" ";
	}

// 'null' if error, or paths is correct; 'existsStartPaths' must be created
	private Path[] checkBinPathOrNull(MyBean b, int numberOfExistsStartPaths, boolean[] existsStartPaths) {
		if (b.binPath == null || b.serviceIntOne <= 0) { // 'serviceIntOne' as countBinItems
			return null;
		}

		if (b.binPath.toFile().exists()) {
			String startPath = getStartPathFromBTwoOrEmpty(false, b.getTwo());// 'getTwo' is column 'StartPath'
			if (startPath.isEmpty()) {
				return null;
			}
			Path[] res = new Path[4];
			existsStartPaths[numberOfExistsStartPaths] = getStartPathExists(b, startPath);

			res[0] = Path.of(startPath);
			res[1] = b.binPath;
			return res;
		}
		return null;
	}

	private List<MyBean> getFillingBeanListMarkOrNull(Set<String> set, Set<String> signaturesSet) {
		if (set.isEmpty() || signaturesSet.isEmpty()) {
			return null;
		}

		ArrayList<MyBean> beans0 = new ArrayList<MyBean>();
		Set<String> checkSetLowerCase = new HashSet<String>();

		String[] columnBinFolderId3Mark = new String[3];
		columnBinFolderId3Mark[1] = viewNoId3 ? "1" : ""; // '1' no matter -> empty or no defined
		columnBinFolderId3Mark[2] = viewNoMark ? "1" : "";

		for (var b : beans) {
//'b.getOne()' need not empty for filling beans0, because not empty in 'getPathStringFromBinItem' is sign for start filling myBean			
			if (!b.check || b.binPath == null || b.getOne().isEmpty()) {
				continue;
			}

			Set<String> setExts = new HashSet<String>();
			var it = set.iterator();
			while (it.hasNext()) {
				var s = it.next();
				if (b.mapCountExt.keySet().contains(s)) {
					setExts.add(s);
				}
			}
			if (setExts.isEmpty()) {
				continue;
			}

			var startPath = getStartPathFromBTwoOrEmpty(true, b.getTwo());

			if (!getStartPathExists(b, startPath)) {
				startPath = Const.prefixInTableForNoExists + startPath;
			}
			var bin = CommonLib.readFile(2, 0, b.binPath);

			for (var s : bin) {
				String signature = ConverterBinFunc.getSignatureOrEmpty(s);
				if (signature.isEmpty() || !signaturesSet.contains(signature)) {
					continue;
				}

				columnBinFolderId3Mark[0] = b.getOne();
				ConverterBinFunc.getPathStringFromBinItem(columnBinFolderId3Mark, startPath, s, "", b.binPath, setExts,
						beans0, checkSetLowerCase, 0);
			}
		}
		return beans0;
	}

//if 'startPath' == null, this parameters no checked; if empty - returns 'false'	
	private boolean getStartPathExists(MyBean b, String startPath) {
		boolean result = b.getOne().startsWith(Const.BRACE_START) && !b.getTwo().startsWith(Const.BRACE_START);

		return startPath == null ? result
				: startPath.isEmpty() ? false : result && Path.of(startPath).toFile().isDirectory();
	}

	private List<MyBean> getFillingBeanListOrNull(Set<String> set) {
		if (set.isEmpty()) {
			return null;
		}

		ArrayList<MyBean> beans0 = new ArrayList<MyBean>();
		Set<String> checkSetLowerCase = new HashSet<String>();

		String[] columnBinFolderId3Mark = new String[3];
		columnBinFolderId3Mark[1] = viewNoId3 ? "1" : ""; // '1' no matter -> empty or no defined
		columnBinFolderId3Mark[2] = viewNoMark ? "1" : "";

		for (var b : beans) {
//'b.getOne()' need not empty for filling beans0, because not empty in 'getPathStringFromBinItem' is sign for start filling myBean			
			if (!b.check || b.binPath == null || b.getOne().isEmpty()) {
				continue;
			}

			Set<String> setExts = new HashSet<String>();
			var it = set.iterator();
			while (it.hasNext()) {
				var s = it.next();
				if (b.mapCountExt.keySet().contains(s)) {
					setExts.add(s);
				}
			}
			if (setExts.isEmpty()) {
				continue;
			}

			var startPath = getStartPathFromBTwoOrEmpty(true, b.getTwo());
			if (!getStartPathExists(b, startPath)) {
				startPath = Const.prefixInTableForNoExists + startPath;
			}

			var bin = CommonLib.readFile(2, 0, b.binPath);

			for (var s : bin) {
				columnBinFolderId3Mark[0] = b.getOne();
				ConverterBinFunc.getPathStringFromBinItem(columnBinFolderId3Mark, startPath, s, "", b.binPath, setExts,
						beans0, checkSetLowerCase, 0);
			}
		}
		return beans0;
	}

// return not empty string, startPath in '.dat' file or adapted, if exists on other disk;
// return empty string, if error
	private String getStartPathFromBTwoOrEmpty(boolean needCheckEndSeparator, String two) {
		if (two.isEmpty()) {
			return ""; // not must be so
		}
		if (two.startsWith(Const.BRACE_START)) {
			var pos = two.indexOf(Const.BRACE_END_WITH_SPACE);// length '> '==2
			if (pos < 0) {
				return "";
			}
			two = two.substring(pos + Const.BRACE_END_WITH_SPACE.length());// length '> '==2
		}

		var pos = two.indexOf(Const.BRACE_START_FIRST_SPACE);
		String twoUpdated = pos < 0 ? two : two.substring(0, pos);
		return (twoUpdated.isEmpty() || !needCheckEndSeparator) ? twoUpdated
				: CommonLib.fileSeparatorAddIfNeed(false, true, twoUpdated);
	}

	/**
	 * @param extNew if not null: be filling with extensions, example 'fb2 : 25'
	 * @return array, checked/all or null, if 'countBinItems'(serviceIntOne) or
	 *         'mapCountExt' not defined for any checked
	 */
	private int[] getBeanCountOrNull(List<String> extNew) {
		Map<String, Integer> hm = null;
		if (extNew != null) {
			hm = new HashMap<String, Integer>();
		}
		int countItems = 0;
		int checkCount = 0;

		for (var b : beans) {
			if (!b.check) {
				continue;
			}

			if (b.serviceIntOne <= 0) {
				return null; // or continue?
			}

			checkCount++;
			countItems += b.serviceIntOne;

			if (extNew == null) {
				continue;
			}
			if (b.mapCountExt == null) {
				return null;
			}
			for (var s : b.mapCountExt.keySet()) {
				var y = b.mapCountExt.get(s);
				hm.compute(s, (k, v) -> v == null ? y : v + y);
			}
		}

		if (extNew != null) {
			extNew.clear();
			// sort hashmap
			var sortedList = CommonLib.getSortedListFromMap(hm);
			for (var s : sortedList) {
				extNew.add(s.getKey().concat(Const.extSeparator).concat(s.getValue().toString()));
			}
		}
		return new int[] { checkCount, countItems };
	}

	@Override
	public Integer call() throws Exception {
		while (isCheckResult == Const.MR_NO_CHOOSED) {
			Thread.sleep(1024);
		}
		return isCheckResult;
	}
}
