package alexnick.filedatabase;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import alexnick.CommonLib;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class DuplicateTable extends JDialog implements Callable<List<String>> {
	private static final long serialVersionUID = 1L;

	private int isCheckResult = Const.MR_NO_CHOOSED;
	private List<MyBean> beans;
	private JLabel choosed;
	private JTextField tfSize;
	private BeansFourTableDefault myTable;

	private JTextField tfSkipInfo = null;
	private boolean[] sortedListExtSkipped = null;
	private String[] sortedListExts = null;
	private int lastSortType = SortBeans.sortNoDefined;
	private String standardTitle;
	private String lastSortByFinding = "";

	public DuplicateTable(JFrame frame, String caption, List<MyBean> beans0) { // list not null
		super(frame, caption, true);
		FileDataBase.isShiftDown = false;
		this.standardTitle = caption;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (isCheckResult == Const.MR_NO_CHOOSED) {
					isCheckResult = Const.MR_CANCEL;
				}
			}
		});

		// the same list, returns changed (checked for deleting)
		this.beans = beans0;
		List<Map.Entry<String, Integer>> sortedListExtCount = FileDataBase.fillSortedListExtCountOrNull(beans);

		// !!! path must be last string argument (fourCapt)
		Box contents = new Box(BoxLayout.Y_AXIS);

		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, true, true, true, "CRC",
				"Size, sort by finding", "Modified", "Path", beans);
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
						printCount();
					}
					return;
				}
				if (e.getClickCount() == 2) {
					FileDataBase.openDirectory(1, FileDataBase.isShiftDown, myTable, beans);
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

//FILLING JPANEL
		JPanel buttons = new JPanel();
		int extSize = sortedListExtCount == null ? 0 : sortedListExtCount.size();

		if (extSize > 1) {// sortedListExtCount size > 1, because 1 no need

			sortedListExtSkipped = new boolean[extSize];
			Arrays.fill(sortedListExtSkipped, false);
			sortedListExts = new String[extSize];

			String[] cbSkipItems = new String[extSize];
			for (int i = 0; i < extSize; i++) {
				var e = sortedListExtCount.get(i);
				cbSkipItems[i] = e.getKey() + Const.extSeparator + e.getValue();
				sortedListExts[i] = e.getKey();
			}

			JButton cbSkipReset = new JButton("Reset");
			cbSkipReset.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					CommonLib.resetArrayBoolean(sortedListExtSkipped);
					FileDataBase.setSkipInfo(-1, sortedListExtSkipped, sortedListExts, tfSkipInfo);
				}
			});
			tfSkipInfo = new JTextField(FileDataBase.sizeTextField);
			tfSkipInfo.setEditable(false);
			tfSkipInfo.setText(Const.skipInfoPrefix);

			JComboBox<String> cmbSkipExts = new JComboBox<>(cbSkipItems);
			JButton butSkipYesNo = new JButton(Const.butSkipYesNoCaption);
			butSkipYesNo.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					FileDataBase.setSkipInfo(cmbSkipExts.getSelectedIndex(), sortedListExtSkipped, sortedListExts,
							tfSkipInfo);
				}
			});

			buttons.add(cbSkipReset);
			buttons.add(cmbSkipExts);
			buttons.add(butSkipYesNo);
			buttons.add(tfSkipInfo);

			cmbSkipExts.addKeyListener(FileDataBase.keyListenerShiftDown);
			butSkipYesNo.addKeyListener(FileDataBase.keyListenerShiftDown);
			tfSkipInfo.addKeyListener(FileDataBase.keyListenerShiftDown);
		}

		JComboBox<String> cmbSelectType = new JComboBox<>(
				new String[] { "all, except first", "all, except second", "all", "nothing", "invert" });
		buttons.add(cmbSelectType);

		JComboBox<String> cmbSelectSize = new JComboBox<>(new String[] { "all sizes", "more (KB)", "more (MB)" });

		tfSize = new JTextField(5);
		tfSize.setText("1");
		tfSize.setEnabled(false);
		cmbSelectSize.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				var y = cmbSelectSize.getSelectedIndex();
				tfSize.setEnabled(y > 0);
			}
		});

		JButton butCheck = new JButton("Check");
		butCheck.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				checking(cmbSelectType.getSelectedIndex(), cmbSelectSize.getSelectedIndex());
			}
		});

		choosed = new JLabel();

		JButton butDelete = new JButton("Delete");
		butDelete.addActionListener(e -> {
			isCheckResult = Const.MR_DELETE;
			dispose();
		});

		JButton butCancel = new JButton("Cancel");
		butCancel.addActionListener(e -> {
			isCheckResult = Const.MR_CANCEL;
			dispose();
		});

		JButton butToList = new JButton("toList");
		butToList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FileDataBase.beansToList(false, 0, null, beans);
			}
		});

		buttons.add(cmbSelectSize);
		buttons.add(tfSize);
		buttons.add(butCheck);
		buttons.add(choosed);
		buttons.add(butDelete);
		buttons.add(butCancel);
		buttons.add(butToList);

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

		cmbSelectSize.addKeyListener(FileDataBase.keyListenerShiftDown);
		tfSize.addKeyListener(FileDataBase.keyListenerShiftDown);
		butCheck.addKeyListener(FileDataBase.keyListenerShiftDown);
		butDelete.addKeyListener(FileDataBase.keyListenerShiftDown);
		butCancel.addKeyListener(FileDataBase.keyListenerShiftDown);
		butToList.addKeyListener(FileDataBase.keyListenerShiftDown);

		new SortBeans(SortBeans.sortServiceIntThree, "", beans);
		var t = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(0, 0, t.width - 200, t.height - 200);

		setLocationRelativeTo(null);
		setVisible(true);
	}

