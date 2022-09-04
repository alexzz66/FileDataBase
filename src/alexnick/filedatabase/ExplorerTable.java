package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import alexnick.CommonLib;

public class ExplorerTable extends JDialog implements Callable<Integer> {
	private static final long serialVersionUID = 1L;
	private static String[] columns;
	private int isCheckResult = Const.MR_NO_CHOOSED;

	private List<MyBean> beans = null;
	private BeansFourTableDefault myTable;

	final private Path binPath;
	final private String pathForBeansRoot; // no lower case, with file separator
	Map<String, DirInfo> folderMap = null;

	private String pathForBeans = "";
	private JButton butUp = null;
	private JButton butPrevious = null;
	private JButton butNext = null;
	private JTextField tfPathForBeans = null;
	final private String standardTitle;
	volatile private int lastSortType = SortBeans.sortNoDefined;
	private List<String> previousKeys = null;// keys in lower case
	private List<String> nextKeys = null;// keys in lower case
	private boolean filesCanExist;

//COMPONENTS for checking, toList	
	private JTextField tfFindColumn;
	private JLabel checkInfo;
	private int[] lastIndex = { 0, 0 }; // first as cmbCheckItems index;second as cmbCheckItemsApp

	private final String[] cmbCheckItems; // init in constructor
	private String[] cmbCheckItemsApp; // init in constructor

	// append const indexes from 'cmbCheckItems'
	private final int cmbAppEnabStartIndex;
	private final int cmbAppEnabEndIndex;
	private final int textSearchIndex; // must be -42 (if not defined) or corrected (cmbItemsList.size - 2)

