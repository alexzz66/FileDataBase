package alexnick.filedatabase;

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
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;

import alexnick.CommonLib;

public class EqualComparingTable extends JDialog {
	private static final long serialVersionUID = 1L;

	private int isCheckResult = Const.MR_NO_CHOOSED;

	int getIsCheckResult() {
		return isCheckResult;
	}

	final private String standardTitle;
	private BeansFourTableDefault myTable;
	private List<MyBean> beans;
	private JLabel checkInfo;
	boolean[] existsStartPaths;
	private String[] cmbCheckItemsApp = new String[] { "only ", "add ", "sub " };

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

		String one = "ONE: " + sourceStartPathString;
		if (existsStartPaths[0]) {
			one += " (exists)";
		}
		String two = "TWO: " + destStartPathString;
		if (existsStartPaths[1]) {
			two += " (exists)";
		}

		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, true, false, false, "Signature",
				"Diff, msec: ONE - TWO (last modified) <info>", one, two, beans);
		initComponents();
		printCount();

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds(0, 0, t.width - 8, t.height - 40);

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initComponents() { // on constructor
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
// NB: for ALL items is comboBoxes appended ' ' on end (so width be more and something else?)
		List<String> list = new ArrayList<String>();
		list.add("all "); // 0
		list.add("no "); // 1
		list.add("invert "); // 2
		list.add("one, two "); // 3
		list.add("one "); // 4
		list.add("two ");// 5
		// optionally, end on list
		if (existsStartPaths[0] || existsStartPaths[1]) {
			list.add("exists "); // 6
		}

		JComboBox<String> cmbChecking = new JComboBox<>(CommonLib.getArrayFromListOrNull(list));
		var cmbCheckingApp = new JComboBox<String>(cmbCheckItemsApp);

		ActionListener butCheckActionListener = e -> checking(cmbChecking.getSelectedIndex(),
				cmbCheckingApp.getSelectedIndex());

		cmbChecking.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				var index = cmbChecking.getSelectedIndex();
				cmbCheckingApp.setEnabled(index >= 3); // more 'one,two'
			}
		});

		cmbCheckingApp.setEnabled(false);

		var keyAdapterEnter = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					checking(cmbChecking.getSelectedIndex(), cmbCheckingApp.getSelectedIndex());
				}
			}
		};

		cmbChecking.addKeyListener(keyAdapterEnter);
		cmbCheckingApp.addKeyListener(keyAdapterEnter);

		var butCheck = new JButton("Set");
		butCheck.addActionListener(butCheckActionListener);

		checkInfo = new JLabel();

		// may be appended functions later
		JComboBox<String> cmbAction = new JComboBox<>(new String[] { "toList " });
		JComboBox<String> cmbSelectBin = new JComboBox<>(new String[] { "one ", "two ", "both " });
		JButton butAction = new JButton(">>");
		butAction.addActionListener(e -> doAction(cmbAction.getSelectedIndex(), cmbSelectBin.getSelectedIndex()));

//ADDING
		JPanel buttons = new JPanel();
		buttons.add(cmbChecking);
		buttons.add(cmbCheckingApp);
		buttons.add(butCheck);
		buttons.add(checkInfo);

		buttons.add(cmbAction);
		buttons.add(cmbSelectBin);
		buttons.add(butAction);

		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));

		Box contents = new Box(BoxLayout.Y_AXIS);
		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents);
		getContentPane().add(buttons, "South");

	}

	private void doAction(int selectedIndex, int selectedIndex2) {
		// TODO Auto-generated method stub

	}

	// 0:"all", 1:"no", 2:"invert", 3:"one,two",
//4:"one", 5:"two", LAST >> (optionally) 6:"exist"
//app: 0:"only", 1:"add", 2:"sub" 
	private void checking(final int indexOne, int indexTwo) {
		// TODO Auto-generated method stub
	}

	private void openDirectory() { // on double click in table
		// TODO Auto-generated method stub

	}

	private int printCount() {
		int checkCount = 0;
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			checkCount++;
		}

		checkInfo.setText("count: " + checkCount);
		return checkCount;
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
