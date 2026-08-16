package org.joinmastodon.android.fork;

/**
 * Single source of truth for everything that differs between this fork (masto.nyc) and
 * upstream mastodon/mastodon-android.
 *
 * <p>Rule of thumb for keeping upstream merges cheap: put the <i>value</i> here, and keep the
 * edit inside an upstream file down to a one-liner that reads from this class. See FORK.md
 * for the full inventory of upstream files this fork touches.
 */
public class ForkConfig{
	/** The only server this app talks to. Both signup and login are locked to it. */
	public static final String INSTANCE_DOMAIN="masto.nyc";

	/** Base URL of {@link #INSTANCE_DOMAIN}, without a trailing slash. */
	public static final String INSTANCE_URL="https://"+INSTANCE_DOMAIN;

	/** {@code owner/repo} the self-updater in githubRelease builds pulls releases from. */
	public static final String GITHUB_REPO="Five-Borough-Fedi-Project/masto.nyc-android";

	/**
	 * Product token for the {@code User-Agent} header, sent as {@code <product>/<versionName>}.
	 *
	 * <p>Upstream sends {@code MastodonAndroid}. This fork must be distinguishable from it, so that
	 * masto.nyc's logs, rate limiting and any per-client debugging can tell the two apart — and so
	 * that other instances aren't misled into attributing this client's traffic to the official app.
	 *
	 * <p>The {@code Android} suffix mirrors upstream's token deliberately: it keeps the platform
	 * visible in server logs, and anything already matching {@code *Android/*} keeps working. No
	 * device model or OS version is added — upstream doesn't send them, and doing so would hand
	 * every instance a fingerprinting signal for no benefit to the user.
	 */
	public static final String USER_AGENT_PRODUCT="MastoNYCAndroid";

	private ForkConfig(){}

	/**
	 * @return true if {@code domain} is {@link #INSTANCE_DOMAIN}, case-insensitively.
	 */
	public static boolean isOurInstance(String domain){
		return INSTANCE_DOMAIN.equalsIgnoreCase(domain);
	}
}
