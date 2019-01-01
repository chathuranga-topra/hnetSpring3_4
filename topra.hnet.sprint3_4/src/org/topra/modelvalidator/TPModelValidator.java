package org.topra.modelvalidator;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.PO;
import org.compiere.model.ModelValidator;

//org.topra.modelvalidator.TPModelValidator
public class TPModelValidator implements ModelValidator{

	private int ad_client_id;
	
	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		
		this.ad_client_id = client.get_ID();
		//Doc validators
		engine.addDocValidate(MInOut.Table_Name, this);
		//model validators
		//engine.addModelChange(MInOutLine.Table_Name, this);
		engine.addModelChange(MOrderLine.Table_Name, this);
	}

	@Override
	public int getAD_Client_ID() {
		// TODO Auto-generated method stub
		return ad_client_id;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		
		//Shipment Expiry control
		/*if(po.get_TableName().equalsIgnoreCase(MInOutLine.Table_Name) && (type == CHANGETYPE_NEW || type == CHANGETYPE_CHANGE)){
			MInOutLine inOutLine = (MInOutLine) po;
			MDocType dc = (MDocType) inOutLine.getM_InOut().getC_DocType();
			
			if(dc.get_ValueAsBoolean("IsExpiryControl")){
				MAttributeSetInstance instance = (MAttributeSetInstance) inOutLine.getM_AttributeSetInstance();
				
				if(instance == null || instance.getGuaranteeDate() == null)
					throw new AdempiereException("MISSING EXPIRY DATE - Please fill expiry date!");
			}
			
		}	
		*/	
		//Sales order Expiry control	
		if(po.get_TableName().equalsIgnoreCase(MOrderLine.Table_Name) 
				&& (type == CHANGETYPE_NEW || type == CHANGETYPE_CHANGE)
		){
			MOrderLine ol = (MOrderLine) po;
			//VALIDATE FOR DOCUMWNT TYPE
			MDocType dt = (MDocType) ol.getC_Order().getC_DocTypeTarget();
			
			if(!dt.get_ValueAsBoolean("IsExpiryControl"))
			{
				//document validate for drafted and in progress
				if(!ol.getC_Order().getDocAction().equals("CO") || !ol.getC_Order().getDocAction().equals("PR"))
					return"";
			}
			
			
			int M_Locator_ID = ol.getC_Order().getM_Warehouse_ID();
			MStorageOnHand[] onHands = MStorageOnHand.getAllWithASI(po.getCtx(), ol.getM_Product_ID(), M_Locator_ID, false, ol.get_TrxName());
			BigDecimal notReserved = new BigDecimal(0);
			BigDecimal freeQty = new BigDecimal(0);
			
			for(MStorageOnHand oh : onHands){
				
				notReserved = MOrderLine.getNotReserved(po.getCtx(), 
					ol.getM_Warehouse_ID(), 
					ol.getM_Product_ID(), 
					oh.getM_AttributeSetInstance_ID(), 
					ol.get_ID());
				
				notReserved = notReserved == null?new BigDecimal(0) :notReserved;
				
				freeQty = oh.getQtyOnHand().subtract(notReserved);
				
				if(freeQty.doubleValue() <=0){
					continue;
				}
				ol.setM_AttributeSetInstance_ID(oh.get_ID());
				//validate the entered quantity with onhand + notreserved 
				if(ol.getQtyEntered().doubleValue() > freeQty.doubleValue()){
					ol.setQty(freeQty);
				}
				
				if(ol.getM_AttributeSetInstance() != null)
					break;
			}
		}
		
		return null;
	}

	@Override
	public String docValidate(PO po, int timing) {
		
		//Before complete It should be validated for expire date is picked by the
		//unless picking the expire date in MInout will not be allowed to complete
		if(po.get_TableName().equalsIgnoreCase(MInOut.Table_Name) && timing == TIMING_BEFORE_COMPLETE){
			
			MInOut inOut = (MInOut) po;
			MDocType dc = (MDocType) inOut.getC_DocType();
			//Validation is based on the document type of IsExpiryControll column checked
			if(dc.get_ValueAsBoolean("IsExpiryControl")){
				MInOutLine [] lines = inOut.getLines(true);
				MAttributeSetInstance instance = null;
				
				for(MInOutLine line : lines){
					instance = (MAttributeSetInstance) line.getM_AttributeSetInstance();
					
					if(instance == null || instance.getGuaranteeDate() == null)
						throw new AdempiereException("MISSING EXPIRY DATE - Line no : " + line.getLine() + " Product : " + line.getM_Product().getName());
				}
			}
		}
		
		return null;
	}
}
