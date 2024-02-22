package com.google.android.gms.common.moduleinstall;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ModuleInstallStatusUpdate extends AutoSafeParcelable{
	public static final int STATE_UNKNOWN = 0;
	/**
	 * The request is pending and will be processed soon.
	 */
	public static final int STATE_PENDING = 1;
	/**
	 * The optional module download is in progress.
	 */
	public static final int STATE_DOWNLOADING = 2;
	/**
	 * The optional module download has been canceled.
	 */
	public static final int STATE_CANCELED = 3;
	/**
	 * Installation is completed; the optional modules are available to the client app.
	 */
	public static final int STATE_COMPLETED = 4;
	/**
	 * The optional module download or installation has failed.
	 */
	public static final int STATE_FAILED = 5;
	/**
	 * The optional modules have been downloaded and the installation is in progress.
	 */
	public static final int STATE_INSTALLING = 6;
	/**
	 * The optional module download has been paused.
	 * <p>
	 * This usually happens when connectivity requirements can't be met during download. Once the connectivity requirements
	 * are met, the download will be resumed automatically.
	 */
	public static final int STATE_DOWNLOAD_PAUSED = 7;

	@SafeParceled(1)
	public int sessionID;
	@SafeParceled(2)
	public int installState;
	@SafeParceled(3)
	public Long bytesDownloaded;
	@SafeParceled(4)
	public Long totalBytesToDownload;
	@SafeParceled(5)
	public int errorCode;

	@Override
	public String toString(){
		return "ModuleInstallStatusUpdate{"+
				"sessionID="+sessionID+
				", installState="+installState+
				", bytesDownloaded="+bytesDownloaded+
				", totalBytesToDownload="+totalBytesToDownload+
				", errorCode="+errorCode+
				'}';
	}

	public static final Creator<ModuleInstallStatusUpdate> CREATOR=new AutoCreator<>(ModuleInstallStatusUpdate.class);
}
