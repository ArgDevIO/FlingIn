package argdev.io.flingin.java.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

import argdev.io.flingin.R;
import argdev.io.flingin.java.models.Message;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> mMessageList;
    private LinearLayoutManager mLinearLayout;
    private RecyclerView mRecyclerView;
    private FirebaseAuth mAuth;
    private Context ctx;

    public MessageAdapter(List<Message> mMessageList, Context ctx, LinearLayoutManager mLinearLayout, RecyclerView mRecyclerView) {
        mAuth = FirebaseAuth.getInstance();
        this.mMessageList = mMessageList;
        this.ctx = ctx;
        this.mLinearLayout = mLinearLayout;
        this.mRecyclerView = mRecyclerView;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_single_layout, parent, false);

        return new MessageViewHolder(v);
    }


    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {

        String current_user_id = mAuth.getCurrentUser().getUid();
        Message c = mMessageList.get(position);

        final String imageURL = c.getMessage();

        String from_user = c.getFrom();
        String msg_type = c.getType();
        String timeFormatted = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(c.getTime()));

        final Drawable placeholder;

        if (from_user.equals(current_user_id)) {
            placeholder = ctx.getDrawable(R.drawable.user_image_placeholder);

            holder.mOutGoingPoint.setVisibility(View.VISIBLE);
            holder.mIncomingPoint.setVisibility(View.INVISIBLE);

            holder.mMessageWrapper.setBackgroundResource(R.drawable.message_text_user_background);

            if (msg_type.equals("text")) {
                holder.mImageMessage.setVisibility(View.GONE);
                holder.messageText.setVisibility(View.VISIBLE);

                holder.messageText.setTextColor(ctx.getResources().getColor(R.color.gunmetal5));
                holder.messageText.post(new Runnable() {
                    @Override
                    public void run() {
                        if (holder.messageText.getLineCount() > 1) {
                            holder.messageText.setPadding(20, 9, 20, 50);
                        } else {
                            holder.messageText.setPadding(20, 9, 130, 15);
                        }
                    }
                });
            } else if (msg_type.equals("image")) {
                holder.mImageMessage.setVisibility(View.VISIBLE);
                holder.messageText.setVisibility(View.GONE);
            }

            holder.timeText.setTextColor(ctx.getResources().getColor(R.color.purpleHeavy1));
            holder.timeText.setPadding(0, 0, 0, 0);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.mMessageWrapper.getLayoutParams();
            params.setMarginEnd(20);
            holder.mMessageWrapper.setLayoutParams(params);

            holder.mMessageContainer.setGravity(Gravity.END);

        } else {
            placeholder = ctx.getDrawable(R.drawable.guest_image_placeholder);

            holder.mOutGoingPoint.setVisibility(View.INVISIBLE);
            holder.mIncomingPoint.setVisibility(View.VISIBLE);

            holder.mMessageWrapper.setBackgroundResource(R.drawable.message_text_guest_background);

            if (msg_type.equals("text")) {
                holder.mImageMessage.setVisibility(View.GONE);
                holder.messageText.setVisibility(View.VISIBLE);

                holder.messageText.setTextColor(ctx.getResources().getColor(R.color.lightCyan3));
                holder.messageText.post(new Runnable() {
                    @Override
                    public void run() {
                        if (holder.messageText.getLineCount() > 1) {
                            holder.messageText.setPadding(20, 9, 20, 50);
                        } else {
                            holder.messageText.setPadding(20, 9, 130, 15);
                        }
                    }
                });
            } else if (msg_type.equals("image")) {
                holder.mImageMessage.setVisibility(View.VISIBLE);
                holder.messageText.setVisibility(View.GONE);
            }

            holder.timeText.setTextColor(ctx.getResources().getColor(R.color.paleCerulean2));
            holder.timeText.setPadding(0, 0, 0, 0);

            holder.mMessageContainer.setGravity(Gravity.START);
        }

        if (msg_type.equals("text"))
            holder.messageText.setText(c.getMessage());
        else if (msg_type.equals("image")) {
            holder.mProgressBar.setVisibility(View.VISIBLE);
            final ProgressBar progressBar = holder.mProgressBar;
            Picasso.get().load(imageURL).networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(placeholder)
                    .into(holder.mImageMessage, new Callback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            Log.d("PICASSO", "onSuccess: Loaded");
                            //mRecyclerView.smoothScrollToPosition(mMessageList.size() - 1);
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mRecyclerView.smoothScrollToPosition(mRecyclerView.getAdapter().getItemCount());
                                }
                            }, 500);
                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(imageURL).placeholder(placeholder)
                                    .into(holder.mImageMessage, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            progressBar.setVisibility(View.GONE);
                                            Log.d("PICASSO", "onSuccess: Loaded");
                                            //mRecyclerView.smoothScrollToPosition(mMessageList.size() - 1);
                                            Handler handler = new Handler();
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mRecyclerView.smoothScrollToPosition(mRecyclerView.getAdapter().getItemCount());
                                                }
                                            }, 500);
                                        }

                                        @Override
                                        public void onError(Exception e) {

                                        }
                                    });
                        }
                    });
        }

        holder.timeText.setText(timeFormatted);
    }


    public class MessageViewHolder extends RecyclerView.ViewHolder {

        public RelativeLayout mMessageContainer;
        public RelativeLayout mMessageWrapper;
        public TextView messageText;
        public TextView timeText;
        public ImageView mIncomingPoint;
        public ImageView mOutGoingPoint;
        public ImageView mImageMessage;
        public ProgressBar mProgressBar;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            mMessageContainer = itemView.findViewById(R.id.message_single_layout);
            mMessageWrapper = itemView.findViewById(R.id.message_single_msg_wrapper);
            messageText = itemView.findViewById(R.id.message_single_text_layout);
            timeText = itemView.findViewById(R.id.message_single_time_text);
            mIncomingPoint = itemView.findViewById(R.id.message_single_point_incoming);
            mOutGoingPoint = itemView.findViewById(R.id.message_single_point_outgoing);
            mImageMessage = itemView.findViewById(R.id.message_single_image);
            mProgressBar = itemView.findViewById(R.id.message_single_image_loading);
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

}
