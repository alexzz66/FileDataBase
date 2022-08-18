package alexnick.filedatabase;

import java.util.Comparator;
import java.util.List;

public class SortBeans {
	final static int sortNoDefined = -42;
	final static int sortCheck_ThenFour = 0;
	final static int sortCheck_Shift_ThenFourName = 42;
	final static int sortCheck_ThenStartNumber = 142;

	final static int sortOne = 1;
	final static int sortOne_Shift_CheckOnly = -1;

	final static int sortOneLowerCase = 10; // binfolder <ID3>
	final static int sortOneLowerCase_Shift_CheckOnly = -10;

	final static int sortTwo = 2; // size,signature
	final static int sortTwo_Shift_CheckOnly = -2;

	final static int sortTwoLowerCase = 20;

	final static int sortThree = 3; // modified
	final static int sortThreeLowerCase = 30;
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
	final static int sortServiceIntThreeThenBinPathNoCheckForNull = 5003;

	final static int sortServiceStringOneNoCheckForNull = 5011;
	final static int sortServiceStringTwoNoCheckForNull = 5012;

	final static int sortBinPathNoCheckForNull = 6000; // in lower case

	private final static String initAppendCaption = ", sorted by ";

	private String appendCaption = initAppendCaption;
	private boolean isAppCaptionInQuotes = false;

	public SortBeans(int sortType, String sortCaption, List<MyBean> beans) {
		if (beans != null && beans.size() > 1) {
			sorting(sortType, sortCaption, beans);
		}
	}

	private void sorting(int sortType, String sortCaption, List<MyBean> beans) {
		boolean isSortType = true;
		switch (sortType) {
		case sortCheck_ThenFour -> sortCheck_ThenFour(beans);
		case sortCheck_ThenStartNumber -> sortCheck_ThenStartNumber(beans);
		case sortOne -> sortOne(beans);
		case sortOneLowerCase -> sortOneLowerCase(beans);
		case sortTwo -> sortTwo(beans);
		case sortTwoLowerCase -> sortTwoLowerCase(beans);

		case sortThree -> sortThree(beans);
		case sortThreeLowerCase -> sortThreeLowerCase(beans);

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
		case sortServiceIntThreeThenBinPathNoCheckForNull -> sortServiceIntThreeThenBinPathNoCheckForNull(beans);
		case sortServiceLong -> sortServiceLong(beans);
		case sortServiceStringOneNoCheckForNull -> sortServiceStringOneNoCheckForNull(beans);
		case sortServiceStringTwoNoCheckForNull -> sortServiceStringTwoNoCheckForNull(beans);
		case sortBinPathNoCheckForNull -> sortBinPathNoCheckForNull(beans);
		default -> isSortType = false;
		}
		;
		if (isSortType) {
			appToCaptionInQuotes(sortCaption);
		}
	}

	private int defaultCompare(MyBean o1, MyBean o2) {
		return o1.getFourLowerCase(false, false).compareTo(o2.getFourLowerCase(false, false));
	}

	private int defaultCompareName(MyBean o1, MyBean o2) {
		return o1.getNameFromFour(true).compareTo(o2.getNameFromFour(true));
	}

