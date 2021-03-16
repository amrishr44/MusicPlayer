package com.example.musicplayer.nowplaying;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;

import java.io.InputStream;

public class AlbumArtViewPagerAdapter extends RecyclerView.Adapter<AlbumArtViewPagerAdapter.AlbumArtViewHolder> {


    private final Context context;

    public AlbumArtViewPagerAdapter(Context context) {
        this.context = context;
    }

    public  class AlbumArtViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private final ImageView now_playing_album_art;
        private final LinearLayout volume_bar;
        private LinearLayout speed_bar;

        public AlbumArtViewHolder(@NonNull View itemView) {
            super(itemView);

            now_playing_album_art = itemView.findViewById(R.id.now_playing_album_art);
            volume_bar = ((AppCompatActivity)context).findViewById(R.id.volume_bar);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                speed_bar = ((AppCompatActivity) context).findViewById(R.id.speed_bar);
            }
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            if (volume_bar.getVisibility() == View.VISIBLE){
                volume_bar.setVisibility(View.GONE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    speed_bar.setVisibility(View.GONE);
                }
            }
            else{
                volume_bar.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    speed_bar.setVisibility(View.VISIBLE);
                }
            }
        }


    }

    @NonNull
    @Override
    public AlbumArtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AlbumArtViewHolder(LayoutInflater.from(context).inflate(R.layout.vp_album_art, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final AlbumArtViewHolder holder, final int position) {

        try {
            InputStream is = context.getContentResolver().openInputStream(Uri.parse("content://media/external/audio/albumart/" +  MediaPlayerService.audioList.get(position).getAlbumid()));
            is.close();
            if (MediaPlayerService.audioList.get(position).getAlbumid() != -100) {
                holder.now_playing_album_art.setImageURI(Uri.parse("content://media/external/audio/albumart/" + MediaPlayerService.audioList.get(position).getAlbumid()));
            }
            else{
                MediaMetadataRetriever m = new MediaMetadataRetriever();
                m.setDataSource(MediaPlayerService.audioList.get(position).getData());
                byte [] art = m.getEmbeddedPicture();
                Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
                holder.now_playing_album_art.setImageBitmap(songImage);
            }

        } catch (Exception e) {
            if (MediaPlayerService.audioList.get(position).getAlbumid() == -100) {
                MediaMetadataRetriever m = new MediaMetadataRetriever();
                m.setDataSource(MediaPlayerService.audioList.get(position).getData());
                byte [] art = m.getEmbeddedPicture();
                Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
                holder.now_playing_album_art.setImageBitmap(songImage);
            }
            else holder.now_playing_album_art.setImageResource(R.mipmap.cassette_image_foreground);
        }
    }

    @Override
    public int getItemCount() {
        return MediaPlayerService.audioList.size();
    }

    @Override
    public void onViewRecycled(@NonNull AlbumArtViewHolder holder) {
        super.onViewRecycled(holder);
        holder.now_playing_album_art.setImageDrawable(null);
    }
}
