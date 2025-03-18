package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.util.Map;

@Parcel
public class InstanceV2 extends Instance{
	@RequiredField
	public String domain;
	public Thumbnail thumbnail;
	@RequiredField
	public Registrations registrations;
	public Contact contact;
	public Map<String, Long> apiVersions;

	@Override
	public String getDomain(){
		return domain;
	}

	@Override
	public Account getContactAccount(){
		return contact!=null ? contact.account : null;
	}

	@Override
	public String getContactEmail(){
		return contact!=null ? contact.email : null;
	}

	@Override
	public boolean areRegistrationsOpen(){
		return registrations.enabled;
	}

	@Override
	public boolean isApprovalRequired(){
		return registrations.approvalRequired;
	}

	@Override
	public boolean areInvitesEnabled(){
		return true; // TODO are they though?
	}

	@Override
	public String getThumbnailURL(){
		return thumbnail!=null ? thumbnail.url : null;
	}

	@Override
	public int getVersion(){
		return 2;
	}

	@Override
	public long getApiVersion(String name){
		if(apiVersions==null)
			return 0;
		Long v=apiVersions.get(name);
		return v==null ? 0 : v;
	}

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(contact!=null && contact.account!=null)
			contact.account.postprocess();
	}

	@Parcel
	public static class Thumbnail{
		public String url;
		public String blurhash;
	}

	@Parcel
	public static class Registrations{
		public boolean enabled;
		public boolean approvalRequired;
		public String message;
		public String url;
		public int minAge;
	}

	@Parcel
	public static class Contact{
		public String email;
		public Account account;
	}
}
