package org.joinmastodon.android.ui.photoviewer;

import org.joinmastodon.android.model.Status;

public interface PhotoViewerHost{
	void openPhotoViewer(Status status, int attachmentIndex);
}
