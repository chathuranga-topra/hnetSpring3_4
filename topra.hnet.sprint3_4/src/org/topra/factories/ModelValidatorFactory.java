package org.topra.factories;

import org.adempiere.base.IModelValidatorFactory;
import org.compiere.model.ModelValidator;
import org.topra.modelvalidator.TPModelValidator;

public class ModelValidatorFactory implements IModelValidatorFactory{

	@Override
	public ModelValidator newModelValidatorInstance(String className) {
		
		if(className.equalsIgnoreCase("org.topra.modelvalidator.TPModelValidator"))
			return new TPModelValidator();
		
		return null;
	}

}
