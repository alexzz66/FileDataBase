package alexnick.filedatabase;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import java.util.List;
import java.util.Objects;

public class BeansFourTableDefault extends JTable {
	private static final long serialVersionUID = 1L;

	/**
	 * Model of table with 4 columns
	 * 
	 * @param selectionMode          0 (by
	 *                               default):ListSelectionModel.SINGLE_SELECTION;<br>
	 *                               1:ListSelectionModel.SINGLE_INTERVAL_SELECTION;<br>
	 *                               2:ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
	 * @param setOneColumnMaxWidth   if 'true', will be max width = 500
	 * @param setTwoColumnMaxWidth   if 'true', will be max width = 280
	 * @param setThreeColumnMaxWidth if 'true', will be max width = 200
	 * @param oneCapt                not null, column caption
	 * @param twoCapt                not null, column caption
	 * @param threeCapt              not null, column caption
	 * @param fourCapt               not null, column caption
	 * @param beans                  not null, items in table
	 */
	public BeansFourTableDefault(int selectionMode, boolean setOneColumnMaxWidth, boolean setTwoColumnMaxWidth,
			boolean setThreeColumnMaxWidth, String oneCapt, String twoCapt, String threeCapt, String fourCapt,
			List<MyBean> beans) {
		super(new BeansFourTableModel(oneCapt, twoCapt, threeCapt, fourCapt, beans));
		setRowHeight(24);
		if (selectionMode < 0 || selectionMode > 2) {
			selectionMode = 0;
		}
		setSelectionMode(selectionMode);
		// check
		getColumnModel().getColumn(0).setMaxWidth(30);
		getColumnModel().getColumn(0).setMinWidth(30);

		if (setOneColumnMaxWidth) {
			getColumnModel().getColumn(1).setMaxWidth(500);
		}
		getColumnModel().getColumn(1).setMinWidth(150);

		if (setTwoColumnMaxWidth) {
			getColumnModel().getColumn(2).setMaxWidth(280);
		}
		getColumnModel().getColumn(2).setMinWidth(130);

		// modified
		if (setThreeColumnMaxWidth) {
			getColumnModel().getColumn(3).setMaxWidth(200);
		}
		getColumnModel().getColumn(3).setMinWidth(150);

		getColumnModel().getColumn(4).setMinWidth(50);
	}
}

final class BeansFourTableModel implements TableModel {
	private final String oneCapt;
	private final String twoCapt;
	private final String threeCapt;
	private final String fourCapt;
	private final List<MyBean> beans;

	BeansFourTableModel(String oneCapt, String twoCapt, String threeCapt, String fourCapt, List<MyBean> beans) {
		this.oneCapt = oneCapt;
		this.twoCapt = twoCapt;
		this.threeCapt = threeCapt;
		this.fourCapt = fourCapt;
		this.beans = beans;
	}

	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 0) {
			return Boolean.class;
		}
		return String.class;
	}

	public int getColumnCount() {
		return 5;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return switch (columnIndex) {
		case 1 -> oneCapt;
		case 2 -> twoCapt;
		case 3 -> threeCapt;
		case 4 -> fourCapt;
		default -> "";
		};
	}

	public int getRowCount() {
		return beans.size();
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		MyBean bean = beans.get(rowIndex);
		return switch (columnIndex) {
		case 0 -> bean.check;
		case 1 -> bean.getOne();
		case 2 -> bean.getTwo();
		case 3 -> bean.getThree();
		case 4 -> bean.getFour(false, true);
		default -> throw new IllegalStateException("Unexpected value: " + columnIndex);
		};
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		MyBean bean = beans.get(rowIndex);
		bean.check = (Boolean) value;
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
	}

	public List<MyBean> beans() {
		return beans;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		var that = (BeansFourTableModel) obj;
		return Objects.equals(this.beans, that.beans);
	}

	@Override
	public int hashCode() {
		return Objects.hash(beans);
	}

}