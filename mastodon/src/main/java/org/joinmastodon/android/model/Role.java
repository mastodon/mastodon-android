package org.joinmastodon.android.model;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

@Parcel
public class Role extends BaseModel{
	/**
	 * Users with this permission bypass all permissions.
	 */
	public static final int PERMISSION_ADMINISTRATOR=0x1;

	/**
	 * Allows users to access Sidekiq and PgHero dashboards.
	 */
	public static final int PERMISSION_DEVOPS=0x2;

	/**
	 * Allows users to see history of admin actions.
	 */
	public static final int PERMISSION_VIEW_AUDIT_LOG=0x4;

	/**
	 * Allows users to access the dashboard and various metrics.
	 */
	public static final int PERMISSION_VIEW_DASHBOARD=0x8;

	/**
	 * Allows users to review reports and perform moderation actions against them.
	 */
	public static final int PERMISSION_MANAGE_REPORTS=0x10;

	/**
	 * Allows users to block or allow federation with other domains, and control deliverability.
	 */
	public static final int PERMISSION_MANAGE_FEDERATION=0x20;

	/**
	 * Allows users to change site settings.
	 */
	public static final int PERMISSION_MANAGE_SETTINGS=0x40;

	/**
	 * Allows users to block e-mail providers and IP addresses.
	 */
	public static final int PERMISSION_MANAGE_BLOCKS=0x80;

	/**
	 * Allows users to review trending content and update hashtag settings.
	 */
	public static final int PERMISSION_MANAGE_TAXONOMIES=0x100;

	/**
	 * Allows users to review appeals against moderation actions.
	 */
	public static final int PERMISSION_MANAGE_APPEALS=0x200;

	/**
	 * Allows users to view other users’ details and perform moderation actions against them.
	 */
	public static final int PERMISSION_MANAGE_USERS=0x400;

	/**
	 * Allows users to browse and deactivate invite links.
	 */
	public static final int PERMISSION_MANAGE_INVITES=0x800;

	/**
	 * Allows users to change server rules.
	 */
	public static final int PERMISSION_MANAGE_RULES=0x1000;

	/**
	 * Allows users to manage announcements on the server.
	 */
	public static final int PERMISSION_MANAGE_ANNOUNCEMENTS=0x2000;

	/**
	 * Allows users to manage custom emojis on the server.
	 */
	public static final int PERMISSION_MANAGE_CUSTOM_EMOJIS=0x4000;

	/**
	 * Allows users to set up webhooks for administrative events.
	 */
	public static final int PERMISSION_MANAGE_WEBHOOKS=0x8000;

	/**
	 * Allows users to invite new people to the server.
	 */
	public static final int PERMISSION_INVITE_USERS=0x10000;

	/**
	 * Allows users to manage and assign roles below theirs.
	 */
	public static final int PERMISSION_MANAGE_ROLES=0x20000;

	/**
	 * Allows users to disable other users’ two-factor authentication, change their e-mail address, and reset their password.
	 */
	public static final int PERMISSION_MANAGE_USER_ACCESS=0x40000;

	/**
	 * Allows users to delete other users’ data without delay.
	 */
	public static final int PERMISSION_DELETE_USER_DATA=0x80000;

	/**
	 * Allows users to view publice “firehose”, hashtag and link feeds even when those are disabled.
	 */
	public static final int PERMISSION_VIEW_LIVE_AND_TOPIC_FEEDS=0x100000;

	@RequiredField
	public String id;
	public String name;
	public String color;
	public int permissions;
	public boolean highlighted;
}
