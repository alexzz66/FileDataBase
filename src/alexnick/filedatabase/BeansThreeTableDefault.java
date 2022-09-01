package alexnick.filedatabase;

import java.util.List;
import java.util.Objects;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class BeansThreeTableDefault extends JTable {
	private static final long serialVersionUID = 1L;

	public BeansThreeTableDefault(String oneCapt, String twoCapt, String threeCapt, List<MyBean> beans) {
		super(new BeansThreeTableModel(oneCapt, twoCapt, threeCapt, beans));
		setRowHeight(24);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
}

final class BeansThreeTableModel implements TableModel {
	private final String oneCapt;
	private final String twoCapt;
	private final String threeCapt;
	private final List<MyBean> beans;

	BeansThreeTableModel(String oneCapt, String twoCapt, String threeCapt, List<MyBean> beans) {
		this.oneCapt = oneCapt;
		this.twoCapt = twoCapt;
		this.threeCapt = threeCapt;
		this.beans = beans;
	}

	@Override
	public int getRowCount() {
		return beans.size();
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return switch (columnIndex) {
		case 0 -> oneCapt;
		case 1 -> twoCapt;
		case 2 -> threeCapt;
		default -> "";
		};
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		MyBean bean = beans.get(rowIndex);
		return switch (columnIndex) {
		case 0 -> bean.getOne();
		case 1 -> bean.getTwo();
		case 2 -> bean.getThree();
		default -> throw new IllegalStateException("Unexpected value: " + columnIndex);
		};
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
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
		var that = (BeansThreeTableModel) obj;
		return Objects.equals(this.beans, that.beans);
	}

	@Override
	public int hashCode() {
		return Objects.hash(beans);
	}

}