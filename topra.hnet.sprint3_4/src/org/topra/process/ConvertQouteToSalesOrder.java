package org.topra.process;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MSequence;
import org.compiere.process.SvrProcess;

//org.topra.process.ConvertQouteToSalesOrder
public class ConvertQouteToSalesOrder extends SvrProcess{

	private int Record_ID;
	
	@Override
	protected void prepare() {
		this.Record_ID = this.getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		
		MOrder quotation = new MOrder(getCtx(), Record_ID, get_TrxName());
		//VALIDATE THE QUATATION
		if(!quotation.get_ValueAsString("OrderStatus").equalsIgnoreCase("R"))
			throw new AdempiereException("Quotation status is not completed!");
		if(quotation.getDocStatus().equalsIgnoreCase("VO") 
				|| quotation.getDocStatus().equalsIgnoreCase("RE"))
			throw new AdempiereException("Quotation status is not completed!");
		
		MOrder order = MOrder.copyFrom(quotation, new Timestamp(System.currentTimeMillis()),
				1000032, true, false, false,get_TrxName());
		
		for(MOrderLine ol : order.getLines(false, "")){
			ol.setQty(new BigDecimal(0));
			ol.setPrice();
			ol.setLineNetAmt();
			ol.save();
		}
		order.set_CustomColumn("OrderStatus", "P");
		order.setC_DocTypeTarget_ID(1000033);
		order.setPOReference(quotation.getDocumentNo());
		//SET DOCUMENT NO
		MDocType dt = new MDocType(getCtx(), order.getC_DocTypeTarget_ID(), get_TrxName());
		MSequence sequence = (MSequence) dt.getDocNoSequence();
		int curentNext = sequence.getCurrentNext();
		
		order.setDocumentNo((sequence.getPrefix() == null? "": sequence.getPrefix()) + curentNext);
		sequence.setCurrentNext(curentNext + 1);
		sequence.save();
		
		order.save();
		
		return "Order Document No - " + order.getDocumentNo();
	}
}
