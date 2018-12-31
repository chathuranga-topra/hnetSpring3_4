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

			loc.setAddress1(loc.getAddress1().toUpperCase());
			loc.setAddress2(loc.getAddress2().toUpperCase());
			loc.setAddress3(loc.getAddress2().toUpperCase());
			loc.setAddress4(loc.getAddress4().toUpperCase());
			
			loc.save(get_TrxName());
			
			setName(getName().toUpperCase());
		}
		
		return super.afterSave(newRecord, success);
	}
}
