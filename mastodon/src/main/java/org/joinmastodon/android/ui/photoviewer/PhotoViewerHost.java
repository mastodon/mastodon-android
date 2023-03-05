package org.joinmastodon.android.ui.photoviewer;

import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.MediaGridStatusDisplayItem;

public interface PhotoViewerHost{
	void openPhotoViewer(String parentID, Status status, int attachmentIndex, MediaGridStatusDisplayItem.Holder gridHolder);
}
