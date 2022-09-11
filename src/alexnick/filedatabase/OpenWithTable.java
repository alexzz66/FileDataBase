package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import alexnick.CommonLib;

public class OpenWithTable extends JDialog {
	private static final long serialVersionUID = 1L;
	private List<MyBean> beansFiles = new ArrayList<>();
	private List<MyBean> beansProg = new ArrayList<>();
	private BeansFourTableDefault myTableFiles;
	private BeansProgTableDefault myTableProg;
	private Path programPropertiesPath;
	private Properties programProperties = new Properties();

	private JCheckBox cbArg1;
	private JTextField tfArg1;

	private JCheckBox cbArg2;
	private JTextField tfArg2;

	private JLabel checkInfo;
	private String standardTitle = "";

	private String[] cmbActionsItems = new String[] { "program add to table", "program description",
			"program explorer/remove", "items open with", "items to list", "items to string", "items remove from table",
			"set all checked", "set first 30 checked", "set no checked", "selected -> only checked",
			"selected -> add checked", "selected -> sub checked", "sort by selected" };

	private JButton butAction;
	private int typeInfo;
	final private static String[] columnsFiles = { "name", "path length", "modified;size", "full path" };
	volatile private int lastSortType = SortBeans.sortNoDefined;

	public OpenWithTable(JDialog frame, int typeInfo, List<File> result) {
		super(frame, true);
		this.typeInfo = typeInfo;
		init(result);
	}

	private void resetTitle() {
		String s = FileDataBase.getTypeInfoString(typeInfo) + ". Total count: " + beansFiles.size();
		standardTitle = s;
		if (!getTitle().equals(s)) {
			setTitle(s);
		}
	}

	private void init(List<File> result) {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		initBeansFiles(result);
		if (beansFiles.isEmpty()) {
			CommonLib.errorArgument("no result");
		}

		resetTitle();
		programPropertiesPath = FileDataBase.getPathInPropertyFolder(false, Const.openWithPropertyName, false);
		initBeansProg(); // may be empty

//INIT TABLES		
		myTableFiles = new BeansFourTableDefault(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, true, true, false,
				columnsFiles[0], columnsFiles[1], columnsFiles[2], columnsFiles[3], beansFiles);

		myTableFiles.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1 && e.getButton() == 1) {
					sorting(myTableFiles.convertColumnIndexToModel(myTableFiles.columnAtPoint(e.getPoint())));
				}
			}
		});

		myTableFiles.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() != 1) {
					return;
				}
				if (myTableFiles.getSelectedColumn() == 0) {
					if (e.getClickCount() == 1) {
						printCount(null);
					}
					return;
				}
				if (e.getClickCount() == 2) {
					FileDataBase.openDirectory(0, false, myTableFiles, beansFiles);
				}
			}
		});

