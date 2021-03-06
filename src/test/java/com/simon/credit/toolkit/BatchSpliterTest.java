package com.simon.credit.toolkit;

import java.util.ArrayList;
import java.util.List;

import com.simon.credit.toolkit.batch.BatchCallback;
import com.simon.credit.toolkit.batch.BatchExecuter;

public class BatchSpliterTest {

	public static void main(String[] args) {
		List<Integer> nums = new ArrayList<Integer>(1600);
		for (int i = 0; i < 1000; i++) {
			nums.add(i);
		}

		new BatchExecuter(10).execute(nums, new BatchCallback<List<Integer>>() {
			@Override
			public void process(List<Integer> batchParams) {
				System.out.println(batchParams);
			}
		});
	}

}