//"check", "CRC", "Size, sort by finding", "Modified", "Path": need columnIndex 0,3,4 and 2(sort by finding)
	private void sorting(int columnIndex) {
		if (columnIndex < 0 || beans.size() < 2) {
			return;
		}

		if (columnIndex == 2) {
			var inf = new InputTextGUI(this, true, "Enter substring for sorting (no extension)", lastSortByFinding);
			if (inf.result == null) {
				return;
			}
			lastSortByFinding = inf.result;
			if (lastSortByFinding.isEmpty()) {
				return;
			} // TODO need maximum length check?
		}

		int sortType = SortBeans.sortNoDefined;
		String sortCaption;
		boolean noDubleSort = false;

		if (columnIndex == 0) {
			sortType = SortBeans.sortServiceIntThreeThenCheck;
			sortCaption = "Check -> Path";
		} else if (columnIndex == 2) {
			sortType = SortBeans.sortServiceIntThreeThenSortCaption;
			sortCaption = lastSortByFinding;
		} else if (columnIndex == 3) {
			sortType = SortBeans.sortServiceIntThreeThenThree;
			sortCaption = "Modified";
			noDubleSort = true;
		} else if (columnIndex == 4) {
			sortType = SortBeans.sortServiceIntThree;
			sortCaption = "Path";
			noDubleSort = true;
		} else {
			return;
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

	private void setStandardTitle() {
		setNewTitle(standardTitle);
	}

	private void setNewTitle(String s) {
		if (!getTitle().equals(s)) {
			setTitle(s);
		}
	}

//indexType: 0: all,except first; 1: all, except second; 2: all; 3: nothing; 4:invert
//indexSize: 0:"all sizes; 1:more (KB); 2:more (MB)
	private void checking(int indexType, int indexSize) {
		int minSize = getMinSize(indexSize);

		if (indexType == 0 || indexType == 1) {
			checkingGroup(indexType == 0, minSize);
			return;
		}

		boolean needUpdate = (indexType == 4);
		boolean bCheck = (indexType == 2);

		for (var b : beans) {
			boolean skipExt = FileDataBase.getSkipExt(b, sortedListExtSkipped, sortedListExts);
			boolean bSkipLess = skipExt || (minSize > 0 && sizeLessThan(b.getTwo(), minSize));

			if (bSkipLess || (indexType != 4)) {
				boolean ch = bSkipLess ? false : bCheck;
				if (b.check != ch) {
					if (!needUpdate) {
						needUpdate = true;
					}
					b.check = ch;
				}
				continue;
			}
			// here (indexType == 4) not skipped
			b.check = !b.check;
		}
		if (!needUpdate) {
			return;
		}
		myTable.updateUI();
		printCount();
	}

//0 - all; 1 - from kb; 2 - from mb ==> (more 1)
	private int getMinSize(int indexSize) {
		if (indexSize < 1 || indexSize > 2) {
			return 0;
		}
		int res = 0;
		try {
			res = Integer.valueOf(tfSize.getText());
		} catch (Exception e) {
		}
		int c = (indexSize == 2) ? 20 : 10;
		if (res > 0) {
			res = res << c;
		}
		if (res < 1) {
			tfSize.setText("1");
			res = 1 << c;
		}
		return res;
	}

//first == true -> check all, except first; first == false -> check all, except second
	private void checkingGroup(boolean first, int minSize) {
		String curCrc = "";
		int curGroup = -1; // additional verification

		int unCheckInd = -1;
		boolean needUpdate = false;

		for (int x = 0; x < beans.size(); x++) {
			var b = beans.get(x);

			if (minSize > 0 && sizeLessThan(b.getTwo(), minSize)) {
				if (b.check == false) {
					continue;
				}
				b.check = false;
				if (!needUpdate) {
					needUpdate = true;
				}
				continue;
			}

			boolean skipExt = FileDataBase.getSkipExt(b, sortedListExtSkipped, sortedListExts);

			if (!b.getOne().equals(curCrc) || (b.serviceIntThree != curGroup)) {
				curCrc = b.getOne();
				curGroup = b.serviceIntThree;
				unCheckInd = first ? x : x + 1;
			}

			boolean needCheck = !skipExt && (x != unCheckInd);

			if (b.check != needCheck) {
				if (!needUpdate) {
					needUpdate = true;
				}
				b.check = needCheck;
			}
		}
		if (!needUpdate) {
			return;
		}
		myTable.updateUI();
		printCount();
	}

//return true, if size < minsize (if true, be skipped for checking)
	private boolean sizeLessThan(String size, int minSize) {
		var p = size.indexOf(" (");
		if (p <= 1) {
			return true;
		}
		try {
			var s = size.substring(0, p);
			var sz = Integer.valueOf(s);
			return (sz < minSize);
		} catch (Exception e) {
			return true;
		}
	}

	private void printCount() {
		int checkCount = 0;
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			checkCount++;
		}
		final String ch = "checked: ";
		choosed.setText(ch + checkCount);
	}

	@Override
	public List<String> call() throws Exception {
		while (isCheckResult == Const.MR_NO_CHOOSED) {
			Thread.sleep(1024);
		}
		if (isCheckResult != Const.MR_DELETE) {
			return null;
		}
		ArrayList<String> res = new ArrayList<>();
		for (var b : beans) {
			if (!b.check || b.isFourPrefixNoExists()) { // no must be prefix for 'duplicates'
				continue;
			}
			res.add(b.getFour(false, true));
		}
		return res;
	}
}
