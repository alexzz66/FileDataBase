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
import java.awt.event.MouseListener;
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

	private JTextField tfFindBinFolder;
	private JLabel checkInfo;
	private JButton butMark;
	private JCheckBox cbFilter;
	private JComboBox<String> cmbFindPosition;
	private JComboBox<String> cmbFindFullPathOrName;

	private JTextField tfFindPath;
	private JLabel choosed;

	private JTextField tfInfo;
	private int checkNow = -1;
	private String[] cmbCheckItems = new String[] { "all", "exist", "no", "invert", "by BinFolder only",
			"by Signature only", "by Modified only", "by Path only", "by BinFolder add", "by Signature add",
			"by Modified add", "by Path add" };

	private final String caption;
	volatile private int lastSortType = SortBeans.sortNoDefined;

	// 'beans0' not null
	public BeanViewTable(JFrame frame, boolean viewNoId3, boolean viewNoMark, List<MyBean> beans0) {
		super(frame, true);
		FileDataBase.isShiftDown = false;

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
		Box contents = new Box(BoxLayout.Y_AXIS);
		myTable.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1 && e.getButton() == 1) {
					sorting(myTable.convertColumnIndexToModel(myTable.columnAtPoint(e.getPoint())));
				}
			}

		});

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
						printCount(true, false, null);
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
		ActionListener butCheckActionListener = e -> checking(cmbChecking.getSelectedIndex());
		cmbChecking.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tfFindBinFolder.setEnabled(cmbChecking.getSelectedIndex() >= 4);
			}
		});
		cmbChecking.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					checking(cmbChecking.getSelectedIndex());
				}
			}
		});

		tfFindBinFolder = new JTextField(FileDataBase.sizeTextField);
		tfFindBinFolder.addActionListener(butCheckActionListener);
		tfFindBinFolder.setToolTipText(Const.textFieldBinFolderToolTip);
		tfFindBinFolder.setEnabled(false);

		var butCheck = new JButton("check");
		butCheck.addActionListener(butCheckActionListener);

		checkInfo = new JLabel();
		printCount(true, false, null); // check on show window

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

		JComboBox<String> cmbAction = new JComboBox<>(
				new String[] { "copy/move", "toList all/paths", "toList pathsNoRoot", "generate *.bin" });
		JButton butAction = new JButton(">>");
		butAction.addActionListener(e -> doAction(cmbAction.getSelectedIndex()));

//FILL JPANEL (second)
		JPanel buttons = new JPanel();
		buttons.add(cmbChecking);
		buttons.add(tfFindBinFolder);
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

		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents, BorderLayout.CENTER);

		var scrollPan = new JScrollPane(buttons, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPan, BorderLayout.SOUTH);

//SHIFT DOWN				
		myTable.addKeyListener(FileDataBase.keyListenerShiftDown);

		cmbChecking.addKeyListener(FileDataBase.keyListenerShiftDown);
		tfFindBinFolder.addKeyListener(FileDataBase.keyListenerShiftDown);
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

//0:"all", 1:"exist", 2:"no", 3:"invert", 4:"by BinFolder only", 5:"by Signature only", 6:"by Modified only", 7:"by Path only",
//8:"by BinFolder add", 9:"by Signature add", 10:"by Modified add", 11:"by Path add"	
	private void checking(final int index) {
		if (index < 0 || index >= cmbCheckItems.length || beans.isEmpty()) {
			return;
		}

		int[] addedInfo = new int[2]; // plus and minus info
		addedInfo[0] = 0;
		addedInfo[1] = 0;

		boolean bAdd = index >= 8;
		int i = bAdd ? index - 4 : index; // 8..11->4..7

		checkNow = index;
		String find = null;

		if (i >= 4) {
			find = tfFindBinFolder.getText().toLowerCase();
			if (find.isEmpty()) {
				updating(false, null);
				return;
			}
		}

		for (var b : beans) {
			var z = false;

			if (i >= 4) { // by column, 4..7->1..4
				z = b.findInColumnLowerCase(i - 3, find, Const.textFieldFindSeparator);
				if (!z && bAdd) { // if not result, for 'add' no change
					continue;
				}
			} else if (i == 0) { // all
				z = true;
			} else if (index == 1) { // exist
				z = !b.isFourPrefixNoExists();
			} else if (i == 2) { // no
				z = false;
			} else if (i == 3) { // invert
				z = !b.check;
			} else {
				continue; // must not be so...
			}

			if (b.check != z) {
				if (z) {
					addedInfo[0]++;
				} else {
					addedInfo[1]++;
				}
				b.check = z;
			}
		}

		updating(false, addedInfo);
	}

