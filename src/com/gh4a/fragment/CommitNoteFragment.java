package com.gh4a.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.EditCommitCommentActivity;
import com.gh4a.adapter.CommitNoteAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.LoaderResult;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;
import java.util.List;

public class CommitNoteFragment extends ListDataBaseFragment<CommitComment> implements
        CommitNoteAdapter.OnCommentAction, CommentBoxFragment.Callback {
    public static CommitNoteFragment newInstance(String repoOwner, String repoName, String commitSha) {
        CommitNoteFragment f = new CommitNoteFragment();

        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putString("sha", commitSha);
        f.setArguments(args);
        return f;
    }

    private static final int REQUEST_EDIT = 1000;

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;

    private CommitNoteAdapter mAdapter;
    private CommentBoxFragment mCommentFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString("owner");
        mRepoName = getArguments().getString("repo");
        mObjectSha = getArguments().getString("sha");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View listContent = super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.commit_comment_list, container, false);

        FrameLayout listContainer = (FrameLayout) v.findViewById(R.id.list_container);
        listContainer.addView(listContent);

        if (!Gh4Application.get().isAuthorized()) {
            v.findViewById(R.id.comment_box).setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAdapter != null) {
            mAdapter.destroy();
            mAdapter = null;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fm = getChildFragmentManager();
        mCommentFragment = (CommentBoxFragment) fm.findFragmentById(R.id.comment_box);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.pause();
    }

    @Override
    public boolean canChildScrollUp() {
        if (mCommentFragment != null && mCommentFragment.canChildScrollUp()) {
            return true;
        }
        return super.canChildScrollUp();
    }

    @Override
    protected RootAdapter<CommitComment, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new CommitNoteAdapter(getActivity(), mRepoOwner, this);
        return mAdapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_comments_found;
    }

    @Override
    public void onItemClick(CommitComment comment) {
    }

    @Override
    public Loader<LoaderResult<List<CommitComment>>> onCreateLoader() {
        return new CommitCommentListLoader(getActivity(), mRepoOwner, mRepoName,
                mObjectSha, true, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == Activity.RESULT_OK) {
                onRefresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void editComment(CommitComment comment) {
        Intent intent = EditCommitCommentActivity.makeIntent(getActivity(),
                mRepoOwner, mRepoName, comment);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    public void quoteText(CharSequence text) {
        mCommentFragment.addQuote(text);
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.commit_comment_hint;
    }

    @Override
    public void onSendCommentInBackground(String comment) throws IOException {
        CommitService commitService = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        CommitComment commitComment = new CommitComment();
        commitComment.setBody(comment);
        commitService.addComment(new RepositoryId(mRepoOwner, mRepoName), mObjectSha, commitComment);
    }

    @Override
    public void onCommentSent() {
        if (isAdded()) {
            onRefresh();
        }
    }
}
