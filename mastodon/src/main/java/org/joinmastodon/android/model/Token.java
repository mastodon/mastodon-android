package org.joinmastodon.android.model;

import org.joinmastodon.android.api.AllFieldsAreRequired;

/**
 * Represents an OAuth token used for authenticating with the API and performing actions.
 */
@AllFieldsAreRequired
public class Token extends BaseModel{
	/**
	 * An OAuth token to be used for authorization.
	 */
	public String accessToken;
	/**
	 * The OAuth token type. Mastodon uses Bearer tokens.
	 */
	public String tokenType;
	/**
	 * The OAuth scopes granted by this token, space-separated.
	 */
	public String scope;
	/**
	 * When the token was generated.
	 * (unixtime)
	 */
	public long createdAt;
}