//0:copy/move, 1:toList all/paths, 2:toList pathsNoRoot, 3:generate .*bin  
	private void doAction(int selectedIndex) {
		if (selectedIndex == 0) {
			doCopyMove();
			return;
		}

		if (selectedIndex == 3) {
			generateBin();
			return;
		}
		FileDataBase.beansToList(false, selectedIndex == 2 ? 3 : 2, null, beans);
	}

	private void generateBin() {
		int checkCount = printCount(false, true, null);
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

			if (CommonLib.nullEmptyString(b.serviceString)) {
				errorNumbersSet.add(i);
				continue;
			}

			if (!beansSet.add(b.serviceString)) {
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

				updating(true, null);
			}
		}

		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		var resultList = CommonLib.getListFromSet(2, beansSet);
		Path path = FileDataBase.getTempPath(Const.GENERATED_BIN_NAME);

		if (CommonLib.saveToFile(false, 1, CopyMove.DeleteIfExists_OLD_DELETE, path, null, resultList)) {
			CommonLib.startProcess(false, path.getParent());
		}
	}

	private void doCopyMove() {
		int checkCount = printCount(false, true, null);
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
			updating(true, null);
			equalsPathInfo = "NB: was been excluded and unchecked equal paths: " + equalsPathCountForCopying
					+ CommonLib.NEW_LINE_UNIX;
		}

		if (!copyFilesEqualSignatureList.isEmpty()) {
			CommonLib.saveAndShowList(false, 1, FileDataBase.getTempPath("equalSignaturesList.txt"),
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

				updating(true, null);
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

		checkCount = printCount(false, true, null);
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
				updating(true, null);
			}

		}

		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		var tempPath = FileDataBase.getTempPathForCopyMove();
		if (!CommonLib.saveToFile(true, 1, CopyMove.DeleteIfExists_OLD_RENAME_TO_BAK, tempPath, null, copyFilesList)) {
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
		int checkCount = printCount(false, true, null); // recount, because click of mouse on table may be no corrected
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

			updating(true, null);
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
	private void updating(boolean resetCheckNow, int[] addedInfo) {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount(resetCheckNow, false, addedInfo);
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
				sortType = SortBeans.sortCheck_ThenFour;
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
		var sortBeans = new SortBeans(sortType, sortCaption, beans);
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
	private int printCount(boolean resetCheckNow, boolean messageIfNoChecked, int[] addedInfo) {
		int checkCount = 0;
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			checkCount++;
		}

		if (resetCheckNow) {
			checkNow = -1;
		}
//TODO checkNow
		var sb = new StringBuilder();
		sb.append((checkNow < 0 || checkNow >= cmbCheckItems.length) ? "count" : cmbCheckItems[checkNow]);
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
		String substringInLowerCase = "";
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

		if (isFilter) {
			substringInLowerCase = tfFindPath.getText().toLowerCase();
			if (substringInLowerCase.isEmpty()) {
				return;
			}

			tmpLastSearchHash = "\"" + substringInLowerCase + "\"" + Const.BRACE_START_FIRST_SPACE
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
					var pathString = b.getFourLowerCase(true, withExt);
					if (pathString.isEmpty()) {
						continue;
					}
					// findArea:fullPath,name,parent
					if (findFullPathOrName > 0) {
						var sep = pathString.lastIndexOf(File.separator);
						if (sep < 0) {
							continue;
						}
						pathString = findFullPathOrName == 1 ? pathString.substring(sep + 1)
								: pathString.substring(0, sep);
					}
					if (!b.findInLowerCase(findPosition, pathString, substringInLowerCase,
							Const.textFieldFindSeparator)) {
						continue;
					}
					beans.add(b);

				} catch (Exception e) {
				}
			}
		}
		sorting(4);
		setLabelChoosed(beans);
		updating(true, null);
	}

	private void setLabelChoosed(List<MyBean> beans0) {
		final String ch = "chosen: ";
		choosed.setText(ch + beans0.size());
	}

}