//PROG TABLE		
		myTableProg = new BeansProgTableDefault("name", "description", beansProg);

		myTableProg.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() != 1) {
					return;
				}
				if (myTableProg.getSelectedColumn() == 0) {
					if (e.getClickCount() == 1) {
					}
					return;
				}
				if (e.getClickCount() == 2) {
					myTableProgDoubleClick();
				}
			}
		});

		initComponents();
		sortProgByName(false);

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(0, 0, t.width - 100, t.height - 100);

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void sortProgByName(boolean needUpdateTable) {
		new SortBeans(SortBeans.sortOneLowerCase, "", beansProg);
		if (needUpdateTable) {
			myTableProg.updateUI();
		}
	}

	private void sortBySelected() {
		if (beansFiles.size() < 2) {
			return;
		}

		int sortType = SortBeans.sortSelectedIfNoEmpty;
		String sortCaption = "Selected -> full path";
		lastSortType = sortType;
		setStandardTitle();

		var sortBeans = new SortBeans(sortType, sortCaption, beansFiles, myTableFiles);
		if (!sortBeans.isBeansWasSorted()) {
			return;
		}
		setNewTitle(standardTitle.concat(sortBeans.getAppendCaption()));
	}

	private void sorting(int columnIndex) {
		if (columnIndex < 0 || beansFiles.size() < 2) {
			return;
		}

		int sortType = SortBeans.sortNoDefined;
		String sortCaption = "";
		boolean noDubleSort = false;

//columnsFiles = { "name", "path length", "modified;size", "full path"};
		final String column = (columnIndex >= 1 && columnIndex <= 3) ? columnsFiles[columnIndex - 1] : columnsFiles[3];
		final String sortFileSize = " [file size]";

		if (columnIndex == 0) {
			sortType = SortBeans.sortCheck;
			sortCaption = "Checked -> " + column;
		} else if (columnIndex == 1) { // name
			sortType = SortBeans.sortOneLowerCase;
			sortCaption = column;
			noDubleSort = true;
		} else if (columnIndex == 2) { // "path length"
			sortType = SortBeans.sortServiceIntOne;
			sortCaption = column;
			noDubleSort = true;
		} else if (columnIndex == 3) { // "modified;size"
			if (lastSortType != SortBeans.sortThree) {
				sortType = SortBeans.sortThree;
				sortCaption = column;
			} else {
				sortType = SortBeans.sortServiceLong;
				sortCaption = column + sortFileSize;
			}
		} else { // four
			sortType = SortBeans.sortFourLowerCase;
			sortCaption = column;
			noDubleSort = true;
		}

		if (sortType == lastSortType && noDubleSort) {
			return;
		}

		lastSortType = sortType;
		setStandardTitle();

		var sortBeans = new SortBeans(sortType, sortCaption, beansFiles, myTableFiles);
		if (!sortBeans.isBeansWasSorted()) {
			return;
		}

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

	private void updatingFiles() {
		myTableFiles.updateUI();
		printCount(null);
	}

	// 'checkSet' if not null will be cleared and filled by checked indexes
	private int printCount(Set<Integer> checkSet) {
		int checkCount = 0;
		boolean needSet = checkSet != null;
		if (needSet) {
			checkSet.clear();
		}

		for (int i = 0; i < beansFiles.size(); i++) {
			var b = beansFiles.get(i);
			if (!b.check) {
				continue;
			}
			checkCount++;
			if (needSet) {
				checkSet.add(i);
			}
		}

		checkInfo.setText("Count: " + checkCount);
		return checkCount;
	}

	private void initComponents() {
//FILL PANEL BUTTONS		
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));

		cbArg1 = new JCheckBox("arg1");
		tfArg1 = new JTextField(10);
		cbArg1.addActionListener(e -> enab(cbArg1, tfArg1));
		tfArg1.setEditable(false);

		cbArg2 = new JCheckBox("arg2");
		tfArg2 = new JTextField(10);
		cbArg2.addActionListener(e -> enab(cbArg2, tfArg2));
		tfArg2.setEditable(false);

		checkInfo = new JLabel("Count: 0");

		JComboBox<String> cmbActions = new JComboBox<String>(cmbActionsItems);
		butAction = new JButton("do");
		butAction.addActionListener(e -> doAction(cmbActions.getSelectedIndex()));

		var keyAdapterEnter = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					doAction(cmbActions.getSelectedIndex());
				}
			}
		};

		cmbActions.addKeyListener(keyAdapterEnter);

		buttons.add(cbArg1);
		buttons.add(tfArg1);
		buttons.add(cbArg2);
		buttons.add(tfArg2);

		buttons.add(checkInfo);
		buttons.add(cmbActions);
		buttons.add(butAction);

		getContentPane().add(new JScrollPane(myTableFiles), BorderLayout.CENTER);
		getContentPane().add(new JScrollPane(myTableProg), BorderLayout.AFTER_LINE_ENDS);
		getContentPane().add(buttons, BorderLayout.SOUTH);
	}

