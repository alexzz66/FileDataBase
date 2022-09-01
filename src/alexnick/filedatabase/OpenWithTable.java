package alexnick.filedatabase;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
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
	private BeansThreeTableDefault myTableProg;
	private Path programPropertiesPath;

	private JCheckBox cbArg1;
	private JTextField tfArg1;

	private JCheckBox cbArg2;
	private JTextField tfArg2;

	private JButton butCheck;
	private JLabel choosed;

	private String[] cmbActionsItems = new String[] { "program add to table", "program remove from table",
			"program explorer", "items open with", "items to string", "items remove from table" };

	private JButton butAction;

	public OpenWithTable(JDialog frame, List<File> result) {
		super(frame, true);
		init(result);
	}

	private void init(List<File> result) { // TODO constructor
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		initBeansFiles(result);
		if (beansFiles.isEmpty()) {
			CommonLib.errorArgument("no result");
		}

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
						// TODO printCount(null, false, null);
					}
					return;
				}
				if (e.getClickCount() == 2) {
					FileDataBase.openDirectory(0, false, myTableFiles, beansFiles);
				}
			}
		});

//PROG TABLE		
		myTableProg = new BeansThreeTableDefault("name", "arg1", "arg2", beansProg);

		initComponents();

		var t = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(0, 0, t.width - 100, t.height - 100);

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initComponents() {// TODO Auto-generated method stub
		var cmbActions = new JComboBox<>(new String[] { "any", "starts", "ends" });
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.LEADING));
		buttons.add(cmbActions);

		getContentPane().add(new JScrollPane(myTableFiles), BorderLayout.CENTER);
		getContentPane().add(new JScrollPane(myTableProg), BorderLayout.AFTER_LINE_ENDS);
		getContentPane().add(buttons, BorderLayout.SOUTH);
	}

	private void initBeansProg() {
		// !!! properties in standard folder only
		Properties prop = new Properties();
		CommonLib.loadOrStoreProperties(false, null, programPropertiesPath, null, prop);

		if (prop.isEmpty()) {
			return;
		}
		// prop-> full name=arguments(0,1 or 2 pieces, delimiter ")
		for (var keyRow : prop.keySet()) {
			try {
				String key = keyRow.toString();
				if (key.isEmpty()) {
					continue;
				}

				var f = Path.of(key).toFile();
				if (!f.exists() || f.isDirectory()) {
					continue;
				}

				String arg = prop.getProperty(key, "");
				String arg2 = "";
				final char delimiter = '"';

				if (!arg.isEmpty()) {
					int pos = arg.indexOf(delimiter);
					if (pos >= 0) {
						arg2 = arg.substring(pos + 1); // check later: must not be delimiter
						arg = arg.substring(0, pos); // no delimiter here

						if (arg2.indexOf(delimiter) >= 0) {// must not be delimiter
							arg2 = "";
						}
					}
				}

				long fileLength = f.length();
				var bean = new MyBean(f.getName(), arg, arg2, f.toString(), "");
				bean.serviceLong = fileLength;
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
