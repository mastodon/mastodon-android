package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.util.Map;

@Parcel
public class InstanceV1 extends Instance{
	/**
	 * The domain name of the instance.
	 */
	@RequiredField
	public String uri;
	/**
	 * A shorter description defined by the admin.
	 */
//	@RequiredField
	public String shortDescription;
	/**
	 * An email that may be contacted for any inquiries.
	 */
	@RequiredField
	public String email;
	/**
	 * Whether registrations are enabled.
	 */
	public boolean registrations;
	/**
	 * Whether registrations require moderator approval.
	 */
	public boolean approvalRequired;
	/**
	 * Whether invites are enabled.
	 */
	public boolean invitesEnabled;
	/**
	 * URLs of interest for clients apps.
	 */
	public Map<String, String> urls;
	/**
	 * A user that can be contacted, as an alternative to email.
	 */
	public Account contactAccount;
	public Stats stats;
	/**
	 * Banner image for the website.
	 */
	public String thumbnail;

	@Override
	public String getDomain(){
		return uri;
	}

	@Override
	public Account getContactAccount(){
		return contactAccount;
	}

	@Override
	public String getContactEmail(){
		return email;
	}

	@Override
	public boolean areInvitesEnabled(){
		return invitesEnabled;
	}

	@Override
	public boolean areRegistrationsOpen(){
		return registrations;
	}

	@Override
	public boolean isSignupReasonRequired(){
		return approvalRequired;
	}

	@Override
	public String getThumbnailURL(){
		return thumbnail;
	}

	@Override
	public int getVersion(){
		return 1;
	}

	@Override
	public long getApiVersion(String name){
		return 0;
	}

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(shortDescription==null)
			shortDescription="";
		if(contactAccount!=null)
			contactAccount.postprocess();
	}

	@Parcel
	public static class Stats{
		public int userCount;
		public int statusCount;
		public int domainCount;
	}
}