//0:"program add to table", 1:"program description", 2:"program explorer/remove",  
//3:"items open with", 4: "items to list", 5:"items to string", 6:"items remove from table", 
//7:"set all checked", 8:"set first 30 checked", 9:"set no checked",
//10:"selected -> only checked, 11: "selected -> add checked", 12: "selected -> sub checked"
//13:"sort by selected"	
	private void doAction(int index) {
		if (index < 0 || index >= cmbActionsItems.length) {
			return;
		}

		Set<Integer> setChecked = new HashSet<Integer>();
		printCount(setChecked);

//need message Checked/Selected for: 3,4,5,6 
		switch (index) {
		case 0 -> addProgram();
		case 1, 2 -> programDescriptionExplorerRemove(index == 1);
		case 3, 4, 5, 6 -> chooseCheckSelected(index, setChecked);
		case 7, 8, 9 -> setChecked(index - 7);// 7,8,9 -> 0,1,2
		case 10, 11, 12 -> selectedAddChecked(index - 10); // 10,11,12 -> 0:"only", 1:"add", 2:"sub"
		case 13 -> sortBySelected();
		}

	}

	// 'allFirst30No' 0: all; 1:first 30; 2: no
	private void setChecked(int allFirst30No) {
		if (allFirst30No < 0 || allFirst30No > 2 || beansFiles.isEmpty()) {
			return;
		}

		boolean res = allFirst30No < 2; // means all or first 30

		for (int i = 0; i < beansFiles.size(); i++) {
			beansFiles.get(i).check = res;
			if (i == 29 && allFirst30No == 1) {
				res = false; // after first 30 -> 'false' below
			}
		}

		updatingFiles();
	}

	// returns correct row or '-1'
	private int getProgramRow(boolean messageIfError) {
		var row = myTableProg.getSelectedRow();
		if (row < 0) {
			if (messageIfError) {
				JOptionPane.showMessageDialog(this, "Select a row in table of programs");
			}
			return -1;
		}

		return row;
	}

// 'changeDescription' if true, description for selected program; false: explorer/remove
	private void programDescriptionExplorerRemove(boolean changeDescription) {
		int row = getProgramRow(true);
		if (row < 0) {
			return;
		}

		var b = beansProg.get(row);
		Path path = b.binPath;

		if (changeDescription) {
			var inf = new InputTextGUI(this, true, "Enter new description of program", b.getTwo());
			if (inf.result == null) {
				return;
			}

			String descr = inf.result.trim();
			if (descr.equals(b.getTwo())) {
				return;
			}

			b.setTwo(descr);
			myTableProg.updateUI();

			programProperties.put(path.toString(), descr);
			saveProgramProperties();
			JOptionPane.showMessageDialog(this, "Description changed");
			return;
		}

		var message = CommonLib.formatConfirmYesNoMessage("Program: " + path, "show in windows explorer",
				"REMOVE from program table", null);
		var confirm = JOptionPane.showConfirmDialog(this, message, "Explorer/remove", JOptionPane.YES_NO_CANCEL_OPTION);

		if (confirm == JOptionPane.YES_OPTION) { // explorer
			CommonLib.startProcess(false, path.getParent());
		} else if (confirm == JOptionPane.NO_OPTION) { // remove prog from table
			if (programProperties.containsKey(path.toString())) {

				confirm = JOptionPane.showConfirmDialog(this,
						"Program " + path + " will be removed from table." + CommonLib.NEW_LINE_UNIX + "Continue?",
						"Remove from table", JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION) {
					programProperties.remove(path.toString());
					beansProg.remove(row);
					myTableProg.updateUI();
					saveProgramProperties();
					JOptionPane.showMessageDialog(this, "Removed from table: " + path);
				}
			}
		}
	}

	private void addProgram() {
		String title = "Add program";
		var message = CommonLib.formatConfirmYesNoMessage("Need choose file 'exe', 'bat' or 'cmd'", "select from disk",
				"enter full path", null);

		var confirm = JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_CANCEL_OPTION);
		Path path = null;
		try {

			if (confirm == JOptionPane.YES_OPTION) {
				path = CommonLib.getDestExecFileGUI(false).toPath();
				if (path == null) { // user cancel
					return;
				}
			} else if (confirm == JOptionPane.NO_OPTION) {
				var inf = new InputTextGUI(this, false, "Enter full path of 'exe', 'bat' or 'cmd' file", title);
				if (inf.result == null) { // user cancel
					return;
				}
				path = Path.of(inf.result.trim());
			} else {
				return; // user cancel
			}

			var f = path.toAbsolutePath().normalize().toFile().getCanonicalFile();
			if (f.exists() && !f.isDirectory() && CommonLib.checkExecutableExtension(f.getName())) {
			} else {
				CommonLib.errorArgument("no correct path " + f);
			}

			path = f.toPath();
			for (var b : beansProg) {
				if (b.binPath.equals(path)) {
					CommonLib.errorArgument("this path allready added " + path);
				}
			}

			String descr = getDescription(false, path.toString());

			long fileLength = f.length();

			var bean = new MyBean(f.getName(), descr, CommonLib.bytesToKBMB(false, 3, fileLength), f.toString(), "");
			bean.binPath = path;
			beansProg.add(bean);

			sortProgByName(true);

			programProperties.put(path.toString(), descr);
			saveProgramProperties();
			JOptionPane.showMessageDialog(this, "Added program: " + path);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
		}

	}

	private void saveProgramProperties() {
		CommonLib.loadOrStoreProperties(true, "Open with; executable programs with descriptions", programPropertiesPath,
				null, programProperties);
	}

