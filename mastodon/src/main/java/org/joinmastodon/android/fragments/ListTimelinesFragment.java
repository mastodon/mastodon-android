package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.lists.GetLists;
import org.joinmastodon.android.fragments.ScrollableToTop;
import org.joinmastodon.android.model.ListTimeline;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class ListTimelinesFragment extends BaseRecyclerFragment<ListTimeline> implements ScrollableToTop {
    private String accountId;

    public ListTimelinesFragment() {
        super(10);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountId=getArguments().getString("account");
    }

    @Override
    protected void doLoadData(int offset, int count){
        currentRequest=new GetLists()
                .setCallback(new SimpleCallback<>(this) {
                    @Override
                    public void onSuccess(List<ListTimeline> result) {
                        onDataLoaded(result, false);
                    }
                })
                .exec(accountId);
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        return new ListsAdapter();
    }

    @Override
    public void scrollToTop() {
        smoothScrollRecyclerViewToTop(list);
    }

    private class ListsAdapter extends RecyclerView.Adapter<ListViewHolder>{
        @NonNull
        @Override
        public ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            return new ListViewHolder();
        }

        @Override
        public void onBindViewHolder(@NonNull ListViewHolder holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class ListViewHolder extends BindableViewHolder<ListTimeline> implements UsableRecyclerView.Clickable{
        private final TextView title;

        public ListViewHolder(){
            super(getActivity(), R.layout.item_list_timeline, list);
            title=findViewById(R.id.title);
        }

        @Override
        public void onBind(ListTimeline item) {
            title.setText(item.title);
        }

        @Override
        public void onClick() {
            UiUtils.openListTimeline(getActivity(), accountId, item);
        }
    }
}