	/**
	 * @param frame           may be 'null' or 'this' in case calling from some
	 *                        Frame
	 * @param viewNoMark      if 'false' -> for files, into column 'info' be append
	 *                        'Mark'
	 * @param filesCanExist   if 'true', on double click in table with files, will
	 *                        be try opening parent folder / file (if Shift pressed)
	 * @param startPathString path before each path in '*.bin', must be correct path
	 *                        of folder; MUST NOT BE null/empty
	 * @param binPath         path to exist '.bin' file
	 * @param initialPath     if not null/empty, the table will be showed from this
	 *                        path
	 * @throws IOException
	 */
	public ExplorerTable(JFrame frame, boolean viewNoMark, boolean filesCanExist, String startPathString, Path binPath,
			String initialPath) throws IOException {
		super(frame, true);
		FileDataBase.isShiftDown = false;

		if (binPath == null || !binPath.toFile().exists() || binPath.toFile().isDirectory()) {
			startPathString = "";
		}

		List<String> cmbItemsList = List.of("all", "no", "invert", "by Type, size", "by Name", "by ExtInfo, mark",
				"by Full path", "toList all/paths", "toList pathsNoRoot", "textSearch", "check exists", "openWithFiles",
				"openWithFolders");

		int endIndex = cmbItemsList.size() - 1;
		if (!filesCanExist) {
			endIndex -= 4;
		}

		cmbCheckItems = CommonLib.getArrayFromListOrNullByIndexes(0, endIndex, cmbItemsList);
		cmbAppEnabStartIndex = 3;
		cmbAppEnabEndIndex = 6;

		textSearchIndex = filesCanExist ? (cmbItemsList.size() - 2) : -42;

		List<String> cmbItemsAppList = List.of("only", "add", "sub", "onlyCase", "addCase", "subCase", "TEST");
		endIndex = cmbItemsAppList.size() - 1;
		if (textSearchIndex < 0) { // not defined testSearch
			endIndex -= 1;
		}

		cmbCheckItemsApp = CommonLib.getArrayFromListOrNullByIndexes(0, endIndex, cmbItemsAppList);

		columns = new String[] { "Type / Size", "Name",
				"Extensions info / Crc,modified" + (viewNoMark ? "" : " **mark"), "Full path" };
		// 'startFolderFile' must be null or correct path
		File startFolderFile = CommonLib.nullEmptyString(startPathString) ? null : Path.of(startPathString).toFile();

		this.binPath = binPath;
		this.filesCanExist = filesCanExist;
		pathForBeansRoot = startFolderFile == null ? ""
				: CommonLib.fileSeparatorAddIfNeed(false, true, startFolderFile.toString());
		standardTitle = "Explorer table (files can " + (filesCanExist ? "" : "not ") + "exist)";

		if (startFolderFile == null) {
			CommonLib.errorArgument("not correct *.bin path: " + binPath);
			return;
		}

		initFolderMap(viewNoMark);
		if (CommonLib.nullEmptyMap(folderMap)) {
			CommonLib.errorArgument("no found items for explorer table");
			return;
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
		var s2 = CommonLib.nullEmptyString(initialPath) ? startFolderFile.toString() : initialPath;

		var path = Path.of(s2);
		if (path == null) {
			s2 = "";
		} else {
			s2 = path.toString();// no 'lower case', no add 'file separator' -> later it
		}

		previousKeys = new ArrayList<String>();
		nextKeys = new ArrayList<String>();

		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, true, false, false, columns[0],
				columns[1], columns[2], columns[3], beans);
		initComponents();

		// 'initBeans()' after 'initTable()'
		initBeans(false, false, s2.isEmpty() ? pathForBeansRoot : s2);
		setStandardTitle();
		var t = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds(0, 0, t.width - 120, t.height - 80);

		setLocationRelativeTo(null);
		setVisible(true);
	}

//INIT COMPONENTS	
	private void initComponents() { // on constructor
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
				var bFindEnab = (index == textSearchIndex)
						|| (index >= cmbAppEnabStartIndex && index <= cmbAppEnabEndIndex);
				tfFindColumn.setEnabled(bFindEnab);
				cmbCheckingApp.setEnabled(bFindEnab);
			}
		});

		tfFindColumn = new JTextField(FileDataBase.sizeTextField);
		tfFindColumn.addActionListener(butCheckActionListener);
		tfFindColumn.setToolTipText(Const.textFieldBinFolderToolTip);
		tfFindColumn.setEnabled(false);

		var butCheck = new JButton("set");
		butCheck.addActionListener(butCheckActionListener);

		checkInfo = new JLabel();
		printCount(0, null, false, null); // check on show window

		butUp = new JButton("Up");
		butUp.setToolTipText("Shift + click: open root folder");
		butUp.addActionListener(e -> initBeans(true, true, getUpPathForBeansOrEmpty(FileDataBase.isShiftDown)));

		butPrevious = new JButton("<=");
		butPrevious.setEnabled(false);
		butPrevious.addActionListener(e -> previousNext(false));

		butNext = new JButton("=>");
		butNext.addActionListener(e -> previousNext(true));
		butNext.setEnabled(false);

		tfPathForBeans = new JTextField(64);
		tfPathForBeans.setEditable(false);

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
						printCount(0, null, false, null);
					}
					return;
				}

				if (e.getClickCount() == 2) {
					doMyTableDoubleClick();
				}
			}
		});

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

		tfFindColumn.addKeyListener(FileDataBase.keyListenerShiftDown);
		butCheck.addKeyListener(FileDataBase.keyListenerShiftDown);

		butUp.addKeyListener(FileDataBase.keyListenerShiftDown);
		butPrevious.addKeyListener(FileDataBase.keyListenerShiftDown);
		butNext.addKeyListener(FileDataBase.keyListenerShiftDown);

		tfPathForBeans.addKeyListener(FileDataBase.keyListenerShiftDown);

//ADDING	
		JPanel buttons = new JPanel();

		buttons.add(cmbChecking);
		buttons.add(cmbCheckingApp);

		buttons.add(tfFindColumn);
		buttons.add(butCheck);
		buttons.add(checkInfo);

		buttons.add(butUp);
		buttons.add(butPrevious);
		buttons.add(butNext);

		buttons.add(tfPathForBeans);

		JTextArea area = new JTextArea(3, 0); // add 'buttons' height
		area.setBackground(buttons.getBackground());
		area.setEditable(false);
		buttons.add(area);
		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));

		Box contents = new Box(BoxLayout.Y_AXIS);

		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents, BorderLayout.CENTER);

		var scrollPan = new JScrollPane(buttons, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPan, BorderLayout.SOUTH);
	}

