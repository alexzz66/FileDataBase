package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import alexnick.CommonLib;

public class DeleteEmptyFoldersTable extends JFrame implements Callable<Integer> {
	private static final long serialVersionUID = 1L;
	private int isCheckResult = Const.MR_NO_CHOOSED;
	private BeansFourTableDefault myTable;
	private JLabel checkInfo;
	private List<MyBean> beans;
	private List<String> pathsForDelete = null;
	final private static String[] columns = { "Id (+subId)", "Name", "Modified", "Full path" };
	private int checkNow;
	private final int CHECK_NOW_MAX = 2; // minimum == 0

// CONSTRUCTOR !!! NO SORT TABLE for correct deleting result
// 'MyBean.serviceSet' in beans must not be null

	public DeleteEmptyFoldersTable(String startFolder, List<MyBean> beans0) {
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
		initBeans(beans0);

		if (beans.isEmpty()) {
			return;
		}

		setTitle("Delete empty folders: " + beans.size() + ". Start folder: " + startFolder);
		myTable = new BeansFourTableDefault(ListSelectionModel.SINGLE_SELECTION, true, true, false, columns[0],
				columns[1], columns[2], columns[3], beans);
		initComponents();

		printCount(true, false, null);

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds(0, 0, t.width - 8, t.height - 40);

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initBeans(List<MyBean> beans0) {
		Map<Integer, Integer> idMap = new HashMap<>();

		for (var b : beans0) { // intOne:currentId; intTwo:totalId
			idMap.put(b.serviceIntTwo, b.serviceIntOne);
		}

		beans.clear();
		for (var b : beans0) { // set 'one': "Id (+subId)"
			var sb = new StringBuilder();
			sb.append(b.serviceIntOne); // current
			if (!b.serviceSet.isEmpty()) {
				sb.append(" (+");
				int count = b.serviceSet.size();
				for (int y : b.serviceSet) {
					sb.append(idMap.get(y));

					if (--count > 0) {
						sb.append("; ");
					}
				}

				sb.append(")");
			}
			b.setOne(sb.toString());
			beans.add(b);
		}
	}

	private void initComponents() {// on constructor
//MY_TABLE INIT			
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
					FileDataBase.openDirectory(0, true, myTable, beans);
				}
			}
		});

//FILL JPANEL		
		var butCheck = new JButton("Check");
		butCheck.addActionListener(e -> checking());

		var butInvert = new JButton("Invert");
		butInvert.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (var b : beans) {
					b.check = !b.check;
				}
				updating(true);
			}
		});

		checkInfo = new JLabel();
		var butDelete = new JButton("Delete");
		butDelete.addActionListener(e -> deleting());

		JButton butClose = new JButton("Close");
		butClose.addActionListener(e -> {
			dispose();
		});

//ADDING	
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));
		buttons.add(butCheck);
		buttons.add(butInvert);
		buttons.add(checkInfo);
		buttons.add(butDelete);
		buttons.add(butClose);

		Box contents = new Box(BoxLayout.Y_AXIS);
		contents.add(new JScrollPane(myTable));
		getContentPane().add(contents, BorderLayout.CENTER);
		getContentPane().add(buttons, "South");
	}

	private void deleting() {
		Set<Integer> addTotalId = new HashSet<Integer>(); // totalId, serviceIntTwo
		if (printCount(false, true, addTotalId) <= 0) {
			return;
		}

		int count = 0;

		if (!addTotalId.isEmpty()) {
			for (int i = 0; i < beans.size(); i++) {
				var b = beans.get(i);
				if (!b.check && addTotalId.contains(b.serviceIntTwo)) {
					b.check = true;
					count++;
				}
			}
		}

		var sb = new StringBuilder();
		if (count > 0) {
			updating(true);
			sb.append("Were added associated directory for deleting, count: ").append(count)
					.append(CommonLib.NEW_LINE_UNIX);
		}

		count = printCount(false, true, null);
		if (count <= 0) { // must not be so
			return;
		}
		sb.append("Will be deleted checked directories, count: ").append(count).append(CommonLib.NEW_LINE_UNIX)
				.append("Continue?").append(CommonLib.NEW_LINE_UNIX).append(CommonLib.NEW_LINE_UNIX)
				.append("if 'yes', this window will be closed");
		int confirm = JOptionPane.showConfirmDialog(this, sb.toString(), "Delete empty folders",
				JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		pathsForDelete = new ArrayList<>();
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			pathsForDelete.add(b.binPath.toString());
		}

		isCheckResult = Const.MR_DELETE;
		dispose();
	}

	private void updating(boolean resetCheckNow) {
		myTable.updateUI();
		printCount(resetCheckNow, false, null);
	}

	private void checking() { // onClick 'butCheck'
		checkNow = (checkNow <= 0) ? 1 : (checkNow >= CHECK_NOW_MAX) ? 0 : checkNow + 1;
		// -1: not defined; 0:no; 1: totalEmpty; 2: all (last always 'all')
		for (var b : beans) {
			b.check = (checkNow == 1) ? b.serviceSet.isEmpty() : checkNow == CHECK_NOW_MAX;
		}

		updating(false);
	}

	// 'addTotalIdForReturn' if not null, will be filled associated idTotal for
	// checked
	private int printCount(boolean resetCheckNow, boolean messageIfNoChecked, Set<Integer> addTotalIdForReturn) {
		int checkCount = 0;
		for (var b : beans) {
			if (!b.check) {
				continue;
			}
			checkCount++;
			if (addTotalIdForReturn == null || b.serviceSet.isEmpty()) {
				continue;
			}

			for (int y : b.serviceSet) {
				addTotalIdForReturn.add(y);
			}
		}

		if (resetCheckNow) {
			checkNow = -1;
		}

		// -1: not defined; 0:no; 1: totalEmpty; 2: all
		var s = (checkNow < 0 || checkNow > CHECK_NOW_MAX) ? "count"
				: checkNow == CHECK_NOW_MAX ? "all checked" : checkNow == 1 ? "total empty" : "no";

		checkInfo.setText(checkCount == 0 ? Const.NOT_CHOSEN : s.concat(": ") + checkCount);
		if (checkCount == 0 && messageIfNoChecked) {
			JOptionPane.showMessageDialog(this, "No checked items");
		}
		return checkCount;
	}

	int getIsCheckResult() {
		return isCheckResult;
	}

	public List<String> getPathsForDelete() {
		return pathsForDelete;
	}

	@Override
	public Integer call() throws Exception {
		while (isCheckResult == Const.MR_NO_CHOOSED) {
			Thread.sleep(1024);
		}
		return isCheckResult;
	}

}
