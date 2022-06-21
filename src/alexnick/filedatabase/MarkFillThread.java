package alexnick.filedatabase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.JButton;

import alexnick.CommonLib;

public class MarkFillThread implements Callable<Integer> {
	private JButton butMark;
	private List<MyBean> beans;
	private List<Map<String, MarkInfo>> arMarkInfoList;

//'arMarkInfoMap' be filled	
	public MarkFillThread(JButton butMark, List<MyBean> beans, List<Map<String, MarkInfo>> arMarkInfoList) { // beans_not_empty
		this.butMark = butMark;
		this.beans = beans;
		this.arMarkInfoList = arMarkInfoList;
	}

	@Override
	public Integer call() throws Exception {
		if (butMark == null || CommonLib.nullEmptyList(beans) || CommonLib.notNullEmptyList(arMarkInfoList)
				|| CommonLib.nullEmptySet(FileDataBase.markPropertySet)) {
			return Const.MR_CANCEL;
		}
		butMark.setEnabled(false);
		int count = 0;

		for (int i = 0; i < beans.size(); i++) {
			int fourApp = 0;
// for each base, try create own map with found there 'mark' and markInfo fill to this 'mark'
// finally this map (no matter empty or filled) be added anyway to 'arMarkInfoList'			
			Map<String, MarkInfo> mapMarkExtsSignatures = new HashMap<String, MarkInfo>();
			var b = beans.get(i);

			try {
				if (b == null || !b.binPath.toFile().exists()) {
					throw new IllegalArgumentException("");
				}

				var list = CommonLib.readFile(2, 0, b.binPath);
				if (list.isEmpty()) {
					throw new IllegalArgumentException("");
				}

				for (var s : list) {
					String signature = ConverterBinFunc.getSignatureOrEmpty(s);
					if (signature.isEmpty()) {
						continue;
					}
					var mark = FileDataBase.getMarkFromPropertiesOrEmpty(signature);
					if (mark.isEmpty() || !FileDataBase.markPropertySet.contains(mark)) {
						continue;
					}
					fourApp++;

					var posIndexOfExt = s.lastIndexOf(Const.BRACE_END);
					if (posIndexOfExt < 0) {
						continue;
					}
					String ext = (posIndexOfExt == s.length() - 1) ? Const.EXTEMPTY
							: s.substring(posIndexOfExt + 1).toLowerCase();

					MarkInfo markInfo = mapMarkExtsSignatures.getOrDefault(mark, new MarkInfo());
					Set<String> extSignatures = markInfo.mapExtSignatures.getOrDefault(ext, new HashSet<String>());

					if (!extSignatures.add(signature)) { // duplicates
						continue;
					}
					markInfo.mapExtSignatures.put(ext, extSignatures);
					mapMarkExtsSignatures.put(mark, markInfo);
				}

			} catch (Exception e) {
			}

			b.setFour(null, fourApp == 0 ? "" : " **mark: " + fourApp);
			arMarkInfoList.add(mapMarkExtsSignatures);
			if (!mapMarkExtsSignatures.isEmpty()) {
				count++;
			}
		}
		butMark.setEnabled(count > 0);
		return count > 0 ? Const.MR_OK : Const.MR_CANCEL;
	}

}
