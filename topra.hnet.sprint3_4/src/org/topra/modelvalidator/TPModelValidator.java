package org.topra.modelvalidator;

import java.math.BigDecimal;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.topra.collouts.OrderLineExpiryDate;
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
		engine.addModelChange(MOrderLine.Table_Name, this);
		engine.addModelChange(MMovementLine.Table_Name, this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public String modelChange(PO po, int type) throws Exception {
		
		//Sales order Expiry control	
		if(po.get_TableName().equalsIgnoreCase(MOrderLine.Table_Name) 
				&& (type == CHANGETYPE_NEW || type == CHANGETYPE_CHANGE)
		){
			
			MOrderLine ol = (MOrderLine) po;
			int M_Locator_ID = ol.getC_Order().getM_Warehouse_ID();
			
			//VALIDATE FOR DOCUMWNT TYPE
 			MDocType dt = (MDocType) ol.getC_Order().getC_DocTypeTarget();
			if(!dt.get_ValueAsBoolean("IsExpiryControl"))
				return"";
			//VALIDATE FOR DOCUMENT STATUS USING WORKFLOWS
			if(this.getWorkFlowCount(po , ol.getC_Order_ID()) != 0)
				return"";
			//VALIDATE ITEM IS STOCKED
			if(!ol.getM_Product().isStocked())
				return "";
			//VALIDATE FOR ALREADY PICKED
			BigDecimal notReserved = new BigDecimal(0);
			BigDecimal freeQty = new BigDecimal(0);
			MStorageOnHand storageOnHand = null;
			if(ol.getM_AttributeSetInstance_ID() == 0){ //NO LOT-BATCH PICKED
				storageOnHand = OrderLineExpiryDate.getImmediateASI(po.getCtx(), ol.getM_Product_ID(), M_Locator_ID, ol.get_TrxName(), po.get_ID());
				if(storageOnHand== null)
					throw new AdempiereException("No more lot-batch available for this item!");
				
			}else{//VALIDATE THE ALREADY PICKED ATTRIBUTE SET INSTANCE 
				
				storageOnHand = MStorageOnHand.get(po.getCtx(),  M_Locator_ID,  ol.getM_Product_ID(), ol.getM_AttributeSetInstance_ID(), ol.get_TrxName());
			}
			
			ol.setM_AttributeSetInstance_ID(storageOnHand.getM_AttributeSetInstance_ID());
			notReserved = MOrderLine.getNotReserved(po.getCtx(), 
					ol.getM_Warehouse_ID(), 
					ol.getM_Product_ID(), 
					storageOnHand.getM_AttributeSetInstance_ID(), 
					ol.get_ID());
				
			notReserved = notReserved == null?new BigDecimal(0) :notReserved;
			
			freeQty = storageOnHand.getQtyOnHand().subtract(notReserved);
			
			//validate the entered quantity with onhand + notreserved 
			if(ol.getQtyEntered().doubleValue() > freeQty.doubleValue()){
				
				//validate zero free qty
				if(freeQty.doubleValue() <= 0.0){
					throw new AdempiereException("No more stock available for this item to place new order!");
				}else{
					ol.setQty(freeQty);
					ol.setPrice();
					ol.setLineNetAmt();
				}
			}
			
		}
		//Inventory movement expiary validation
		else if(po.get_TableName().equalsIgnoreCase(MMovementLine.Table_Name) 
						&& (type == CHANGETYPE_NEW || type == CHANGETYPE_CHANGE)){
			
			MMovementLine ml = (MMovementLine) po;
			MMovement movement = (MMovement) ml.getM_Movement();
			//VALIDATE FOR DOCUMWNT TYPE
 			MDocType dt = (MDocType) movement.getC_DocType();
			//DOCUMANR LEVEL C0NTROLL
			if(!dt.get_ValueAsBoolean("IsExpiryControl"))
				return"";
			//VALIDATE FOR DOCUMENT STATUS USING WORKFLOWS
			if(this.getWorkFlowCount(po , movement.get_ID()) != 0)
				return"";
			//VALIDATE ITEM IS STOCKED
			if(!ml.getM_Product().isStocked())
				return "";
			MStorageOnHand storageOnHand = null;
			BigDecimal notReserved = new BigDecimal(0);
			BigDecimal freeQty = new BigDecimal(0);
			int M_Locator_ID = ml.getM_Locator_ID();
			
			if(ml.getM_AttributeSetInstance_ID() == 0){ //NO LOT-BATCH PICKED
				storageOnHand = OrderLineExpiryDate.getImmediateASI(po.getCtx(), ml.getM_Product_ID(), M_Locator_ID, ml.get_TrxName(), po.get_ID());
				if(storageOnHand== null)
					throw new AdempiereException("No more lot-batch available for this item!");
				
			}else{//VALIDATE THE ALREADY PICKED ATTRIBUTE SET INSTANCE 
				storageOnHand = MStorageOnHand.get(po.getCtx(),  M_Locator_ID,  ml.getM_Product_ID(), ml.getM_AttributeSetInstance_ID(), ml.get_TrxName());
			}
			
			//EXPIERY LOGIC GOES HERE
			ml.setM_AttributeSetInstance_ID(storageOnHand.getM_AttributeSetInstance_ID());
			notReserved = MOrderLine.getNotReserved(po.getCtx(), M_Locator_ID, ml.getM_Product_ID(), storageOnHand.getM_AttributeSetInstance_ID(), ml.get_ID());
				
			notReserved = notReserved == null ? new BigDecimal(0) : notReserved;
			freeQty = storageOnHand.getQtyOnHand().subtract(notReserved);
			
			//validate the entered quantity with onhand + notreserved 
			if(ml.getMovementQty().doubleValue() > freeQty.doubleValue()){
				
				//validate zero free qty
				if(freeQty.doubleValue() <= 0.0)
					throw new AdempiereException("No more stock available for this item to place new movement!");
				else
					ml.setMovementQty(freeQty);
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
	
	
	private int getWorkFlowCount(PO po , int Record_ID){
		
		int AD_Workflow_ID = 0;
		
		if(po.get_TableName().equalsIgnoreCase("C_OrderLine"))
			AD_Workflow_ID = 116;
		else if(po.get_TableName().equalsIgnoreCase("M_MovementLine"))
			AD_Workflow_ID = 128;
		
		return new Query(po.getCtx(), "AD_WF_Process", " AD_Workflow_ID=? AND Record_ID" +"=?", po.get_TrxName())
		.setParameters(AD_Workflow_ID,Record_ID)
		.list().size();
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
}
