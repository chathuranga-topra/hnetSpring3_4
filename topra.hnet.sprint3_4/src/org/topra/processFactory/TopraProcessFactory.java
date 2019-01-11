package org.topra.processFactory;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;
import org.topra.process.ConvertQouteToSalesOrder;

public class TopraProcessFactory implements IProcessFactory{

	@Override
	public ProcessCall newProcessInstance(String className) {
		
		if(className.equalsIgnoreCase("org.topra.process.ConvertQouteToSalesOrder"))
			return new ConvertQouteToSalesOrder();
			
		return null;
	}

}
