package org.joinmastodon.android.model;

/**
 * A model object from which {@link org.joinmastodon.android.ui.displayitems.StatusDisplayItem}s can be generated.
 */
public interface DisplayItemsParent{
	String getID();

	default String getAccountID(){
		return null;
	}
}
