package alexnick.filedatabase;

import javax.swing.*;
import alexnick.CommonLib;
import alexnick.CopyMove;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;

public final class ExtensionFrame extends JDialog implements Callable<List<String>> {
	private static final long serialVersionUID = 1L;
	private int isCheckResult = Const.MR_NO_CHOOSED;

	int getIsCheckResult() {
		return isCheckResult;
	}

	private JCheckBox[] cbArray;
	private JComboBox<String> cmbExtGroups;
	private JTextField tfExtGroups;
	private JCheckBox cbCheck;
	private Map<String, String> hmGroups;

	private List<String> noNeedExts = new ArrayList<>();
	private Set<String> extsResult = new HashSet<String>();
	private List<String> extsList = new ArrayList<String>();

	Set<String> getExtsResult() {
		return extsResult;
	}

	/**
	 * @param frame
	 * @param needChooseSomething if 'false', 'OK' click allowed to accept result
	 *                            anyway, but need confirmation;<br>
	 *                            if 'true', for 'OK', need choose something
	 * @param extsList0
	 * @param extsNeedListSet     must be set 'null', if not need save to properties
	 * @param extsNoNeedListSet   must be set 'null', if not need save to properties
	 */
	public ExtensionFrame(JFrame frame, boolean needChooseSomething, List<String> extsList0,
			Set<String> extsNeedListSet, Set<String> extsNoNeedListSet) {
		super(frame, true);
		extsList.addAll(extsList0);
		var pan = new JPanel();
		pan.setLayout(new GridLayout(20, 20));
		cbArray = new JCheckBox[extsList.size()];

		JCheckBox checkAll = new JCheckBox("checkAll", true);
		checkAll.addActionListener(e -> {
			for (JCheckBox jCheckBox : cbArray) {
				jCheckBox.setSelected(checkAll.isSelected());
			}
		});
		pan.add(checkAll);

		for (int i = 0; i < cbArray.length; i++) {
			cbArray[i] = new JCheckBox(extsList.get(i), true);
			pan.add(cbArray[i]);
		}

		Box contents = new Box(BoxLayout.Y_AXIS);
		contents.add(pan);
		getContentPane().add(contents);

		JButton butOk = new JButton("Ok");
		butOk.addActionListener(e -> {

			var result = false;
			for (int i = 0; i < cbArray.length; i++) {
				if (cbArray[i].isSelected()) {
					result = true;
					break;
				}
			}
			if (!result) {
				if (needChooseSomething || JOptionPane.showConfirmDialog(this, "Nothing is selected. Continue?",
						"Empty set", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
					return;
				}
			}

			setProp(Const.MR_OK, extsNeedListSet, extsNoNeedListSet);
			extsResult.clear();
			for (int i = 0; i < cbArray.length; i++) {
				if (cbArray[i].isSelected()) {
					extsResult.add(getTrimmedExt(extsList.get(i)));
				}
			}
			if (isCheckResult == Const.MR_NO_CHOOSED) {
				isCheckResult = Const.MR_OK;
			}
			dispose();
		});

		JPanel buttons = new JPanel();
		int sz = 0;

		hmGroups = getExtGroups();
		sz = hmGroups.size();

		if (hmGroups != null && sz > 0) {
			String[] arExtGroups = new String[sz];
			int ind = 0;
			tfExtGroups = new JTextField(FileDataBase.sizeTextField);
			tfExtGroups.setEditable(false);
			for (var x : hmGroups.keySet()) {
				arExtGroups[ind++] = x;
			}

			Arrays.sort(arExtGroups);

			cmbExtGroups = new JComboBox<>(arExtGroups);
			cmbExtGroups.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					setTextToTfExtGroups();
				}

			});
			cbCheck = new JCheckBox("check");
			cbCheck.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					checkExtsByHm();
				}
			});
			buttons.add(cmbExtGroups);
			setTextToTfExtGroups();
			buttons.add(tfExtGroups);
			buttons.add(cbCheck);
		}

		buttons.add(butOk);
		getContentPane().add(buttons, "South");

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent e) {
				setProp(Const.MR_CANCEL, extsNeedListSet, extsNoNeedListSet);
				if (isCheckResult == Const.MR_NO_CHOOSED) {
					isCheckResult = Const.MR_CANCEL;
				}
			}

		});

		setTitle("Uncheck unnecessary extensions");
		pack();
		setLocationRelativeTo(null);
		setAlwaysOnTop(true);
		setVisible(true);
	}

	private void setTextToTfExtGroups() {
		if (hmGroups != null) {
			var x = cmbExtGroups.getItemAt(cmbExtGroups.getSelectedIndex());
			tfExtGroups.setText(hmGroups.get(x));
		}
	}

	private void checkExtsByHm() {
		if (tfExtGroups == null) {
			return;
		}
		var s = tfExtGroups.getText();
		if (s.isEmpty()) {
			return;
		}
		var selected = cbCheck.isSelected();
		for (var i = 0; i < cbArray.length; i++) {
			if (cbArray[i].isSelected() == selected) {
				continue;
			}
			var s2 = getTrimmedExt(extsList.get(i));
			if (s.contains(s2)) { // 's' not empty
				cbArray[i].setSelected(selected);
			}
		}
	}

	private String getTrimmedExt(String s) {
		var pos = s.indexOf(Const.extSeparator);
		return (pos > 0) ? s.substring(0, pos) : s;
	}

	private List<String> trimExtNew() {
		List<String> tmp = new ArrayList<>();
		for (int i = 0; i < cbArray.length; i++) {
			tmp.add(getTrimmedExt(extsList.get(i)));
		}
		return tmp;
	}

	// returns map, key=key of 'propertyGroups', value=exist exts only
	synchronized private Map<String, String> getExtGroups() {
		Set<String> extsListSet = new HashSet<String>();
		extsListSet.addAll(trimExtNew());

		Map<String, String> hm = new HashMap<>();
		Properties propertyGroups = new Properties();
		Path pathGroups = FileDataBase.getPathInPropertyFolder(false, Const.groupsProperty, false);
		if (!pathGroups.toFile().exists()) {
			propertyGroups.put("audio", "aac;amr;au;ape:flac;it:mid;mkv;mp3;ogg;wav;wma");
			propertyGroups.put("video", "avi;flv;m4v;mov;mp4;mpeg:mpg;srt;swf:wmv");
			propertyGroups.put("documents", "doc;docx:hlp;htm;html;maff;ppt;pptx;txt");
			propertyGroups.put("archives", "7z;gz;iso;jar:nfo;nrg;rar;zip");
			propertyGroups.put("books", "chm;djv;djvu;epub;fb2;mobi;pdf;rtf");
			propertyGroups.put("pictures", "bmp;gif;jpe:jpeg;jpg;png;");
			propertyGroups.put("programm", "exe");
			CommonLib.loadOrStoreProperties(true, "Extension groups. Separator ';' or ':'. All spaces will be removed",
					pathGroups, null, propertyGroups);
		} else {
			CommonLib.loadOrStoreProperties(false, null, pathGroups, null, propertyGroups);
			if (propertyGroups.isEmpty()) {
				return hm;
			}
		}

		final String extSeparatorChar = ":";
		var groupsNamesKeySet = propertyGroups.keySet();

		Set<String> extsListSetNoFound = new HashSet<String>();
		extsListSetNoFound.addAll(extsListSet);

		for (var ext : extsListSet) {
			var extFounded = false;
			var extCheck = extSeparatorChar + ext + extSeparatorChar;
			for (var grn : groupsNamesKeySet) {
				String groupsName = grn.toString();
				if (groupsName.isEmpty() || groupsName.equalsIgnoreCase(Const.GROUPS_NO_GROUP)) {
					continue;
				}
				var grnValue = propertyGroups.getProperty(groupsName, "").toLowerCase();
				if (grnValue.contains(" ")) {
					grnValue = grnValue.replace(" ", "");
				}
				if (grnValue.contains(";")) {
					grnValue = grnValue.replace(";", extSeparatorChar);
				}
				if (grnValue.isEmpty()) {
					continue;
				}
				grnValue = extSeparatorChar + grnValue + extSeparatorChar;
				if (!grnValue.contains(extCheck)) { // 'extCheck' not empty
					continue;
				}
				hm.compute(groupsName,
						(k, v) -> v == null ? ext.concat(extSeparatorChar) : v.concat(ext).concat(extSeparatorChar));
				extFounded = true;
			}
			if (extFounded) {
				extsListSetNoFound.remove(ext);
			}
		}

		if (!extsListSetNoFound.isEmpty()) {
			for (var ext : extsListSetNoFound) {
				hm.compute(Const.GROUPS_NO_GROUP, (k, v) -> v == null ? ext.concat(":") : v.concat(ext).concat(":"));
			}
		}
		return hm;
	}

	private void setProp(int mr, Set<String> extsNeedListSet, Set<String> extsNoNeedListSet) {
		if (extsNeedListSet == null || extsNoNeedListSet == null || isCheckResult != Const.MR_NO_CHOOSED) {
			return;
		}
		boolean addedNewNeed = false;
		boolean addedNewNoNeed = false;

		for (int i = 0; i < cbArray.length; i++) {
			var ext = getTrimmedExt(extsList.get(i));
			if (ext.isEmpty()) {
				continue;
			}
			if (cbArray[i].isSelected()) {
				extsNeedListSet.add(ext);
				addedNewNeed = true;
			} else {
				extsNoNeedListSet.add(ext);
				noNeedExts.add(ext);
				addedNewNoNeed = true;
			}
		}

		if (addedNewNeed) {
			CommonLib.saveToFile(false, 0, CopyMove.DeleteIfExists_OLD_DELETE,
					FileDataBase.getPathPropertyExtsNeed(false), FileDataBase.getPathPropertyExtsNeed(true),
					CommonLib.getListFromSet(1, extsNeedListSet));
		}

		if (addedNewNoNeed) {
			CommonLib.saveToFile(false, 0, CopyMove.DeleteIfExists_OLD_DELETE,
					FileDataBase.getPathPropertyExtsNoNeed(false), FileDataBase.getPathPropertyExtsNoNeed(true),
					CommonLib.getListFromSet(1, extsNoNeedListSet));
		}
	}

	@Override
	public List<String> call() throws InterruptedException {
		while (isCheckResult == Const.MR_NO_CHOOSED) {
			Thread.sleep(1024);
		}
		return noNeedExts;
	}

}
