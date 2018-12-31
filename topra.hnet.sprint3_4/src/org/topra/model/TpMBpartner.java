package org.topra.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;

@SuppressWarnings("serial")
public class TpMBpartner extends MBPartner{

	public TpMBpartner(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}
	
	public TpMBpartner(Properties ctx, int C_BPartner_ID, String trxName) {
		super(ctx, C_BPartner_ID, trxName);
		// TODO Auto-generated constructor stub
	}
	//validate name must have two words name 1 and name 2
	@Override
	protected boolean beforeSave(boolean newRecord) {
		
		if(getName() != null || getName().length() > 0){
			
			System.out.println("Hello Hello");
			
			String name  = getName().trim();
			String [] nameArr = name.split("\\s+");
			
			if(nameArr.length <=1){
				throw new AdempiereException("Name should be consist with at least two parts!");
			}
		}
		
		return super.beforeSave(newRecord);
	}

}
