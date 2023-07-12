package alexnick.filedatabase;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import alexnick.CommonLib;

public class SortBeans {
	final static int sortNoDefined = -42;

	final static int checkStartID = 1_000_000;

	final static int sortCheck = checkStartID + 1;
	final static int sortCheck_Shift_ThenFourName = checkStartID + 2;
	final static int sortCheck_ThenFourStartNumber = checkStartID + 3;

	final static int sortOne = 1;
	final static int sortOne_Shift_CheckOnly = -1;

	final static int sortOneLowerCase = 10; // binfolder <ID3>
	final static int sortOneLowerCase_Shift_CheckOnly = -10;

	final static int sortTwo = 2; // size,signature
	final static int sortTwo_Shift_CheckOnly = -2;

	final static int sortTwoLowerCase = 20;

	final static int sortThree = 3; // modified
	final static int sortThreeLowerCase = 30;
	final static int sortThreeNameLowerCase = 300;// pathName
	final static int sortThree_Shift_CheckOnly = -3;

	final static int sortFourLowerCase = 40; // path
	final static int sortFourLowerCase_Shift_CheckOnly = -40;

	final static int sortFourNameLowerCase = 400;// pathName
	final static int sortFourNameLowerCase_Shift_CheckOnly = -400;

	final static int sortFourStartNumber = 4000;

	final static int sortServiceLong = 5000;
	final static int sortServiceLong_Shift_CheckOnly = -5000;

	final static int sortServiceIntOne = 5001;
	final static int sortServiceIntTwo = 5002;

	final static int sortServiceIntThree = 5003;
	final static int sortServiceIntThreeThenCheck = 5004;
	final static int sortServiceIntThreeThenBinPathNoCheckForNull = 5005;
	final static int sortServiceIntThreeThenThree = 5006;
	final static int sortServiceIntThreeThenSortCaption = 5007;

	final static int sortServiceStringOneNoCheckForNull = 5011;
	final static int sortServiceStringTwoNoCheckForNull = 5012;

	final static int sortBinPathNoCheckForNull = 6000; // in lower case
	final static int sortSelectedIfNoEmpty = 7000;

	private final static String initAppendCaption = ", sorted by ";

	private String appendCaption = initAppendCaption;
	private boolean isAppCaptionInQuotes = false;
	private boolean beansWasSorted = false;

	public SortBeans(int sortType, String sortCaption, List<MyBean> beans) {
		sorting(sortType, sortCaption, beans);
	}

	public SortBeans(int sortType, String sortCaption, List<MyBean> beans, BeansFourTableDefault myTable) {
		Set<Integer> setSelected = getSelectedRowsFilesOrEmpty(myTable);

		if (sortType == sortSelectedIfNoEmpty && setSelected.isEmpty()) {
			return;
		}

		for (int i = 0; i < beans.size(); i++) {
			beans.get(i).selectedForSorting = setSelected.contains(i);
		}

		sorting(sortType, sortCaption, beans);
		myTable.updateUI();
		setSelectionRowsFiles(setSelected, beans, myTable);
	}

	private void setSelectionRowsFiles(Set<Integer> setSelected, List<MyBean> beans, BeansFourTableDefault myTable) {
		myTable.clearSelection();
		if (CommonLib.nullEmptySet(setSelected)) {
			return;
		}

		for (var i = 0; i < beans.size(); i++) {
			if (beans.get(i).selectedForSorting) {
				myTable.addRowSelectionInterval(i, i);
			}
		}
	}

	static Set<Integer> getSelectedRowsFilesOrEmpty(BeansFourTableDefault myTable) {
		Set<Integer> setSelected = new HashSet<Integer>();
		for (var row : myTable.getSelectedRows()) {
			setSelected.add(row);
		}
		return setSelected;
	}

