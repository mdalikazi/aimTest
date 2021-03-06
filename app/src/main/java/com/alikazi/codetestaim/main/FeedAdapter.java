package com.alikazi.codetestaim.main;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.alikazi.codetestaim.R;
import com.alikazi.codetestaim.models.PlayoutItem;
import com.alikazi.codetestaim.utils.AimViewUtils;
import com.alikazi.codetestaim.utils.AppConstants;
import com.alikazi.codetestaim.utils.DLog;
import com.alikazi.codetestaim.utils.NetConstants;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import eu.gsottbauer.equalizerview.EqualizerView;

public class FeedAdapter extends ListAdapter<PlayoutItem, FeedAdapter.ItemViewHolder> {

    private static final String LOG_TAG = AppConstants.AIM_LOG_TAG;

    private int mNowPlayingPosition = -1;
    private boolean mIsPlaying;
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private MediaPlayer mMediaPlayer;
    private ItemSelectionListener mItemSelectionListener;

    public FeedAdapter(Context context, ItemSelectionListener itemSelectionListener) {
        super(ITEM_COMPARATOR);
        mContext = context;
        mItemSelectionListener = itemSelectionListener;
        mLayoutInflater = LayoutInflater.from(mContext);
        mMediaPlayer = new MediaPlayer();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.item_playout, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemViewHolder holder, final int position) {
        final PlayoutItem item = getItem(position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemSelectionListener != null) {
                    mItemSelectionListener.onItemSelected(item);
                }
            }
        });
        AimViewUtils.showImageWithGlide(mContext, item.imageUrl, holder.heroImageView);
        holder.titleTextView.setText(item.title);
        holder.artistTextView.setText(item.artist);
        holder.albumTextView.setText(item.album);
        if (item.customFields != null) {
            final PlayoutItem.CustomField customField = item.customFields.get(0);
            if (customField.name.equalsIgnoreCase(NetConstants.CUSTOM_FIELDS_KEY_ITUNES_BUY)) {
                holder.cartImageView.setVisibility(View.VISIBLE);
                holder.cartImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(mContext, R.string.toast_message_itunes, Toast.LENGTH_LONG).show();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(customField.value));
                        mContext.startActivity(browserIntent);
                    }
                });
            }
        } else {
            holder.cartImageView.setVisibility(View.GONE);
        }

        View.OnClickListener playPreviewClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (position == mNowPlayingPosition &&
                        mMediaPlayer != null &&
                        mMediaPlayer.isPlaying()) {
                    DLog.i(LOG_TAG, "NowPlaying return");
                    // Ignore multiple taps by user for the same preview if it is already playing
                    return;
                } else {
                    // Stop currently playing preview and prepare new one
                    resetMediaPlayer();
                }
                mNowPlayingPosition = position;
                Animation rotation = AnimationUtils.loadAnimation(mContext, R.anim.rotation);
                rotation.setInterpolator(new AccelerateDecelerateInterpolator());
                rotation.setFillAfter(true);
                rotation.setDuration(streamPreview(item.previewUrl));
                rotation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        holder.equalizerView.animate().alpha(0.4f).setDuration(1000);
                        holder.equalizerView.animateBars();
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        holder.equalizerView.animate().alpha(0f).setDuration(1000);
                        holder.equalizerView.stopBars();
                        holder.viewFlipper.showNext();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                holder.heroImageView.startAnimation(rotation);
                holder.viewFlipper.showNext();
                item.isPlaying = true;
                mIsPlaying = true;
            }
        };

        if (item.previewUrl != null && !item.previewUrl.isEmpty()) {
            holder.heroImageView.setOnClickListener(playPreviewClickListener);
            holder.playButton.setOnClickListener(playPreviewClickListener);
        } else {
            holder.viewFlipper.setVisibility(View.GONE);
        }

        holder.pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetMediaPlayer();
                holder.viewFlipper.showNext();
                holder.heroImageView.clearAnimation();
                holder.equalizerView.stopBars();
                item.isPlaying = false;
                mIsPlaying = false;
            }
        });
    }

    private int streamPreview(String url) {
        try {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepare(); // might take long! (for buffering, etc)
            mMediaPlayer.start();
            return mMediaPlayer.getDuration();
        } catch (Exception e) {
            DLog.d(LOG_TAG, "Exception streaming preview: " + e.toString());
            Toast.makeText(mContext, R.string.toast_message_streaming_error, Toast.LENGTH_LONG).show();
        }

        return 0;
    }

    public void resetMediaPlayer() {
        if (mMediaPlayer != null) {
            DLog.d(LOG_TAG, "Stop");
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = new MediaPlayer();
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ItemViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (mIsPlaying) {
            holder.setIsRecyclable(false);
        } else {
            holder.setIsRecyclable(true);
        }
    }

    protected class ItemViewHolder extends RecyclerView.ViewHolder {

        private ImageView heroImageView;
        private TextView titleTextView;
        private TextView artistTextView;
        private TextView albumTextView;
        private ImageView cartImageView;
        private ViewFlipper viewFlipper;
        private ImageView playButton;
        private ImageView pauseButton;
        private EqualizerView equalizerView;

        private ItemViewHolder(View view) {
            super(view);
            heroImageView = view.findViewById(R.id.hero_item_image);
            titleTextView = view.findViewById(R.id.item_title);
            artistTextView = view.findViewById(R.id.item_artist);
            albumTextView = view.findViewById(R.id.item_album);
            cartImageView = view.findViewById(R.id.item_cart);
            playButton = view.findViewById(R.id.item_play);
            pauseButton = view.findViewById(R.id.item_stop);
            viewFlipper = view.findViewById(R.id.item_play_pause_view_flipper);
            equalizerView = view.findViewById(R.id.equalizer);
        }
    }

    private static final DiffUtil.ItemCallback<PlayoutItem> ITEM_COMPARATOR =
            new DiffUtil.ItemCallback<PlayoutItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull PlayoutItem oldItem, @NonNull PlayoutItem newItem) {
                    return oldItem.id.equalsIgnoreCase(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull PlayoutItem oldItem, @NonNull PlayoutItem newItem) {
                    return oldItem == newItem;
                }
            };

    public interface ItemSelectionListener {
        void onItemSelected(PlayoutItem item);
    }
}
