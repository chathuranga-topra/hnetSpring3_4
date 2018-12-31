package org.topra.factories;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.IColumnCalloutFactory;
import org.compiere.model.MBPartner;
import org.compiere.model.MOrderLine;
import org.topra.collouts.CalloutOrder;
import org.topra.collouts.BpName;
import org.topra.collouts.OrderLineExpiryDate;

public class ColloutFactory implements IColumnCalloutFactory{

	@Override
	public IColumnCallout[] getColumnCallouts(String tableName, String columnName) {
		
		List<IColumnCallout> list = new ArrayList<IColumnCallout>();
		//Sales Order Lines :: M_Product_ID
		if(tableName.equalsIgnoreCase(MOrderLine.Table_Name) 
				&& columnName.equalsIgnoreCase(MOrderLine.COLUMNNAME_M_Product_ID)){
			list.add(new OrderLineExpiryDate());
		}else if(tableName.equalsIgnoreCase(MOrderLine.Table_Name) 
				&& columnName.equalsIgnoreCase(MOrderLine.COLUMNNAME_QtyEntered)){
			list.add(new OrderLineExpiryDate());
		}
		if(tableName.equalsIgnoreCase(MBPartner.Table_Name) && columnName.equalsIgnoreCase(MBPartner.COLUMNNAME_Name)){
			list.add(new BpName());
		}
		if(tableName.equalsIgnoreCase(MOrderLine.Table_Name) && columnName.equalsIgnoreCase(MOrderLine.COLUMNNAME_M_Product_ID)){
			list.add(new CalloutOrder());
		}
		
		return list != null ? list.toArray(new IColumnCallout[0]) : new IColumnCallout[0];
	}

}