	private void sorting(int sortType, String sortCaption, List<MyBean> beans) {
		if (beans == null || beans.size() < 2) {
			return;
		}

		beansWasSorted = true;
		switch (sortType) {
		case sortCheck -> sortCheck(beans);
		case sortCheck_ThenFourStartNumber -> sortCheck_ThenFourStartNumber(beans);

		case sortSelectedIfNoEmpty -> sortSelectedIfNoEmpty(beans);
		case sortOne -> sortOne(beans);
		case sortOneLowerCase -> sortOneLowerCase(beans);
		case sortTwo -> sortTwo(beans);
		case sortTwoLowerCase -> sortTwoLowerCase(beans);

		case sortThree -> sortThree(beans);
		case sortThreeLowerCase -> sortThreeLowerCase(beans);
		case sortThreeNameLowerCase -> sortThreeNameLowerCase(beans);

		case sortFourLowerCase -> sortFourLowerCase(beans);
		case sortFourNameLowerCase -> sortFourNameLowerCase(beans);
		case sortFourStartNumber -> sortFourStartNumber(beans);

		case sortCheck_Shift_ThenFourName -> sortCheck_Shift_ThenFourName(beans);
		case sortOne_Shift_CheckOnly -> sortOne_Shift_CheckOnly(beans);
		case sortOneLowerCase_Shift_CheckOnly -> sortOneLowerCase_Shift_CheckOnly(beans);
		case sortTwo_Shift_CheckOnly -> sortTwo_Shift_CheckOnly(beans);
		case sortThree_Shift_CheckOnly -> sortThree_Shift_CheckOnly(beans);
		case sortFourLowerCase_Shift_CheckOnly -> sortFourLowerCase_Shift_CheckOnly(beans);
		case sortFourNameLowerCase_Shift_CheckOnly -> sortFourNameLowerCase_Shift_CheckOnly(beans);
		case sortServiceLong_Shift_CheckOnly -> sortServiceLong_Shift_CheckOnly(beans);

		case sortServiceIntOne -> sortServiceIntOne(beans);
		case sortServiceIntTwo -> sortServiceIntTwo(beans);

		case sortServiceIntThree -> sortServiceIntThree(beans);
		case sortServiceIntThreeThenCheck -> sortServiceIntThreeThenCheck(beans);
		case sortServiceIntThreeThenBinPathNoCheckForNull -> sortServiceIntThreeThenBinPathNoCheckForNull(beans);
		case sortServiceIntThreeThenThree -> sortServiceIntThreeThenThree(beans);
		case sortServiceIntThreeThenSortCaption -> sortServiceIntThreeThenSortCaption(sortCaption, beans);

		case sortServiceLong -> sortServiceLong(beans);
		case sortServiceStringOneNoCheckForNull -> sortServiceStringOneNoCheckForNull(beans);
		case sortServiceStringTwoNoCheckForNull -> sortServiceStringTwoNoCheckForNull(beans);
		case sortBinPathNoCheckForNull -> sortBinPathNoCheckForNull(beans);

		default -> beansWasSorted = false;
		}
		;
		if (beansWasSorted) {
			appToCaptionInQuotes(sortCaption);
		}
	}

	private int sortFourLowerCase(MyBean o1, MyBean o2) { // BASIC: by four lower case without fourApp
		return o1.getFourLowerCase(false, false).compareTo(o2.getFourLowerCase(false, false));
	}

	private void sortFourLowerCase(List<MyBean> beans) { // by four lower case without fourApp
		beans.sort(Comparator.comparing(bean -> bean.getFourLowerCase(false, false)));
	}

	private int sortFourNameLowerCase(MyBean o1, MyBean o2) { // BASIC: by name lower case from four without fourApp
		return o1.getNameFromFour(true).compareTo(o2.getNameFromFour(true));
	}

	private void sortFourNameLowerCase(List<MyBean> beans) { // by name lower case from four without fourApp
		beans.sort(Comparator.comparing(bean -> bean.getNameFromFour(true)));
	}

	private void sortThreeNameLowerCase(List<MyBean> beans) { // by name lower case from three
		beans.sort(Comparator.comparing(bean -> bean.getNameFromThree(true)));
	}

