/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package andrompd.org.andrompd.application.listviewitems;


import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.lang.ref.WeakReference;

import andrompd.org.andrompd.application.utils.AsyncLoader;

public abstract class GenericGridItem extends RelativeLayout {

    protected final AsyncLoader.CoverViewHolder mHolder;
    protected final ImageView mImageView;
    protected final TextView mTitleView;
    protected final ViewSwitcher mSwitcher;

    protected boolean mCoverDone = false;

    public GenericGridItem(Context context, String imageURL, ViewGroup.LayoutParams layoutParams) {
        super(context);

        setLayoutParams(layoutParams);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(provideLayout(), this, true);
        setLayoutParams(layoutParams);

        mImageView = provideImageView();
        mTitleView = provideTitleView();

        mSwitcher = provideViewSwitcher();

        mHolder = new AsyncLoader.CoverViewHolder();
        mHolder.coverViewReference = new WeakReference<>(provideImageView());
        mHolder.coverViewSwitcher = new WeakReference<>(provideViewSwitcher());
        mHolder.imageDimension = new Pair<>(mImageView.getWidth(),mImageView.getHeight());

        mCoverDone = false;
        mHolder.imagePath = imageURL;
        mSwitcher.setOutAnimation(null);
        mSwitcher.setInAnimation(null);
        mImageView.setImageDrawable(null);
        mSwitcher.setDisplayedChild(0);
        mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
        mSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
    }

    /* Methods needed to provide generic imageview, generic and textview
    viewswitcher and layout to inflate.
     */
    abstract ImageView provideImageView();

    abstract TextView provideTitleView();

    abstract ViewSwitcher provideViewSwitcher();

    abstract int provideLayout();

    /*
    * Sets the title for the GridItem
     */
    public void setTitle(String text) {
        mTitleView.setText(text);
    }

    /*
    * Starts the image retrieval task
    */
    public void startCoverImageTask() {
        if (mHolder.imagePath != null && mHolder.task == null && !mCoverDone) {
            mCoverDone = true;
            mHolder.task = new AsyncLoader();
            mHolder.task.execute(mHolder);
        }
    }


    /*
    * Sets the new image url for this particular gridItem. If already an image
    * getter task is running it will be cancelled. The image is reset to the
    * dummy picture.
    */
    public void setImageURL(String url) {
        // Check if image url has actually changed, otherwise there is no need to redo the image.
        if ( (mHolder.imagePath == null) ||  (!mHolder.imagePath.equals(url) ) ) {
            // Cancel old task
            if (mHolder.task != null) {
                mHolder.task.cancel(true);
                mHolder.task = null;
            }

            mCoverDone = false;
            mHolder.imagePath = url;
            mSwitcher.setOutAnimation(null);
            mSwitcher.setInAnimation(null);
            mImageView.setImageDrawable(null);
            mSwitcher.setDisplayedChild(0);
            mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
            mSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        }
    }

    /*
    * If this GridItem gets detached from the parent it makes no sense to let
    * the task for image retrieval runnig. (non-Javadoc)
    *
    * @see android.view.View#onDetachedFromWindow()
    */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mHolder.task != null) {
            mHolder.task.cancel(true);
            mHolder.task = null;
        }
    }
}
