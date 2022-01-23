package org.joinmastodon.android.ui.photoviewer;

import org.joinmastodon.android.model.Status;

public interface PhotoViewerHost{
	void openPhotoViewer(String parentID, Status status, int attachmentIndex);
}