//0:"all", 1:"no", 2:"invert"
//3:"by Type, size", 4:"by Name", 5:"by ExtInfo, mark",	6:"by Full path"
//7:"toList all/paths", 8:"toList pathsNoRoot",
//OPTIONAL >> 9:"textSearch", 10:"check exists", 11:"openWithFiles", 12:"openWithFolders" >> init together if 'filesCanExist'
//indexTwo, app: 0:"only", 1:"add", 2:"sub" , 3:"onlyCase", 4:"addCase", 5:"subCase", 6: "TEST" (optional)
	private void checking(final int indexOne, int indexTwo) {
		if (beans.isEmpty() || indexOne < 0 || indexOne >= cmbCheckItems.length) {
			return;
		}

		if (indexOne == 11 || indexOne == 12) { // 11:"openWithFiles", 12:"openWithFolders"
			toCommandLine(indexOne == 11);
			return;
		}

		if (indexOne == 7 || indexOne == 8) { // last indexes -> to list
			// if not 'filesCanExist', no checking on existing files
			FileDataBase.beansToList(!filesCanExist, indexOne == 8 ? 3 : 2, null, beans);
			return;
		}

		var bNeedFilterApp = (indexOne == textSearchIndex)
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

		int[] addedInfo = new int[2]; // plus and minus info
		addedInfo[0] = 0;
		addedInfo[1] = 0;

		boolean bAdd = bNeedFilterApp && indexTwoResult == 1;
		boolean bSub = !bAdd && bNeedFilterApp && indexTwoResult == 2;

		List<String> substringsAND = null;
		List<String> substringsOr = null;

		if (bNeedFilterApp) { // by column, textSearch
			substringsOr = new ArrayList<String>();
			substringsAND = FileDataBase.getSubstringsAND_DivideByOR_NullIfError(true, toLowerCase,
					tfFindColumn.getText(), substringsOr);

			if (substringsOr.isEmpty()) {
				updating(lastIndex, null);
				return;
			}

			if (test) {
				FileDataBase.testInfo(this, substringsAND, substringsOr);
				return;
			}
		}

		for (var b : beans) {
			var res = false; // false by default

			if (bNeedFilterApp) {

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

				} else { // by column 3..6->1..4; 'find' not null here
					res = true;

					if (CommonLib.notNullEmptyList(substringsAND)) { // first finding by AND, if defined
						res = b.findSubstringsInColumn(indexOne - 2, toLowerCase ? 1 : 0, substringsAND);
					}

					if (res) {
						res = b.findSubstringsInColumn(indexOne - 2, toLowerCase ? 1 : 0, substringsOr);
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
			} else if (indexOne == 10) { // check exists (optional)
				try {
					res = Path.of(b.getFour(false, true)).toFile().exists();
				} catch (Exception e) {
					res = false;
				}
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

//'files' if true: files; if false: folders	
	private void toCommandLine(boolean files) {
		var numbers = printCount(files ? 1 : 2, null, true, null);
		if (CommonLib.nullEmptySet(numbers)) {
			return;
		}

		FileDataBase.toCommandLine(this, true, files ? 1 : 2, 0, numbers, beans);
	}

	private void previousNext(boolean next) {
		List<String> list = next ? nextKeys : previousKeys;
		if (list.isEmpty()) {
			return;
		}

		var s = list.remove(list.size() - 1);

		if (next) {
			if (list.isEmpty()) {
				butNext.setEnabled(false);
			}
			previousKeys.add(pathForBeans.toLowerCase());
			butPrevious.setEnabled(true);
		} else {
			if (list.isEmpty()) {
				butPrevious.setEnabled(false);
			}
			nextKeys.add(pathForBeans.toLowerCase());
			butNext.setEnabled(true);
		}

		initBeans(false, false, s);
	}

	private String getUpPathForBeansOrEmpty(boolean needRoot) {
		if (CommonLib.nullEmptyString(pathForBeans) || !pathForBeans.endsWith(File.separator)) {
			return "";
		}

		if (needRoot) {
			return pathForBeansRoot;
		}

		var s = pathForBeans.toLowerCase(); // will be with file separator on end
		if (!canUp(s)) {
			return "";
		}

		s = s.substring(0, pathForBeans.length() - 1);

		var pos = s.lastIndexOf(File.separator);
		return pos < 0 ? "" : s.substring(0, pos + 1);
	}

// for 'folder' - try open that; for 'file' and 'filesCanExist' open in OS Windows explorer
	private void doMyTableDoubleClick() {
		int y = checkAndGetSelected();
		if (y < 0) {
			return;
		}

		if (beans.get(y).serviceIntOne == CommonLib.SIGN_FOLDER) {
			initBeans(true, true, pathForBeans.concat(beans.get(y).getTwo()));
			return;
		}

		if (filesCanExist) {
			FileDataBase.openDirectory(1, FileDataBase.isShiftDown, myTable, beans);
		}
	}

	/**
	 * @return number of selected (must be one); or '-1' if error
	 */
	private int checkAndGetSelected() {
		if (myTable.getSelectedRowCount() != 1) {
			return -1;
		}
		int y = myTable.getSelectedRow();
		if (y < 0) {
			return -1;
		}
		return y;
	}

//'addedInfo' if not null and length == 2, be added info to label	
//'index' must be null OR as indexes in 'cmbCheckItems', 'cmbCheckItemsApp'
	private void updating(int index[], int[] addedInfo) {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount(0, index, false, addedInfo);
	}

	/**
	 * @param typeReturn         1:numbers of files; 2: of folders; else (0): return
	 *                           null
	 * @param index              must be null OR as indexes in 'cmbCheckItems',
	 *                           'cmbCheckItemsApp'
	 * @param messageIfNoChecked if true and result count == 0, will be message<br>
	 *                           NB: for not null 'resultSet', result count be set
	 *                           as size 'resultSet'
	 * @param addedInfo          if not null and length == 2, be added info to label
	 * @return null or 'resultSet'
	 */
	private Set<Integer> printCount(int typeReturn, int[] index, boolean messageIfNoChecked, int[] addedInfo) {
		lastIndex = index;

		int checkCount = 0;

		if (typeReturn < 0 || typeReturn > 2) {
			typeReturn = 0;
		}

		Set<Integer> resultSet = typeReturn > 0 ? new HashSet<>() : null;

		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);

			if (!b.check) {
				continue;
			}

			checkCount++;

			if (resultSet != null) {
				// may by SIGN_FILE or SIGN_FOLDER only
				if (b.serviceIntOne == CommonLib.SIGN_FOLDER) { // folders
					if (typeReturn == 2) {
						resultSet.add(i);
					}
				} else { // files
					if (typeReturn == 1) {
						resultSet.add(i);
					}
				}
			}

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

		if (resultSet != null) {
			checkCount = resultSet.size();
		}

		if (checkCount == 0 && messageIfNoChecked) {
			JOptionPane.showMessageDialog(this, "No checked items");
		}

		return resultSet;
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

//columns = { "Type / Size", "Name", "Extensions info / Crc,modified", "Full path" };
		final String column = (columnIndex >= 1 && columnIndex <= 3) ? columns[columnIndex - 1] : columns[3];

		if (columnIndex == 0) {
			sortType = SortBeans.sortCheck;
			sortCaption = "Checked -> " + column;
		} else if (columnIndex == 1) {
			sortType = SortBeans.sortOne;
			sortCaption = column;
			noDubleSort = true;
		} else if (columnIndex == 2) {
			if (lastSortType == SortBeans.sortTwoLowerCase) {
				sortType = SortBeans.sortServiceStringOneNoCheckForNull;
				sortCaption = "Name's extensions";
			} else {
				sortType = SortBeans.sortTwoLowerCase;
				sortCaption = column;
			}

			noDubleSort = true;
		} else if (columnIndex == 3) {
			if (lastSortType == SortBeans.sortThree) {
				sortType = SortBeans.sortServiceLong;
				sortCaption = "Modified";
			} else {
				sortType = SortBeans.sortThree;
				sortCaption = column;
			}
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
		var sortBeans = new SortBeans(sortType, sortCaption, beans, myTable);
		if (!sortBeans.isBeansWasSorted()) {
			return;
		}

		setNewTitle(standardTitle.concat(sortBeans.getAppendCaption()));
	}

	/**
	 * @param addToPreviousKeys add current folder ('pathForBeans') to
	 *                          'previousKeys'
	 * @param clearNextKeys
	 * @param keyFolderMap      will set to lower case and add file separator on
	 *                          end, if need
	 */
	private void initBeans(boolean addToPreviousKeys, boolean clearNextKeys, String keyFolderMap) {
		if (CommonLib.nullEmptyString(keyFolderMap)) {
			return;
		}

		keyFolderMap = CommonLib.fileSeparatorAddIfNeed(false, true, keyFolderMap.toLowerCase());
		butUp.setEnabled(canUp(keyFolderMap));
		var dirInfo = folderMap.get(keyFolderMap);
		if (dirInfo == null) {
			return;
		}

		if (addToPreviousKeys && CommonLib.notNullEmptyString(pathForBeans)
				&& CommonLib.addItemtoList(true, pathForBeans.toLowerCase(), previousKeys)) {
			butPrevious.setEnabled(true);
		}

		if (clearNextKeys) {
			nextKeys.clear();
			butNext.setEnabled(false);
		}

		pathForBeans = dirInfo.fullPathCanonical;
		beans.clear();
		tfPathForBeans.setText(pathForBeans);

		var mapFolders = dirInfo.transitFoldersNames;

//FOLDERS		
		for (var folderName : mapFolders) {
			var subKeyFolderMap = keyFolderMap.concat(folderName.toLowerCase()) + File.separator;
			var subDirInfo = folderMap.get(subKeyFolderMap);

			String one = "$<dir> ";
			String two = folderName;
			var sbThree = new StringBuilder();

			if (subDirInfo != null) {
				one += CommonLib.bytesToKBMB(false, 4, subDirInfo.sizeTotalFiles) + "; files: "
						+ subDirInfo.countTotalFiles;

				sbThree.append("[");
				for (var ext : subDirInfo.extsInfoMap.keySet()) {
					sbThree.append(ext).append(":").append(subDirInfo.extsInfoMap.get(ext)).append("; ");
				}
				sbThree.append("]");
			}

			String four = pathForBeans + folderName;

			var bean = new MyBean(one, two, sbThree.toString(), four, "");
			bean.serviceIntOne = CommonLib.SIGN_FOLDER;
			bean.serviceStringOne = "";
			bean.serviceLong = Long.MAX_VALUE - subDirInfo.sizeTotalFiles >>> 6;
			beans.add(bean);
		}

//FILES
		var mapFiles = dirInfo.filesMap;
		for (var fileName : mapFiles.keySet()) {
			var fileInfo = mapFiles.get(fileName);
			String one = CommonLib.bytesToKBMB(false, 4, fileInfo.getSize());
			String two = fileName;
			String three = fileInfo.getCrc() + ", " + CommonLib.dateModifiedToString(fileInfo.getDate())
					+ fileInfo.getMark();
			String four = pathForBeans + fileName;

			var bean = new MyBean(one, two, three, four, "");
			bean.serviceIntOne = CommonLib.SIGN_FILE;
			bean.serviceStringOne = fileInfo.getExtForFourApp();
			bean.serviceLong = fileInfo.getDate();
			beans.add(bean);
		}

		updating(null, null);
	}

	private boolean canUp(String key) {
//commented, because -> must not be null or empty!!! if (pathForBeansRootLowerCase.isEmpty()) {return !key.isEmpty();}		

		return key.startsWith(pathForBeansRoot.toLowerCase()) && !key.equalsIgnoreCase(pathForBeansRoot);
	}

	private void initFolderMap(boolean viewNoMark) { // on constructor
		var list = CommonLib.readFile(1, 0, binPath);
		if (list.isEmpty()) {
			return;
		}

		folderMap = new HashMap<String, DirInfo>();
//INIT MARK PROPERTY
		if (!viewNoMark && (!FileDataBase.initMarkIsProperty() || FileDataBase.markPropertySet.isEmpty())) {
			viewNoMark = true;
		}

//for FileInfo: (long)date,size,crc; 
		for (var s : list) {
			Path path = ConverterBinFunc.getPathFromBinItemOrNull(pathForBeansRoot, s);
			if (path == null) {
				continue;
			}

			var ar = ConverterBinFunc.dividePathToAll_Ext(0, path.toString());
			if (ar[0] == null) {
				continue;
			}

			long[] arrInf = ConverterBinFunc.getDecodeDateSizeCrc(s);
			if (arrInf[0] == 0) {
				continue;
			}

			String fileName = path.toFile().getName();
			Path folder = path.getParent();
			if (fileName.isEmpty() || folder == null) {
				continue;
			}

			String mark = viewNoMark ? "" : getMarkOrEmpty(s);
			FileInfo fileInfo = new FileInfo(arrInf[0], arrInf[1], arrInf[2], ar[2], mark);
			fillDirInfo(folder, ar[0], fileName, fileInfo); // for current file and all parents
		}
	}

	private String getMarkOrEmpty(String s) {
		String signature = ConverterBinFunc.getSignatureOrEmpty(s);
		if (signature.isEmpty()) {
			return "";
		}

		var mark = FileDataBase.getMarkFromPropertiesOrEmpty(signature);
		if (mark.isEmpty() || !FileDataBase.markPropertySet.contains(mark)) {
			return "";
		}

		return " " + FileDataBase.formatMark(mark, true);
	}

	/**
	 * All parameters must be created
	 * 
	 * @param fileName
	 * @param parentDir
	 * @param fileInfo
	 */
	private void fillDirInfo(Path folder, String extLowerCase, String fileName, FileInfo fileInfo) { // on constructor
		String transitFolderName = "";
		while (folder != null) {
			var dirString = CommonLib.fileSeparatorAddIfNeed(false, true, folder.toString());
			var dirStringLowerCase = dirString.toLowerCase(); // key in 'folderMap'
			if (!dirStringLowerCase.startsWith(pathForBeansRoot.toLowerCase())) {
				break;
			}

			DirInfo dirInfo = folderMap.containsKey(dirStringLowerCase) ? folderMap.get(dirStringLowerCase)
					: new DirInfo(dirString);

			if (!fileName.isEmpty()) {
				dirInfo.filesMap.put(fileName, fileInfo);
			}

			fileName = ""; // for parent initializations, no need current 'fileInfo' and 'fileName'

			if (fileInfo.getSize() >= 0) {
				dirInfo.countTotalFiles++;
				dirInfo.sizeTotalFiles += fileInfo.getSize();
			}

			if (CommonLib.notNullEmptyString(transitFolderName)) {
				dirInfo.transitFoldersNames.add(transitFolderName);
			}

			dirInfo.extsInfoMap.compute(extLowerCase, (k, v) -> v == null ? 1 : ++v);
			folderMap.put(dirStringLowerCase, dirInfo);

			transitFolderName = folder.toFile().getName();
			folder = folder.getParent();
		}
	}

	@Override
	public Integer call() throws Exception {
		while (isCheckResult == Const.MR_NO_CHOOSED) {
			Thread.sleep(1024);
		}
		return isCheckResult;
	}

}
