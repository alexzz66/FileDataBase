package alexnick.filedatabase;

import javax.swing.*;

import alexnick.CommonLib;

import static alexnick.CommonLib.startProcess;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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

	private int checkNow = -1;
	private final int CHECK_NOW_MAX = 3; // minimum == 0

	private String lastFindBinFolder = "";
	private JTextField tfFindBinFolder;
	private JLabel checkInfo;
	private JButton butMark = null;
	volatile private int lastSortType = SortBeans.sortNoDefined;
	private final String captionPrefix;
	private String caption = "";
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

		Box contents = new Box(BoxLayout.Y_AXIS);
		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, false, false, true, columns[0],
				columns[1], columns[2], columns[3], beans);
		myTable.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1 && e.getButton() == 1) {
					sorting(myTable.convertColumnIndexToModel(myTable.columnAtPoint(e.getPoint())));
				}
			}

		});
		contents.add(new JScrollPane(myTable));

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

				if (e.getClickCount() == 1 && myTable.getSelectedColumn() == 0) {
					printCount(true, null);
					return;
				}

				if (e.getClickCount() != 2 || myTable.getSelectedRowCount() != 1) {
					return;
				}
				showExplorerTableOrDeleteBin();
			}
		});

		var butCheckActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (beans.isEmpty()) {
					return;
				}

				var clickOnTfFindBindFolder = e.getActionCommand().equals(Const.textFieldBinFolderClick);
				var findBinFolder = tfFindBinFolder.getText().toLowerCase();

				if (!findBinFolder.equals(lastFindBinFolder) || clickOnTfFindBindFolder) {
					checkNow = findBinFolder.isEmpty() ? 0 : 1;
					lastFindBinFolder = findBinFolder;
				} else { // 0:no;1:filter(optional);2:exists;3:all
					checkNow = (checkNow <= 0) ? 1 : (checkNow >= CHECK_NOW_MAX) ? 0 : checkNow + 1;

					if (checkNow == 1 && findBinFolder.isEmpty()) {// filter, no need if empty
						checkNow = 2;
					}
				}

				for (var b : beans) {
					b.check = (checkNow <= 0) ? false
							: (checkNow >= CHECK_NOW_MAX) ? true
									: (checkNow == 2) ? getStartPathExists(b, null)
											: b.findInOneLowerCase(findBinFolder, Const.textFieldFindSeparator);// filter
				}
				updating(false);
			}
		};

//FILL JPANEL
		JPanel buttons = new JPanel();
		tfFindBinFolder = new JTextField(FileDataBase.sizeTextField);
		tfFindBinFolder.setActionCommand(Const.textFieldBinFolderClick);
		tfFindBinFolder.addActionListener(butCheckActionListener);
		tfFindBinFolder.setToolTipText(Const.textFieldBinFolderToolTip);

		buttons.add(tfFindBinFolder);
		var butCheck = new JButton("check");
		butCheck.addActionListener(butCheckActionListener);
		butCheck.setToolTipText(Const.butCheckToolTip);
		buttons.add(butCheck);

		var butInvert = new JButton("invert");
		butInvert.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (var b : beans) {
					b.check = !b.check;
				}
				updating(true);
			}
		});
		buttons.add(butInvert);

		checkInfo = new JLabel();
		printCount(true, null); // set count on start window
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
				if (printCount(false, extNew) == null || extNew.isEmpty()) {
					return;
				}
				var sc = new ExtensionFrame(ViewTable.this, true, extNew, null, null);
				if (sc.getIsCheckResult() == Const.MR_OK) {
					showBeanViewTable(getFillingBeanListOrNull(sc.getExtsResult()));
				}
			}
		});
		buttons.add(butViewExts);

		JButton butClose = new JButton("Close");
		butClose.addActionListener(e -> {
			dispose();
		});
		buttons.add(butClose);

		JButton butCompareTwoBin = new JButton("CompareTwoBin");
		butCompareTwoBin.addActionListener(e -> compareTwoBin());
		buttons.add(butCompareTwoBin);

		getContentPane().add(contents);
		getContentPane().add(buttons, "South");

		setPreferredSize(new Dimension(1024, 600));
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
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

	private void updating(boolean resetCheckNow) {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount(resetCheckNow, null);
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
			sortType = SortBeans.sortCheck_ThenFour;
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
		var sortBeans = new SortBeans(sortType, sortCaption, beans);
		setNewTitle(caption.concat(sortBeans.getAppendCaption()));
	}

	private void setNewTitle(String s) {
		if (!getTitle().equals(s)) {
			setTitle(s);
		}
	}

	private void setStandardTitle() {
		if (beans.size() != sizeInCaption) {
			sizeInCaption = beans.size();
			caption = captionPrefix + " (" + beans.size() + ")";
		}
		setNewTitle(caption);
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
		updating(true);
		return false;
	}

	private void compareTwoBin() {
		String[] columnsForCompare = new String[2];
		boolean[] existsStartPaths = new boolean[2];
		Path[] binPaths = getBinPathForCompareOrNull(existsStartPaths, columnsForCompare);
		if (binPaths == null) {
			JOptionPane.showMessageDialog(this,
					"For compare *.bin, check 2 items in table, they must be with correct 'StartPath' and 'BinPath'");
			return;
		}

		String s = "Will be compared two *.bin from start paths:" + CommonLib.NEW_LINE_UNIX + "SOURCE: "
				+ columnsForCompare[0] + CommonLib.NEW_LINE_UNIX + "DEST: " + columnsForCompare[1];

		var confirm = JOptionPane.showConfirmDialog(this,
				s.concat(CommonLib.NEW_LINE_UNIX + CommonLib.NEW_LINE_UNIX + "[No]: swap 'SOURCE' and 'DEST'"),
				"Compare two *.bin", JOptionPane.YES_NO_CANCEL_OPTION);

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
		} else if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

