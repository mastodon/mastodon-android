package org.joinmastodon.android.model;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.OnUnwrap;
import org.parceler.ParcelerRuntimeException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class BaseModel implements Cloneable{
	@CallSuper
	public void postprocess() throws ObjectValidationException{
		try{
			boolean allRequired=getClass().isAnnotationPresent(AllFieldsAreRequired.class);
			for(Field fld:getClass().getFields()){
				if(!fld.getType().isPrimitive() && !Modifier.isTransient(fld.getModifiers()) && (allRequired || fld.isAnnotationPresent(RequiredField.class))){
					if(fld.get(this)==null){
						throw new ObjectValidationException("Required field '"+fld.getName()+"' of type "+fld.getType().getSimpleName()+" was null in "+getClass().getSimpleName());
					}
				}
			}
		}catch(IllegalAccessException ignore){}
	}

	// Makes sure to call postprocess() for BaseModel objects deserialized via Parceler
	@OnUnwrap
	protected void parcelerPostprocess() {
		try {
			postprocess();
		} catch (ObjectValidationException e) {
			throw new ParcelerRuntimeException("Could not parse Parcel", e);
		}
	}

	@NonNull
	@Override
	public Object clone(){
		try{
			return super.clone();
		}catch(CloneNotSupportedException x){
			throw new RuntimeException(x);
		}
	}
}
