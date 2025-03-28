package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.catalog.CatalogInstance;
import org.parceler.Parcel;

import java.net.IDN;
import java.util.Collections;
import java.util.List;

public abstract class Instance extends BaseModel{
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
	 * The version of Mastodon installed on the instance.
	 */
	@RequiredField
	public String version;
	/**
	 * Primary languages of the website and its staff.
	 */
//	@RequiredField
	public List<String> languages;


	public List<Rule> rules;
	public Configuration configuration;

	// non-standard field in some Mastodon forks
	public int maxTootChars;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(rules==null)
			rules=Collections.emptyList();
	}

	public CatalogInstance toCatalogInstance(){
		CatalogInstance ci=new CatalogInstance();
		ci.domain=getDomain();
		ci.normalizedDomain=IDN.toUnicode(getDomain());
		ci.description=description.trim();
		if(languages!=null && !languages.isEmpty()){
			ci.language=languages.get(0);
			ci.languages=languages;
		}else{
			ci.languages=List.of();
			ci.language="unknown";
		}
		ci.proxiedThumbnail=getThumbnailURL();
//		if(stats!=null)
//			ci.totalUsers=stats.userCount;
		return ci;
	}

	public abstract String getDomain();
	public abstract Account getContactAccount();
	public abstract String getContactEmail();
	public abstract boolean areRegistrationsOpen();
	public abstract boolean isSignupReasonRequired();
	public abstract boolean areInvitesEnabled();
	public abstract String getThumbnailURL();
	public abstract int getVersion();
	public abstract long getApiVersion(String name);

	public long getApiVersion(){
		return getApiVersion("mastodon");
	}

	@Parcel
	public static class Rule{
		public String id;
		public String text;

		public transient CharSequence parsedText;
	}

	@Parcel
	public static class Configuration{
		public StatusesConfiguration statuses;
		public MediaAttachmentsConfiguration mediaAttachments;
		public PollsConfiguration polls;
		public URLsConfiguration urls;
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

	@Parcel
	public static class URLsConfiguration{
		public String streaming;
		public String status;
		public String about;
		public String privacyPolicy;
		public String termsOfService;
	}
}