//'nullIFCancel' if user cancelled: if true, returns null; else empty
	private String getDescription(boolean nullIFCancel, String title) {
		var inf = new InputTextGUI(this, false, "Enter description of program or leave empty", title);
		if (inf.result == null) {
			return nullIFCancel ? null : "";
		}
		return inf.result.trim();
	}

	private void selectedAddChecked(int onlyAddSub) { // onlyAddSub: 0:"only", 1:"add", 2:"sub"
		var addedInfo = FileDataBase.selectedToCheckedOrNull(onlyAddSub, myTableFiles, beansFiles);
		if (addedInfo != null && (addedInfo[0] > 0 || addedInfo[1] > 0)) {
			updatingFiles();
		}
	}

	private void myTableProgDoubleClick() {
		Set<Integer> setChecked = new HashSet<Integer>();
		printCount(setChecked);
		chooseCheckSelected(3, setChecked);
	}

//3:"items open with", 4: "items to list", 5:"items to string", 6:"items remove from table"
//'setChecked' not null	
	private void chooseCheckSelected(int index, Set<Integer> setChecked) {
		switch (index) {
		case 3, 4, 5, 6:
			break;
		default:
			return;
		}

		var rows = myTableFiles.getSelectedRows();
		Set<Integer> setSelected = new HashSet<Integer>();

		for (var row : rows) {
			setSelected.add(row);
		}

		if (setChecked.isEmpty() && setSelected.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No checked/selected rows found");
			return;
		}

		Path programPath = null;
		boolean argInfo = false;
		int lengthChecked = 0;
		int lengthSelected = 0;

		String actionCaption = cmbActionsItems[index];
		var sbCaption = new StringBuilder("Action '");
		sbCaption.append(actionCaption).append("'.");

		if (index == 3) { // open with
			int row = getProgramRow(true);
			if (row < 0) {
				return;
			}

			var b = beansProg.get(row);
			programPath = b.binPath;
			if (programPath == null || !programPath.toFile().exists() || programPath.toFile().isDirectory()) {
				JOptionPane.showMessageDialog(this, "No defined program for 'open with'");
				return;
			}

			sbCaption.append(CommonLib.NEW_LINE_UNIX).append("Program: ").append(programPath);
			sbCaption.append(CommonLib.NEW_LINE_UNIX).append("Modified; size: ").append(b.getThree());

			if (cbArg1.isSelected()) {
				var s = tfArg1.getText().trim();
				if (!s.isEmpty()) {
					argInfo = true;
				}
			}

			if (!argInfo && cbArg2.isSelected()) {
				var s = tfArg2.getText().trim();
				if (!s.isEmpty()) {
					argInfo = true;
				}
			}
		}

		if (index != 6) { // except 'remove from table'
			for (var i : setChecked) {
				lengthChecked += beansFiles.get(i).serviceIntOne;
			}

			for (var i : setSelected) {
				lengthSelected += beansFiles.get(i).serviceIntOne;
			}
		}

		var sbYes = new StringBuilder("for CHECKED items: ");
		sbYes.append(setChecked.size());

		if (lengthChecked > 0) {
			sbYes.append(" (total length: ").append(lengthChecked).append(")");
		}

		var sbNo = new StringBuilder("for SELECTED items: ");
		sbNo.append(setSelected.size());

		if (lengthSelected > 0) {
			sbNo.append(" (total length: ").append(lengthSelected).append(")");
		}

		var message = CommonLib.formatConfirmYesNoMessage(sbCaption.toString(), sbYes.toString(), sbNo.toString(),
				argInfo ? "Defined 'arg1', 'arg2'  will be set first" : null);

		int confirm = JOptionPane.showConfirmDialog(this, message, actionCaption, JOptionPane.YES_NO_CANCEL_OPTION);

//!!! remains 'setChecked' only		
		if (confirm == JOptionPane.YES_OPTION) {
		} else if (confirm == JOptionPane.NO_OPTION) {
			setChecked = setSelected;
		} else {
			return;
		}

		if (index == 3) { // open with; may be empty result
			openWith(programPath, setChecked);
			return;
		}

		if (setChecked.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Result is empty");
			return;
		}

		if (index == 4) { // to list
			FileDataBase.beansToList(false, 2, setChecked, beansFiles);
			return;
		}

		if (index == 5) { // to string
			FileDataBase.toCommandLine(this, 0, typeInfo, 0, setChecked, beansFiles);
			return;
		}

		if (index == 6) { // remove
			removeFromTable(setChecked);
			return;
		}
	}

	private void openWith(Path programPath, Set<Integer> set) { // 'programPath' is correct exist file
		try {
			List<String> list = new ArrayList<String>();
			list.add(programPath.toString());

			if (cbArg1.isSelected()) {
				var s = tfArg1.getText().trim();
				if (!s.isEmpty()) {
					list.add(s);
				}
			}

			if (cbArg2.isSelected()) {
				var s = tfArg2.getText().trim();
				if (!s.isEmpty()) {
					list.add(s);
				}
			}

			for (var index : set) {
				var b = beansFiles.get(index);
				list.add(b.getFour(false, true));
			}

			new ProcessBuilder(list).start();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
		}

	}

	private void removeFromTable(Set<Integer> set) {
		if (CommonLib.nullEmptySet(set)) {
			return;
		}
		if (set.size() >= beansFiles.size()) {
			if (JOptionPane.showConfirmDialog(this, "Do you want close this window?", "Remove all",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				dispose();
			}
			return;
		}

		try {
			myTableFiles.clearSelection();
			for (int i : set) {
				beansFiles.set(i, null);
			}

			beansFiles.removeIf(Objects::isNull);

			resetTitle();
			updatingFiles();
			return;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage() + ", window will be closed");
			dispose();
		}
	}

	private void enab(JCheckBox cb, JTextField tf) {
		tf.setEditable(cb.isSelected());
	}

	private void initBeansProg() {
		// !!! properties in standard folder only
		CommonLib.loadOrStoreProperties(false, null, programPropertiesPath, null, programProperties);

		if (programProperties.isEmpty()) {
			return;
		}
		// prop-> full name=arguments(0,1 or 2 pieces, delimiter ")
		for (var keyRow : programProperties.keySet()) {
			try {
				String key = keyRow.toString();
				if (key.isEmpty()) {
					continue;
				}

				var f = Path.of(key).toAbsolutePath().normalize().toFile().getCanonicalFile();
				if (!f.exists() || f.isDirectory() || !CommonLib.checkExecutableExtension(f.getName())) {
					continue;
				}

				String descr = programProperties.getProperty(key, "");
				long fileLength = f.length();
				String modified = CommonLib.dateModifiedToString(f.lastModified());

				var bean = new MyBean(f.getName(), descr, modified + "; " + CommonLib.bytesToKBMB(false, 3, fileLength),
						f.toString(), "");
				bean.binPath = f.toPath();
				beansProg.add(bean);
			} catch (Exception e) {
				continue;
			}
		}
	}

	private void initBeansFiles(List<File> result) {
		if (CommonLib.nullEmptyList(result)) {
			CommonLib.errorArgument("no result");
		}

		for (var f : result) {
			if (!f.exists()) {
				continue;
			}

			String fullPathString = f.toString();
			boolean isDir = f.isDirectory();

			long fileLength = isDir ? 0 : f.length();
			String modified = CommonLib.dateModifiedToString(f.lastModified());

			int pathLength = fullPathString.length();

			var bean = new MyBean(f.getName(), "" + pathLength,
					modified + "; " + CommonLib.bytesToKBMB(false, 3, fileLength), fullPathString, "");
			bean.serviceIntOne = pathLength;
			bean.serviceLong = fileLength;
			bean.binPath = f.toPath();
			beansFiles.add(bean);
		}

	}

}
