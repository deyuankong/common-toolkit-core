package com.simon.credit.toolkit;

import java.util.Arrays;

import com.simon.credit.toolkit.sort.ShellSort;

public class ShellSortTest {

	public static void main(String[] args) {
		int[] array = { 8, 9, 1, 7, 2, 3, 5, 4, 6, 0 };
		ShellSort.sort(array);
		System.out.println(Arrays.toString(array));

//		// 创建要给80000个的随机的数组
//		int[] arr = new int[8000000];
//		for (int i = 0; i < 8000000; i++) {
//			arr[i] = (int) (Math.random() * 8000000); // 生成一个[0, 8000000) 数
//		}
//
//		System.out.println("排序前");
//		Date data1 = new Date();
//		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		String date1Str = simpleDateFormat.format(data1);
//		System.out.println("排序前的时间是=" + date1Str);
//		
//		//shellSort(arr); //交换式
//		shellSort2(arr);//移位方式
//		
//		Date data2 = new Date();
//		String date2Str = simpleDateFormat.format(data2);
//		System.out.println("排序前的时间是=" + date2Str);
	}

}
