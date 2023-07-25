package org.joinmastodon.android.model;

import android.text.Html;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.catalog.CatalogInstance;
import org.parceler.Parcel;

import java.net.IDN;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Parcel
public class Instance extends BaseModel{
	/**
	 * The domain name of the instance.
	 */
	@RequiredField
	public String uri;
	/**
	 * The title of the website.
	 */
	@RequiredField
	public String title;
	/**
	 * Admin-defined description of the Mastodon site.
	 */
	@RequiredField
	public String description;
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
	 * The version of Mastodon installed on the instance.
	 */
	@RequiredField
	public String version;
	/**
	 * Primary languages of the website and its staff.
	 */
//	@RequiredField
	public List<String> languages;
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
	 * Banner image for the website.
	 */
	public String thumbnail;
	/**
	 * A user that can be contacted, as an alternative to email.
	 */
	public Account contactAccount;
	public Stats stats;

	public List<Rule> rules;
	public Configuration configuration;

	// non-standard field in some Mastodon forks
	public int maxTootChars;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(contactAccount!=null)
			contactAccount.postprocess();
		if(rules==null)
			rules=Collections.emptyList();
		if(shortDescription==null)
			shortDescription="";
	}

	@Override
	public String toString(){
		return "Instance{"+
				"uri='"+uri+'\''+
				", title='"+title+'\''+
				", description='"+description+'\''+
				", shortDescription='"+shortDescription+'\''+
				", email='"+email+'\''+
				", version='"+version+'\''+
				", languages="+languages+
				", registrations="+registrations+
				", approvalRequired="+approvalRequired+
				", invitesEnabled="+invitesEnabled+
				", urls="+urls+
				", thumbnail='"+thumbnail+'\''+
				", contactAccount="+contactAccount+
				'}';
	}

	public CatalogInstance toCatalogInstance(){
		CatalogInstance ci=new CatalogInstance();
		ci.domain=uri;
		ci.normalizedDomain=IDN.toUnicode(uri);
		ci.description=Html.fromHtml(shortDescription).toString().trim();
		if(languages!=null&&languages.size()>0){
			ci.language=languages.get(0);
			ci.languages=languages;
		}else{
			ci.languages=Collections.emptyList();
			ci.language="unknown";
		}
		ci.proxiedThumbnail=thumbnail;
		if(stats!=null)
			ci.totalUsers=stats.userCount;
		return ci;
	}

	@Parcel
	public static class Rule{
		public String id;
		public String text;

		public transient CharSequence parsedText;
	}

	@Parcel
	public static class Stats{
		public int userCount;
		public int statusCount;
		public int domainCount;
	}

	@Parcel
	public static class Configuration{
		public StatusesConfiguration statuses;
		public MediaAttachmentsConfiguration mediaAttachments;
		public PollsConfiguration polls;
	}

	@Parcel
	public static class StatusesConfiguration{
		public int maxCharacters;
		public int maxMediaAttachments;
		public int charactersReservedPerUrl;
	}

	@Parcel
	public static class MediaAttachmentsConfiguration{
		public List<String> supportedMimeTypes;
		public int imageSizeLimit;
		public int imageMatrixLimit;
		public int videoSizeLimit;
		public int videoFrameRateLimit;
		public int videoMatrixLimit;
	}

	@Parcel
	public static class PollsConfiguration{
		public int maxOptions;
		public int maxCharactersPerOption;
		public int minExpiration;
		public int maxExpiration;
	}
}
