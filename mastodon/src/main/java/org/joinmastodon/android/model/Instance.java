package org.joinmastodon.android.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.catalog.CatalogInstance;

import java.net.IDN;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
	@RequiredField
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
	 * Primary langauges of the website and its staff.
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

	public int maxTootChars;
	public List<Rule> rules;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(contactAccount!=null)
			contactAccount.postprocess();
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
		if(languages!=null){
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



	public static class Rule implements Parcelable{
		public String id;
		public String text;


		@Override
		public int describeContents(){
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags){
			dest.writeString(this.id);
			dest.writeString(this.text);
		}

		public void readFromParcel(Parcel source){
			this.id=source.readString();
			this.text=source.readString();
		}

		public Rule(){
		}

		protected Rule(Parcel in){
			this.id=in.readString();
			this.text=in.readString();
		}

		public static final Parcelable.Creator<Rule> CREATOR=new Parcelable.Creator<Rule>(){
			@Override
			public Rule createFromParcel(Parcel source){
				return new Rule(source);
			}

			@Override
			public Rule[] newArray(int size){
				return new Rule[size];
			}
		};
	}

	public static class Stats implements Parcelable{
		public int userCount;
		public int statusCount;
		public int domainCount;


		@Override
		public int describeContents(){
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags){
			dest.writeInt(this.userCount);
			dest.writeInt(this.statusCount);
			dest.writeInt(this.domainCount);
		}

		public void readFromParcel(Parcel source){
			this.userCount=source.readInt();
			this.statusCount=source.readInt();
			this.domainCount=source.readInt();
		}

		public Stats(){
		}

		protected Stats(Parcel in){
			this.userCount=in.readInt();
			this.statusCount=in.readInt();
			this.domainCount=in.readInt();
		}

		public static final Parcelable.Creator<Stats> CREATOR=new Parcelable.Creator<Stats>(){
			@Override
			public Stats createFromParcel(Parcel source){
				return new Stats(source);
			}

			@Override
			public Stats[] newArray(int size){
				return new Stats[size];
			}
		};
	}
}