//!!!'compareLogType' = 2 recommended for full log format, because need information about equals paths/signatures
		int compareLogType = 2;
		try {

//!!! copyMode MUST BE '0', because comparing only, without checking start path exists	
// binPaths: 0, 1: source: startPath,binPath; 2, 3: dest: startPath, binPath
			var cf = new CompareFolders(program, compareLogType, 0, binPaths[0].toString(), binPaths[1],
					binPaths[2].toString(), binPaths[3], existsStartPaths[0]);
			if (cf.getIsCheckResult() == Const.MR_NEED_UPDATE_BASE) {
				isCheckResult = Const.MR_NEED_UPDATE_BASE;
				pathForUpdate = binPaths[0];
				dispose();
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error: " + e);
		}

	}

// 0, 1: source: startPath,binPath; 2, 3: dest: startPath, binPath; 'columnsForCompare' created and size == 2
//'existsStartPaths' will be filling of start paths exist information, must be created and size == 2
	private Path[] getBinPathForCompareOrNull(boolean[] existsStartPaths, String[] columnsForCompare) {

		if (printCount(false, null) == null) { // null, if checked items no contains 'countBinItems' (serviceIntOne)
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
				columnsForCompare[0] = formatColumnForCompare(b, existsStartPaths[0]);
			} else if (bp2 == null) {
				bp2 = checkBinPathOrNull(b, 1, existsStartPaths);
				if (bp2 == null) {
					return null;
				}
				columnsForCompare[1] = formatColumnForCompare(b, existsStartPaths[1]);
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

	private String formatColumnForCompare(MyBean b, boolean exists) {
		return (exists ? "(exists) " : "") + "\"" + b.getTwo() + "\" " + b.getOne(); // column as is, for confirmation;
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
						beans0,checkSetLowerCase);
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
						beans0, checkSetLowerCase);
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
	 * @param resetCheckNow if 'true', in result label be written 'count', else
	 *                      depends on 'checkNow'
	 * @param extNew        set list for get result
	 * @return
	 */
	private int[] printCount(boolean resetCheckNow, List<String> extNew) {
		var arCheckCount = getBeanCountOrNull(extNew);
		final String noCheck = "not chosen";
		if (arCheckCount == null || arCheckCount.length != 2) {
			checkInfo.setText(noCheck);
			return null;
		}
		if (resetCheckNow) {
			checkNow = -1;
		}
		var s = (checkNow < 0 || checkNow > CHECK_NOW_MAX) ? "count"
				: checkNow == 2 ? "exists" : checkNow == CHECK_NOW_MAX ? "all" : checkNow == 1 ? "filter" : noCheck;

		if (!s.equals(noCheck) || arCheckCount[1] > 0) {
			s += ": " + arCheckCount[0] + " / " + arCheckCount[1];
		}
		checkInfo.setText(s);
		return arCheckCount;
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
		return countItems == 0 ? null : new int[] { checkCount, countItems };
	}

	@Override
	public Integer call() throws Exception {
		while (isCheckResult == Const.MR_NO_CHOOSED) {
			Thread.sleep(1024);
		}
		return isCheckResult;
	}
}
