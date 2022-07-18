package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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

public class CompareTable extends JFrame implements Callable<Map<Path, Integer>> {// JDialog
	private static final long serialVersionUID = 1L;
	final private static String[] columns = { "Type list", "Diff signature, <new-old>", "Info signature <new : old>",
			"Source path" };
	Map<Path, Integer> mapResult = null;
	private int isCheckResult = Const.MR_NO_CHOOSED;

	private List<MyBean> beans;
	private JLabel checkInfo;

	private BeansFourTableDefault myTable;
//!!! 'cbNewList','cbNewerList','cbNewerListEqualSize' may be null, creates dynamically
	private JCheckBox cbNewList;
	private JCheckBox cbNewerList;
	private JCheckBox cbNewerListEqualSize;

	private JTextField tfSkipInfo = null;
	private boolean[] sortedListExtSkipped = null;
	private String[] sortedListExts = null;

	private final String caption;
	volatile private int lastSortType = SortBeans.sortNoDefined;

	// 'beans0' not null and not empty;
	// 'copyToFolder' may be empty, need for advanced information in confirm
	public CompareTable(int countNew, int countNewer, int countNewerEqualSize, String caption, String copyToFolder,
			JFrame frame, List<MyBean> beans0) {// 'beans0' not null
		super(caption);// for 'JDialog' -> super(frame, caption, true);
		this.caption = caption;
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

		beans = new ArrayList<MyBean>();
		beans.addAll(beans0);
		List<Map.Entry<String, Integer>> sortedListExtCount = FileDataBase.fillSortedListExtCountOrNull(beans);

		Box contents = new Box(BoxLayout.Y_AXIS);
		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, true, false, false, columns[0],
				columns[1], columns[2], columns[3], beans);

		myTable.addKeyListener(FileDataBase.keyListenerShiftDown);
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
						printCount();
					}
					return;
				}
				if (e.getClickCount() == 2) {
					FileDataBase.openDirectory(false, FileDataBase.isShiftDown, myTable, beans);
				}
			}
		});

