package com.simon.credit.toolkit;

import java.util.Arrays;

import com.simon.credit.toolkit.sort.MergeSort;

/**
 * 归并排序测试
 * @author XUZIMING 2019-11-28
 */
public class MergeSortTest {

	public static void main(String[] args) {
		int[] array = { 8, 4, 5, 7, 1, 3, 6, 2 };
		MergeSort.sort(array);
		System.out.println("归并排序之后 = " + Arrays.toString(array));
	}

}