	private void sortCheck_ThenFour(List<MyBean> beans) {
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

	private void sortCheck_ThenStartNumber(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.getCheck() == o2.getCheck()) {
					return defaultCompare(o1, o2);
				}
				return o1.getCheck() ? -1 : 1;
			}
		});
	}

	private void sortCheck_Shift_ThenFourName(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.getCheck() == o2.getCheck()) {
					return defaultCompareName(o1, o2);
				}
				return o1.getCheck() ? -1 : 1;
			}
		});
	}

	private void sortOne(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getOne();
				var s2 = o2.getOne();
				if (s1.equals(s2)) {
					return defaultCompare(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortOneLowerCase(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getOneLowerCase();
				var s2 = o2.getOneLowerCase();
				if (s1.equals(s2)) {
					return defaultCompare(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortOne_Shift_CheckOnly(List<MyBean> beans) {
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
						return defaultCompare(o1, o2);
					}
					return s1.compareTo(s2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortOneLowerCase_Shift_CheckOnly(List<MyBean> beans) {
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
						return defaultCompare(o1, o2);
					}
					return s1.compareTo(s2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortTwo(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getTwo();
				var s2 = o2.getTwo();
				if (s1.equals(s2)) {
					return defaultCompare(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortTwoLowerCase(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getTwo().toLowerCase();
				var s2 = o2.getTwo().toLowerCase();
				if (s1.equals(s2)) {
					return defaultCompare(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortTwo_Shift_CheckOnly(List<MyBean> beans) {
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
						return defaultCompare(o1, o2);
					}
					return s1.compareTo(s2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortThree(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getThree();
				var s2 = o2.getThree();
				if (s1.equals(s2)) {
					return defaultCompare(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortThreeLowerCase(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.getThree().toLowerCase();
				var s2 = o2.getThree().toLowerCase();
				if (s1.equals(s2)) {
					return defaultCompare(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortThree_Shift_CheckOnly(List<MyBean> beans) {
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
						return defaultCompare(o1, o2);
					}
					return s1.compareTo(s2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortFourLowerCase(List<MyBean> beans) {
		beans.sort(Comparator.comparing(bean -> bean.getFourLowerCase(false, false)));
	}

	private void sortFourLowerCase_Shift_CheckOnly(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				// sort if both checked
				boolean o1Check = o1.getCheck();
				if (o1Check == o2.getCheck()) {
					if (!o1Check) { // both unchecked
						return 0;
					}
					return defaultCompare(o1, o2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortFourNameLowerCase(List<MyBean> beans) {
		beans.sort(Comparator.comparing(bean -> bean.getNameFromFour(true)));
	}

	private void sortFourNameLowerCase_Shift_CheckOnly(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				// sort if both checked
				boolean o1Check = o1.getCheck();
				if (o1Check == o2.getCheck()) {
					if (!o1Check) { // both unchecked
						return 0;
					}
					return defaultCompareName(o1, o2);
				}
				return o1Check ? -1 : 1;
			}
		});
	}

	private void sortFourStartNumber(List<MyBean> beans) {
		beans.sort(Comparator.comparingInt(bean -> bean.getStartNumberFromFour()));
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
	private void sortServiceIntOne(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceIntOne == o2.serviceIntOne) {
					return defaultCompare(o1, o2);
				}
				return o1.serviceIntOne - o2.serviceIntOne;
			}
		});
	}

	private void sortServiceIntTwo(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceIntTwo == o2.serviceIntTwo) {
					return defaultCompare(o1, o2);
				}
				return o1.serviceIntTwo - o2.serviceIntTwo;
			}
		});
	}

	private void sortServiceStringOneNoCheckForNull(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.serviceStringOne;
				var s2 = o2.serviceStringOne;
				if (s1.equals(s2)) {
					return defaultCompare(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private void sortServiceStringTwoNoCheckForNull(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				var s1 = o1.serviceStringTwo;
				var s2 = o2.serviceStringTwo;
				if (s1.equals(s2)) {
					return defaultCompare(o1, o2);
				}
				return s1.compareTo(s2);
			}
		});
	}

	private int compareBinPathNoCheckForNull(MyBean o1, MyBean o2) {
		var s1 = o1.binPath.toString().toLowerCase();
		var s2 = o2.binPath.toString().toLowerCase();
		if (s1.equals(s2)) {
			return defaultCompare(o1, o2);
		}
		return s1.compareTo(s2);
	}

	private void sortBinPathNoCheckForNull(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				return compareBinPathNoCheckForNull(o1, o2);
			}
		});
	}

	private void sortServiceIntThreeThenBinPathNoCheckForNull(List<MyBean> beans) {
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

	private void sortServiceLong(List<MyBean> beans) {
		beans.sort(new Comparator<MyBean>() {
			@Override
			public int compare(MyBean o1, MyBean o2) {
				if (o1.serviceLong == o2.serviceLong) {
					return defaultCompare(o1, o2);
				}
				return o1.serviceLong > o2.serviceLong ? 1 : -1;
			}
		});
	}

	private void sortServiceLong_Shift_CheckOnly(List<MyBean> beans) {
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
						return defaultCompare(o1, o2);
					}
					return o1.serviceLong > o2.serviceLong ? 1 : -1;
				}

				return o1Check ? -1 : 1;
			}
		});
	}

}
