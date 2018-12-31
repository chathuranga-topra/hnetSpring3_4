package org.topra.collouts;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;

public class BpName implements IColumnCallout{

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		
		if(value == null) return "";
		//CHANGE CASE UPPER IN NAME AND VALIDATE FOR NAME HAS TWO WORDS
		mTab.setValue("name", value.toString().toUpperCase());
		return null;
	}
}
