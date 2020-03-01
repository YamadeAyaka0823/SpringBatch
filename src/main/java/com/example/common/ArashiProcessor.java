package com.example.common;

import java.util.Date;

import org.springframework.batch.item.ItemProcessor;

import com.example.domain.Arashi;

public class ArashiProcessor implements ItemProcessor<Arashi,Arashi> {
	
	@Override
	public Arashi process(Arashi arashi) {
		Integer id = arashi.getId();
		String name = arashi.getName();
		Date day = arashi.getDay();
		String email = arashi.getEmail();
		Integer companyId = arashi.getCompanyId();
		
		Arashi arashiMember = new Arashi(id,name,day,email,companyId);
		return arashiMember;
	}

}
