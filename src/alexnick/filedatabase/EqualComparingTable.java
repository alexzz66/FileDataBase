package alexnick.filedatabase;

import static alexnick.CommonLib.formatter;
import static alexnick.CommonLib.saveToFile;
import static alexnick.CommonLib.startProcess;

import java.awt.Color;
import java.awt.Component;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;

import alexnick.CommonLib;
import alexnick.CopyMove;

public class EqualComparingTable extends JDialog {
	private static final long serialVersionUID = 1L;

	private int isCheckResult = Const.MR_NO_CHOOSED;

	int getIsCheckResult() {
		return isCheckResult;
	}

	final private String standardTitle;
	private int lastSortType = SortBeans.sortNoDefined;

	private BeansFourTableDefault myTable;
	private List<MyBean> beans;
	private JLabel checkInfo;
	boolean[] existsStartPaths;

	private final String[] cmbCheckItems; // init in constructor
	private String[] cmbCheckItemsApp; // init in constructor

	// append const indexes from 'cmbCheckItems'
	private final int cmbAppEnabStartIndex;
	private final int cmbAppEnabEndIndex;

	final private String sourceStartPathString;
	final private String destStartPathString;

	private int indexExistsOne = -42;
	private int indexExistsTwo = -42;

//List's must not be null and BOTH must not be empty; '..startPath..' must not be null/empty, must be with '/' on end (no checked here)	
	public EqualComparingTable(JFrame frame, String sourceStartPathString, String destStartPathString,
			boolean[] existsStartPaths0, String[] binFolders, List<MyBean> equalComparingFullBeans,
			List<MyBean> equalComparingSignatureOnlyBeans) {
		super(frame, true);

		if ((equalComparingFullBeans.isEmpty() && equalComparingSignatureOnlyBeans.isEmpty())
				|| CommonLib.nullEmptyString(sourceStartPathString) || CommonLib.nullEmptyString(destStartPathString)) {
			CommonLib.errorArgument("error of creating Equal Comparing Table");
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

		this.sourceStartPathString = sourceStartPathString;
		this.destStartPathString = destStartPathString;

		existsStartPaths = new boolean[] { false, false };

		if (existsStartPaths0 != null && existsStartPaths.length == 2) {
			existsStartPaths[0] = existsStartPaths0[0];
			existsStartPaths[1] = existsStartPaths0[1];
		}

		var sb = new StringBuilder();
		if (binFolders == null || binFolders.length != 2) {
			sb.append("bin folders no defined");
		} else {
			sb.append("ONE: ").append(binFolders[0]).append("; TWO: ").append(binFolders[1]);
		}

		this.standardTitle = sb.toString();
		setStandardTitle();

		initBeans(equalComparingFullBeans, equalComparingSignatureOnlyBeans);
		if (CommonLib.nullEmptyList(beans)) {
			CommonLib.errorArgument("no found items for Equal Comparing Table");
		}

		String one = "ONE: " + sourceStartPathString + getExists(false);
		String two = "TWO: " + destStartPathString + getExists(true);

		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, true, false, false, "Signature",
				"Diff, msec: ONE - TWO (last modified) <info>", one, two, beans);

		List<String> cmbItemsList = List.of("all", "no", "invert", "one,two", "one", "two", "exists one", "exists two");

		int endIndex = cmbItemsList.size() - 1;
		if (!existsStartPaths[0] && !existsStartPaths[1]) {
			endIndex -= 2;
		} else {
			indexExistsOne = endIndex - 1;
			indexExistsTwo = endIndex;
		}

		cmbCheckItems = CommonLib.getArrayFromListOrNullByIndices(0, endIndex, cmbItemsList);
		cmbAppEnabStartIndex = 3;
		cmbAppEnabEndIndex = endIndex;

		List<String> cmbItemsAppList = List.of("only", "add", "sub");
		endIndex = cmbItemsAppList.size() - 1;

		cmbCheckItemsApp = CommonLib.getArrayFromListOrNullByIndices(0, endIndex, cmbItemsAppList);

		initComponents();
		printCount(false, null, null);

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds(0, 0, t.width - 8, t.height - 40);

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private String getExists(boolean bTwo) {
		return (existsStartPaths[bTwo ? 1 : 0]) ? " (exists)" : "";
	}

	private void initComponents() { // on constructor
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
						printCount(false, null, null);
					}
					return;
				}
				if (e.getClickCount() == 2) {
					openDirectory();
				}
			}
		});

		myTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
						column);

				MyBean bean = beans.get(row);
				if (bean.serviceIntThree % 2 == 0) {
					c.setBackground(Const.GREY_COLOR);
				} else {
					c.setBackground(Color.WHITE);
				}
				return c;
			}
		});

