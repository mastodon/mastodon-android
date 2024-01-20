package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class AnnualReport extends BaseModel{
	public Archetype archetype;
	public Map<String, Double> percentiles;
	public List<TimeSeriesPoint> timeSeries;
	public List<NameAndCount> topHashtags;
	public TopStatuses topStatuses;
	public List<NameAndCount> mostUsedApps;
	public TypeDistribution typeDistribution;
	public List<AccountAndCount> mostRebloggedAccounts;
	public List<AccountAndCount> commonlyInteractedWithAccounts;

	public enum Archetype{
		@SerializedName("lurker")
		LURKER,
		@SerializedName("booster")
		BOOSTER,
		@SerializedName("replier")
		REPLIER,
		@SerializedName("pollster")
		POLLSTER,
		@SerializedName("oracle")
		ORACLE
	}

	public static class TimeSeriesPoint{
		public int month;
		public int statuses;
		public int followers;
		public int following;
	}

	public static class NameAndCount{
		public String name;
		public int count;
	}

	public static class TopStatuses{
		public String byReblogs;
		public String byReplies;
		public String byFavourites;
	}

	public static class TypeDistribution{
		public int total;
		public int reblogs;
		public int replies;
		public int standalone;
	}

	public static class AccountAndCount{
		public String accountId;
		public int count;
	}
}
