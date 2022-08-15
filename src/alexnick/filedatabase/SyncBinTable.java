package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import alexnick.CommonLib;

public class SyncBinTable extends JFrame implements Callable<Integer> {
	private static final long serialVersionUID = 1L;
	private static String[] columns;
	private int isCheckResult = Const.MR_NO_CHOOSED;

	private List<MyBean> beans;
	private Map<Path, Integer> pathsToCopy = null;
	private BeansFourTableDefault myTable;

	final private String standardTitle;
	private JLabel checkInfo;
	volatile private int lastSortType = SortBeans.sortNoDefined;

	public SyncBinTable(Path ownRepoPath, Path anotherRepoPath, int[] expNewUpd_ImpNewUpd_count, List<MyBean> beans0) {
		if (CommonLib.nullEmptyList(beans0)) {
			CommonLib.errorArgument("error: empty beans");
		}

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				if (isCheckResult == Const.MR_NO_CHOOSED) {
					isCheckResult = Const.MR_CANCEL;
				}
			}
		});

		new SortBeans(SortBeans.sortServiceIntThreeThenBinPathNoCheckForNull, "", beans0);
		final String[] expNewUpd_ImpNewUpd_type = new String[] { "-> export new", "-> export update", "<- import new",
				"<- import update" };

		beans = new ArrayList<MyBean>();

		for (int i = 0; i < beans0.size(); i++) {
			var b0 = beans0.get(i);
			var type = b0.serviceIntThree;
			if (type < 0 || type >= expNewUpd_ImpNewUpd_type.length) {
				continue;
			}

			var s = CommonLib.formatInt(i, 3, Const.BRACE_START, Const.BRACE_END_WITH_SPACE)
					.concat(b0.getFour(false, false));

			b0.setOne(expNewUpd_ImpNewUpd_type[type]);
			if (type == 0 || type == 1) { // export new, update
				b0.setTwo(b0.serviceIntOne + Const.countSeparator + b0.serviceStringOne);

				if (type == 1) { // update
					b0.setThree(b0.serviceIntTwo + Const.countSeparator + b0.serviceStringTwo);
				}
			} else if (type == 2 || type == 3) { // import new, update
				b0.setThree(b0.serviceIntTwo + Const.countSeparator + b0.serviceStringTwo);

				if (type == 3) { // update
					b0.setTwo(b0.serviceIntOne + Const.countSeparator + b0.serviceStringOne);
				}
			} else {
				continue;
			}

			b0.setFour(s, null);
			beans.add(b0);
		}

		if (beans.isEmpty()) {
			CommonLib.errorArgument("error: empty beans");
		}

		columns = new String[] { "Type", "own: " + ownRepoPath.toString(), "another: " + anotherRepoPath.toString(),
				"<ID> Diffenent (own - another)" }; // <ID> need for correct sorting by 'four'

		var sb = new StringBuilder();
		sb.append("Sync *bin (total: ").append(beans.size());
		sb.append("). Export new: ").append(expNewUpd_ImpNewUpd_count[0]);
		sb.append(", update: ").append(expNewUpd_ImpNewUpd_count[1]);
		sb.append(". Import new: ").append(expNewUpd_ImpNewUpd_count[2]);
		sb.append(", update: ").append(expNewUpd_ImpNewUpd_count[3]);
		standardTitle = sb.toString();
		setStandardTitle();

		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, true, false, false, columns[0],
				columns[1], columns[2], columns[3], beans);
		initComponents(expNewUpd_ImpNewUpd_count);

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds(0, 0, t.width - 120, t.height - 80);

		setLocationRelativeTo(null);
		setVisible(true);
	}

//INIT COMPONENTS	
	private void initComponents(int[] impNewUpd_ExpNewUpd_count) {
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
					printCount(false);
					return;
				}

				if (e.getClickCount() != 2 || myTable.getSelectedRowCount() != 1) {
					return;
				}
				FileDataBase.openDirectory(0, false, myTable, beans);
			}
		});

