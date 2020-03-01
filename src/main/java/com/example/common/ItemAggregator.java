package com.example.common;

import org.springframework.batch.item.file.transform.LineAggregator;

import com.example.domain.Item;

public class ItemAggregator implements LineAggregator<Item> {
	
	/**
	 * DBから読み込んだitems情報をcsvに書き込む
	 * @param item
	 * @return
	 */
	@Override
	public String aggregate(Item item) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(item.getId());
		sb.append(","); //csvだからカンマ区切り
		sb.append(item.getName());
		sb.append(",");
		sb.append(item.getPrice());
		
		return sb.toString();
	}

}