	private void sortCheck(List<MyBean> beans) { // by check then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.getCheck() == o2.getCheck()) {
					return sortFourLowerCase(o1, o2);
				}
				return o1.getCheck() ? -1 : 1;
			}
		});
	}

	private void sortCheck_Shift_ThenFourName(List<MyBean> beans) { // by checked then name from four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.getCheck() == o2.getCheck()) {
					return sortFourNameLowerCase(o1, o2);
				}
				return o1.getCheck() ? -1 : 1;
			}
		});
	}

	private void sortCheck_ThenFourStartNumber(List<MyBean> beans) { // by checked then start number from four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.getCheck() == o2.getCheck()) {
					return o1.getStartNumberFromFour() - o2.getStartNumberFromFour();
				}
				return o1.getCheck() ? -1 : 1;
			}
		});
	}

	private void sortFourStartNumber(List<MyBean> beans) { // by start number from four
		beans.sort(Comparator.comparingInt(bean -> bean.getStartNumberFromFour()));
	}

	private void sortSelectedIfNoEmpty(List<MyBean> beans) { // by selected then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.selectedForSorting == o2.selectedForSorting) {
					return sortFourLowerCase(o1, o2);
				}
				return o1.selectedForSorting ? -1 : 1;
			}
		});
	}

	private void sortOne(List<MyBean> beans) { // by one then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getOne();
				var s2 = o2.getOne();
				if (s1.equals(s2)) {
					return sortFourLowerCase(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortOneLowerCase(List<MyBean> beans) { // by one to lower case then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getOneLowerCase();
				var s2 = o2.getOneLowerCase();
				if (s1.equals(s2)) {
					return sortFourLowerCase(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortOne_Shift_CheckOnly(List<MyBean> beans) { // by checked only then one then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				// sort if both checked
				boolean o1Check = o1.getCheck();
				if (o1Check == o2.getCheck()) {
					if (!o1Check) { // both unchecked
						return 0;
					}
					var s1 = o1.getOne();
					var s2 = o2.getOne();
					if (s1.equals(s2)) {
						return sortFourLowerCase(o1, o2);
					}
					return s1.compareTo(s2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortOneLowerCase_Shift_CheckOnly(List<MyBean> beans) { // by checked only then one lower case then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				// sort if both checked
				boolean o1Check = o1.getCheck();
				if (o1Check == o2.getCheck()) {
					if (!o1Check) { // both unchecked
						return 0;
					}
					var s1 = o1.getOneLowerCase();
					var s2 = o2.getOneLowerCase();
					if (s1.equals(s2)) {
						return sortFourLowerCase(o1, o2);
					}
					return s1.compareTo(s2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortTwo(List<MyBean> beans) { // by two then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getTwo();
				var s2 = o2.getTwo();
				if (s1.equals(s2)) {
					return sortFourLowerCase(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortTwoLowerCase(List<MyBean> beans) { // by two lower case then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getTwo().toLowerCase();
				var s2 = o2.getTwo().toLowerCase();
				if (s1.equals(s2)) {
					return sortFourLowerCase(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortTwo_Shift_CheckOnly(List<MyBean> beans) { // by checked only then two then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				// sort if both checked
				boolean o1Check = o1.getCheck();
				if (o1Check == o2.getCheck()) {
					if (!o1Check) { // both unchecked
						return 0;
					}
					var s1 = o1.getTwo();
					var s2 = o2.getTwo();
					if (s1.equals(s2)) {
						return sortFourLowerCase(o1, o2);
					}
					return s1.compareTo(s2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortThree(List<MyBean> beans) { // by three then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getThree();
				var s2 = o2.getThree();
				if (s1.equals(s2)) {
					return sortFourLowerCase(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortThreeLowerCase(List<MyBean> beans) { // by three lower case then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getThree().toLowerCase();
				var s2 = o2.getThree().toLowerCase();
				if (s1.equals(s2)) {
					return sortFourLowerCase(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortThree_Shift_CheckOnly(List<MyBean> beans) { // by checked only then three then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				// sort if both checked
				boolean o1Check = o1.getCheck();
				if (o1Check == o2.getCheck()) {
					if (!o1Check) { // both unchecked
						return 0;
					}
					var s1 = o1.getThree();
					var s2 = o2.getThree();
					if (s1.equals(s2)) {
						return sortFourLowerCase(o1, o2);
					}
					return s1.compareTo(s2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortFourLowerCase_Shift_CheckOnly(List<MyBean> beans) { // by checked only then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				// sort if both checked
				boolean o1Check = o1.getCheck();
				if (o1Check == o2.getCheck()) {
					if (!o1Check) { // both unchecked
						return 0;
					}
					return sortFourLowerCase(o1, o2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortFourNameLowerCase_Shift_CheckOnly(List<MyBean> beans) { // by checked only then name lower case
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				// sort if both checked
				boolean o1Check = o1.getCheck();
				if (o1Check == o2.getCheck()) {
					if (!o1Check) { // both unchecked
						return 0;
					}
					return sortFourNameLowerCase(o1, o2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void appToCaptionInQuotes(String s) {
		if (isAppCaptionInQuotes || s.isEmpty()) {
			return;
		}
		isAppCaptionInQuotes = true;
		appendCaption += "'" + s + "'";
	}

	String getAppendCaption() {
		return appendCaption.equals(initAppendCaption) ? "" : appendCaption;
	}

//SERVICE SORT	
	private void sortServiceIntOne(List<MyBean> beans) { // by serviceIntOne then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceIntOne == o2.serviceIntOne) {
					return sortFourLowerCase(o1, o2);
				}
				return o1.serviceIntOne - o2.serviceIntOne;
			}
		});
	}

	private void sortServiceIntTwo(List<MyBean> beans) { // by serviceIntTwo then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceIntTwo == o2.serviceIntTwo) {
					return sortFourLowerCase(o1, o2);
				}
				return o1.serviceIntTwo - o2.serviceIntTwo;
			}
		});
	}

	private void sortServiceStringOneNoCheckForNull(List<MyBean> beans) { // by serviceStringOne then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.serviceStringOne;
				var s2 = o2.serviceStringOne;
				if (s1.equals(s2)) {
					return sortFourLowerCase(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortServiceStringTwoNoCheckForNull(List<MyBean> beans) { // by serviceStringTwo then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.serviceStringTwo;
				var s2 = o2.serviceStringTwo;
				if (s1.equals(s2)) {
					return sortFourLowerCase(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortServiceIntThree(List<MyBean> beans) { // by serviceIntThree then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceIntThree == o2.serviceIntThree) {
					return sortFourLowerCase(o1, o2);
				}
				return o1.serviceIntThree - o2.serviceIntThree;
			}
		});
	}

	private void sortServiceIntThreeThenCheck(List<MyBean> beans) { // by serviceIntThree then check
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceIntThree == o2.serviceIntThree) {
					boolean o1Check = o1.getCheck();
					if (o1Check == o2.getCheck()) {
						return sortFourLowerCase(o1, o2);
					}
					return o1Check ? -1 : 1;
				}
				return o1.serviceIntThree - o2.serviceIntThree;
			}
		});
	}

	private void sortServiceIntThreeThenSortCaption(String sortCaption, List<MyBean> beans) { // by serviceIntThree then
																								// sortCaption
		if (CommonLib.nullEmptyString(sortCaption)) {
			return;
		}

		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceIntThree == o2.serviceIntThree) {
					var p1 = o1.getFourLowerCase(false, false); // without extension, because standard sort by path the
																// same
					var p2 = o2.getFourLowerCase(false, false);

					boolean o1Check = p1.contains(sortCaption);
					boolean o2Check = p2.contains(sortCaption);

					if (o1Check == o2Check) {
						return p1.compareTo(p2);
					}
					return o1Check ? -1 : 1;
				}
				return o1.serviceIntThree - o2.serviceIntThree;
			}
		});
	}

	private int compareBinPathNoCheckForNull(MyBean o1, MyBean o2) { // BASIC: by binPath then four
		var s1 = o1.binPath.toString().toLowerCase();
		var s2 = o2.binPath.toString().toLowerCase();
		if (s1.equals(s2)) {
			return sortFourLowerCase(o1, o2);
		}
		return s1.compareTo(s2);
	}

	private void sortBinPathNoCheckForNull(List<MyBean> beans) { // by binPath then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				return compareBinPathNoCheckForNull(o1, o2);
			}
		});
	}

	private void sortServiceIntThreeThenBinPathNoCheckForNull(List<MyBean> beans) { // by serviceIntThree then binPath
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceIntThree == o2.serviceIntThree) {
					return compareBinPathNoCheckForNull(o1, o2);
				}
				return o1.serviceIntThree - o2.serviceIntThree;
			}
		});
	}

	private void sortServiceIntThreeThenThree(List<MyBean> beans) { // by serviceIntThree then three
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceIntThree == o2.serviceIntThree) {
					var s1 = o1.getThree();
					var s2 = o2.getThree();
					if (s1.equals(s2)) {
						return sortFourLowerCase(o1, o2);
					}
					return s1.compareTo(s2);
				}
				return o1.serviceIntThree - o2.serviceIntThree;
			}
		});
	}

	private void sortServiceLong(List<MyBean> beans) { // by serviceLong then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceLong == o2.serviceLong) {
					return sortFourLowerCase(o1, o2);
				}
				return o1.serviceLong > o2.serviceLong ? 1 : -1;
			}
		});
	}

	private void sortServiceLong_Shift_CheckOnly(List<MyBean> beans) { // by checked only then serviceLong then four
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				// sort if both checked
				boolean o1Check = o1.getCheck();
				if (o1Check == o2.getCheck()) {
					if (!o1Check) { // both unchecked
						return 0;
					}

					if (o1.serviceLong == o2.serviceLong) {
						return sortFourLowerCase(o1, o2);
					}
					return o1.serviceLong > o2.serviceLong ? 1 : -1;
				}

				return o1Check ? -1 : 1;
			}
		});
	}

	boolean isBeansWasSorted() {
		return beansWasSorted;
	}

}