//FILL JPANEL	
		var cmbChecking = new JComboBox<String>(cmbCheckItems);
		var cmbCheckingApp = new JComboBox<String>(cmbCheckItemsApp);
		cmbCheckingApp.setEnabled(false);

		ActionListener butCheckActionListener = e -> checking(cmbChecking.getSelectedIndex(),
				cmbCheckingApp.getSelectedIndex());

		cmbChecking.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				var indexOne = cmbChecking.getSelectedIndex();
				cmbCheckingApp.setEnabled(indexOne >= cmbAppEnabStartIndex && indexOne <= cmbAppEnabEndIndex);
			}
		});

		var butCheck = new JButton("Set");
		butCheck.addActionListener(butCheckActionListener);

		checkInfo = new JLabel();

		JComboBox<String> cmbAction = new JComboBox<>(new String[] { "toList " });
		JButton butAction = new JButton(">>");
		butAction.addActionListener(e -> doAction(cmbAction.getSelectedIndex()));

//SHIFT DOWN AND KEYADAPTER	
		var keyAdapterEnter = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					checking(cmbChecking.getSelectedIndex(), cmbCheckingApp.getSelectedIndex());
				}
			}
		};

		cmbChecking.addKeyListener(keyAdapterEnter);
		cmbCheckingApp.addKeyListener(keyAdapterEnter);

//ADDING
		JPanel buttons = new JPanel();
		buttons.add(cmbChecking);
		buttons.add(cmbCheckingApp);
		buttons.add(butCheck);
		buttons.add(checkInfo);

		buttons.add(cmbAction);
		buttons.add(butAction);

		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));

		Box contents = new Box(BoxLayout.Y_AXIS);
		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents);
		getContentPane().add(buttons, "South");

	}

