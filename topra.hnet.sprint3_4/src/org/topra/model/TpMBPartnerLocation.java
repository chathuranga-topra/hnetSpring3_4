package org.topra.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MLocation;

public class TpMBPartnerLocation extends MBPartnerLocation{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6436336372093070827L;

	public TpMBPartnerLocation(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	public TpMBPartnerLocation(Properties ctx, int C_BPartner_Location_ID, String trxName) {
		super(ctx, C_BPartner_Location_ID, trxName);
		// TODO Auto-generated constructor stub
	}
	
	//set all caps
	@Override
	protected boolean beforeSave(boolean newRecord) {
		
		return super.beforeSave(newRecord);
	}
	
	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		
		MLocation loc =  getLocation(false);
		
		if(loc != null){

			if(loc.getAddress1()!=null){
				loc.setAddress1(loc.getAddress1().toUpperCase());
			}
			
			if(loc.getAddress2()!=null){
				loc.setAddress2(loc.getAddress2().toUpperCase());
			}
			
			if(loc.getAddress3()!=null){
				loc.setAddress3(loc.getAddress3().toUpperCase());
			}
			
			if(loc.getAddress4()!=null){
				loc.setAddress4(loc.getAddress4().toUpperCase());
			}
			
			loc.save(get_TrxName());
			
			if(getName()!=null){
				setName(getName().toUpperCase());
			}
		}
		
		return super.afterSave(newRecord, success);
	}
}