//FILLING JPANEL
		JPanel buttons = new JPanel();
		int extSize = sortedListExtCount == null ? 0 : sortedListExtCount.size();

		if (extSize > 1) { // sortedListExtCount size > 1, because 1 no need

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
					setChecking(0, cbNewList);
					setChecking(1, cbNewerList);
					setChecking(2, cbNewerListEqualSize);
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
					setChecking(0, cbNewList);
					setChecking(1, cbNewerList);
					setChecking(2, cbNewerListEqualSize);
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

		if (countNew > 0) {
			cbNewList = new JCheckBox("newList", true);
			cbNewList.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setChecking(0, cbNewList);
				}
			});

			buttons.add(cbNewList);
			cbNewList.addKeyListener(FileDataBase.keyListenerShiftDown);
		}
		if (countNewer > 0) {
			cbNewerList = new JCheckBox("newerList", true);
			cbNewerList.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setChecking(1, cbNewerList);
				}
			});

			buttons.add(cbNewerList);
			cbNewerList.addKeyListener(FileDataBase.keyListenerShiftDown);
		}
		if (countNewerEqualSize > 0) {
			cbNewerListEqualSize = new JCheckBox("newListEqualSize", true);
			cbNewerListEqualSize.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setChecking(2, cbNewerListEqualSize);
				}
			});

			buttons.add(cbNewerListEqualSize);
			cbNewerListEqualSize.addKeyListener(FileDataBase.keyListenerShiftDown);
		}

		var butInvert = new JButton("invert");
		butInvert.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (var b : beans) {
					b.check = !b.check;
				}
				updating();
			}
		});

		checkInfo = new JLabel();
		printCount();

		JButton butCopy = new JButton("Copy");
		butCopy.addActionListener(e -> {
			var checked = printCount();
			var result = fillMapResult(checked, copyToFolder);
			if (result == JOptionPane.DEFAULT_OPTION) {
				JOptionPane.showMessageDialog(this, "Choose files for copying");
			}
			if (result == JOptionPane.YES_OPTION) {
				isCheckResult = Const.MR_OK;
				dispose();
			}
		});

		JButton butCancel = new JButton("Cancel");
		butCancel.addActionListener(e -> {
			isCheckResult = Const.MR_CANCEL;
			dispose();
		});

		buttons.add(butInvert);
		buttons.add(checkInfo);
		buttons.add(butCopy);
		buttons.add(butCancel);

		butInvert.addKeyListener(FileDataBase.keyListenerShiftDown);
		butCopy.addKeyListener(FileDataBase.keyListenerShiftDown);
		butCancel.addKeyListener(FileDataBase.keyListenerShiftDown);

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
		setBounds(0, 0, t.width - 200, t.height - 200);

		setLocationRelativeTo(null);
		setVisible(true);

	}

	private void updating() {
		setStandardTitle();
		lastSortType = SortBeans.sortNoDefined;
		myTable.updateUI();
		printCount();
	}

	private void sorting(int columnIndex) {
		if (columnIndex < 0 || beans.size() < 2) {
			return;
		}

		int sortType = SortBeans.sortNoDefined;
		String sortCaption = "";
		boolean noDubleSort = false;// columns 0,4 and if's shift: always sort

//columns = { "Type list", "Diff signature, <new-old>","Info signature <new : old>","Source path" };
		final String column = (columnIndex >= 1 && columnIndex <= 3) ? columns[columnIndex - 1] : columns[3];
		if (FileDataBase.isShiftDown) {
			if (columnIndex == 0) {
				sortType = SortBeans.sortCheck_Shift_ThenFourName;
				sortCaption = "Checked (Shift) -> " + column + " [names]";
			} else if (columnIndex == 1) {
				sortType = SortBeans.sortOne_Shift_CheckOnly;
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
					sortCaption = "Checked only (Shift) -> " + column;
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
				sortType = SortBeans.sortOne;
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

	private int fillMapResult(int checked, String copyToFolder) {
		if (checked <= 0) {
			return JOptionPane.DEFAULT_OPTION;
		}
		if (mapResult == null) {
			mapResult = new HashMap<Path, Integer>();
		} else {
			mapResult.clear();
		}
		// no check file exists, filling only
		for (var b : beans) {
			if (!b.check || b.isFourPrefixNoExists()) {
				continue;
			}
			mapResult.put(Path.of(b.getFour(false, true)), b.serviceIntOne);
		}
		if (mapResult.isEmpty()) {
			return JOptionPane.DEFAULT_OPTION;
		}
		String s = "Checked files: " + mapResult.size() + CommonLib.NEW_LINE_UNIX + "Confirm copy to " + copyToFolder;
		return JOptionPane.showConfirmDialog(this, s, "Copy checked files", JOptionPane.YES_NO_OPTION);
	}

	private void setChecking(int numberList, JCheckBox cb) {
		if (cb == null) {
			return;
		}
		boolean selected = cb.isSelected();
		int result = 0;
		for (var b : beans) {
			if (b.serviceIntOne == numberList) {
				boolean needCheck = selected && !FileDataBase.getSkipExt(b, sortedListExtSkipped, sortedListExts);
				if (b.check != needCheck) {
					b.check = needCheck;
					result++;
				}
			}
		}
		if (result > 0) {
			updating();
		}
	}

	private int printCount() {
		int checkCount = 0;
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			checkCount++;
		}
		checkInfo.setText("checked: " + checkCount);
		return checkCount;
	}

	int getIsCheckResult() {
		return isCheckResult;
	}

	@Override
	public Map<Path, Integer> call() throws Exception {
		// 'while' need for 'JFrame', not for 'JDialog'
		while (isCheckResult == Const.MR_NO_CHOOSED) {
			Thread.sleep(1024);
		}
		if (isCheckResult != Const.MR_OK) {
			return null;
		}
		return mapResult;
	}
}