//FILL JPANEL
		String s = "export new";
		int count = impNewUpd_ExpNewUpd_count[0];
		var cbImportNew = new JCheckBox(count > 0 ? s + " (" + count + ")" : s);
		cbImportNew.setEnabled(count > 0);

		s = "export update";
		count = impNewUpd_ExpNewUpd_count[1];
		var cbImportUpdate = new JCheckBox(count > 0 ? s + " (" + count + ")" : s);
		cbImportUpdate.setEnabled(count > 0);

		s = "import new";
		count = impNewUpd_ExpNewUpd_count[2];
		var cbExportNew = new JCheckBox(count > 0 ? s + " (" + count + ")" : s);
		cbExportNew.setEnabled(count > 0);

		s = "import update";
		count = impNewUpd_ExpNewUpd_count[3];
		var cbExportUpdate = new JCheckBox(count > 0 ? s + " (" + count + ")" : s);
		cbExportUpdate.setEnabled(count > 0);

		cbImportNew.addActionListener(e -> checking(0, cbImportNew.isSelected()));
		cbImportUpdate.addActionListener(e -> checking(1, cbImportUpdate.isSelected()));
		cbExportNew.addActionListener(e -> checking(2, cbExportNew.isSelected()));
		cbExportUpdate.addActionListener(e -> checking(3, cbExportUpdate.isSelected()));

		checkInfo = new JLabel();
		printCount(false);

		var butCopy = new JButton("Copy");
		butCopy.addActionListener(e -> doCopy());

		var butCancel = new JButton("Cancel");
		butCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				isCheckResult = Const.MR_CANCEL;
				dispose();
			}
		});
//ADDING		
		JPanel buttons = new JPanel();

		buttons.add(cbImportNew);
		buttons.add(cbImportUpdate);
		buttons.add(cbExportNew);
		buttons.add(cbExportUpdate);
		buttons.add(checkInfo);
		buttons.add(butCopy);
		buttons.add(butCancel);

		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));

		Box contents = new Box(BoxLayout.Y_AXIS);

		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents, BorderLayout.CENTER);

		var scrollPan = new JScrollPane(buttons, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scrollPan, BorderLayout.SOUTH);
	}

	private void doCopy() {
		int checkCount = printCount(true); // will be message if no checked
		if (checkCount == 0) {
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this,
				" checked *bin files: " + checkCount + "?" + CommonLib.NEW_LINE_UNIX
						+ "[ window will be closed; will be copied also corresponding *dat files ]",
				"Copy *.bin; *.dat", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		pathsToCopy = new HashMap<>(); // path to copy; type
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			pathsToCopy.put(b.binPath, b.serviceIntThree);
		}

		isCheckResult = Const.MR_COPY_MOVE;
		dispose();
	}

	private void checking(int type, boolean selected) {
		for (var b : beans) {
			if (b.serviceIntThree != type || b.check == selected) {
				continue;
			}

			b.check = selected;
		}

		updating();
	}

	private void updating() {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount(false);
	}

	private int printCount(boolean messageIfNoChecked) {
		int checkCount = 0;
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			checkCount++;
		}

		checkInfo.setText("count: " + checkCount);
		if (checkCount == 0 && messageIfNoChecked) {
			JOptionPane.showMessageDialog(this, "No checked items");
		}
		return checkCount;
	}

	private void sorting(int columnIndex) {
		if (columnIndex < 0 || beans.size() < 2) {
			return;
		}

		int sortType = SortBeans.sortNoDefined;
		String sortCaption = "";
		boolean noDubleSort = false;

//columns: "Type", "own: " + ownRepoPath, "another: " + anotherRepoPath, "<ID> Diffenent (own - another)" 
		final String column = (columnIndex >= 1 && columnIndex <= 3) ? columns[columnIndex - 1] : columns[3];

		if (columnIndex == 0) {
			sortType = SortBeans.sortCheck_ThenFour;
			sortCaption = "Check -> " + column;
		} else if (columnIndex == 1) {
			sortType = SortBeans.sortOne;
			sortCaption = column;
			noDubleSort = true;
		} else if (columnIndex == 2) {
			if (lastSortType != SortBeans.sortServiceIntOne) {
				sortType = SortBeans.sortServiceIntOne;
				sortCaption = "Own repo -> count";
			} else {
				sortType = SortBeans.sortServiceStringOneNoCheckForNull;
				sortCaption = "Own repo -> path";
			}
		} else if (columnIndex == 3) {
			if (lastSortType != SortBeans.sortServiceIntTwo) {
				sortType = SortBeans.sortServiceIntTwo;
				sortCaption = "Another repo -> count";
			} else {
				sortType = SortBeans.sortServiceStringTwoNoCheckForNull;
				sortCaption = "Another repo -> path";
			}
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

	private void setNewTitle(String s) {
		if (!getTitle().equals(s)) {
			setTitle(s);
		}
	}

	private void setStandardTitle() {
		setNewTitle(standardTitle);
	}

	@Override
	public Integer call() throws Exception {
		while (isCheckResult == Const.MR_NO_CHOOSED) {
			Thread.sleep(1024);
		}
		return isCheckResult;
	}

	public Map<Path, Integer> getPathsToCopyOrNull() {
		return pathsToCopy;
	}

}
