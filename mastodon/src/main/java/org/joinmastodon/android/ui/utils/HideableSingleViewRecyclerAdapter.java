package org.joinmastodon.android.ui.utils;

import android.view.View;

import me.grishka.appkit.utils.SingleViewRecyclerAdapter;

public class HideableSingleViewRecyclerAdapter extends SingleViewRecyclerAdapter {
    private boolean visible = true;

    public HideableSingleViewRecyclerAdapter(View view) {
        super(view);
    }

    @Override
    public int getItemCount() {
        return visible ? 1 : 0;
    }

    public void setVisible(boolean visible) {
        if (visible == this.visible)
            return;
        this.visible = visible;
        if (visible)
            notifyItemInserted(0);
        else
            notifyItemRemoved(0);
    }

    public boolean isVisible() {
        return visible;
    }
}
