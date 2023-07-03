package org.joinmastodon.android.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetStatusContext;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.LegacyFilter;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusContext;
import org.joinmastodon.android.ui.displayitems.ExtendedFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.RecyclerView;

import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class ThreadFragment extends StatusListFragment {
    private Status mainStatus;
    private ImageView endMark;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainStatus = Parcels.unwrap(getArguments().getParcelable("status"));
        Account inReplyToAccount = Parcels.unwrap(getArguments().getParcelable("inReplyToAccount"));
        if (inReplyToAccount != null)
            knownAccounts.put(inReplyToAccount.id, inReplyToAccount);
        data.add(mainStatus);
        onAppendItems(Collections.singletonList(mainStatus));
        if (AccountSessionManager.get(accountID).getLocalPreferences().customEmojiInNames)
            setTitle(HtmlParser.parseCustomEmoji(getString(R.string.post_from_user, mainStatus.account.displayName), mainStatus.account.emojis));
        else
            setTitle(getString(R.string.post_from_user, mainStatus.account.displayName));
    }

    @Override
    protected List<StatusDisplayItem> buildDisplayItems(Status s) {
        List<StatusDisplayItem> items = super.buildDisplayItems(s);
        if (s.id.equals(mainStatus.id)) {
            for (StatusDisplayItem item : items) {
                if (item instanceof TextStatusDisplayItem text)
                    text.textSelectable = true;
                else if (item instanceof FooterStatusDisplayItem footer)
                    footer.hideCounts = true;
            }
            items.add(new ExtendedFooterStatusDisplayItem(s.id, this, s.getContentStatus()));
        }
        return items;
    }

    @Override
    protected void doLoadData(int offset, int count) {
        currentRequest = new GetStatusContext(mainStatus.id)
                .setCallback(new SimpleCallback<>(this) {
                    @Override
                    public void onSuccess(StatusContext result) {
                        if (getActivity() == null)
                            return;
                        if (refreshing) {
                            data.clear();
                            displayItems.clear();
                            data.add(mainStatus);
                            onAppendItems(Collections.singletonList(mainStatus));
                        }
                        filterStatuses(result.descendants);
                        filterStatuses(result.ancestors);
                        if (footerProgress != null)
                            footerProgress.setVisibility(View.GONE);
                        data.addAll(result.descendants);
                        int prevCount = displayItems.size();
                        onAppendItems(result.descendants);
                        int count = displayItems.size();
                        if (!refreshing)
                            adapter.notifyItemRangeInserted(prevCount, count - prevCount);
                        prependItems(result.ancestors, !refreshing);
                        dataLoaded();
                        if (refreshing) {
                            refreshDone();
                            adapter.notifyDataSetChanged();
                        }
                        list.scrollToPosition(displayItems.size() - count);
                    }
                })
                .exec(accountID);
    }

    private void filterStatuses(List<Status> statuses) {
        AccountSessionManager.get(accountID).filterStatuses(statuses, FilterContext.THREAD);
    }

    @Override
    protected void onShown() {
        super.onShown();
        if (!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading) {
            dataLoading = true;
            doLoadData();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UiUtils.loadCustomEmojiInTextView(toolbarTitleView);
        showContent();
        if (!loaded)
            footerProgress.setVisibility(View.VISIBLE);
    }

    protected void onStatusCreated(Status status) {
        if (status.inReplyToId != null && getStatusByID(status.inReplyToId) != null) {
            onAppendItems(Collections.singletonList(status));
            data.add(status);
        }
    }

    @Override
    public boolean isItemEnabled(String id) {
        return !id.equals(mainStatus.id);
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        MergeRecyclerAdapter a = new MergeRecyclerAdapter();
        a.addAdapter(super.getAdapter());

        endMark = new ImageView(getActivity());
        endMark.setScaleType(ImageView.ScaleType.CENTER);
        endMark.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant)));
        endMark.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(25)));
        endMark.setImageResource(R.drawable.thread_end_mark);
        a.addAdapter(new SingleViewRecyclerAdapter(endMark));

        return a;
    }

    @Override
    protected boolean needDividerForExtraItem(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder) {
        return bottomSibling == endMark;
    }

    @Override
    protected void onErrorRetryClick() {
        if (preloadingFailed) {
            preloadingFailed = false;
            V.setVisibilityAnimated(footerProgress, View.VISIBLE);
            V.setVisibilityAnimated(footerError, View.GONE);
            doLoadData();
            return;
        }
        super.onErrorRetryClick();
    }
}
