package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
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
	private boolean programPropertiesChanged = false;

	private JCheckBox cbArg1;
	private JTextField tfArg1;

	private JCheckBox cbArg2;
	private JTextField tfArg2;

	private JLabel checkInfo;

	private String[] cmbActionsItems = new String[] { "program add to table", "program description",
			"program explorer/remove", "items open with", "items to list", "items to string", "items remove from table",
			"set all checked", "set first 30 checked", "set no checked", "selected -> add checked" };

	private JButton butAction;
	private int typeInfo;

	public OpenWithTable(JDialog frame, int typeInfo, List<File> result) { // TODO caption
		super(frame, true);
		this.typeInfo = typeInfo;
		init(result);
	}

	private void resetTitle() {
		String s = FileDataBase.getTypeInfoString(typeInfo) + ". Total count: " + beansFiles.size();
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
		myTableFiles = new BeansFourTableDefault(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, true, true, true,
				"name", "path length", "size/modified", "full path", beansFiles);

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

		initComponents();

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(0, 0, t.width - 100, t.height - 100);

		setLocationRelativeTo(null);
		setVisible(true);
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
//7:"set all checked", 8:"set first 30 checked", 9:"set no checked", 10:"selected -> add checked"
	private void doAction(int index) {// TODO
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
		case 10 -> selectedAddChecked();
		}

	}

//'allFirst30No' 0: all; 1:first 30; 2: no	
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
	private void programDescriptionExplorerRemove(boolean changeDescription) { // TODO
		int row = getProgramRow(true);
		if (row < 0) {
			return;
		}
	}

	private void addProgram() {
		// TODO Auto-generated method stub
	}

	private void selectedAddChecked() {
		var addedInfo = FileDataBase.selectedToCheckedOrNull(1, myTableFiles, beansFiles);
		if (addedInfo != null && (addedInfo[0] > 0 || addedInfo[1] > 0)) {
			updatingFiles();
		}
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

		if (index == 3) { // open with
			int row = getProgramRow(true);
			if (row < 0) {
				return;
			}

			programPath = beansProg.get(row).binPath;
			if (programPath == null || !programPath.toFile().exists() || programPath.toFile().isDirectory()) {
				JOptionPane.showMessageDialog(this, "No defined program for 'open with'");
				return;
			}

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

		String actionCaption = cmbActionsItems[index];
		String programInfo = programPath == null ? "" : CommonLib.NEW_LINE_UNIX + "Program: " + programPath;
		var message = CommonLib.formatConfirmYesNoMessage("Action '" + actionCaption + "'." + programInfo,
				"for CHECKED items: " + setChecked.size(), "for SELECTED items: " + setSelected.size(),
				argInfo ? "Defined PARAMETERS will be set first" : null);

		int confirm = JOptionPane.showConfirmDialog(this, message, actionCaption, JOptionPane.YES_NO_CANCEL_OPTION);

//!!! remains 'setChecked' only		
		if (confirm == JOptionPane.YES_OPTION) {
		} else if (confirm == JOptionPane.NO_OPTION) {
			setChecked = setSelected;
		} else {
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
			FileDataBase.toCommandLine(this, false, typeInfo, 0, setChecked, beansFiles);
			return;
		}

		if (index == 6) { // remove
			removeFromTable(setChecked);
			return;
		}

		if (index == 3) { // open with
			openWith(programPath, setChecked);
			return;
		}
	}

	private void openWith(Path programPath, Set<Integer> set) { // 'programPath' is correct exist file
		if (CommonLib.nullEmptySet(set)) {
			return;
		}

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

				var f = Path.of(key).toFile();
				if (!f.exists() || f.isDirectory()) {
					continue;
				}

				String descr = programProperties.getProperty(key, "");
				long fileLength = f.length();

				var bean = new MyBean(f.getName(), descr, CommonLib.bytesToKBMB(false, 3, fileLength), f.toString(),
						"");
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
					modified + " " + CommonLib.bytesToKBMB(false, 3, fileLength), fullPathString, "");
			bean.serviceLong = fileLength;
			bean.binPath = f.toPath();
			beansFiles.add(bean);
		}

	}

}