//columns: "Signature", "Diff, msec: ONE - TWO (last modified) <info>", one, two	
	private void sorting(int columnIndex) {
		if (columnIndex < 0 || beans.size() < 2) {
			return;
		}

		int sortType = SortBeans.sortNoDefined;
		String sortCaption = "";
		boolean noDubleSort = false;
		final String sortNames = " [names]";

		if (columnIndex == 0) {
			sortType = SortBeans.sortCheck;
			sortCaption = "Checked -> TWO";
		} else if (columnIndex == 1) {
			sortType = SortBeans.sortOne;
			sortCaption = "Signature";
			noDubleSort = true;
		} else if (columnIndex == 2) {
			sortType = SortBeans.sortTwo;
			sortCaption = "Diff <info>";
			noDubleSort = true;
		} else if (columnIndex == 3) {
			if (lastSortType != SortBeans.sortThree) {
				sortType = SortBeans.sortThree;
				sortCaption = "ONE";
			} else {
				sortType = SortBeans.sortThreeNameLowerCase;
				sortCaption = "ONE" + sortNames;
			}
		} else {
			if (lastSortType != SortBeans.sortFourLowerCase) {
				sortType = SortBeans.sortFourLowerCase;
				sortCaption = "TWO";
			} else {
				sortType = SortBeans.sortFourNameLowerCase;
				sortCaption = "TWO" + sortNames;
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

		setNewTitle(standardTitle.concat(sortBeans.getAppendCaption()));
	}

//actionIndex == 0 (to list); may be added later	
	private void doAction(int actionIndex) {
		if (actionIndex < 0 || actionIndex > 0) {
			return;
		}

		var checkCount = printCount(true, null, null);
		if (checkCount == null) {
			JOptionPane.showMessageDialog(this, "No checked items");
			return;
		}

		var message = CommonLib.formatConfirmYesNoMessage(checkInfo.getText() + "; to list, choose action:",
				"paths and names", "all info from table (without sorting)", null);
		int confirm = JOptionPane.showConfirmDialog(this, message, "to list", JOptionPane.YES_NO_CANCEL_OPTION);

		if (confirm == JOptionPane.YES_OPTION || confirm == JOptionPane.NO_OPTION) {
			beansToList(confirm == JOptionPane.NO_OPTION);
		}
	}

	private void beansToList(boolean allInfo) {
		var list = new ArrayList<String>();
		list.add(0, formatter.format(new Date()));
		list.add("");
		list.add(checkInfo.getText());
		list.add(standardTitle);
		list.add("");
		list.add("start path ONE: " + sourceStartPathString + getExists(false));
		list.add("start path TWO: " + destStartPathString + getExists(true));
		list.add("");

		var listPathsOne = new ArrayList<String>();
		var listNamesWithExtOne = new ArrayList<String>();
		var listNamesOne = new ArrayList<String>();

		var listPathsTwo = new ArrayList<String>();
		var listNamesWithExtTwo = new ArrayList<String>();
		var listNamesTwo = new ArrayList<String>();

		for (var b : beans) {
			if (!b.check) {
				continue;
			}

			var one = b.getThree();
			if (!one.isEmpty()) {
				listPathsOne.add(sourceStartPathString + one);
				fillLists(one, listNamesWithExtOne, listNamesOne);
			}

			var two = b.getFour(false, false);
			if (!two.isEmpty()) {
				listPathsTwo.add(destStartPathString + two);
				fillLists(two, listNamesWithExtTwo, listNamesTwo);
			}

			if (allInfo) {
				var sb = new StringBuilder();
				sb.append(b.getOne()).append("; ").append(b.getTwo());
				if (!one.isEmpty()) {
					sb.append("; ONE: ").append(one);
				}

				if (!two.isEmpty()) {
					sb.append("; TWO: ").append(two);
				}

				sb.append(" <group: ").append(b.serviceIntThree).append(">");
				list.add(sb.toString());
			}
		}

		if (!listPathsOne.isEmpty()) {
			list.add("");
			list.add("--- ONE --- size: " + listPathsOne.size());
			list.addAll(listPathsOne);

			list.add("");
			list.addAll(listNamesWithExtOne);

			list.add("");
			list.addAll(listNamesOne);
		}

		if (!listPathsTwo.isEmpty()) {
			list.add("");
			list.add("--- TWO --- size: " + listPathsTwo.size());
			list.addAll(listPathsTwo);

			list.add("");
			list.addAll(listNamesWithExtTwo);

			list.add("");
			list.addAll(listNamesTwo);
		}

		Path resPath = FileDataBase.getTempPath("toListResultEquals.txt");
		if (saveToFile(false, false, 0, CopyMove.DeleteIfExists_OLD_DELETE, resPath, null, list)) {
			startProcess(false, resPath);
		}
	}

	private void fillLists(String source, ArrayList<String> listNamesWithExt, ArrayList<String> listNames) {
		var pos = source.lastIndexOf(File.separator);

		if (pos >= 0) {
			source = source.substring(pos + 1);
		} // if < 0 = name only, take as is

		if (source.isEmpty()) {
			return;
		}

		listNamesWithExt.add(source);

		pos = source.lastIndexOf(".");

		if (pos >= 0) {
			source = source.substring(0, pos);
		}

		listNames.add(source.isEmpty() ? "<EMPTY_NAME>" : source);
	}

// 0:"all", 1:"no", 2:"invert", 3:"one,two",
//4:"one", 5:"two", LAST >> (optionally) 6:"exist one" , 7: "exist two" (both if any exists, or none)
//app: 0:"only", 1:"add", 2:"sub" 
	private void checking(final int indexOne, int indexTwo) {
		var bNeedFilterApp = indexOne >= cmbAppEnabStartIndex && indexOne <= cmbAppEnabEndIndex;
		var error = false;
		if (beans.isEmpty() || indexOne < 0 || indexOne >= cmbCheckItems.length) {
			error = true;
		} else {
			if (bNeedFilterApp) {
				if ((indexTwo < 0 || indexTwo > 2)) {
					error = true;
				} else if (indexOne == indexExistsOne && !existsStartPaths[0]) {
					error = true;
				} else if (indexOne == indexExistsTwo && !existsStartPaths[1]) {
					error = true;
				}
			} else {
				indexTwo = 0; // not need, just init
			}
		}

		if (error) {
			printCount(false, null, null);
			return;
		}

		int[] addedInfo = new int[2]; // plus and minus info
		addedInfo[0] = 0;
		addedInfo[1] = 0;

		final boolean bAdd = bNeedFilterApp && indexTwo == 1;
		final boolean bSub = !bAdd && bNeedFilterApp && indexTwo == 2;

		for (var b : beans) {
			var res = false; // false by default

			if (bNeedFilterApp) {
				if ((b.check && bAdd) || (!b.check && bSub)) {
					continue;
				}

				var one = b.getThree();
				var two = b.getFour(false, false);

				if (indexOne == indexExistsOne || indexOne == indexExistsTwo) { // check exists (optional)
					try {
						var isOne = indexOne == indexExistsOne;
						var s = isOne ? one : two;

						if (!s.isEmpty()) {
							Path path = Path.of(isOne ? sourceStartPathString : destStartPathString, s);
							res = path.toFile().exists();
						}
					} catch (Exception e) {
						res = false;
					}
				} else if (indexOne == 3) { // 3:"one,two"
					res = !one.isEmpty() && !two.isEmpty();
				} else if (indexOne == 4) { // 4:"one"
					res = !one.isEmpty();
				} else if (indexOne == 5) { // 5:"two"
					res = !two.isEmpty();
				} else {
					continue; // must not be so...
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

	private void openDirectory() { // on double click in table
		try {
			if ((!existsStartPaths[0] && !existsStartPaths[1]) || myTable.getSelectedRowCount() != 1) {
				return;
			}
			var y = myTable.getSelectedRow();
			if (y < 0) {
				return;
			}

			Path onePath = null;
			Path twoPath = null;

			var b = beans.get(y);

			if (existsStartPaths[0]) {
				var one = b.getThree();
				if (!one.isEmpty()) {
					onePath = Path.of(sourceStartPathString + one);
					if (!onePath.toFile().exists()) {
						onePath = null;
					}
				}
			}

			if (existsStartPaths[1]) {
				var two = b.getFour(false, false);
				if (!two.isEmpty()) {
					twoPath = Path.of(destStartPathString + two);
					if (!twoPath.toFile().exists()) {
						twoPath = null;
					}
				}
			}

			if (onePath == null && twoPath == null) {
				return;
			}

			if (onePath != null && twoPath != null) { // need something from (not both)
				var message = CommonLib.formatConfirmYesNoMessage("Choose path to show in explorer", onePath.toString(),
						twoPath.toString(), null);
				int confirm = JOptionPane.showConfirmDialog(this, message, "Show in explorer",
						JOptionPane.YES_NO_CANCEL_OPTION);

				if (confirm == JOptionPane.YES_OPTION) {
					twoPath = null;
				} else if (confirm == JOptionPane.NO_OPTION) {
					onePath = null;
				} else {
					return;
				}
			}

			Path path = onePath == null ? twoPath : onePath;
			startProcess(false, path.getParent());

		} catch (Exception e) {
		}
	}

//'addedInfo' if not null and length == 2, will be added info to label	
//'index' must be null OR as indices in 'cmbCheckItems', 'cmbCheckItemsApp'
	private void updating(int index[], int[] addedInfo) {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount(false, index, addedInfo);
	}

	/**
	 * Writes info about checked to JLabel
	 * 
	 * @param needResult if false - returns null; if true - if not result: null;
	 *                   else new array[2] with result
	 * @param index      must be null OR as indices in 'cmbCheckItems',
	 *                   'cmbCheckItemsApp'
	 * @param addedInfo  if not null and length == 2, will be added info to label
	 * @return
	 */
	private int[] printCount(boolean needResult, int[] index, int[] addedInfo) {
		int checkOne = 0;
		int checkTwo = 0;
		int checkCount = 0;

		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			checkCount++;

			if (!b.getThree().isEmpty()) {
				checkOne++;
			}

			if (!b.getFour(false, false).isEmpty()) {
				checkTwo++;
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

		sb.append("; one: ").append(checkOne).append(", two: ").append(checkTwo);
		checkInfo.setText(sb.toString());

		return needResult && (checkOne > 0 || checkTwo > 0) ? new int[] { checkOne, checkTwo } : null;
	}

// returns created, filled and sorted 'beans'; that may be null/empty if error
// will be added 'bean.serviceIntThree' (groups) for coloring table as in DuplicateTable	
// groups must starts with '1' (not '0'), because top item can be WHITE color (not gray)
	private void initBeans(List<MyBean> equalComparingFullBeans, List<MyBean> equalComparingSignatureOnlyBeans) {
		beans = new ArrayList<MyBean>();
		beans.addAll(equalComparingFullBeans);
		beans.addAll(equalComparingSignatureOnlyBeans);
		if (beans.size() < 2) {
			return;
		}

		new SortBeans(SortBeans.sortOne, "", beans);
		int group = 0;
		String prev = "";
		for (int i = 0; i < beans.size(); i++) {
			var b = beans.get(i);

			if (b.getOne().equals(prev)) {
				b.serviceIntThree = group;
				continue;
			}

			b.serviceIntThree = ++group;
			prev = b.getOne();
		}
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
