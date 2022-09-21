package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import alexnick.CommonLib;

public class MarkInfoTable extends JDialog {
	private static final long serialVersionUID = 1L;
	final private String[] columns;
	private int isCheckResult = Const.MR_NO_CHOOSED;

	public int getIsCheckResult() {
		return isCheckResult;
	}

	private List<MyBean> beans = null;
	private BeansFourTableDefault myTable = null;
	volatile private int lastSortType = SortBeans.sortNoDefined;
	private final String caption;

	private JLabel checkInfo;
	private JTextField tfFindMark;
	private String lastFindMark = "";
	private int checkNow = -1;
	private final int CHECK_NOW_MAX = 2; // minimum == 0

//'beansSet' numbers 'id' in viewTable (MyBean.serviceIntTwo); 'arMarkInfoBinTotalList' mark and markInfo of it
	public MarkInfoTable(JFrame frame, Set<Integer> beansSet, List<Map<String, MarkInfo>> arMarkInfoList) {
		super(frame, true);
		caption = "Mark info, base count: " + beansSet.size() + "; extensions count without duplicates";
		setStandardTitle();

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (isCheckResult == Const.MR_NO_CHOOSED) {
					isCheckResult = Const.MR_CANCEL;
				}
			}
		});

		this.columns = new String[] { "Mark", "Count of base", "Total count", "Extensions info" };
		initBeans(beansSet, arMarkInfoList);
		if (CommonLib.nullEmptyList(beans)) {
			dispose();
			return;
		}

		Box contents = new Box(BoxLayout.Y_AXIS);
		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, false, true, true, columns[0],
				columns[1], columns[2], columns[3], beans);

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
					printCount(true);
				}
			}
		});

		var butInvert = new JButton("invert");
		butInvert.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (var b : beans) {
					b.check = !b.check;
				}
				myTable.updateUI();
			}
		});

		JButton butOk = new JButton("OK");
		butOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int checkCount = printCount(false);
				if (checkCount > 0) {
					isCheckResult = Const.MR_OK;
					dispose();
				}
			}
		});

		JButton butCancel = new JButton("Cancel");
		butCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				isCheckResult = Const.MR_CANCEL;
				dispose();
			}
		});

		var butCheckActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (beans.isEmpty()) {
					return;
				}

				var clickOnTfFindBindFolder = e.getActionCommand().equals(Const.textFieldBinFolderClick);
				var findLowerCase = tfFindMark.getText().toLowerCase();

				if (!findLowerCase.equals(lastFindMark) || clickOnTfFindBindFolder) {
					checkNow = findLowerCase.isEmpty() ? 0 : 1;
					lastFindMark = findLowerCase;
				} else { // 0:no;1:filter(optional);2:exists;3:all
					checkNow = (checkNow <= 0) ? 1 : (checkNow >= CHECK_NOW_MAX) ? 0 : checkNow + 1;

					if (checkNow == 1 && findLowerCase.isEmpty()) {// filter, no need if empty
						checkNow = 2;
					}
				}

				List<String> substringsAND = null;
				List<String> substringsOr = null;

				if (checkNow == 1) {

					substringsOr = new ArrayList<String>();
					substringsAND = FileDataBase.getSubstringsAND_DivideByOR_NullIfError(true, true, true,
							findLowerCase, substringsOr, null);

					if (substringsOr.isEmpty()) {
						checkNow = 2;
					}
				}

				for (var b : beans) {
					b.check = (checkNow == 1) ? findFilter(substringsOr, substringsAND, b)
							: (checkNow >= CHECK_NOW_MAX) ? true : false; // <=0

				}
				updating(false);
			}

			private boolean findFilter(List<String> substringsOr, List<String> substringsAND, MyBean b) {
				if (CommonLib.notNullEmptyList(substringsAND)) { // first finding by AND, if defined
					if (!b.findSubstringsInColumn(1, 1, substringsAND)) {
						return false;
					}
				}
				// substringsOr not null/empty
				return b.findSubstringsInColumn(1, 1, substringsOr);
			}
		};

		JPanel buttons = new JPanel();
		tfFindMark = new JTextField(FileDataBase.sizeTextField);
		tfFindMark.setActionCommand(Const.textFieldFindMarkClick);
		tfFindMark.addActionListener(butCheckActionListener);
		tfFindMark.setToolTipText(Const.textFieldMarkToolTip);

		buttons.add(tfFindMark);
		var butCheck = new JButton("check");
		butCheck.addActionListener(butCheckActionListener);
		butCheck.setToolTipText(Const.butCheckToolTip);
		buttons.add(butCheck);

		buttons.add(butInvert);

		checkInfo = new JLabel();
		printCount(true); // set count on start window
		buttons.add(checkInfo);

		buttons.add(butOk);
		buttons.add(butCancel);
		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents, BorderLayout.CENTER);
		getContentPane().add(buttons, BorderLayout.SOUTH);

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(0, 0, t.width - 200, t.height - 200);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void updating(boolean resetCheckNow) {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount(resetCheckNow);

	}

	private int printCount(boolean resetCheckNow) {
		final String noCheck = "not chosen";

		if (resetCheckNow) {
			checkNow = -1;
		}
		int checkCount = 0;
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			checkCount++;
		}

		// no;filter;all
		var s = (checkNow < 0 || checkNow > CHECK_NOW_MAX) ? "count"
				: checkNow == CHECK_NOW_MAX ? "  all " : checkNow == 1 ? "filter" : noCheck;
		checkInfo.setText(s.equals(noCheck) ? s : s + ": " + checkCount);
		return checkCount;
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
			sortType = SortBeans.sortCheck;
			sortCaption = "Check -> " + column;
		} else if (columnIndex == 1) {
			sortType = SortBeans.sortOne; // mark must be in lower case, so just 'sortOne'
			sortCaption = column;
			noDubleSort = true;
		} else if (columnIndex == 2) {
			sortType = SortBeans.sortServiceIntOne;
			sortCaption = column;
			noDubleSort = true;
		} else if (columnIndex == 3) {
			sortType = SortBeans.sortServiceIntTwo;
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

	private void initBeans(Set<Integer> beansSet, List<Map<String, MarkInfo>> arMarkInfoList) {
		// map with result 'markInfo', for init beans
		Map<String, MarkInfo> mapMarkInfoResult = new TreeMap<String, MarkInfo>();

		for (int numberIdInBeans : beansSet) { // 'numberIdInBeans' >> MyBean.serviceIntTwo
			var mapMarkInfoSource = arMarkInfoList.get(numberIdInBeans); // <mark, markInfoSource //<ext,
			// signatures>>
			if (CommonLib.nullEmptyMap(mapMarkInfoSource)) { // not must be so
				continue;
			}

			for (var markSource : mapMarkInfoSource.keySet()) { // marks for this (checked) base
				if (markSource.isEmpty()) { // not must be so
					continue;
				}

				MarkInfo markInfoResult = mapMarkInfoResult.getOrDefault(markSource, new MarkInfo());

				var mapExtSignatures = mapMarkInfoSource.get(markSource).mapExtSignatures;

				for (var extSignEntry : mapExtSignatures.entrySet()) {
					var ext = extSignEntry.getKey();
					int signSize = extSignEntry.getValue().size();
					markInfoResult.countTotal += signSize;
					markInfoResult.mapCountExt.compute(ext, (k, v) -> v == null ? signSize : v + signSize);

					markInfoResult.mapExtSignatures.compute(ext, (k, v) -> v == null ? extSignEntry.getValue()
							: markInfoResult.addAll(k, extSignEntry.getValue()));
				}

				markInfoResult.countSet.add(numberIdInBeans);
				mapMarkInfoResult.put(markSource, markInfoResult);
			}
		}

		beans = new ArrayList<MyBean>();
		for (var entry : mapMarkInfoResult.entrySet()) {
			var markInfoResult = entry.getValue();
			String four = markInfoResult.mapCountExt.toString();
			var countTwo = markInfoResult.countSet.size();
			var countThree = markInfoResult.countTotal;

			var bean = new MyBean(entry.getKey(), String.valueOf(countTwo), String.valueOf(countThree), four, "");

			bean.mapExtSignatures = markInfoResult.mapExtSignatures;
			bean.mapCountExt = markInfoResult.mapCountExt;
			bean.serviceIntOne = countTwo; // for sort by 'two'
			bean.serviceIntTwo = countThree; // for sort by 'three'
			beans.add(bean);
		}
	}

	List<String> getExtListOrNull() {
		Map<String, Integer> hm = new HashMap<String, Integer>();
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			if (b.mapCountExt == null) {
				return null; // or continue?
			}

			for (var s : b.mapCountExt.keySet()) {
				var y = b.mapCountExt.get(s);
				hm.compute(s, (k, v) -> v == null ? y : v + y);
			}
		}
		List<String> extList = new ArrayList<String>();
		// sort hashmap
		var sortedList = CommonLib.getSortedListFromMap(hm);
		for (var s : sortedList) {
			extList.add(s.getKey().concat(Const.extSeparator).concat(s.getValue().toString()));
		}
		return extList;
	}

	Set<String> getSignaturesSetOrNull(Set<String> extSet) {
		if (CommonLib.nullEmptySet(extSet)) {
			return null;
		}
		Set<String> signaturesSet = new HashSet<String>();
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			if (b.mapExtSignatures == null) {
				return null; // or continue?
			}
			var extSetFromMap = b.mapExtSignatures.keySet();
			for (var ext : extSetFromMap) {
				if (!extSet.contains(ext)) {
					continue;
				}
				signaturesSet.addAll(b.mapExtSignatures.get(ext));
			}
		}
		return signaturesSet;
	}

}
