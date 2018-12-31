package org.topra.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MSysConfig;
import org.compiere.model.MWarehouse;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.process.DocAction;
import org.compiere.util.Util;

public class TpMOrder extends MOrder{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7928577218828437533L;
	
	public TpMOrder(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public TpMOrder(Properties ctx, int C_Order_ID, String trxName) {
		super(ctx, C_Order_ID, trxName);
	}
	
	/**************************************************************************
	 * 	Complete Document
	 * 	@return new status (Complete, In Progress, Invalid, Waiting ..)
	 */
	//override due to the separate order date and invoice date on POS order
	@Override
	public String completeIt()
	{	
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		String DocSubTypeSO = dt.getDocSubTypeSO();
		
		//	Just prepare
		if (DOCACTION_Prepare.equals(getDocAction()))
		{
			setProcessed(false);
			return DocAction.STATUS_InProgress;
		}

		// Set the definite document number after completed (if needed)
		setDefiniteDocumentNo();

		//	Offers
		if (MDocType.DOCSUBTYPESO_Proposal.equals(DocSubTypeSO)
			|| MDocType.DOCSUBTYPESO_Quotation.equals(DocSubTypeSO)) 
		{
			//	Binding
			if (MDocType.DOCSUBTYPESO_Quotation.equals(DocSubTypeSO))
				reserveStock(dt, getLines(true, MOrderLine.COLUMNNAME_M_Product_ID));
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
			if (m_processMsg != null)
				return DocAction.STATUS_Invalid;
			m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
			if (m_processMsg != null)
				return DocAction.STATUS_Invalid;
			setProcessed(true);
			return DocAction.STATUS_Completed;
		}
		//	Waiting Payment - until we have a payment
		if (!m_forceCreation 
			&& MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO) 
			&& getC_Payment_ID() == 0 && getC_CashLine_ID() == 0)
		{
			setProcessed(true);
			return DocAction.STATUS_WaitingPayment;
		}
		
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			m_justPrepared = false;
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		getLines(true,null);
		if (log.isLoggable(Level.INFO)) log.info(toString());
		StringBuilder info = new StringBuilder();
		
		boolean realTimePOS = MSysConfig.getBooleanValue(MSysConfig.REAL_TIME_POS, false , getAD_Client_ID());
		
		//	Create SO Shipment - Force Shipment
		MInOut shipment = null;
		if (MDocType.DOCSUBTYPESO_OnCreditOrder.equals(DocSubTypeSO)		//	(W)illCall(I)nvoice
			|| MDocType.DOCSUBTYPESO_WarehouseOrder.equals(DocSubTypeSO)	//	(W)illCall(P)ickup	
			|| MDocType.DOCSUBTYPESO_POSOrder.equals(DocSubTypeSO)			//	(W)alkIn(R)eceipt
			|| MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO)) 
		{
			if (!DELIVERYRULE_Force.equals(getDeliveryRule()))
			{
				MWarehouse wh = new MWarehouse (getCtx(), getM_Warehouse_ID(), get_TrxName());
				if (!wh.isDisallowNegativeInv())
					setDeliveryRule(DELIVERYRULE_Force);
			}
			//
			shipment = createShipment (dt, realTimePOS ? null : new Timestamp(System.currentTimeMillis()));
			if (shipment == null)
				return DocAction.STATUS_Invalid;
			info.append("@M_InOut_ID@: ").append(shipment.getDocumentNo());
			String msg = shipment.getProcessMsg();
			if (msg != null && msg.length() > 0)
				info.append(" (").append(msg).append(")");
		}	//	Shipment
		

		//	Create SO Invoice - Always invoice complete Order
		if ( MDocType.DOCSUBTYPESO_POSOrder.equals(DocSubTypeSO)
			|| MDocType.DOCSUBTYPESO_OnCreditOrder.equals(DocSubTypeSO) 	
			|| MDocType.DOCSUBTYPESO_PrepayOrder.equals(DocSubTypeSO)) 
		{
			MInvoice invoice = createInvoice (dt, shipment, realTimePOS ? null : new Timestamp(System.currentTimeMillis()));
			if (invoice == null)
				return DocAction.STATUS_Invalid;
			info.append(" - @C_Invoice_ID@: ").append(invoice.getDocumentNo());
			String msg = invoice.getProcessMsg();
			if (msg != null && msg.length() > 0)
				info.append(" (").append(msg).append(")");
		}	//	Invoice
		
		String msg = createPOSPayments();
		if (msg != null) {
			m_processMsg = msg;
			return DocAction.STATUS_Invalid;
		}

		//	Counter Documents
		MOrder counter = createCounterDoc();
		if (counter != null)
			info.append(" - @CounterDoc@: @Order@=").append(counter.getDocumentNo());
		//	User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			if (info.length() > 0)
				info.append(" - ");
			info.append(valid);
			m_processMsg = info.toString();
			return DocAction.STATUS_Invalid;
		}

		//landed cost
		if (!isSOTrx())
		{
			String error = landedCostAllocation();
			if (!Util.isEmpty(error))
			{
				m_processMsg = error;
				return DocAction.STATUS_Invalid;
			}
		}

		setProcessed(true);	
		m_processMsg = info.toString();
		//
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	
}
