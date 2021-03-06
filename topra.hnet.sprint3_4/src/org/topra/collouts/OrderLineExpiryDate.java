package org.topra.collouts;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;
import org.compiere.model.MOrderLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MTab;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

public class OrderLineExpiryDate implements IColumnCallout{

	private static CLogger		s_log = CLogger.getCLogger (MStorageOnHand.class);
	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		
		if(value == null)
			return "";
		if(mField.getColumnName().equalsIgnoreCase(MOrderLine.COLUMNNAME_M_Product_ID)){
		
		int	C_DocTypeTarget_ID = (int) mTab.getParentTab().getValue("C_DocTypeTarget_ID");
		MDocType dt = new MDocType(ctx, C_DocTypeTarget_ID, mTab.getTrxInfo());
		//Need to validate only document type based only
		if(!dt.get_ValueAsBoolean("isexpirycontrol")){
			return "";
		}
		
		int M_Product_ID = 0;
		int M_Locator_ID = 0;
		
		//when changing product column 
		//get ALL ASI
		M_Product_ID = (int) value;
		M_Locator_ID = (int) mTab.getParentTab().getValue("M_WareHouse_ID");
			
			MStorageOnHand storageOnHand = getImmediateASI(ctx ,M_Product_ID ,M_Locator_ID,mTab.getTrxInfo(), mTab.getRecord_ID());
			if(storageOnHand!= null){
				mTab.setValue("M_AttributeSetInstance_ID", storageOnHand.getM_AttributeSetInstance_ID());
			}
		}
		
		return "";
	}
	
	//getting the immediate expiery date based Attribute set instance
	public static MStorageOnHand getImmediateASI(Properties ctx,int M_Product_ID , int M_Locator_ID , String trx , int C_OrderLine_ID){
		
		MStorageOnHand storageOnHand = null;
		BigDecimal reserved = null;
		
		String sql = "SELECT M_StorageOnHand.* "
			+ "FROM M_StorageOnHand "
			+ "INNER JOIN M_AttributeSetInstance "
			+ "ON M_StorageOnHand.M_AttributeSetInstance_ID = M_AttributeSetInstance.M_AttributeSetInstance_ID "
			+ "WHERE "
			+ "M_StorageOnHand.M_Product_ID = ? "
			+ "AND M_StorageOnHand.M_Locator_ID = ? "
			+ "AND M_StorageOnHand.M_AttributeSetInstance_ID > 0 "
			+ "AND QtyOnHand <> 0 "
			+ "ORDER BY M_AttributeSetInstance.guaranteedate ASC ";
			
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try
		{
			pstmt = DB.prepareStatement (sql, trx);
			pstmt.setInt (1, M_Product_ID);
			pstmt.setInt (2, M_Locator_ID);
			rs = pstmt.executeQuery ();
			while(rs.next ()){
				storageOnHand = new MStorageOnHand (ctx, rs, trx);
				reserved = MOrderLine.getNotReserved(ctx, M_Locator_ID, M_Product_ID, storageOnHand.getM_AttributeSetInstance_ID(),C_OrderLine_ID);
				
				//No reserved quantity
				if(reserved == null)
					return storageOnHand;
				
				//reserved quantity is equal to the on hand quantity
				if(storageOnHand.getQtyOnHand().doubleValue() == reserved.doubleValue())
					continue;
				
				//reserved quantity is less than to the on hand quantity
				if(storageOnHand.getQtyOnHand().doubleValue() > reserved.doubleValue())
					return storageOnHand;
				
				//reserved quantity is greater than to the on hand quantity
				if(storageOnHand.getQtyOnHand().doubleValue() < reserved.doubleValue())
					continue;
			}
		}
		catch (SQLException ex)
		{
			s_log.log(Level.SEVERE, sql, ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		
		return storageOnHand;
	}
}
