package alexnick.filedatabase;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JFrame;

public class DeleteEmptyFoldersTable extends JFrame implements Callable<Integer> {
	private static final long serialVersionUID = 1L;
	private int isCheckResult = Const.MR_NO_CHOOSED;
	private List<MyBean> beans;
	private List<String> pathsForDelete = null;

	// CONSTRUCTOR !!! NO SORT TABLE for correct deleting result
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
		beans.addAll(beans0);
		if (beans.isEmpty()) {
			return;
		}
		setTitle("Delete empty folders: " + beans.size() + ". Start folder: " + startFolder);
		pathsForDelete = new ArrayList<>();
		for (var b : beans) {
			pathsForDelete.add(b.binPath.toString());
		}

		isCheckResult = Const.MR_DELETE;
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
