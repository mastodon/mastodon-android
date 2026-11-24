package org.joinmastodon.android.api;

import android.net.Uri;

public record PaginationHeaders(Uri next, Uri prev){
}
